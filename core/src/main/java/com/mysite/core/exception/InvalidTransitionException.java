package com.mysite.core.exception;

import com.mysite.core.statemachine.TicketStatus;

/**
 * Thrown when a requested ticket status change violates the lifecycle state machine.
 * <p>
 * Maps to HTTP <strong>409 Conflict</strong> with code {@code INVALID_TRANSITION}
 * per api-contract.md.
 * </p>
 */
public class InvalidTransitionException extends DomainException {

    private final TicketStatus from;
    private final TicketStatus to;

    /**
     * @param from current ticket status
     * @param to   requested target status
     */
    public InvalidTransitionException(TicketStatus from, TicketStatus to) {
        super("Invalid transition: " + from.getLabel() + " -> " + to.getLabel());
        this.from = from;
        this.to = to;
    }

    public TicketStatus getFrom() {
        return from;
    }

    public TicketStatus getTo() {
        return to;
    }

    @Override
    public String errorCode() {
        return "INVALID_TRANSITION";
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
