package com.mysite.core.servlets.util;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Helpers for parsing Sling path suffixes on REST servlets.
 */
public final class ServletPathUtil {

  private ServletPathUtil() {
  }

  /**
   * Returns the request path suffix with a leading {@code /} stripped.
   *
   * @param request Sling request
   * @return normalized suffix, or {@code null} when absent or blank
   */
  public static String getSuffix(SlingHttpServletRequest request) {
    if (request.getRequestPathInfo() == null) {
      return null;
    }
    String suffix = request.getRequestPathInfo().getSuffix();
    if (suffix == null || suffix.isBlank()) {
      return null;
    }
    if (suffix.startsWith("/")) {
      return suffix.substring(1);
    }
    return suffix;
  }

  /**
   * Resolves a ticket business id from the Sling suffix or request URI.
   * <p>
   * Sling suffix routing ({@code sling.servlet.paths=/bin/api/v1/tickets/}) is preferred per
   * api-contract.md. When the runtime does not populate {@link #getSuffix(SlingHttpServletRequest)}
   * (observed on local AEM SDK for {@code /bin/api/v1/tickets/{id}}), this method falls back to
   * parsing the path segment after {@code /bin/api/v1/tickets/}.
   * </p>
   *
   * @param request      Sling request
   * @param ticketsPath  collection path without trailing slash (e.g. {@code /bin/api/v1/tickets})
   * @return ticket id when the request targets a single ticket resource; otherwise {@code null}
   */
  public static String resolveTicketId(SlingHttpServletRequest request, String ticketsPath) {
    String suffix = getSuffix(request);
    if (isTicketIdOnly(suffix)) {
      return suffix;
    }

    String uri = request.getRequestURI();
    if (uri == null || uri.isBlank()) {
      return null;
    }

    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }

    String prefix = ticketsPath + "/";
    if (!uri.startsWith(prefix)) {
      return null;
    }

    String remainder = uri.substring(prefix.length());
    if (remainder.contains("?")) {
      remainder = remainder.substring(0, remainder.indexOf('?'));
    }
    if (isTicketIdOnly(remainder)) {
      return remainder;
    }
    return null;
  }

  /**
   * @return {@code true} when {@code suffix} is a single ticket id segment (no sub-path)
   */
  public static boolean isTicketIdOnly(String suffix) {
    return suffix != null && !suffix.isBlank() && !suffix.contains("/");
  }

  /**
   * Returns the path remainder after {@code {ticketsPath}/}, from Sling suffix or request URI.
   *
   * @param request     Sling request
   * @param ticketsPath collection path without trailing slash
   * @return remainder (e.g. {@code TKT-1001} or {@code TKT-1001/assignee}), or {@code null}
   */
  public static String resolveRemainderAfterTickets(SlingHttpServletRequest request, String ticketsPath) {
    String suffix = getSuffix(request);
    if (suffix != null && !suffix.isBlank()) {
      return suffix;
    }

    String uri = request.getRequestURI();
    if (uri == null || uri.isBlank()) {
      return null;
    }

    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }

    String prefix = ticketsPath + "/";
    if (!uri.startsWith(prefix)) {
      return null;
    }

    String remainder = uri.substring(prefix.length());
    if (remainder.contains("?")) {
      remainder = remainder.substring(0, remainder.indexOf('?'));
    }
    return remainder.isBlank() ? null : remainder;
  }

  /**
   * Resolves a ticket id when the request targets {@code /tickets/{id}/{subResource}}.
   *
   * @param request       Sling request
   * @param ticketsPath   collection path without trailing slash
   * @param subResource   expected sub-resource segment (e.g. {@code assignee})
   * @return ticket business id, or {@code null} when the path does not match
   */
  public static String resolveTicketIdForSubResource(
      SlingHttpServletRequest request,
      String ticketsPath,
      String subResource) {
    String remainder = resolveRemainderAfterTickets(request, ticketsPath);
    if (remainder == null || subResource == null || subResource.isBlank()) {
      return null;
    }

    String expectedSuffix = "/" + subResource;
    if (!remainder.endsWith(expectedSuffix)) {
      return null;
    }

    String ticketId = remainder.substring(0, remainder.length() - expectedSuffix.length());
    if (ticketId.isBlank() || ticketId.contains("/")) {
      return null;
    }
    return ticketId;
  }

  /**
   * Resolves a user id from the Sling suffix or request URI for {@code GET /users/{userId}}.
   *
   * @param request   Sling request
   * @param usersPath collection path without trailing slash (e.g. {@code /bin/api/v1/users})
   * @return user id when the request targets a single user resource; otherwise {@code null}
   */
  public static String resolveUserId(SlingHttpServletRequest request, String usersPath) {
    String suffix = getSuffix(request);
    if (isTicketIdOnly(suffix)) {
      return suffix;
    }

    String uri = request.getRequestURI();
    if (uri == null || uri.isBlank()) {
      return null;
    }

    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }

    String prefix = usersPath + "/";
    if (!uri.startsWith(prefix)) {
      return null;
    }

    String remainder = uri.substring(prefix.length());
    if (remainder.contains("?")) {
      remainder = remainder.substring(0, remainder.indexOf('?'));
    }
    if (isTicketIdOnly(remainder)) {
      return remainder;
    }
    return null;
  }
}
