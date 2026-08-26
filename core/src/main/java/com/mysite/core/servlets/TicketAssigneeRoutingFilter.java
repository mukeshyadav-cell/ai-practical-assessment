package com.mysite.core.servlets;

import com.mysite.core.services.TicketService;
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
 * Routes {@code PUT /bin/api/v1/tickets/{id}/assignee} to {@link TicketAssigneeServlet}
 * handlers (same filter-dispatch pattern as {@link TicketByIdRoutingFilter} on local AEM SDK).
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_PATTERN + "=" + ServletConstants.TICKETS_PATH
            + "/[^/]+/" + ServletConstants.ASSIGNEE_SUB_RESOURCE + "$"
    })
@ServiceDescription("REST API — route ticket assignee PUT to TicketAssigneeServlet handler")
public class TicketAssigneeRoutingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(TicketAssigneeRoutingFilter.class);

  @Reference
  private TicketService ticketService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof SlingHttpServletRequest) || !(response instanceof SlingHttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

    if (!"PUT".equalsIgnoreCase(slingRequest.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    LOG.debug("Routing PUT ticket assignee: {}", slingRequest.getRequestURI());
    TicketAssigneeServlet.doPut(slingRequest, slingResponse, ticketService);
  }
}
