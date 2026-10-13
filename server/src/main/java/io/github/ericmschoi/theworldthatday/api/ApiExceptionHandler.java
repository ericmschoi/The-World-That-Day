package io.github.ericmschoi.theworldthatday.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders {@link ApiProblem} as {@code application/problem+json} (RFC 9457) with the extension
 * field {@code code}. {@code instance} is the request path without the query string. Nothing is
 * logged here: 400s are not logged, and 503s are logged by the service with source and state only.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  /** Written out although RFC 9457 makes it the default, as in the plan's examples. */
  private static final URI ABOUT_BLANK = URI.create("about:blank");

  @ExceptionHandler(ApiProblem.class)
  ResponseEntity<ProblemDetail> handle(ApiProblem problem, HttpServletRequest request) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(problem.status(), problem.getMessage());
    body.setType(ABOUT_BLANK);
    body.setTitle(problem.status().getReasonPhrase());
    body.setInstance(URI.create(request.getRequestURI()));
    body.setProperty("code", problem.code());
    return ResponseEntity.status(problem.status()).body(body);
  }
}
