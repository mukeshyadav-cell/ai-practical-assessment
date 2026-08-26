package com.mysite.core.statemachine;

/**
 * Canonical ticket lifecycle status values.
 * <p>
 * Each constant maps to the human-readable label stored in Content Fragments and JSON
 * (e.g. {@code "In Progress"}). Use {@link #getLabel()} when persisting or serializing;
 * use {@link #fromLabel(String)} when parsing external input.
 * </p>
 */
public enum TicketStatus {

    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    /**
     * Display label used in CF elements, DTO {@code status} strings, and REST JSON.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves a label to the matching enum constant.
     *
     * @param label status label (e.g. {@code "In Progress"})
     * @return matching {@link TicketStatus}
     * @throws IllegalArgumentException if {@code label} is null, blank, or unknown
     */
    public static TicketStatus fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Status label must not be null or blank");
        }
        for (TicketStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status label: " + label);
    }

    /**
     * @param label status label to check
     * @return {@code true} if {@code label} matches a known status label
     */
    public static boolean isValidLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        for (TicketStatus status : values()) {
            if (status.label.equals(label)) {
                return true;
            }
        }
        return false;
    }
}
