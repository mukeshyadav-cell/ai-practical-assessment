package com.mysite.core.servlets;

import com.mysite.core.repositories.UserRepository;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Routes {@code GET /bin/api/v1/users/{userId}} to {@link UserByIdServlet} handlers
 * (same filter-dispatch pattern as ticket sub-resources on local AEM SDK).
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_PATTERN + "=" + ServletConstants.USERS_PATH + "/[^/]+$"
    })
@ServiceDescription("REST API — route user by-id GET to UserByIdServlet handler")
public class UserByIdRoutingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(UserByIdRoutingFilter.class);

  @Reference(target = "(impl.type=aem)")
  private UserRepository userRepository;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof SlingHttpServletRequest) || !(response instanceof SlingHttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

    if (!"GET".equalsIgnoreCase(slingRequest.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    LOG.debug("Routing GET user by id: {}", slingRequest.getRequestURI());
    UserByIdServlet.doGet(slingRequest, slingResponse, userRepository);
  }
}
