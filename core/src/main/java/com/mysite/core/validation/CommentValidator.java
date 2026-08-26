package com.mysite.core.validation;

import com.mysite.core.dto.CommentDTO;
import com.mysite.core.exception.ValidationException;

/**
 * Pure validation helpers for comment field values.
 * <p>
 * Ticket existence is validated in {@link com.mysite.core.services.impl.CommentServiceImpl}
 * via {@link com.mysite.core.repositories.TicketRepository}.
 * </p>
 */
public final class CommentValidator {

    private CommentValidator() {
    }

    /**
     * Validates comment input before persistence: non-blank message and {@code createdBy}.
     * <p>
     * Empty comment messages reuse {@link ValidationException} (not a separate exception type)
     * per api-contract {@code VALIDATION_ERROR} catalog alignment.
     * </p>
     *
     * @param comment comment data from the client
     * @throws ValidationException when {@code comment} is null, or message/createdBy is blank
     */
    public static void validateComment(CommentDTO comment) {
        if (comment == null) {
            throw new ValidationException("comment", "must not be null");
        }
        String message = comment.getMessage();
        if (message == null || message.isBlank()) {
            throw new ValidationException("Comment message is required");
        }
        String createdBy = comment.getCreatedBy();
        if (createdBy == null || createdBy.isBlank()) {
            throw new ValidationException("createdBy", "is required");
        }
    }
}
