package com.mysite.core.dto;

import java.util.Objects;

/**
 * Source-agnostic data transfer object for a seeded AEM user exposed to the ticketing API.
 * <p>
 * Populated from UserManager profile data in the repository layer. No AEM, JCR, or Sling types
 * on this class.
 * </p>
 */
public class UserDTO {

    private String userId;
    private String displayName;
    private String email;

    public UserDTO() {
    }

    public UserDTO(String userId, String displayName, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDTO)) {
            return false;
        }
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(userId, userDTO.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserDTO{"
                + "userId='" + userId + '\''
                + ", displayName='" + displayName + '\''
                + '}';
    }
}
