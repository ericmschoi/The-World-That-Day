package io.github.ericmschoi.theworldthatday.wikimedia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers, per host, until when a 429 asked us to stop. Requests never sleep inside a call; a
 * blocked host is simply skipped until the window ends.
 */
final class HostRateLimiter {

  static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);

  private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();
  private final Clock clock;
  private final Duration maxWait;

  HostRateLimiter(Clock clock, Duration maxWait) {
    this.clock = clock;
    this.maxWait = maxWait;
  }

  boolean isBlocked(String host) {
    Instant until = blockedUntil.get(host);
    return until != null && clock.instant().isBefore(until);
  }

  void block(String host, String retryAfter) {
    Instant until = clock.instant().plus(waitFor(retryAfter));
    blockedUntil.merge(host, until, (old, next) -> old.isAfter(next) ? old : next);
  }

  /** Retry-After in seconds, capped at the max wait. Missing or not a number: 30 seconds. */
  private Duration waitFor(String retryAfter) {
    String value = retryAfter == null ? "" : retryAfter.strip();
    if (!value.matches("\\d+")) {
      return DEFAULT_WAIT;
    }
    Duration wait =
        value.length() > 9 ? maxWait : Duration.ofSeconds(Long.parseLong(value)); // no overflow
    return wait.compareTo(maxWait) > 0 ? maxWait : wait;
  }
}
