package com.mysite.core.repositories;

import com.mysite.core.dto.UserDTO;

import java.util.List;
import java.util.Optional;

/**
 * Read-only persistence port for seeded assignable users.
 * <p>
 * Users are resolved from AEM UserManager in the Content Fragment adapter; they are not
 * stored as Content Fragments. Implementations are registered as OSGi components and selected
 * via the {@code impl.type} service property ({@code contentfragment} now; {@code database}
 * later). Services depend on this interface only — not on AEM/JCR types.
 * </p>
 */
public interface UserRepository {

    /**
     * Loads a single user by id (e.g. {@code agent-1}).
     *
     * @param userId AEM authorizable id
     * @return the user if found; {@link Optional#empty()} when no user exists for {@code userId}
     */
    Optional<UserDTO> getById(String userId);

    /**
     * Returns all seeded, non-system users available for assignee selection and display.
     *
     * @return non-null list of users; empty when none are configured
     */
    List<UserDTO> getAll();

    /**
     * Case-insensitive partial match on {@code displayName} or {@code email}.
     *
     * @param query search term; blank may be treated as match-all by the implementation
     * @return non-null list of matching users; empty when none match
     */
    List<UserDTO> search(String query);
}
