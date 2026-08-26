package com.mysite.core.repositories.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.mysite.core.dto.CommentDTO;
import com.mysite.core.mappers.CommentMapper;
import com.mysite.core.repositories.CommentRepository;
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
import java.util.Map;

/**
 * Content Fragment adapter for {@link CommentRepository}.
 * <p>
 * Persists and reads ticket comments from Content Fragments under {@link #COMMENTS_PATH}.
 * Ticket existence is validated in the service layer — this repository only persists comments.
 * </p>
 */
@Component(service = CommentRepository.class, property = "impl.type=contentfragment")
public class ContentFragmentCommentRepository implements CommentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentCommentRepository.class);

    static final String SERVICE_SUBSERVICE = "assessment-service";
    static final String COMMENTS_PATH = "/content/dam/assessment/comments";
    static final String COMMENT_MODEL_PATH = "/conf/assessment/settings/dam/cfm/models/comment";
    static final String VAR_ASSESSMENT_PATH = "/var/assessment";
    static final String COUNTER_PATH = "/var/assessment/comment-id-counter";

    private static final String COMMENT_ID_PREFIX = "CMT-";
    private static final long INITIAL_COUNTER_VALUE = 1000L;
    private static final String COUNTER_PROPERTY = "counter";
    private static final String MIME_TEXT_PLAIN = "text/plain";

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Persists a new comment Content Fragment with a generated {@code CMT-{n}} id.
     * <p>
     * Does not validate that {@code ticketId} references an existing ticket — that is enforced
     * in the service layer (Sprint 4.1).
     * </p>
     *
     * @param comment comment data to persist (id and createdAt on input are ignored)
     * @return the persisted comment including generated id and timestamp
     */
    @Override
    public CommentDTO add(CommentDTO comment) {
        if (comment == null) {
            throw new IllegalArgumentException("comment must not be null");
        }

        try (ResourceResolver resolver = obtainServiceResolver()) {
            String commentId = generateCommentId(resolver);

            Resource modelResource = resolver.getResource(COMMENT_MODEL_PATH);
            FragmentTemplate fragmentTemplate =
                    modelResource != null ? modelResource.adaptTo(FragmentTemplate.class) : null;
            Resource commentsFolder = resolver.getResource(COMMENTS_PATH);

            if (fragmentTemplate == null || commentsFolder == null) {
                throw new IllegalStateException(
                        "Comment CF model or comments folder not found at " + COMMENT_MODEL_PATH
                                + " / " + COMMENTS_PATH);
            }

            Instant now = Instant.now();
            String nowText = TimeUtil.formatInstant(now);
            String fragmentTitle = comment.getMessage() != null ? comment.getMessage() : commentId;

            ContentFragment contentFragment =
                    fragmentTemplate.createFragment(commentsFolder, commentId, fragmentTitle);

            setElementContent(contentFragment, CommentMapper.ELEMENT_COMMENT_ID, commentId);
            setElementContent(contentFragment, CommentMapper.ELEMENT_TICKET_ID, comment.getTicketId());
            setElementContent(contentFragment, CommentMapper.ELEMENT_MESSAGE, comment.getMessage());
            setElementContent(contentFragment, CommentMapper.ELEMENT_CREATED_BY, comment.getCreatedBy());
            setElementContent(contentFragment, CommentMapper.ELEMENT_CREATED_AT, nowText);

            resolver.commit();
            return CommentMapper.toDto(contentFragment);
        } catch (LoginException | ContentFragmentException | PersistenceException e) {
            LOG.error("Failed to add comment for ticket {}", comment.getTicketId(), e);
            throw new IllegalStateException("Failed to add comment", e);
        }
    }

    /**
     * Returns all comments for the given ticket, ordered by {@code createdAt} ascending.
     *
     * @param ticketId parent ticket business id
     * @return non-null list of comments; empty when none match or the comments folder is missing
     */
    @Override
    public List<CommentDTO> listByTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            return Collections.emptyList();
        }

        try (ResourceResolver resolver = obtainServiceResolver()) {
            Resource commentsFolder = resolver.getResource(COMMENTS_PATH);
            if (commentsFolder == null) {
                return Collections.emptyList();
            }

            List<CommentDTO> comments = new ArrayList<>();
            for (Resource child : commentsFolder.getChildren()) {
                ContentFragment contentFragment = child.adaptTo(ContentFragment.class);
                if (contentFragment == null) {
                    continue;
                }

                String fragmentTicketId = CommentMapper.readTicketId(contentFragment);
                if (!ticketId.equals(fragmentTicketId)) {
                    continue;
                }

                comments.add(CommentMapper.toDto(contentFragment));
            }

            comments.sort(this::compareCreatedAtAscending);
            return comments;
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resource resolver for subservice {}", SERVICE_SUBSERVICE, e);
            return Collections.emptyList();
        }
    }

    private ResourceResolver obtainServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Map.of(
                ResourceResolverFactory.SUBSERVICE,
                SERVICE_SUBSERVICE);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    /**
     * Generates the next comment id ({@code CMT-{n}}) using a persistent counter node.
     * <p>
     * The counter at {@link #COUNTER_PATH} is read and incremented on each call. When the
     * counter node is absent it is created with value {@link #INITIAL_COUNTER_VALUE}, so the
     * first generated id is {@code CMT-1001}.
     * </p>
     * <p>
     * <strong>Concurrency:</strong> this increment is not atomic under concurrent creates and is
     * acceptable for this learning scope. A production implementation should use Oak counter
     * nodes, explicit locking, or a database sequence.
     * </p>
     *
     * @param resolver active service resource resolver
     * @return generated comment id (e.g. {@code CMT-1001})
     * @throws PersistenceException when the counter node cannot be read or updated
     */
    private String generateCommentId(ResourceResolver resolver) throws PersistenceException {
        ensureVarAssessmentPath(resolver);

        Resource counterResource = resolver.getResource(COUNTER_PATH);
        if (counterResource == null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("jcr:primaryType", "nt:unstructured");
            properties.put(COUNTER_PROPERTY, INITIAL_COUNTER_VALUE);

            Resource parent = resolver.getResource(VAR_ASSESSMENT_PATH);
            counterResource = resolver.create(parent, "comment-id-counter", properties);
        }

        ModifiableValueMap properties = counterResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new PersistenceException("Counter node is not modifiable at " + COUNTER_PATH);
        }

        long current = properties.get(COUNTER_PROPERTY, INITIAL_COUNTER_VALUE);
        long next = current + 1;
        properties.put(COUNTER_PROPERTY, next);
        resolver.commit();

        return COMMENT_ID_PREFIX + next;
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

    private void setElementContent(ContentFragment contentFragment, String elementName, String value)
            throws ContentFragmentException {
        ContentElement element = contentFragment.getElement(elementName);
        if (element == null) {
            LOG.warn("Content element '{}' not found on comment fragment", elementName);
            return;
        }
        element.setContent(value != null ? value : "", MIME_TEXT_PLAIN);
    }

    private int compareCreatedAtAscending(CommentDTO left, CommentDTO right) {
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
        return leftCreatedAt.compareTo(rightCreatedAt);
    }
}
