package io.github.ericmschoi.theworldthatday;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock that tests move by hand, instead of sleeping (plan M3a1 test policy 5). */
public final class MutableClock extends Clock {

  private volatile Instant now;
  private final ZoneId zone;

  public MutableClock(Instant start, ZoneId zone) {
    this.now = start;
    this.zone = zone;
  }

  public void advance(Duration duration) {
    now = now.plus(duration);
  }

  @Override
  public Instant instant() {
    return now;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId other) {
    return new MutableClock(now, other);
  }
}
