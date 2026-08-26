package com.mysite.core.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Source-agnostic data transfer object for a support ticket.
 * <p>
 * Used across the repository, service, and servlet layers. No AEM, JCR, or Sling types.
 * JSON field {@code id} maps from the Content Fragment element {@code ticketId}; translation
 * is handled in the repository mapper (Sprint 3.1).
 * </p>
 */
public class TicketDTO {

    private String id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String assignedTo;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public TicketDTO() {
    }

    public TicketDTO(
            String id,
            String title,
            String description,
            String priority,
            String status,
            String assignedTo,
            String createdBy,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketDTO)) {
            return false;
        }
        TicketDTO ticketDTO = (TicketDTO) o;
        return Objects.equals(id, ticketDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TicketDTO{"
                + "id='" + id + '\''
                + ", title='" + title + '\''
                + ", priority='" + priority + '\''
                + ", status='" + status + '\''
                + ", assignedTo='" + assignedTo + '\''
                + ", createdBy='" + createdBy + '\''
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
