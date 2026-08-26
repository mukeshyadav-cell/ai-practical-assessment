package com.mysite.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysite.core.dto.TicketDTO;
import com.mysite.core.services.TicketService;
import com.mysite.core.servlets.util.ServletPathUtil;
import com.mysite.core.servlets.util.ServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request handlers for {@code GET} and {@code PUT /bin/api/v1/tickets/{id}}.
 * <p>
 * Invoked by {@link TicketByIdRoutingFilter} when Sling suffix servlet registration does not
 * route sub-paths under {@code /bin/} on the local AEM SDK.
 * </p>
 */
public final class TicketByIdServlet {

  private static final Logger LOG = LoggerFactory.getLogger(TicketByIdServlet.class);

  private static final String MESSAGE_TICKET_ID_REQUIRED = "Ticket id is required";
  private static final String MESSAGE_STATUS_NOT_ALLOWED = "status cannot be updated on this endpoint";
  private static final String MESSAGE_ASSIGNEE_NOT_ALLOWED = "assignedTo cannot be updated on this endpoint";

  private TicketByIdServlet() {
  }

  /**
   * Returns a single ticket by business id.
   */
  public static void doGet(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      TicketService ticketService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      TicketDTO ticket = ticketService.getTicket(ticketId);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, ticket);
    });
  }

  /**
   * Updates mutable ticket fields ({@code title}, {@code description}, {@code priority}).
   */
  public static void doPut(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      TicketService ticketService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      TicketDTO changes = parseUpdateBody(request, response);
      if (changes == null) {
        return;
      }

      TicketDTO updated = ticketService.updateTicket(ticketId, changes);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, updated);
    });
  }

  /**
   * @return ticket id, or {@code null} when the response has already been written
   */
  private static String requireTicketId(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String ticketId = ServletPathUtil.resolveTicketId(request, ServletConstants.TICKETS_PATH);
    if (!ServletPathUtil.isTicketIdOnly(ticketId)) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_TICKET_ID_REQUIRED);
      return null;
    }
    return ticketId;
  }

  /**
   * @return change DTO, or {@code null} when the response has already been written
   */
  private static TicketDTO parseUpdateBody(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    JsonNode body = ServletResponseUtil.getObjectMapper().readTree(request.getReader());

    if (body.has("status")) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_STATUS_NOT_ALLOWED);
      return null;
    }
    if (body.has("assignedTo")) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_ASSIGNEE_NOT_ALLOWED);
      return null;
    }

    return ServletResponseUtil.getObjectMapper().treeToValue(body, TicketDTO.class);
  }
}
