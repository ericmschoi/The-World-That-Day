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
    /**
     * A 429 (or a 503 with Retry-After) was received, or the host is still inside its Retry-After
     * window or a {@link WikimediaHttpClient#blockHost} window.
     */
    RATE_LIMITED,
    /**
     * No request slot ({@code wikimedia.max-concurrent-requests}) was free within the read timeout;
     * no request was sent.
     */
    BUSY,
    TIMEOUT,
    HTTP_ERROR,
    IO_ERROR,
    /** The URI has no host; no request was sent. */
    INVALID_URI
  }
}
