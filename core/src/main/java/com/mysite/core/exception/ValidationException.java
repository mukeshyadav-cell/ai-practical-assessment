package com.mysite.core.exception;

/**
 * Thrown when request or entity field validation fails (missing, blank, or invalid value).
 * <p>
 * Maps to HTTP <strong>400 Bad Request</strong> with code {@code VALIDATION_ERROR}
 * per api-contract.md.
 * </p>
 * <p>
 * Empty comment messages and other field-level validation failures reuse this type rather
 * than a separate {@code CommentValidationException}, keeping the domain exception hierarchy
 * aligned with the api-contract catalog.
 * </p>
 */
public class ValidationException extends DomainException {

    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    /**
     * @param field  optional field name (e.g. {@code "title"}, {@code "message"})
     * @param reason human-readable validation failure
     */
    public ValidationException(String field, String reason) {
        super(field != null && !field.isBlank()
                ? field + ": " + reason
                : reason);
        this.field = (field != null && !field.isBlank()) ? field : null;
    }

    /**
     * @return validated field name, or {@code null} if not field-specific
     */
    public String getField() {
        return field;
    }

    @Override
    public String errorCode() {
        return "VALIDATION_ERROR";
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
