package com.mysite.core.servlets.util;

/**
 * JSON error body shape per api-contract.md: {@code {"error": "...", "code": "..."}}.
 */
public class ErrorResponse {

  private final String error;
  private final String code;

  /**
   * @param error human-readable message
   * @param code  stable machine-readable error code
   */
  public ErrorResponse(String error, String code) {
    this.error = error;
    this.code = code;
  }

  public String getError() {
    return error;
  }

  public String getCode() {
    return code;
  }
}
