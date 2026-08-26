package com.mysite.core.repositories;

import com.mysite.core.dto.TicketDTO;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for support tickets.
 * <p>
 * Defines the repository contract used by the service layer. Implementations are registered
 * as OSGi components and selected via the {@code impl.type} service property
 * ({@code contentfragment} for the Content Fragment adapter in Sprint 3.1; {@code database}
 * for a future relational adapter). Services depend on this interface only — not on AEM/JCR types.
 * </p>
 */
public interface TicketRepository {

    /**
     * Returns all tickets, typically ordered by {@code createdAt} descending (newest first).
     *
     * @return non-null list of tickets; empty when none exist
     */
    List<TicketDTO> getAll();

    /**
     * Loads a single ticket by business id (e.g. {@code TKT-1001}).
     *
     * @param id ticket id (DTO/API field {@code id}, CF element {@code ticketId})
     * @return the ticket if found; {@link Optional#empty()} when no ticket exists for {@code id}
     */
    Optional<TicketDTO> getById(String id);

    /**
     * Returns tickets matching the given lifecycle status.
     *
     * @param status one of: Open, In Progress, Resolved, Closed, Cancelled
     * @return non-null list of matching tickets; empty when none match
     */
    List<TicketDTO> findByStatus(String status);

    /**
     * Case-insensitive partial match on ticket title (keyword search).
     *
     * @param query search term; blank may be treated as match-all by the implementation
     * @return non-null list of matching tickets; empty when none match
     */
    List<TicketDTO> searchByTitle(String query);

    /**
     * Persists a new ticket. The implementation assigns {@code id}, {@code createdAt}, and
     * {@code updatedAt}; initial {@code status} is {@code Open} per business rules.
     *
     * @param ticket ticket data to persist (id may be null on input)
     * @return the persisted ticket including generated id and timestamps
     */
    TicketDTO create(TicketDTO ticket);

    /**
     * Updates an existing ticket. The implementation sets {@code updatedAt}.
     * Ticket delete is out of scope — there is no delete operation on this port.
     *
     * @param ticket ticket with id and mutable fields to persist
     * @return the updated ticket
     */
    TicketDTO update(TicketDTO ticket);
}
