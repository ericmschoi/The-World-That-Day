package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/**
 * Plan M3a5 T3: the two daily quota buckets. Time moves only through a {@link MutableClock}. The
 * 2026 instants are boundaries of the quota clock (Pacific midnight around the daylight-saving
 * changes), not story dates.
 */
@Timeout(30)
class YoutubeQuotaTest {

  private static final Instant MIDDAY = Instant.parse("2026-06-10T19:00:00Z");

  private final MutableClock clock = new MutableClock(MIDDAY, ZoneOffset.UTC);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  private final Logger quotaLogger = (Logger) LoggerFactory.getLogger(YoutubeQuota.class);
  private Level levelBefore;

  @BeforeEach
  void captureLogs() {
    levelBefore = quotaLogger.getLevel();
    quotaLogger.setLevel(Level.DEBUG);
    logs.start();
    quotaLogger.addAppender(logs);
  }

  @AfterEach
  void releaseLogs() {
    quotaLogger.detachAppender(logs);
    quotaLogger.setLevel(levelBefore);
    logs.stop();
  }

  private static YoutubeProperties properties(int searchBudget, int unitBudget) {
    return new YoutubeProperties(
        "",
        URI.create("http://localhost:9/youtube/v3"),
        List.of(YoutubePropertiesTest.KTV_ARCHIVE),
        searchBudget,
        unitBudget,
        25,
        Duration.ofSeconds(2),
        List.of());
  }

  private YoutubeQuota quota(int searchBudget, int unitBudget) {
    return new YoutubeQuota(properties(searchBudget, unitBudget), clock);
  }

  private void moveTo(Instant instant) {
    clock.advance(Duration.between(clock.instant(), instant));
  }

  private List<String> warnings() {
    return logs.list.stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }

  // ---- reserve ----

  @Test
  void reserveTakesTheCountWhileItFitsAndNothingWhenItDoesNot() {
    YoutubeQuota quota = quota(80, 8000);

    assertThat(quota.reserve(Bucket.SEARCH, 79)).isTrue();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(79);

    assertThat(quota.reserve(Bucket.SEARCH, 2)).isFalse();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(79);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(80);
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(80);
  }

  @Test
  void theBucketsCountApart() {
    YoutubeQuota quota = quota(80, 8000);

    assertThat(quota.reserve(Bucket.SEARCH, 80)).isTrue();

    assertThat(quota.reserve(Bucket.UNITS, 8000)).isTrue();
    assertThat(quota.used(Bucket.UNITS)).isEqualTo(8000);
    assertThat(quota.reserve(Bucket.UNITS, 1)).isFalse();
  }

