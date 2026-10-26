package io.github.ericmschoi.theworldthatday.media.youtube;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The two daily YouTube quota buckets (plan M3a5 "쿼터"): {@link Bucket#SEARCH} for {@code
 * search.list} calls and {@link Bucket#UNITS} for the other calls, capped by {@code
 * youtube.daily-search-budget} and {@code youtube.daily-unit-budget}. A caller reserves what one
 * lookup needs before it sends a request; the reservation is all or nothing and is not given back
 * on an error or a timeout (the API counts a bad request too). After a 403 {@code quotaExceeded}
 * the caller marks the bucket {@link #exhausted}, which refuses reservations until the reset.
 *
 * <p>Google resets its quota at midnight in {@code America/Los_Angeles}, so the counts and the
 * exhausted marks clear when that date changes on the injected clock (daylight saving included).
 * The counts live in memory only: a restart starts from 0 and the 403 handling backs this up (a
 * restart on the same Pacific day can spend the 20% margin below Google's limit). Used by {@link
 * YoutubeVideoSource} (plan M3a5a T5).
 */
@Component
public class YoutubeQuota {

  /** The two quota buckets; {@code label} is the name in the log line. */
  public enum Bucket {
    SEARCH("search"),
    UNITS("units");

    private final String label;

    Bucket(String label) {
      this.label = label;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(YoutubeQuota.class);
  private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

  private final Clock clock;
  private final int[] caps = new int[Bucket.values().length];
  private final int[] used = new int[Bucket.values().length];
  private final boolean[] exhausted = new boolean[Bucket.values().length];
  private final boolean[] warned = new boolean[Bucket.values().length];
  private LocalDate day;

  public YoutubeQuota(YoutubeProperties properties, Clock clock) {
    this.clock = clock;
    caps[Bucket.SEARCH.ordinal()] = properties.dailySearchBudget();
    caps[Bucket.UNITS.ordinal()] = properties.dailyUnitBudget();
    this.day = today();
  }

  /**
   * Takes {@code n} (at least 1) from the bucket when {@code used + n} stays within the cap and the
   * bucket is not marked exhausted; otherwise takes nothing and returns false.
   */
  public synchronized boolean reserve(Bucket bucket, int n) {
    if (n < 1) {
      throw new IllegalArgumentException("n must be at least 1, got: " + n);
    }
    rollOver();
    int i = bucket.ordinal();
    if (exhausted[i] || (long) used[i] + n > caps[i]) {
      return false;
    }
    used[i] += n;
    if (used[i] == caps[i]) {
      warnOnce(bucket);
    }
    return true;
  }

  /**
   * {@link #reserve}, giving the Pacific day the units were taken on, or {@code null} if refused.
   */
  public synchronized LocalDate reserveOn(Bucket bucket, int n) {
    return reserve(bucket, n) ? day : null;
  }

  /**
   * {@link #exhausted(Bucket)} unless the Pacific day is no longer {@code reservedOn}: a 403 that
   * arrives after the reset answers a request of the old day and says nothing about the new one
   * (M3a5 review recommendation 1).
   */
  public synchronized void exhausted(Bucket bucket, LocalDate reservedOn) {
    rollOver();
    if (day.equals(reservedOn)) {
      exhausted(bucket);
    }
  }

  /** Marks the bucket spent until the next reset (Google answered 403 {@code quotaExceeded}). */
  public synchronized void exhausted(Bucket bucket) {
    rollOver();
    exhausted[bucket.ordinal()] = true;
    warnOnce(bucket);
  }

  /** What the bucket has handed out on the current Pacific day. */
  public synchronized int used(Bucket bucket) {
    rollOver();
    return used[bucket.ordinal()];
  }

  private void rollOver() {
    LocalDate today = today();
    if (!today.equals(day)) {
      day = today;
      Arrays.fill(used, 0);
      Arrays.fill(exhausted, false);
      Arrays.fill(warned, false);
    }
  }

  /** One WARN per bucket and Pacific day, with no date in the line. */
  private void warnOnce(Bucket bucket) {
    int i = bucket.ordinal();
    if (!warned[i]) {
      warned[i] = true;
      log.warn("YouTube quota spent: bucket={}", bucket.label);
    }
  }

  private LocalDate today() {
    return clock.instant().atZone(PACIFIC).toLocalDate();
  }
}
