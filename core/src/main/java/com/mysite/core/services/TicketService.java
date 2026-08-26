package com.mysite.core.services;

import com.mysite.core.dto.TicketDTO;
import com.mysite.core.exception.InvalidTransitionException;
import com.mysite.core.exception.TicketNotEditableException;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.UnknownUserException;
import com.mysite.core.exception.ValidationException;

import java.util.List;

/**
 * Business operations for support tickets.
 * <p>
 * Orchestrates validation, state-machine enforcement, and persistence via repository ports.
 * Servlets depend on this interface only — not on Content Fragment or JCR types.
 * </p>
 */
public interface TicketService {

    /**
     * Creates a new ticket with initial status {@code Open}.
     *
     * @param ticket input data ({@code title}, {@code description}, {@code priority},
     *               optional {@code assignedTo}, {@code createdBy}); incoming {@code status} is ignored
     * @return persisted ticket including generated id and timestamps
     * @throws ValidationException  when {@code title} is blank or {@code priority} is not P1–P4
     * @throws UnknownUserException   when {@code assignedTo} is non-blank and does not reference a seeded user
     */
    TicketDTO createTicket(TicketDTO ticket);

    /**
     * Lists tickets with optional status filter and/or title keyword search.
     * <p>
     * When both {@code query} and {@code statusFilter} are provided, results match the query
     * <strong>and</strong> the status (intersection). Results are ordered by {@code createdAt}
     * descending (newest first).
     * </p>
     *
     * @param statusFilter optional status label ({@code Open}, {@code In Progress}, etc.); null/blank ignored
     * @param query        optional case-insensitive title search term; null/blank ignored
     * @return non-null list of matching tickets
     * @throws ValidationException when {@code statusFilter} is non-blank but not a valid status label
     */
    List<TicketDTO> listTickets(String statusFilter, String query);

    /**
     * Loads a single ticket by business id.
     *
     * @param id ticket id (e.g. {@code TKT-1001})
     * @return the ticket
     * @throws TicketNotFoundException when no ticket exists for {@code id}
     */
    TicketDTO getTicket(String id);

    /**
     * Updates mutable fields ({@code title}, {@code description}, {@code priority}) only.
     * Status and assignee are not changed here — use {@link #changeStatus} and
     * {@link #reassignTicket} respectively.
     *
     * @param id      ticket id
     * @param changes fields to apply (non-null fields from this DTO overwrite existing values)
     * @return updated ticket
     * @throws TicketNotFoundException     when the ticket does not exist
     * @throws TicketNotEditableException  when the ticket is {@code Closed} or {@code Cancelled}
     * @throws ValidationException         when updated {@code title} is blank or {@code priority} is invalid
     */
    TicketDTO updateTicket(String id, TicketDTO changes);

    /**
     * Changes the assignee of a ticket.
     * <p>
     * MVP: {@code assigneeUserId} must be non-blank and reference an existing seeded user
     * (unassign via blank id is not supported at the service layer).
     * </p>
     *
     * @param id             ticket id
     * @param assigneeUserId target user id (required)
     * @return updated ticket
     * @throws TicketNotFoundException     when the ticket does not exist
     * @throws TicketNotEditableException  when the ticket is {@code Closed} or {@code Cancelled}
     * @throws ValidationException         when {@code assigneeUserId} is blank
     * @throws UnknownUserException        when the user id does not exist
     */
    TicketDTO reassignTicket(String id, String assigneeUserId);

    /**
     * Applies a state-machine-valid status transition.
     *
     * @param id              ticket id
     * @param newStatusLabel  target status label (e.g. {@code In Progress})
     * @return updated ticket
     * @throws TicketNotFoundException    when the ticket does not exist
     * @throws ValidationException        when {@code newStatusLabel} is unknown or blank
     * @throws InvalidTransitionException when the transition is not allowed (maps to HTTP 409)
     */
    TicketDTO changeStatus(String id, String newStatusLabel);
}
