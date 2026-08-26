package com.mysite.core.exception;

/**
 * Base type for domain errors raised by the service layer and mapped to REST JSON responses.
 * <p>
 * Servlets catch {@link DomainException} subclasses and serialize
 * {@code {"error": getMessage(), "code": errorCode()}} with {@link #httpStatus()}.
 * </p>
 * <p>
 * <strong>api-contract.md error catalog mapping:</strong>
 * </p>
 * <table border="1">
 *   <caption>Domain exception codes and HTTP status</caption>
 *   <tr><th>errorCode()</th><th>HTTP</th><th>Exception type</th></tr>
 *   <tr><td>{@code VALIDATION_ERROR}</td><td>400</td><td>{@link ValidationException}</td></tr>
 *   <tr><td>{@code UNKNOWN_USER}</td><td>400</td><td>{@link UnknownUserException}</td></tr>
 *   <tr><td>{@code TICKET_NOT_EDITABLE}</td><td>400</td><td>{@link TicketNotEditableException}</td></tr>
 *   <tr><td>{@code NOT_FOUND}</td><td>404</td><td>{@link TicketNotFoundException}</td></tr>
 *   <tr><td>{@code INVALID_TRANSITION}</td><td>409</td><td>{@link InvalidTransitionException}</td></tr>
 * </table>
 * <p>
 * Unhandled failures map to {@code INTERNAL_ERROR} / HTTP 500 in the servlet layer (not a
 * {@link DomainException} subclass).
 * </p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Stable machine-readable code matching the api-contract error catalog.
     */
    public abstract String errorCode();

    /**
     * HTTP status code for this domain error per api-contract.md.
     */
    public abstract int httpStatus();
}
