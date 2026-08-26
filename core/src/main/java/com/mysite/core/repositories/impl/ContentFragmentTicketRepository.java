package com.mysite.core.repositories.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.mysite.core.dto.TicketDTO;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.mappers.TicketMapper;
import com.mysite.core.repositories.TicketRepository;
import com.mysite.core.util.TimeUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Content Fragment adapter for {@link TicketRepository}.
 * <p>
 * Reads and writes ticket data from Content Fragments under {@link #TICKETS_PATH}.
 * </p>
 */
@Component(service = TicketRepository.class, property = "impl.type=contentfragment")
public class ContentFragmentTicketRepository implements TicketRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentTicketRepository.class);

    static final String SERVICE_SUBSERVICE = "assessment-service";
    static final String TICKETS_PATH = "/content/dam/assessment/tickets";
    static final String TICKET_MODEL_PATH = "/conf/assessment/settings/dam/cfm/models/ticket";
    static final String VAR_ASSESSMENT_PATH = "/var/assessment";
    static final String TICKET_ID_COUNTER_PATH = "/var/assessment/ticket-id-counter";

    private static final String TICKET_ID_PREFIX = "TKT-";
    private static final long INITIAL_COUNTER_VALUE = 1000L;
    private static final String COUNTER_PROPERTY = "counter";
    private static final String STATUS_OPEN = "Open";
    private static final String MIME_TEXT_PLAIN = "text/plain";

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Returns all tickets ordered by {@code createdAt} descending (newest first).
     *
     * @return non-null list of tickets; empty when none exist or the tickets folder is missing
     */
    @Override
    public List<TicketDTO> getAll() {
        try (ResourceResolver resolver = obtainServiceResolver()) {
            Resource ticketsFolder = resolver.getResource(TICKETS_PATH);
            if (ticketsFolder == null) {
                return Collections.emptyList();
            }

            List<TicketDTO> tickets = new ArrayList<>();
            for (Resource child : ticketsFolder.getChildren()) {
                ContentFragment contentFragment = child.adaptTo(ContentFragment.class);
                if (contentFragment == null) {
                    continue;
                }
                tickets.add(TicketMapper.toDto(contentFragment));
            }

            tickets.sort((left, right) -> compareCreatedAtDescending(left, right));
            return tickets;
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resource resolver for subservice {}", SERVICE_SUBSERVICE, e);
            return Collections.emptyList();
        }
    }

    /**
     * Loads a single ticket by business id (CF element {@code ticketId}).
     *
     * @param id ticket id (e.g. {@code TKT-1001})
     * @return the ticket if found; empty when not found or the tickets folder is missing
     */
    @Override
    public Optional<TicketDTO> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        try (ResourceResolver resolver = obtainServiceResolver()) {
            return findContentFragmentByTicketId(resolver, id).map(TicketMapper::toDto);
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resource resolver for subservice {}", SERVICE_SUBSERVICE, e);
            return Optional.empty();
        }
    }

    /**
     * Returns tickets matching the given lifecycle status.
     *
     * @param status ticket status value
     * @return non-null list of matching tickets; empty when none match
     */
    @Override
    public List<TicketDTO> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            return Collections.emptyList();
        }

        List<TicketDTO> matches = new ArrayList<>();
        for (TicketDTO ticket : getAll()) {
            if (status.equals(ticket.getStatus())) {
                matches.add(ticket);
            }
        }
        return matches;
    }

    /**
     * Case-insensitive partial match on ticket title.
     *
     * @param query search term; blank returns all tickets (match-all)
     * @return non-null list of matching tickets; empty when none match
     */
    @Override
    public List<TicketDTO> searchByTitle(String query) {
        List<TicketDTO> allTickets = getAll();
        if (query == null || query.isBlank()) {
            return allTickets;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        List<TicketDTO> matches = new ArrayList<>();
        for (TicketDTO ticket : allTickets) {
            String title = ticket.getTitle();
            if (title != null && title.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                matches.add(ticket);
            }
        }
        return matches;
    }

    /**
     * Persists a new ticket Content Fragment with a generated {@code TKT-{n}} id and
     * {@code Open} status.
     *
     * @param ticket ticket data to persist (id and status on input are ignored)
     * @return the persisted ticket including generated id and timestamps
     */
    @Override
    public TicketDTO create(TicketDTO ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("ticket must not be null");
        }

        try (ResourceResolver resolver = obtainServiceResolver()) {
            String ticketId = generateTicketId(resolver);

            Resource modelResource = resolver.getResource(TICKET_MODEL_PATH);
            FragmentTemplate fragmentTemplate =
                    modelResource != null ? modelResource.adaptTo(FragmentTemplate.class) : null;
            Resource ticketsFolder = resolver.getResource(TICKETS_PATH);

            if (fragmentTemplate == null || ticketsFolder == null) {
                throw new IllegalStateException(
                        "Ticket CF model or tickets folder not found at " + TICKET_MODEL_PATH
                                + " / " + TICKETS_PATH);
            }

            Instant now = Instant.now();
            String nowText = TimeUtil.formatInstant(now);
            String fragmentTitle = ticket.getTitle() != null ? ticket.getTitle() : ticketId;

            ContentFragment contentFragment =
                    fragmentTemplate.createFragment(ticketsFolder, ticketId, fragmentTitle);

            setElementContent(contentFragment, TicketMapper.ELEMENT_TICKET_ID, ticketId);
            setElementContent(contentFragment, TicketMapper.ELEMENT_TITLE, ticket.getTitle());
            setElementContent(contentFragment, TicketMapper.ELEMENT_DESCRIPTION, ticket.getDescription());
            setElementContent(contentFragment, TicketMapper.ELEMENT_PRIORITY, ticket.getPriority());
            setElementContent(contentFragment, TicketMapper.ELEMENT_STATUS, STATUS_OPEN);
            setElementContent(contentFragment, TicketMapper.ELEMENT_ASSIGNED_TO, nullToBlank(ticket.getAssignedTo()));
            setElementContent(contentFragment, TicketMapper.ELEMENT_CREATED_BY, ticket.getCreatedBy());
            setElementContent(contentFragment, TicketMapper.ELEMENT_CREATED_AT, nowText);
            setElementContent(contentFragment, TicketMapper.ELEMENT_UPDATED_AT, nowText);

            resolver.commit();
            return TicketMapper.toDto(contentFragment);
        } catch (LoginException | ContentFragmentException | PersistenceException e) {
            LOG.error("Failed to create ticket", e);
            throw new IllegalStateException("Failed to create ticket", e);
        }
    }

    /**
     * Updates mutable ticket fields on an existing Content Fragment.
     *
     * @param ticket ticket with id and mutable fields to persist
     * @return the updated ticket
     * @throws TicketNotFoundException when no ticket exists for {@code ticket.getId()}
     */
    @Override
    public TicketDTO update(TicketDTO ticket) {
        if (ticket == null || ticket.getId() == null || ticket.getId().isBlank()) {
            throw new IllegalArgumentException("ticket id is required for update");
        }

        String ticketId = ticket.getId();

        try (ResourceResolver resolver = obtainServiceResolver()) {
            ContentFragment contentFragment = findContentFragmentByTicketId(resolver, ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));

            Instant now = Instant.now();
            setElementContent(contentFragment, TicketMapper.ELEMENT_TITLE, ticket.getTitle());
            setElementContent(contentFragment, TicketMapper.ELEMENT_DESCRIPTION, ticket.getDescription());
            setElementContent(contentFragment, TicketMapper.ELEMENT_PRIORITY, ticket.getPriority());
            setElementContent(contentFragment, TicketMapper.ELEMENT_STATUS, ticket.getStatus());
            setElementContent(contentFragment, TicketMapper.ELEMENT_ASSIGNED_TO, nullToBlank(ticket.getAssignedTo()));
            setElementContent(contentFragment, TicketMapper.ELEMENT_UPDATED_AT, TimeUtil.formatInstant(now));

            resolver.commit();
            return TicketMapper.toDto(contentFragment);
        } catch (LoginException | ContentFragmentException | PersistenceException e) {
            LOG.error("Failed to update ticket {}", ticketId, e);
            throw new IllegalStateException("Failed to update ticket " + ticketId, e);
        }
    }

    private ResourceResolver obtainServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Map.of(
                ResourceResolverFactory.SUBSERVICE,
                SERVICE_SUBSERVICE);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    /**
     * Generates the next ticket id ({@code TKT-{n}}) using a persistent counter node.
     * <p>
     * The counter at {@link #TICKET_ID_COUNTER_PATH} is read and incremented on each call.
     * When the counter node is absent it is created with value {@link #INITIAL_COUNTER_VALUE},
     * so the first generated id is {@code TKT-1001}.
     * </p>
     * <p>
     * <strong>Concurrency:</strong> this increment is not atomic under concurrent creates and is
     * acceptable for this learning scope. A production implementation should use Oak counter
     * nodes, explicit locking, or a database sequence.
     * </p>
     *
     * @param resolver active service resource resolver
     * @return generated ticket id (e.g. {@code TKT-1001})
     * @throws PersistenceException when the counter node cannot be read or updated
     */
    private String generateTicketId(ResourceResolver resolver) throws PersistenceException {
        ensureVarAssessmentPath(resolver);

        Resource counterResource = resolver.getResource(TICKET_ID_COUNTER_PATH);
        if (counterResource == null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("jcr:primaryType", "nt:unstructured");
            properties.put(COUNTER_PROPERTY, INITIAL_COUNTER_VALUE);

            Resource parent = resolver.getResource(VAR_ASSESSMENT_PATH);
            counterResource = resolver.create(parent, "ticket-id-counter", properties);
        }

        ModifiableValueMap properties = counterResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new PersistenceException("Counter node is not modifiable at " + TICKET_ID_COUNTER_PATH);
        }

        long current = properties.get(COUNTER_PROPERTY, INITIAL_COUNTER_VALUE);
        long next = current + 1;
        properties.put(COUNTER_PROPERTY, next);
        resolver.commit();

        return TICKET_ID_PREFIX + next;
    }

    private void ensureVarAssessmentPath(ResourceResolver resolver) throws PersistenceException {
        if (resolver.getResource(VAR_ASSESSMENT_PATH) != null) {
            return;
        }

        Resource varRoot = resolver.getResource("/var");
        if (varRoot == null) {
            throw new IllegalStateException("/var path not found");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "nt:unstructured");
        resolver.create(varRoot, "assessment", properties);
        resolver.commit();
    }

    private Optional<ContentFragment> findContentFragmentByTicketId(ResourceResolver resolver, String id) {
        Resource ticketsFolder = resolver.getResource(TICKETS_PATH);
        if (ticketsFolder == null) {
            return Optional.empty();
        }

        for (Resource child : ticketsFolder.getChildren()) {
            ContentFragment contentFragment = child.adaptTo(ContentFragment.class);
            if (contentFragment == null) {
                continue;
            }

            String ticketId = TicketMapper.readTicketId(contentFragment);
            if (id.equals(ticketId)) {
                return Optional.of(contentFragment);
            }
        }

        return Optional.empty();
    }

    private void setElementContent(ContentFragment contentFragment, String elementName, String value)
            throws ContentFragmentException {
        ContentElement element = contentFragment.getElement(elementName);
        if (element == null) {
            LOG.warn("Content element '{}' not found on ticket fragment", elementName);
            return;
        }
        element.setContent(value != null ? value : "", MIME_TEXT_PLAIN);
    }

    private String nullToBlank(String value) {
        return value != null ? value : "";
    }

    private int compareCreatedAtDescending(TicketDTO left, TicketDTO right) {
        Instant leftCreatedAt = left.getCreatedAt();
        Instant rightCreatedAt = right.getCreatedAt();

        if (leftCreatedAt == null && rightCreatedAt == null) {
            return 0;
        }
        if (leftCreatedAt == null) {
            return 1;
        }
        if (rightCreatedAt == null) {
            return -1;
        }
        return rightCreatedAt.compareTo(leftCreatedAt);
    }
}
