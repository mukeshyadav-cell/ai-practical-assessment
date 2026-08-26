package com.mysite.core.exception;

/**
 * Thrown when a ticket update or reassignment is attempted while the ticket is in a terminal
 * status ({@code Closed} or {@code Cancelled}) per FR-7.
 * <p>
 * Maps to HTTP <strong>400 Bad Request</strong> with code {@code TICKET_NOT_EDITABLE}
 * per api-contract.md.
 * </p>
 */
public class TicketNotEditableException extends DomainException {

    private final String ticketId;
    private final String status;

    /**
     * @param ticketId ticket business id (e.g. {@code TKT-1001})
     * @param status   current terminal status label (e.g. {@code Closed})
     */
    public TicketNotEditableException(String ticketId, String status) {
        super("Ticket " + ticketId + " is not editable in status " + status);
        this.ticketId = ticketId;
        this.status = status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String errorCode() {
        return "TICKET_NOT_EDITABLE";
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
