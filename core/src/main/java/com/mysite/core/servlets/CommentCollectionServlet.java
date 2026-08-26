package com.mysite.core.servlets;

import com.mysite.core.dto.CommentDTO;
import com.mysite.core.services.CommentService;
import com.mysite.core.servlets.util.ServletPathUtil;
import com.mysite.core.servlets.util.ServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * Request handlers for comment collection on a ticket.
 * <p>
 * Invoked by {@link CommentCollectionRoutingFilter}.
 * </p>
 * <ul>
 *   <li>{@code GET /bin/api/v1/tickets/{id}/comments} — list comments</li>
 *   <li>{@code POST /bin/api/v1/tickets/{id}/comments} — add comment ({@code 201})</li>
 * </ul>
 * <p>
 * Comments are allowed for tickets in any status, including terminal {@code Closed} and
 * {@code Cancelled} states.
 * </p>
 */
public final class CommentCollectionServlet {

  private static final Logger LOG = LoggerFactory.getLogger(CommentCollectionServlet.class);

  private static final String MESSAGE_INVALID_PATH = "Ticket id and comments sub-resource are required";

  private CommentCollectionServlet() {
  }

  /**
   * Lists all comments for a ticket, ordered by {@code createdAt} ascending.
   */
  public static void doGet(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      CommentService commentService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireCommentsTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      List<CommentDTO> comments = commentService.listComments(ticketId);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_OK, comments);
    });
  }

  /**
   * Adds a comment to a ticket. Path {@code ticketId} is authoritative over any body field.
   */
  public static void doPost(
      SlingHttpServletRequest request,
      SlingHttpServletResponse response,
      CommentService commentService) throws IOException {
    ServletResponseUtil.execute(response, LOG, () -> {
      String ticketId = requireCommentsTicketId(request, response);
      if (ticketId == null) {
        return;
      }

      CommentDTO comment = ServletResponseUtil.getObjectMapper().readValue(request.getReader(), CommentDTO.class);
      comment.setTicketId(ticketId);
      comment.setCreatedBy(resolveCreatedBy(request, comment));

      CommentDTO created = commentService.addComment(ticketId, comment);
      ServletResponseUtil.writeJson(response, HttpServletResponse.SC_CREATED, created);
    });
  }

  /**
   * @return ticket id, or {@code null} when the response has already been written
   */
  private static String requireCommentsTicketId(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    String ticketId = ServletPathUtil.resolveTicketIdForSubResource(
        request,
        ServletConstants.TICKETS_PATH,
        ServletConstants.COMMENTS_SUB_RESOURCE);
    if (ticketId == null) {
      ServletResponseUtil.writeValidationError(response, LOG, MESSAGE_INVALID_PATH);
      return null;
    }
    return ticketId;
  }

  /**
   * Resolves {@code createdBy} from the request body when present; otherwise uses the
   * authenticated session user per api-contract.md.
   */
  private static String resolveCreatedBy(SlingHttpServletRequest request, CommentDTO comment) {
    if (comment.getCreatedBy() != null && !comment.getCreatedBy().isBlank()) {
      return comment.getCreatedBy();
    }
    String remoteUser = request.getRemoteUser();
    if (remoteUser != null && !remoteUser.isBlank()) {
      return remoteUser;
    }
    Principal principal = request.getUserPrincipal();
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
      return principal.getName();
    }
    return comment.getCreatedBy();
  }
}
