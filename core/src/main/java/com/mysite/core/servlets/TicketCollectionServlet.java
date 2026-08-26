package com.mysite.core.servlets;

import com.mysite.core.dto.TicketDTO;
import com.mysite.core.services.TicketService;
import com.mysite.core.servlets.util.ServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * REST servlet for the ticket collection resource.
 * <p>
 * Registered on {@link ServletConstants#TICKETS_PATH} (exact match). By-id
 * {@code GET}/{@code PUT} requests are dispatched by {@link TicketByIdRoutingFilter} because
 * Sling suffix servlet registration for {@code /bin/api/v1/tickets/{id}} is not reliable on
 * the local AEM SDK (see routing notes in Task 5.1.2).
 * </p>
 * <ul>
 *   <li>{@code GET /bin/api/v1/tickets} — list (optional {@code ?status=}, {@code ?q=})</li>
 *   <li>{@code POST /bin/api/v1/tickets} — create ({@code 201})</li>
 * </ul>
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=" + ServletConstants.TICKETS_PATH,
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    })
@ServiceDescription("REST API — list and create support tickets")
public class TicketCollectionServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(TicketCollectionServlet.class);

  @Reference
  private TicketService ticketService;

  /**
   * Lists tickets with optional status filter and title keyword search.
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String status = request.getParameter("status");
      String query = request.getParameter("q");

      List<TicketDTO> tickets = ticketService.listTickets(status, query);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, tickets);
    });
  }

  /**
   * Creates a new ticket; {@code createdBy} is taken from the authenticated session user.
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      TicketDTO ticket = ServletResponseUtil.getObjectMapper().readValue(request.getReader(), TicketDTO.class);
      ticket.setCreatedBy(resolveCurrentUser(request));

      TicketDTO created = ticketService.createTicket(ticket);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_CREATED, created);
    });
  }

  /**
   * Resolves the AEM session user id for {@code createdBy} (api-contract: derived from login).
   *
   * @return authenticated user id, or {@code null} when anonymous
   */
  private String resolveCurrentUser(SlingHttpServletRequest request) {
    String remoteUser = request.getRemoteUser();
    if (remoteUser != null && !remoteUser.isBlank()) {
      return remoteUser;
    }
    Principal principal = request.getUserPrincipal();
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
      return principal.getName();
    }
    return null;
  }
}
