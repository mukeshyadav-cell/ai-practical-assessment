package com.mysite.core.servlets;

/**
 * Shared constants for REST servlet registration and HTTP conventions.
 */
public final class ServletConstants {

  /** REST API base path per api-contract.md. */
  public static final String API_BASE = "/bin/api/v1";

  /** Ticket collection servlet path ({@code GET}, {@code POST}). */
  public static final String TICKETS_PATH = API_BASE + "/tickets";

  /** User collection servlet path ({@code GET}). */
  public static final String USERS_PATH = API_BASE + "/users";

  /** Current user servlet path ({@code GET}). */
  public static final String ME_PATH = API_BASE + "/me";

  /**
   * Ticket suffix servlet base path ({@code GET}, {@code PUT}, etc. on {@code /tickets/{id}}).
   * Trailing slash enables Sling suffix routing without conflicting with {@link #TICKETS_PATH}.
   */
  public static final String TICKETS_SUFFIX_PATH = TICKETS_PATH + "/";

  /** Assignee sub-resource segment ({@code PUT /tickets/{id}/assignee}). */
  public static final String ASSIGNEE_SUB_RESOURCE = "assignee";

  /** Status sub-resource segment ({@code PUT /tickets/{id}/status}). */
  public static final String STATUS_SUB_RESOURCE = "status";

  /** Comments sub-resource segment ({@code GET, POST /tickets/{id}/comments}). */
  public static final String COMMENTS_SUB_RESOURCE = "comments";

  /** JSON request/response field for assignee user id. */
  public static final String FIELD_ASSIGNED_TO = "assignedTo";

  /** JSON request/response field for ticket status label. */
  public static final String FIELD_STATUS = "status";

  /** JSON response content type. */
  public static final String CONTENT_TYPE_JSON = "application/json";

  /** Response character encoding. */
  public static final String CHARSET_UTF8 = "UTF-8";

  private ServletConstants() {
  }
}
