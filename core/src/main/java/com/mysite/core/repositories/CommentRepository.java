package com.mysite.core.repositories;

import com.mysite.core.dto.CommentDTO;

import java.util.List;

/**
 * Persistence port for ticket comments.
 * <p>
 * Defines the repository contract used by the service layer. Implementations are registered
 * as OSGi components and selected via the {@code impl.type} service property
 * ({@code contentfragment} for the Content Fragment adapter in Sprint 3.1; {@code database}
 * for a future relational adapter). Services depend on this interface only — not on AEM/JCR types.
 * </p>
 */
public interface CommentRepository {

    /**
     * Persists a new comment for a ticket. The implementation assigns {@code id} and
     * {@code createdAt}.
     *
     * @param comment comment data to persist (id may be null on input)
     * @return the persisted comment including generated id and timestamp
     */
    CommentDTO add(CommentDTO comment);

    /**
     * Returns all comments for the given ticket, ordered by {@code createdAt} ascending.
     *
     * @param ticketId parent ticket business id
     * @return non-null list of comments; empty when the ticket has no comments (ticket
     *         existence is validated in the service layer)
     */
    List<CommentDTO> listByTicket(String ticketId);
}
