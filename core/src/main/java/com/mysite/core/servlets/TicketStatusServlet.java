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
 * Request handler for {@code PUT /bin/api/v1/tickets/{id}/status}.
 * <p>
 * Invoked by {@link TicketStatusRoutingFilter}. Status changes go only through
 * {@link TicketService#changeStatus(String, String)}, which delegates to
 * {@link com.mysite.core.statemachine.TicketStateMachine}.
 * </p>
 */
public final class TicketStatusServlet {

  private static final Logger LOG = LoggerFactory.getLogger(TicketStatusServlet.class);

  private static final String MESSAGE_INVALID_PATH = "Ticket id and status sub-resource are required";

  private TicketStatusServlet() {
  }

  /**
   * Applies a state-machine-valid status transition to the ticket identified by the path.
   */
  public static void doPut(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      TicketService ticketService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireStatusTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      String statusLabel = parseStatusLabel(request, response);
      if (statusLabel == null) {
        return;
      }

      LOG.info("Status change request for ticket {} to {}", ticketId, statusLabel);
      TicketDTO updated = ticketService.changeStatus(ticketId, statusLabel);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, updated);
    });
  }

  /**
   * @return ticket id, or {@code null} when the response has already been written
   */
  private static String requireStatusTicketId(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String ticketId = ServletPathUtil.resolveTicketIdForSubResource(
        request,
        ServletConstants.TICKETS_PATH,
        ServletConstants.STATUS_SUB_RESOURCE);
    if (ticketId == null) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_INVALID_PATH);
      return null;
    }
    return ticketId;
  }

  /**
   * Parses the target {@code status} label from the request body per api-contract.md.
   *
   * @return status label, or {@code null} when the response has already been written
   */
  private static String parseStatusLabel(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    JsonNode body = ServletResponseUtil.getObjectMapper().readTree(request.getReader());
    JsonNode statusNode = body.get(ServletConstants.FIELD_STATUS);

    if (statusNode == null || statusNode.isNull() || statusNode.asText().isBlank()) {
      ServletResponseUtil.writeValidationError(
          response,
          LOG,
          ServletConstants.FIELD_STATUS + ": is required");
      return null;
    }

    return statusNode.asText();
  }
}
