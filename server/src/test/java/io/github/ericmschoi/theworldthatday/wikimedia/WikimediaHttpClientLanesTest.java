package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient.Lane;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient.PhotoPath;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4b T3 (9), the CI failure of PR #24: with the three shared request slots, the video
 * requests never take the slot an open photo path needs, whichever thread comes first, and use
 * every slot when no photo path is open. The slots are taken and given back directly (no HTTP, no
 * sleep): a refused video request is one whose wait ran out, and a waiting one is seen through the
 * lock's condition.
 */
class WikimediaHttpClientLanesTest {

  /** How long a request waits for a slot; short, as a refused video request waits it out. */
  private static final Duration READ_TIMEOUT = Duration.ofMillis(200);

  private final WikimediaHttpClient client =
      new WikimediaHttpClient(
          new WikimediaProperties(
              WikimediaWireMockTest.TEST_USER_AGENT,
              Duration.ofSeconds(2),
              READ_TIMEOUT,
              1,
              Duration.ofSeconds(120),
              3,
              Duration.ofDays(7),
              Duration.ofSeconds(5),
              URI.create("http://localhost:1/w/api.php")),
          new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE));

  @Test
  void videosFirstStillLeaveASlotForEachOpenPhotoPath() {
    PhotoPath day = client.openPhotoPath();
    PhotoPath events = client.openPhotoPath();

    // the video links and the date category come before both photo requests
    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).as("kept for the photo paths").isFalse();
    assertThat(client.acquirePermit(Lane.PHOTO)).as("day category").isTrue();
    assertThat(client.acquirePermit(Lane.PHOTO)).as("PageImages").isTrue();
    assertThat(client.permits().availablePermits()).isZero();

    day.close();
    events.close();
  }

  @Test
  void photosFirstLetTheVideosHaveTheRest() {
    PhotoPath day = client.openPhotoPath();
    PhotoPath events = client.openPhotoPath();

    assertThat(client.acquirePermit(Lane.PHOTO)).isTrue();
    assertThat(client.acquirePermit(Lane.PHOTO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).as("both paths hold their slot").isTrue();
    assertThat(client.permits().availablePermits()).isZero();

    day.close();
    events.close();
  }

  @Test
  void betweenItsTwoRequestsAPhotoPathKeepsItsSlot() {
    PhotoPath day = client.openPhotoPath();
    assertThat(client.acquirePermit(Lane.PHOTO)).isTrue();
    client.releasePermit(Lane.PHOTO); // the day category answered; the file batch comes next

    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).as("the file batch's slot").isFalse();
    assertThat(client.acquirePermit(Lane.PHOTO)).as("the file batch").isTrue();

    day.close();
  }

  @Test
  void withoutAnOpenPhotoPathTheVideosUseEverySlot() {
    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(client.permits().availablePermits()).isZero();
  }

  @Test
  void aWaitingVideoTakesTheSlotWhenThePhotoPathCloses() throws Exception {
    WikimediaHttpClient patient =
        new WikimediaHttpClient(
            new WikimediaProperties(
                WikimediaWireMockTest.TEST_USER_AGENT,
                Duration.ofSeconds(2),
                Duration.ofSeconds(30),
                1,
                Duration.ofSeconds(120),
                3,
                Duration.ofDays(7),
                Duration.ofSeconds(5),
                URI.create("http://localhost:1/w/api.php")),
            new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE));
    PhotoPath day = patient.openPhotoPath();
    assertThat(patient.acquirePermit(Lane.VIDEO)).isTrue();
    assertThat(patient.acquirePermit(Lane.VIDEO)).isTrue();

    CompletableFuture<Boolean> third =
        CompletableFuture.supplyAsync(() -> patient.acquirePermit(Lane.VIDEO));
    while (!patient.videoWaiting()) {
      Thread.onSpinWait();
    }
    assertThat(third).isNotDone();
    day.close();
    day.close(); // closing twice keeps no slot back for a path that is gone

    assertThat(third.get(5, TimeUnit.SECONDS)).isTrue();
    assertThat(patient.permits().availablePermits()).isZero();
  }

  @Test
  void aWaitingVideoTakesASlotGivenBack() throws Exception {
    WikimediaHttpClient patient =
        new WikimediaHttpClient(
            new WikimediaProperties(
                WikimediaWireMockTest.TEST_USER_AGENT,
                Duration.ofSeconds(2),
                Duration.ofSeconds(30),
                1,
                Duration.ofSeconds(120),
                3,
                Duration.ofDays(7),
                Duration.ofSeconds(5),
                URI.create("http://localhost:1/w/api.php")),
            new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE));
    assertThat(patient.acquirePermit(Lane.DEFAULT)).isTrue();
    assertThat(patient.acquirePermit(Lane.PHOTO)).isTrue();
    assertThat(patient.acquirePermit(Lane.VIDEO)).isTrue();

    CompletableFuture<Boolean> fourth =
        CompletableFuture.supplyAsync(() -> patient.acquirePermit(Lane.VIDEO));
    while (!patient.videoWaiting()) {
      Thread.onSpinWait();
    }
    patient.releasePermit(Lane.DEFAULT);

    assertThat(fourth.get(5, TimeUnit.SECONDS)).isTrue();
  }
}
