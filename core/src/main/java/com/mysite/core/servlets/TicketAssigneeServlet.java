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
 * Request handler for {@code PUT /bin/api/v1/tickets/{id}/assignee}.
 * <p>
 * Invoked by {@link TicketAssigneeRoutingFilter}. Changes {@code assignedTo} only via
 * {@link TicketService#reassignTicket(String, String)}.
 * </p>
 */
public final class TicketAssigneeServlet {

  private static final Logger LOG = LoggerFactory.getLogger(TicketAssigneeServlet.class);

  private static final String MESSAGE_INVALID_PATH = "Ticket id and assignee sub-resource are required";

  private TicketAssigneeServlet() {
  }

  /**
   * Reassigns a ticket to the user id in the request body {@code assignedTo} field.
   */
  public static void doPut(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      TicketService ticketService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireAssigneeTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      String assignedTo = parseAssignedTo(request, response);
      if (assignedTo == null) {
        return;
      }

      TicketDTO updated = ticketService.reassignTicket(ticketId, assignedTo);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, updated);
    });
  }

  /**
   * @return ticket id, or {@code null} when the response has already been written
   */
  private static String requireAssigneeTicketId(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String ticketId = ServletPathUtil.resolveTicketIdForSubResource(
        request,
        ServletConstants.TICKETS_PATH,
        ServletConstants.ASSIGNEE_SUB_RESOURCE);
    if (ticketId == null) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_INVALID_PATH);
      return null;
    }
    return ticketId;
  }

  /**
   * Parses {@code assignedTo} from the request body per api-contract.md.
   *
   * @return assignee user id, or {@code null} when the response has already been written
   */
  private static String parseAssignedTo(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    JsonNode body = ServletResponseUtil.getObjectMapper().readTree(request.getReader());
    JsonNode assignedToNode = body.get(ServletConstants.FIELD_ASSIGNED_TO);

    if (assignedToNode == null || assignedToNode.isNull()) {
      ServletResponseUtil.writeValidationError(
          response,
          LOG,
          ServletConstants.FIELD_ASSIGNED_TO + ": is required");
      return null;
    }

    return assignedToNode.asText();
  }
}
