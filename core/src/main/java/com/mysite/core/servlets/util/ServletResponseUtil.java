package com.mysite.core.servlets.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mysite.core.exception.DomainException;
import com.mysite.core.exception.InvalidTransitionException;
import com.mysite.core.exception.TicketNotEditableException;
import com.mysite.core.exception.TicketNotFoundException;
import com.mysite.core.exception.UnknownUserException;
import com.mysite.core.exception.ValidationException;
import com.mysite.core.servlets.ServletConstants;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Shared JSON serialization and domain-error mapping for REST servlets.
 * <p>
 * Uses a single {@link ObjectMapper} with JSR-310 support so {@link java.time.Instant}
 * fields on DTOs serialize as ISO-8601 strings.
 * </p>
 * <p>
 * <strong>api-contract.md error catalog (single source of truth):</strong>
 * </p>
 * <table border="1">
 *   <caption>Error code to HTTP status mapping</caption>
 *   <tr><th>code</th><th>HTTP</th><th>Source</th></tr>
 *   <tr><td>{@code VALIDATION_ERROR}</td><td>400</td><td>{@link ValidationException}; malformed JSON; servlet field checks</td></tr>
 *   <tr><td>{@code UNKNOWN_USER}</td><td>400</td><td>{@link UnknownUserException}</td></tr>
 *   <tr><td>{@code TICKET_NOT_EDITABLE}</td><td>400</td><td>{@link TicketNotEditableException}</td></tr>
 *   <tr><td>{@code NOT_FOUND}</td><td>404</td><td>{@link TicketNotFoundException}; unknown user id</td></tr>
 *   <tr><td>{@code INVALID_TRANSITION}</td><td>409</td><td>{@link InvalidTransitionException}</td></tr>
 *   <tr><td>{@code INTERNAL_ERROR}</td><td>500</td><td>Any other non-domain exception</td></tr>
 * </table>
 * <p>
 * All error responses use {@code {"error": "<message>", "code": "<code>"}}.
 * </p>
 */
public final class ServletResponseUtil {

  public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
  public static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String CODE_NOT_FOUND = "NOT_FOUND";
  public static final String MESSAGE_INTERNAL_ERROR = "An unexpected error occurred";
  public static final String MESSAGE_MALFORMED_BODY = "Malformed request body";

  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  private ServletResponseUtil() {
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /**
   * @return shared Jackson mapper for servlet request/response JSON
   */
  public static ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
  }

  /**
   * Runs servlet logic and maps any thrown exception to a JSON error response.
   *
   * @param response Sling response
   * @param log      servlet logger
   * @param action   request handler logic
   */
  public static void execute(SlingHttpServletResponse response, Logger log, ServletAction action)
      throws IOException {
    try {
      action.run();
    } catch (Exception e) {
      handleException(response, log, e);
    }
  }

  /**
   * Writes a JSON response with the given HTTP status.
   *
   * @param resp   Sling response
   * @param status HTTP status code
   * @param body   object to serialize; may be {@code null} for empty body
   */
  public static void writeJson(SlingHttpServletResponse resp, int status, Object body) throws IOException {
    resp.setContentType(ServletConstants.CONTENT_TYPE_JSON);
    resp.setCharacterEncoding(ServletConstants.CHARSET_UTF8);
    resp.setStatus(status);
    if (body != null) {
      OBJECT_MAPPER.writeValue(resp.getWriter(), body);
    }
  }

  /**
   * Writes the standard api-contract error JSON body.
   *
   * @param resp    Sling response
   * @param status  HTTP status code
   * @param code    machine-readable error code
   * @param message human-readable error message
   */
  public static void writeError(SlingHttpServletResponse resp, int status, String code, String message)
      throws IOException {
    writeJson(resp, status, new ErrorResponse(message, code));
  }

  /**
   * Writes a {@code 400 VALIDATION_ERROR} response and logs at warn level.
   */
  public static void writeValidationError(
      SlingHttpServletResponse resp,
      Logger log,
      String message) throws IOException {
    log.warn("Client error [{}]: {}", CODE_VALIDATION_ERROR, message);
    writeError(resp, 400, CODE_VALIDATION_ERROR, message);
  }

  /**
   * Writes a {@code 404 NOT_FOUND} response and logs at warn level.
   */
  public static void writeNotFoundError(
      SlingHttpServletResponse resp,
      Logger log,
      String message) throws IOException {
    log.warn("Client error [{}]: {}", CODE_NOT_FOUND, message);
    writeError(resp, 404, CODE_NOT_FOUND, message);
  }

  /**
   * Maps a {@link DomainException} to HTTP status and error code using
   * {@link DomainException#errorCode()} and {@link DomainException#httpStatus()}.
   *
   * @param exception domain exception from the service layer
   * @return tuple of HTTP status, error code, and message
   */
  public static DomainErrorMapping mapDomainException(DomainException exception) {
    return new DomainErrorMapping(
        exception.httpStatus(),
        exception.errorCode(),
        exception.getMessage());
  }

  /**
   * Handles an exception by writing the appropriate JSON error response.
   * <p>
   * Order of handling:
   * </p>
   * <ol>
   *   <li>Malformed JSON ({@link JsonProcessingException} in cause chain) → {@code 400 VALIDATION_ERROR}</li>
   *   <li>{@link DomainException} → {@code errorCode()} / {@code httpStatus()}</li>
   *   <li>All other exceptions → {@code 500 INTERNAL_ERROR} (stack trace logged server-side only)</li>
   * </ol>
   *
   * @param resp Sling response
   * @param log  servlet logger
   * @param e    caught exception
   */
  public static void handleException(SlingHttpServletResponse resp, Logger log, Exception e) throws IOException {
    if (isMalformedJson(e)) {
      log.warn("Malformed JSON request: {}", e.getMessage());
      writeError(resp, 400, CODE_VALIDATION_ERROR, MESSAGE_MALFORMED_BODY);
      return;
    }

    if (e instanceof DomainException) {
      DomainException domainException = (DomainException) e;
      DomainErrorMapping mapping = mapDomainException(domainException);
      if (mapping.httpStatus() >= 500) {
        log.error("Domain error [{}]: {}", mapping.code(), mapping.message(), e);
      } else {
        log.warn("Client error [{}]: {}", mapping.code(), mapping.message());
      }
      writeError(resp, mapping.httpStatus(), mapping.code(), mapping.message());
      return;
    }

    log.error("Unhandled servlet error", e);
    writeError(resp, 500, CODE_INTERNAL_ERROR, MESSAGE_INTERNAL_ERROR);
  }

  /**
   * @return {@code true} when the cause chain includes a Jackson {@link JsonProcessingException}
   */
  public static boolean isMalformedJson(Exception e) {
    Throwable current = e;
    while (current != null) {
      if (current instanceof JsonProcessingException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  /**
   * Servlet request handler that may throw checked or unchecked exceptions.
   */
  @FunctionalInterface
  public interface ServletAction {
    void run() throws Exception;
  }

  /**
   * HTTP status, error code, and message for a domain error response.
   */
  public static final class DomainErrorMapping {

    private final int httpStatus;
    private final String code;
    private final String message;

    public DomainErrorMapping(int httpStatus, String code, String message) {
      this.httpStatus = httpStatus;
      this.code = code;
      this.message = message;
    }

    public int httpStatus() {
      return httpStatus;
    }

    public String code() {
      return code;
    }

    public String message() {
      return message;
    }
  }
}
