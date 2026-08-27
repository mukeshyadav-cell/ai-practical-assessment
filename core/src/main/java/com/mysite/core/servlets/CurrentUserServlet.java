package com.mysite.core.servlets;

import com.mysite.core.dto.UserDTO;
import com.mysite.core.repositories.UserRepository;
import com.mysite.core.servlets.util.ServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * REST servlet for the authenticated AEM user ({@code GET /bin/api/v1/me}).
 * <p>
 * Returns seeded user profile details when available; otherwise a minimal body so the UI always
 * has a {@code userId} for {@code createdBy} (e.g. admin is excluded from the user repository).
 * </p>
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=" + ServletConstants.ME_PATH,
        "sling.servlet.methods=GET"
    })
@ServiceDescription("REST API — current AEM user")
public class CurrentUserServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(CurrentUserServlet.class);

  @Reference(target = "(impl.type=aem)")
  private UserRepository userRepository;

  /**
   * Returns the current request user's profile, or a minimal fallback when not in the seeded set.
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String rawUserId = resolveCurrentUserId(request);
      final String resolvedUserId = (rawUserId == null || rawUserId.isBlank()) ? "anonymous" : rawUserId;

      LOG.debug("Resolving current user: {}", resolvedUserId);

      Optional<UserDTO> user = userRepository.getById(resolvedUserId);
      UserDTO body = user.orElseGet(() -> new UserDTO(resolvedUserId, resolvedUserId, ""));

      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, body);
    });
  }

  private static String resolveCurrentUserId(SlingHttpServletRequest request) {
    if (request.getUserPrincipal() != null) {
      return request.getUserPrincipal().getName();
    }
    return request.getResourceResolver().getUserID();
  }
}
