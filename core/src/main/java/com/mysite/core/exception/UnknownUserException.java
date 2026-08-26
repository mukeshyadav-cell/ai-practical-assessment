package com.mysite.core.exception;

/**
 * Thrown when {@code assignedTo} or another user reference does not match a seeded AEM user.
 * <p>
 * Maps to HTTP <strong>400 Bad Request</strong> with code {@code UNKNOWN_USER}
 * per api-contract.md.
 * </p>
 */
public class UnknownUserException extends DomainException {

    private final String userId;

    /**
     * @param userId user id that was not found (e.g. {@code nobody})
     */
    public UnknownUserException(String userId) {
        super("Unknown user: " + userId);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String errorCode() {
        return "UNKNOWN_USER";
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
