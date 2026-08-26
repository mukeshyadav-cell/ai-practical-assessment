package com.mysite.core.services;

import com.mysite.core.dto.CommentDTO;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.ValidationException;

import java.util.List;

/**
 * Business operations for ticket comments.
 * <p>
 * Comments may be added and listed for tickets in any status, including terminal
 * {@code Closed} and {@code Cancelled} states.
 * </p>
 */
public interface CommentService {

    /**
     * Adds a comment to an existing ticket.
     * <p>
     * The path {@code ticketId} is authoritative — any {@code ticketId} on the input DTO is
     * overwritten. The repository assigns {@code id} and {@code createdAt}.
     * </p>
     * <p>
     * {@code createdBy} must be non-blank at the service layer; the servlet supplies the
     * authenticated session user (MVP policy).
     * </p>
     *
     * @param ticketId parent ticket business id
     * @param comment  comment data ({@code message}, {@code createdBy} required)
     * @return persisted comment including generated id and timestamp
     * @throws TicketNotFoundException when the ticket does not exist
     * @throws ValidationException     when {@code message} or {@code createdBy} is blank, or input is null
     */
    CommentDTO addComment(String ticketId, CommentDTO comment);

    /**
     * Lists all comments for a ticket, ordered by {@code createdAt} ascending (oldest first).
     *
     * @param ticketId parent ticket business id
     * @return non-null list of comments; empty when the ticket has no comments
     * @throws TicketNotFoundException when the ticket does not exist
     * @throws ValidationException     when {@code ticketId} is blank
     */
    List<CommentDTO> listComments(String ticketId);
}
