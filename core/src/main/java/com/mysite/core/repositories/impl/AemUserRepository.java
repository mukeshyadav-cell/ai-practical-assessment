package com.mysite.core.repositories.impl;

import com.mysite.core.dto.UserDTO;
import com.mysite.core.repositories.UserRepository;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * AEM UserManager adapter for {@link UserRepository}.
 * <p>
 * Resolves seeded assignable users from JCR authorizables — not Content Fragments.
 * </p>
 */
@Component(service = UserRepository.class, property = "impl.type=aem")
public class AemUserRepository implements UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AemUserRepository.class);

    static final String SERVICE_SUBSERVICE = "assessment-service";

    private static final String PROFILE_GIVEN_NAME = "profile/givenName";
    private static final String PROFILE_FAMILY_NAME = "profile/familyName";
    private static final String PROFILE_EMAIL = "profile/email";

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Loads a single non-system user by authorizable id.
     *
     * @param userId AEM authorizable id (e.g. {@code agent-1})
     * @return the user if found and assignable; empty when not found, not a user, or system user
     */
    @Override
    public Optional<UserDTO> getById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        try (ResourceResolver resolver = obtainServiceResolver()) {
            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager == null) {
                LOG.error("UserManager is not available on the service resource resolver");
                return Optional.empty();
            }

            Authorizable authorizable = userManager.getAuthorizable(userId);
            if (!isAssignableUser(authorizable)) {
                return Optional.empty();
            }

            return Optional.of(toDto((User) authorizable));
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resource resolver for subservice {}", SERVICE_SUBSERVICE, e);
            return Optional.empty();
        } catch (RepositoryException e) {
            LOG.error("Failed to load user {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Returns all non-system {@link User} authorizables (groups and system users excluded).
     *
     * @return non-null list of users; empty when none are found
     */
    @Override
    public List<UserDTO> getAll() {
        try (ResourceResolver resolver = obtainServiceResolver()) {
            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager == null) {
                LOG.error("UserManager is not available on the service resource resolver");
                return Collections.emptyList();
            }

            List<UserDTO> users = new ArrayList<>();
            Iterator<Authorizable> authorizables =
                    userManager.findAuthorizables("rep:principalName", "%", UserManager.SEARCH_TYPE_USER);

            while (authorizables.hasNext()) {
                Authorizable authorizable = authorizables.next();
                if (!isAssignableUser(authorizable)) {
                    continue;
                }

                try {
                    users.add(toDto((User) authorizable));
                } catch (RepositoryException e) {
                    LOG.warn("Skipping user due to repository error during mapping", e);
                }
            }

            return users;
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resource resolver for subservice {}", SERVICE_SUBSERVICE, e);
            return Collections.emptyList();
        } catch (RepositoryException e) {
            LOG.error("Failed to enumerate users", e);
            return Collections.emptyList();
        }
    }

    /**
     * Case-insensitive partial match on {@code displayName}, {@code email}, or {@code userId}.
     * <p>
     * A null or blank query returns an empty list (not all users) so UI autocomplete does not
     * dump the full user directory on empty input.
     * </p>
     *
     * @param query search term
     * @return non-null list of matching users; empty when query is blank or none match
     */
    @Override
    public List<UserDTO> search(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        List<UserDTO> matches = new ArrayList<>();

        for (UserDTO user : getAll()) {
            if (matchesQuery(user, lowerQuery)) {
                matches.add(user);
            }
        }

        return matches;
    }

    private ResourceResolver obtainServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Map.of(
                ResourceResolverFactory.SUBSERVICE,
                SERVICE_SUBSERVICE);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    private boolean isAssignableUser(Authorizable authorizable) throws RepositoryException {
        if (authorizable == null || authorizable.isGroup()) {
            return false;
        }

        if (!(authorizable instanceof User)) {
            return false;
        }

        return !((User) authorizable).isSystemUser();
    }

    private boolean matchesQuery(UserDTO user, String lowerQuery) {
        if (containsIgnoreCase(user.getUserId(), lowerQuery)) {
            return true;
        }
        if (containsIgnoreCase(user.getDisplayName(), lowerQuery)) {
            return true;
        }
        return containsIgnoreCase(user.getEmail(), lowerQuery);
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    private UserDTO toDto(User user) throws RepositoryException {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getID());

        String givenName = getPropertyValue(user, PROFILE_GIVEN_NAME);
        String familyName = getPropertyValue(user, PROFILE_FAMILY_NAME);
        String displayName = buildDisplayName(givenName, familyName, dto.getUserId());
        dto.setDisplayName(displayName);

        dto.setEmail(getPropertyValue(user, PROFILE_EMAIL));
        return dto;
    }

    private String buildDisplayName(String givenName, String familyName, String userId) {
        String combined = ((givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "")).trim();
        if (combined.isEmpty()) {
            return userId;
        }
        return combined;
    }

    private String getPropertyValue(Authorizable authorizable, String propertyPath) throws RepositoryException {
        Value[] values = authorizable.getProperty(propertyPath);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0].getString();
    }
}
