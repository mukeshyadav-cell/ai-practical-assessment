package com.mysite.core.servlets;

import com.mysite.core.services.TicketService;
import com.mysite.core.servlets.util.ServletPathUtil;
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
 * Routes {@code GET} and {@code PUT /bin/api/v1/tickets/{id}} to {@link TicketByIdServlet}
 * handlers when Sling suffix servlet registration does not match sub-paths under {@code /bin/}
 * (observed on local AEM SDK).
 * <p>
 * {@link TicketCollectionServlet} continues to handle the collection path
 * ({@code /bin/api/v1/tickets}) via {@code sling.servlet.paths}. This filter only intercepts
 * single-segment ticket id paths (not {@code /assignee}, {@code /status}, {@code /comments}).
 * </p>
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_PATTERN + "=" + ServletConstants.TICKETS_PATH + "/[^/]+$"
    })
@ServiceDescription("REST API — route ticket by-id GET/PUT to TicketByIdServlet handlers")
public class TicketByIdRoutingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(TicketByIdRoutingFilter.class);

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

    String ticketId = ServletPathUtil.resolveTicketId(slingRequest, ServletConstants.TICKETS_PATH);
    if (!ServletPathUtil.isTicketIdOnly(ticketId)) {
      chain.doFilter(request, response);
      return;
    }

    String method = slingRequest.getMethod();
    if ("GET".equalsIgnoreCase(method)) {
      LOG.debug("Routing GET ticket by id: {}", ticketId);
      TicketByIdServlet.doGet(slingRequest, slingResponse, ticketService);
      return;
    }
    if ("PUT".equalsIgnoreCase(method)) {
      LOG.debug("Routing PUT ticket by id: {}", ticketId);
      TicketByIdServlet.doPut(slingRequest, slingResponse, ticketService);
      return;
    }

    chain.doFilter(request, response);
  }
}
