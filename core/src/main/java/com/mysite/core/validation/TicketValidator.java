package com.mysite.core.validation;

import com.mysite.core.dto.TicketDTO;
import com.mysite.core.exception.TicketNotEditableException;
import com.mysite.core.exception.ValidationException;
import com.mysite.core.statemachine.TicketStatus;

import java.util.Set;

/**
 * Pure validation helpers for ticket field values and labels.
 * <p>
 * Does not access repositories — existence checks (ticket/user lookup) remain in the service layer.
 * All methods throw {@link ValidationException} or {@link TicketNotEditableException} on failure.
 * </p>
 */
public final class TicketValidator {

    static final Set<String> VALID_PRIORITIES = Set.of("P1", "P2", "P3", "P4");

    private TicketValidator() {
    }

    /**
     * Validates input for ticket creation: non-blank title and description, valid priority.
     * Does not validate {@code assignedTo} or {@code createdBy} (handled in the service/servlet).
     *
     * @param ticket ticket data from the client
     * @throws ValidationException when a required field is missing or invalid
     */
    public static void validateForCreate(TicketDTO ticket) {
        if (ticket == null) {
            throw new ValidationException("ticket", "must not be null");
        }
        validateTitle(ticket.getTitle());
        validateDescription(ticket.getDescription());
        validatePriority(ticket.getPriority());
    }

    /**
     * Validates mutable fields present on an update payload (non-null fields only).
     *
     * @param changes partial update DTO
     * @throws ValidationException when a provided field is invalid
     */
    public static void validateForUpdate(TicketDTO changes) {
        if (changes == null) {
            throw new ValidationException("ticket", "changes must not be null");
        }
        if (changes.getTitle() != null) {
            validateTitle(changes.getTitle());
        }
        if (changes.getDescription() != null) {
            validateDescription(changes.getDescription());
        }
        if (changes.getPriority() != null) {
            validatePriority(changes.getPriority());
        }
    }

    /**
     * @throws ValidationException when {@code title} is null or blank
     */
    public static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("title", "is required");
        }
    }

    /**
     * @throws ValidationException when {@code description} is null or blank
     */
    public static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("description", "is required");
        }
    }

    /**
     * @throws ValidationException when {@code priority} is null, blank, or not P1–P4
     */
    public static void validatePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            throw new ValidationException("priority", "is required");
        }
        if (!VALID_PRIORITIES.contains(priority)) {
            throw new ValidationException("priority", "must be one of P1, P2, P3, P4");
        }
    }

    /**
     * @throws ValidationException when {@code statusLabel} is not a known ticket status
     */
    public static void validateStatusLabel(String statusLabel) {
        if (statusLabel == null || statusLabel.isBlank()) {
            throw new ValidationException("status", "is required");
        }
        if (!TicketStatus.isValidLabel(statusLabel)) {
            throw new ValidationException("status", "must be a valid ticket status");
        }
    }

    /**
     * Parses a status label to {@link TicketStatus}.
     *
     * @throws ValidationException when the label is unknown or blank
     */
    public static TicketStatus parseStatusLabel(String statusLabel) {
        try {
            return TicketStatus.fromLabel(statusLabel);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("status", ex.getMessage());
        }
    }

    /**
     * Rejects updates and reassignments when the ticket is in a terminal status.
     *
     * @throws TicketNotEditableException when status is {@link TicketStatus#CLOSED} or
     *                                    {@link TicketStatus#CANCELLED}
     */
    public static void assertEditable(String ticketId, String statusLabel) {
        TicketStatus status = parseStatusLabel(statusLabel);
        if (status == TicketStatus.CLOSED || status == TicketStatus.CANCELLED) {
            throw new TicketNotEditableException(ticketId, status.getLabel());
        }
    }

    /**
     * @return trimmed ticket id
     * @throws ValidationException when {@code ticketId} is null or blank
     */
    public static String requireTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new ValidationException("ticketId", "is required");
        }
        return ticketId.trim();
    }

    /**
     * @throws ValidationException when {@code assigneeUserId} is null or blank
     */
    public static void validateAssigneeRequired(String assigneeUserId) {
        if (assigneeUserId == null || assigneeUserId.isBlank()) {
            throw new ValidationException("assignedTo", "is required");
        }
    }
}
