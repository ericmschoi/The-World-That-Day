package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HostRateLimiterTest {

  private static final String HOST = "ko.example.test";

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final HostRateLimiter limiter = new HostRateLimiter(clock, Duration.ofSeconds(120));

  @Test
  void unknownHostIsNotBlocked() {
    assertThat(limiter.isBlocked(HOST)).isFalse();
  }

  @ParameterizedTest(name = "Retry-After [{0}] blocks for {1}s")
  @CsvSource(
      value = {
        "60, 60", // header value in seconds
        "999, 120", // capped at rate-limit-max-wait
        "99999999999999999999, 120", // too large for a long: still the cap
        "NULL, 30", // header missing
        "'', 30", // header empty
        "'Wed, 21 Oct 2026 07:28:00 GMT', 30", // HTTP-date is not used by Wikimedia
        "0, 0"
      },
      nullValues = "NULL")
  void blocksUntilTheWindowEnds(String retryAfter, long seconds) {
    limiter.block(HOST, retryAfter);

    if (seconds > 0) {
      clock.advance(Duration.ofSeconds(seconds - 1));
      assertThat(limiter.isBlocked(HOST)).as("one second before the end").isTrue();
      clock.advance(Duration.ofSeconds(1));
    }
    assertThat(limiter.isBlocked(HOST)).as("at the end").isFalse();
  }

  @Test
  void blockIsPerHost() {
    limiter.block(HOST, "60");

    assertThat(limiter.isBlocked("commons.example.test")).isFalse();
  }

  @Test
  void shorterLaterBlockDoesNotShortenTheWindow() {
    limiter.block(HOST, "60");
    limiter.block(HOST, "5");

    clock.advance(Duration.ofSeconds(59));
    assertThat(limiter.isBlocked(HOST)).isTrue();
  }
}
