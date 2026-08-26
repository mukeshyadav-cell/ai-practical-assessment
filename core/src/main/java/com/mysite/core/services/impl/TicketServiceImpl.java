package com.mysite.core.services.impl;

import com.mysite.core.dto.TicketDTO;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.UnknownUserException;
import com.mysite.core.exception.ValidationException;
import com.mysite.core.repositories.TicketRepository;
import com.mysite.core.repositories.UserRepository;
import com.mysite.core.services.TicketService;
import com.mysite.core.statemachine.TicketStateMachine;
import com.mysite.core.statemachine.TicketStatus;
import com.mysite.core.validation.TicketValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Default {@link TicketService} implementation wiring repository ports, user lookup, and the
 * ticket state machine.
 * <p>
 * Field and enum validation is delegated to {@link TicketValidator}; repository-dependent checks
 * (ticket/user existence) remain here.
 * </p>
 */
@Component(service = TicketService.class)
public class TicketServiceImpl implements TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketStateMachine stateMachine = new TicketStateMachine();

    @Reference(target = "(impl.type=contentfragment)")
    private TicketRepository ticketRepository;

    @Reference(target = "(impl.type=aem)")
    private UserRepository userRepository;

    @Override
    public TicketDTO createTicket(TicketDTO ticket) {
        TicketValidator.validateForCreate(ticket);

        String assignedTo = blankToNull(ticket.getAssignedTo());
        if (assignedTo != null) {
            requireExistingUser(assignedTo);
        }

        TicketDTO toCreate = new TicketDTO();
        toCreate.setTitle(ticket.getTitle().trim());
        toCreate.setDescription(ticket.getDescription());
        toCreate.setPriority(ticket.getPriority());
        toCreate.setStatus(TicketStatus.OPEN.getLabel());
        toCreate.setAssignedTo(assignedTo);
        toCreate.setCreatedBy(ticket.getCreatedBy());

        TicketDTO created = ticketRepository.create(toCreate);
        LOG.info("Created ticket {} with status {}", created.getId(), created.getStatus());
        return created;
    }

    @Override
    public List<TicketDTO> listTickets(String statusFilter, String query) {
        String normalizedStatus = blankToNull(statusFilter);
        String normalizedQuery = blankToNull(query);

        if (normalizedStatus != null) {
            TicketValidator.validateStatusLabel(normalizedStatus);
        }

        List<TicketDTO> tickets;
        if (normalizedQuery != null) {
            tickets = new ArrayList<>(ticketRepository.searchByTitle(normalizedQuery));
            if (normalizedStatus != null) {
                String status = normalizedStatus;
                tickets.removeIf(ticket -> !status.equals(ticket.getStatus()));
            }
        } else if (normalizedStatus != null) {
            tickets = new ArrayList<>(ticketRepository.findByStatus(normalizedStatus));
        } else {
            tickets = new ArrayList<>(ticketRepository.getAll());
        }

        sortByCreatedAtDescending(tickets);
        return tickets;
    }

    @Override
    public TicketDTO getTicket(String id) {
        String ticketId = TicketValidator.requireTicketId(id);
        return ticketRepository.getById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    @Override
    public TicketDTO updateTicket(String id, TicketDTO changes) {
        TicketValidator.validateForUpdate(changes);

        TicketDTO existing = getTicket(id);
        TicketValidator.assertEditable(existing.getId(), existing.getStatus());

        if (changes.getTitle() != null) {
            existing.setTitle(changes.getTitle().trim());
        }
        if (changes.getDescription() != null) {
            existing.setDescription(changes.getDescription());
        }
        if (changes.getPriority() != null) {
            existing.setPriority(changes.getPriority());
        }

        TicketDTO updated = ticketRepository.update(existing);
        LOG.info("Updated ticket {} fields (title/description/priority)", updated.getId());
        return updated;
    }

    @Override
    public TicketDTO reassignTicket(String id, String assigneeUserId) {
        TicketValidator.validateAssigneeRequired(assigneeUserId);

        TicketDTO existing = getTicket(id);
        TicketValidator.assertEditable(existing.getId(), existing.getStatus());

        String assignee = assigneeUserId.trim();
        requireExistingUser(assignee);

        existing.setAssignedTo(assignee);
        TicketDTO updated = ticketRepository.update(existing);
        LOG.info("Reassigned ticket {} to {}", updated.getId(), assignee);
        return updated;
    }

    @Override
    public TicketDTO changeStatus(String id, String newStatusLabel) {
        TicketDTO existing = getTicket(id);

        TicketStatus from = TicketValidator.parseStatusLabel(existing.getStatus());
        TicketStatus to = TicketValidator.parseStatusLabel(newStatusLabel);

        stateMachine.assertCanTransition(from, to);

        existing.setStatus(to.getLabel());
        TicketDTO updated = ticketRepository.update(existing);
        LOG.info("Changed ticket {} status from {} to {}", id, from.getLabel(), to.getLabel());
        return updated;
    }

    private void requireExistingUser(String userId) {
        if (userRepository.getById(userId).isEmpty()) {
            throw new UnknownUserException(userId);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void sortByCreatedAtDescending(List<TicketDTO> tickets) {
        tickets.sort(Comparator.comparing(
                TicketDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    }
}
