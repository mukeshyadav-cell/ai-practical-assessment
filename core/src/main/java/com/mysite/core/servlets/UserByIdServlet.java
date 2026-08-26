package com.mysite.core.servlets;

import com.mysite.core.dto.UserDTO;
import com.mysite.core.repositories.UserRepository;
import com.mysite.core.servlets.util.ServletPathUtil;
import com.mysite.core.servlets.util.ServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Request handler for {@code GET /bin/api/v1/users/{userId}}.
 * <p>
 * Invoked by {@link UserByIdRoutingFilter}. Read-only user detail lookup.
 * </p>
 */
public final class UserByIdServlet {

  private static final Logger LOG = LoggerFactory.getLogger(UserByIdServlet.class);

  private UserByIdServlet() {
  }

  /**
   * Returns a single seeded user by id.
   */
  public static void doGet(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      UserRepository userRepository) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String userId = ServletPathUtil.resolveUserId(request, ServletConstants.USERS_PATH);
      if (userId == null || userId.isBlank()) {
        ServletResponseUtil.writeNotFoundError(response, LOG, "User not found");
        return;
      }

      Optional<UserDTO> user = userRepository.getById(userId);
      if (user.isPresent()) {
        ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, user.get());
      } else {
        ServletResponseUtil.writeNotFoundError(response, LOG, "User not found: " + userId);
      }
    });
  }
}
