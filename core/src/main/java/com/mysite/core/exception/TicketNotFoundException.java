package com.mysite.core.exception;

/**
 * Thrown when a ticket business id does not resolve to a persisted ticket.
 * <p>
 * Maps to HTTP <strong>404 Not Found</strong> with code {@code NOT_FOUND}
 * per api-contract.md.
 * </p>
 */
public class TicketNotFoundException extends DomainException {

    private final String ticketId;

    /**
     * @param ticketId ticket id that was not found (e.g. {@code TKT-9999})
     */
    public TicketNotFoundException(String ticketId) {
        super("Ticket not found: " + ticketId);
        this.ticketId = ticketId;
    }

    public String getTicketId() {
        return ticketId;
    }

    @Override
    public String errorCode() {
        return "NOT_FOUND";
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
