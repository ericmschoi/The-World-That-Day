package io.github.ericmschoi.theworldthatday.wikimedia;

/** Outcome of one Wikimedia GET. Failures are values, never exceptions. */
public sealed interface WikimediaResponse {

  /** A 2xx response and its body. */
  record Ok(String body) implements WikimediaResponse {}

  /** Nothing usable: the caller leaves its section empty. */
  record Unavailable(Reason reason) implements WikimediaResponse {}

  enum Reason {
    /** {@code wikimedia.user-agent} is empty; no request was sent. */
    DISABLED,
    /** A 429 was received, or the host is still inside its Retry-After window. */
    RATE_LIMITED,
    TIMEOUT,
    HTTP_ERROR,
    IO_ERROR
  }
}
