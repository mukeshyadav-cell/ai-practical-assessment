package com.mysite.core.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Source-agnostic data transfer object for a ticket comment.
 * <p>
 * Used across the repository, service, and servlet layers. No AEM, JCR, or Sling types.
 * JSON field {@code id} maps from the Content Fragment element {@code commentId}; translation
 * is handled in the repository mapper (Sprint 3.1).
 * </p>
 */
public class CommentDTO {

    private String id;
    private String ticketId;
    private String message;
    private String createdBy;
    private Instant createdAt;

    public CommentDTO() {
    }

    public CommentDTO(String id, String ticketId, String message, String createdBy, Instant createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.message = message;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommentDTO)) {
            return false;
        }
        CommentDTO that = (CommentDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CommentDTO{"
                + "id='" + id + '\''
                + ", ticketId='" + ticketId + '\''
                + ", createdBy='" + createdBy + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
