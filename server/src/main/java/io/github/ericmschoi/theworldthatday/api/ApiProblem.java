package io.github.ericmschoi.theworldthatday.api;

import org.springframework.http.HttpStatus;

/**
 * An error answered in the project error format (plan M3a2): ProblemDetail plus {@code code}. The
 * {@code detail} is a fixed English sentence and never contains request values.
 */
public class ApiProblem extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public ApiProblem(HttpStatus status, String code, String detail) {
    super(detail, null, false, false);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