  @Test
  void aCapOfZeroAlwaysRefuses() {
    YoutubeQuota quota = quota(0, 0);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.reserve(Bucket.UNITS, 1)).isFalse();
    assertThat(quota.used(Bucket.SEARCH)).isZero();
    assertThat(quota.used(Bucket.UNITS)).isZero();
  }

  @Test
  void aCountThatWouldOverflowTheSumIsRefused() {
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();

    assertThat(quota.reserve(Bucket.SEARCH, Integer.MAX_VALUE)).isFalse();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(1);
  }

  @Test
  void aCountBelowOneIsAMistakeOfTheCaller() {
    YoutubeQuota quota = quota(80, 8000);

    assertThatIllegalArgumentException().isThrownBy(() -> quota.reserve(Bucket.SEARCH, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> quota.reserve(Bucket.UNITS, -1));
    assertThat(quota.used(Bucket.SEARCH)).isZero();
  }

  @Test
  void aNewInstanceStartsFromZero() {
    YoutubeQuota first = quota(80, 8000);
    assertThat(first.reserve(Bucket.SEARCH, 80)).isTrue();
    first.exhausted(Bucket.UNITS);

    YoutubeQuota restarted = quota(80, 8000);

    assertThat(restarted.used(Bucket.SEARCH)).isZero();
    assertThat(restarted.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThat(restarted.reserve(Bucket.UNITS, 1)).isTrue();
  }

  // ---- exhausted ----

  @Test
  void anExhaustedBucketRefusesUntilTheReset() {
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 5)).isTrue();

    quota.exhausted(Bucket.SEARCH);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(5);
    // Time passing on the same Pacific day does not lift it.
    clock.advance(Duration.ofHours(3));
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    // The other bucket is not touched.
    assertThat(quota.reserve(Bucket.UNITS, 1)).isTrue();
  }

  // ---- Pacific reset ----

  static Stream<Arguments> boundaries() {
    return Stream.of(
        Arguments.of("2026-03-07 to 03-08 (before daylight saving)", "2026-03-08T07:59:59Z"),
        Arguments.of("2026-03-08 to 03-09 (23-hour day)", "2026-03-09T06:59:59Z"),
        Arguments.of("2026-10-31 to 11-01 (before the change back)", "2026-11-01T06:59:59Z"),
        Arguments.of("2026-11-01 to 11-02 (25-hour day)", "2026-11-02T07:59:59Z"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("boundaries")
  void bothBucketsClearAtPacificMidnightAndNotASecondBefore(String name, String lastSecond) {
    Instant before = Instant.parse(lastSecond);
    moveTo(before.minus(Duration.ofHours(1)));
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 80)).isTrue();
    assertThat(quota.reserve(Bucket.UNITS, 30)).isTrue();
    quota.exhausted(Bucket.UNITS);

    moveTo(before);
    assertThat(quota.used(Bucket.SEARCH)).as("1 s before: same day").isEqualTo(80);
    assertThat(quota.used(Bucket.UNITS)).isEqualTo(30);
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.reserve(Bucket.UNITS, 1)).isFalse();

    clock.advance(Duration.ofSeconds(1));
    assertThat(quota.used(Bucket.SEARCH)).as("the instant itself: new day").isZero();
    assertThat(quota.used(Bucket.UNITS)).isZero();
    assertThat(quota.reserve(Bucket.SEARCH, 80)).isTrue();
    assertThat(quota.reserve(Bucket.UNITS, 1)).as("exhausted mark cleared").isTrue();
  }

  @Test
  void theResetCheckAlsoRunsOnReserve() {
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 80)).isTrue();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();

    moveTo(Instant.parse("2026-06-11T07:00:00Z"));

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(1);
  }

  @Test
  void theDayWithTheExtraHourIsOneDayLong() {
    // 2026-11-01 00:00 PDT to 2026-11-02 00:00 PST is 25 hours.
    moveTo(Instant.parse("2026-11-01T07:00:00Z"));
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 10)).isTrue();

    // 24 hours later it is 23:00 PST on the same Pacific day.
    clock.advance(Duration.ofHours(24));
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(10);

    clock.advance(Duration.ofHours(1));
    assertThat(quota.used(Bucket.SEARCH)).isZero();
  }

  @Test
  void midnightInSeoulIsNotAResetPoint() {
    // 2026-06-10T15:00:00Z is 00:00 on June 11 in Seoul and 08:00 on June 10 in Los Angeles.
    moveTo(Instant.parse("2026-06-10T14:59:59Z"));
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 4)).isTrue();

    clock.advance(Duration.ofSeconds(1));

    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(4);
  }

  // ---- warning ----

  @Test
  void reachingTheCapWarnsOncePerBucketAndDay() {
    YoutubeQuota quota = quota(2, 3);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThat(warnings()).isEmpty();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThat(warnings()).containsExactly("YouTube quota spent: bucket=search");

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    quota.exhausted(Bucket.SEARCH);
    assertThat(warnings()).hasSize(1);

    assertThat(quota.reserve(Bucket.UNITS, 3)).isTrue();
    assertThat(warnings())
        .containsExactly("YouTube quota spent: bucket=search", "YouTube quota spent: bucket=units");
  }

  @Test
  void aRefusedReservationBelowTheCapDoesNotWarn() {
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 79)).isTrue();

    assertThat(quota.reserve(Bucket.SEARCH, 2)).isFalse();

    assertThat(warnings()).isEmpty();
  }

  @Test
  void aMarkedBucketWarnsOnceAndTheCapAfterwardsAddsNoSecondLine() {
    YoutubeQuota quota = quota(80, 8000);

    quota.exhausted(Bucket.UNITS);
    quota.exhausted(Bucket.UNITS);

    assertThat(warnings()).containsExactly("YouTube quota spent: bucket=units");
  }

  @Test
  void theWarningComesBackOnTheNextPacificDay() {
    YoutubeQuota quota = quota(1, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    quota.exhausted(Bucket.UNITS);
    assertThat(warnings()).hasSize(2);

    moveTo(Instant.parse("2026-06-11T07:00:00Z"));
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
    quota.exhausted(Bucket.UNITS);

    assertThat(warnings())
        .containsExactly(
            "YouTube quota spent: bucket=search",
            "YouTube quota spent: bucket=units",
            "YouTube quota spent: bucket=search",
            "YouTube quota spent: bucket=units");
    // The line never carries a date.
    assertThat(warnings()).allMatch(line -> !line.matches(".*\\d{4}.*"));
  }

  @Test
  void aCapOfZeroSpendsNothingSoItDoesNotWarn() {
    YoutubeQuota quota = quota(0, 0);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();

    assertThat(warnings()).isEmpty();
  }

  // ---- concurrency ----

  @Test
  void twoHundredThreadsTakeExactlyTheCap() throws Exception {
    YoutubeQuota quota = quota(80, 8000);
    int threads = 200;

    List<Boolean> results = runTogether(threads, i -> quota.reserve(Bucket.SEARCH, 1));

    assertThat(results.stream().filter(Boolean::booleanValue)).hasSize(80);
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(80);
    assertThat(warnings()).containsExactly("YouTube quota spent: bucket=search");
  }

  @Test
  void concurrentReservationsOfDifferentSizesAddUpToTheUsedCount() throws Exception {
    YoutubeQuota quota = quota(80, 8000);
    int callers = 120;

    List<Boolean> results = runTogether(callers, i -> quota.reserve(Bucket.SEARCH, 1 + i % 3));

    int accepted = 0;
    for (int i = 0; i < callers; i++) {
      if (results.get(i)) {
        accepted += 1 + i % 3;
      }
    }
    assertThat(results).contains(true, false);
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(accepted).isLessThanOrEqualTo(80);
  }

  /** Starts {@code threads} callers on one gate and collects their results under a deadline. */
  private static List<Boolean> runTogether(int threads, IntFunction<Boolean> task)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch ready = new CountDownLatch(threads);
      CountDownLatch go = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        int index = i;
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  go.await();
                  return task.apply(index);
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).as("all threads started").isTrue();
      go.countDown();
      List<Boolean> results = new ArrayList<>();
      for (Future<Boolean> future : futures) {
        try {
          results.add(future.get(10, TimeUnit.SECONDS));
        } catch (ExecutionException | TimeoutException e) {
          throw new AssertionError("a reservation did not finish", e);
        }
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }
}
