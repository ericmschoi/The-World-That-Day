package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.WorldMedia;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsMediaLookup;
import io.github.ericmschoi.theworldthatday.media.pageimages.KoWikiPageImages;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4c T3 (6): a cache read that hangs for half of {@code media.lookup-deadline} and then
 * throws stays inside the one media window, and both photo paths, started before it, still arrive.
 * Fake clients and a fake repository on the leap day; file names are made up. No HTTP.
 */
@ExtendWith(OutputCaptureExtension.class)
class VideoSearchCacheWindowTest {

  private static final Duration LOOKUP = Duration.ofMillis(1_000);
  private static final Duration PHOTOS = LOOKUP.multipliedBy(7).dividedBy(10);
  private static final LocalDate LEAP_DAY = FakeVideoSource.LEAP_DAY;
  private static final String E1 = "kowiki-2000-02-29-0000000a";

  private static final String PAGE =
      """
      {"pageid": 900101, "ns": 6, "title": "File:Synthetic window photo.jpg", "imageinfo": [{
        "url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_window_photo.jpg",
        "descriptionurl": "https://commons.wikimedia.org/wiki/File:Synthetic_window_photo.jpg",
        "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_window_photo.jpg/640px-Synthetic_window_photo.jpg",
        "thumbwidth": 640, "thumbheight": 480, "mime": "image/jpeg",
        "extmetadata": {"Artist": {"value": "Synthetic Author"},
          "LicenseShortName": {"value": "CC0"}, "DateTimeOriginal": {"value": "2000-02-29"}}}]}
      """;

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void aHangingFailedReadStaysInTheWindowAndDelaysNoPhoto(CapturedOutput output) {
    MutableClock clock =
        new MutableClock(Instant.parse("2026-10-24T00:00:00Z"), ClockConfig.SERVICE_ZONE);
    MediaProperties media = new MediaProperties(List.of(), List.of());
    MediaFilter filter = new MediaFilter(media);
    MediaNormalizer normalizer = new MediaNormalizer(media);
    FakeCacheRepository store = new FakeCacheRepository();
    store.findDelay = LOOKUP.dividedBy(2);
    store.findThrows = new DataAccessResourceFailureException("synthetic");
    SearchCache cache =
        new SearchCache(
            store.proxy(),
            new SearchCacheFormat(filter, normalizer),
            new SearchCacheProperties(Map.of("alpha", Duration.ofDays(7))),
            clock);
    FakeVideoSource alpha = new FakeVideoSource("alpha", events -> FakeVideoSource.ok(Map.of()));
    VideoSearchProperties properties = FakeVideoSource.properties(3);
    VideoSearch videos =
        new VideoSearch(List.of(alpha), new VideoAccuracy(properties), properties, cache);
    JsonNode page = JsonMapper.builder().build().readTree(PAGE);
    CommonsClient client =
        new CommonsClient() {
          @Override
          public Pages dayCategory(LocalDate date, Duration readTimeout) {
            sleep(PHOTOS);
            return new Pages(List.of(page), false);
          }

          @Override
          public Files files(List<String> fileNames, Duration readTimeout) {
            throw new UnsupportedOperationException();
          }
        };
    KoWikiPageImages pageImages =
        (titles, timeout) -> {
          sleep(PHOTOS);
          return Optional.of(Map.of());
        };
    CommonsMediaLookup lookup =
        new CommonsMediaLookup(
            client,
            new CommonsFileMapper(filter, normalizer, clock),
            pageImages,
            new MediaLookupProperties(6, LOOKUP),
            videos);
    EventTarget target = new EventTarget(E1, "알파 행사", "country:KR", "지어낸 문장이다.", "Q999999901");

    long start = System.nanoTime();
    WorldMedia found = lookup.lookup(LEAP_DAY, List.of(target), false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took)
        .isLessThanOrEqualTo(LOOKUP.plus(Deadline.CANCEL_GRACE).plus(Duration.ofMillis(500)));
    assertThat(found.dayMedia())
        .extracting(item -> item.record().id())
        .containsExactly("commons-900101");
    assertThat(found.failed()).isFalse(); // neither photo path timed out; the videos answered
    assertThat(store.calls).containsExactly("find");
    assertThat(alpha.asked).hasSize(1);
    assertThat(output.getAll().lines().filter(l -> l.contains(" WARN ")).toList())
        .singleElement()
        .satisfies(l -> assertThat(l).contains("Media search cache read failed: source=alpha"));
  }
}
