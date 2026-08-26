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
import java.util.Collections;
import java.util.List;

/**
 * REST servlet for the user collection resource (read-only).
 * <p>
 * {@code GET /bin/api/v1/users} — list seeded users (optional {@code ?q=} search).
 * User detail {@code GET /bin/api/v1/users/{userId}} is dispatched by
 * {@link UserByIdRoutingFilter} (suffix paths under {@code /bin/} are unreliable on local AEM SDK).
 * </p>
 * <p>
 * <strong>Architecture note:</strong> This servlet injects {@link UserRepository} directly rather
 * than a {@code UserService} pass-through. User lookups are read-only with no business rules;
 * adding a service layer would add indirection without benefit in this scope. A future
 * {@code UserService} would be warranted if validation, caching, or authorization rules emerge.
 * </p>
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=" + ServletConstants.USERS_PATH,
        "sling.servlet.methods=GET"
    })
@ServiceDescription("REST API — list seeded users")
public class UserCollectionServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(UserCollectionServlet.class);

  @Reference(target = "(impl.type=aem)")
  private UserRepository userRepository;

  /**
   * Lists seeded users, optionally filtered by {@code q} on display name or email.
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String query = request.getParameter("q");
      List<UserDTO> users;

      if (query != null && !query.isBlank()) {
        LOG.debug("Listing users matching query: {}", query);
        users = userRepository.search(query.trim());
      } else {
        users = userRepository.getAll();
      }

      if (users == null) {
        users = Collections.emptyList();
      }

      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, users);
    });
  }
}
