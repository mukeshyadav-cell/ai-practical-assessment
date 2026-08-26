package com.mysite.core.servlets;

import com.mysite.core.services.CommentService;
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
 * Routes {@code GET} and {@code POST /bin/api/v1/tickets/{id}/comments} to
 * {@link CommentCollectionServlet} handlers (same filter-dispatch pattern as other ticket
 * sub-resources on local AEM SDK).
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_PATTERN + "=" + ServletConstants.TICKETS_PATH
            + "/[^/]+/" + ServletConstants.COMMENTS_SUB_RESOURCE + "$"
    })
@ServiceDescription("REST API — route ticket comments GET/POST to CommentCollectionServlet handler")
public class CommentCollectionRoutingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(CommentCollectionRoutingFilter.class);

  @Reference
  private CommentService commentService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof SlingHttpServletRequest) || !(response instanceof SlingHttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

    String method = slingRequest.getMethod();
    if ("GET".equalsIgnoreCase(method)) {
      LOG.debug("Routing GET ticket comments: {}", slingRequest.getRequestURI());
      CommentCollectionServlet.doGet(slingRequest, slingResponse, commentService);
      return;
    }
    if ("POST".equalsIgnoreCase(method)) {
      LOG.debug("Routing POST ticket comments: {}", slingRequest.getRequestURI());
      CommentCollectionServlet.doPost(slingRequest, slingResponse, commentService);
      return;
    }

    chain.doFilter(request, response);
  }
}
