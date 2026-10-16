package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3b T2 (3), (4), (5), (6) and (7) on the lookup itself: order, limit, tragic days,
 * failures and the deadline, against synthetic category answers. File names and authors are made
 * up.
 */
@ExtendWith(OutputCaptureExtension.class)
class CommonsMediaLookupTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);
  private static final Duration DEADLINE = Duration.ofMillis(600);

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  private CommonsMediaLookup lookup(int limit) {
    WikimediaProperties wikimedia =
        new WikimediaProperties(
            WikimediaWireMockTest.TEST_USER_AGENT,
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(commons.baseUrl() + "/w/api.php"));
    CommonsClient client =
        new ActionApiCommonsClient(
            new WikimediaHttpClient(wikimedia, clock),
            new CommonsProperties(URI.create(commons.baseUrl() + PATH)),
            JsonMapper.builder().build());
    MediaProperties media = new MediaProperties(List.of(), List.of());
    CommonsFileMapper mapper =
        new CommonsFileMapper(new MediaFilter(media), new MediaNormalizer(media), clock);
    return new CommonsMediaLookup(client, mapper, new MediaLookupProperties(limit, DEADLINE));
  }

  private static String page(long id, String title, String taken, String mime) {
    return """
        {"pageid": %d, "ns": 6, "title": "File:%s", "imageinfo": [{
          "url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/S%d.jpg",
          "descriptionurl": "https://commons.wikimedia.org/wiki/File:S%d.jpg",
          "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/S%d.jpg/960px-S%d.jpg",
          "thumbwidth": 640, "thumbheight": 480, "mime": "%s",
          "extmetadata": {"Artist": {"value": "Synthetic Author"},
            "LicenseShortName": {"value": "CC0"}, "DateTimeOriginal": {"value": "%s"}}}]}
        """
        .formatted(id, title, id, id, id, id, mime, taken);
  }

  private static String page(long id, String title, String taken) {
    return page(id, title, taken, "image/jpeg");
  }

  private static void category(List<String> pages) {
    commons.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                okJson(
                    "{\"batchcomplete\":true,\"query\":{\"pages\":["
                        + String.join(",", pages)
                        + "]}}")));
  }

  private static List<String> ids(DayMedia media) {
    return media.items().stream().map(item -> item.record().id()).toList();
  }

  @Test
  void sameDayFirstThenOtherDatesThenUnknownEachByTitle() {
    category(
        List.of(
            page(1, "Synthetic c.jpg", "1988-09-17"),
            page(2, "Synthetic 0 two dates.jpg", "1988-09-17<br>2013-05-05 21:10:18"),
            page(3, "Synthetic b other year.jpg", "1990-01-01"),
            page(4, "Synthetic a.jpg", "Taken on 17 September 1988"),
            page(5, "Synthetic 1 same month.jpg", "1988-09-02")));

    DayMedia media = lookup(6).dayMedia(OLYMPICS, false);

    assertThat(media.failed()).isFalse();
    assertThat(ids(media))
        .containsExactly("commons-4", "commons-1", "commons-5", "commons-3", "commons-2");
    MediaItem unknown = media.items().getLast();
    assertThat(unknown.dateRelation().kind()).isEqualTo(Kind.UNKNOWN);
    assertThat(unknown.record().capturedAt()).as("never the category's date").isNull();
    assertThat(unknown.record().datePrecision()).isNull();
  }

  @Test
  void eightPassingFilesGiveSixWithoutRepeatedIds() {
    List<String> pages =
        new ArrayList<>(
            IntStream.rangeClosed(1, 8)
                .mapToObj(i -> page(i, "Synthetic " + i + ".jpg", "1988-09-17"))
                .toList());
    pages.add(page(1, "Synthetic 1.jpg", "1988-09-17")); // the same page twice

    category(pages);
    DayMedia media = lookup(6).dayMedia(OLYMPICS, false);

    assertThat(ids(media))
        .containsExactly(
            "commons-1", "commons-2", "commons-3", "commons-4", "commons-5", "commons-6");
  }

  @Test
  void limitZeroAndTragicDaysAskNothing() {
    category(List.of(page(1, "Synthetic.jpg", "1988-09-17")));

    assertThat(lookup(0).dayMedia(OLYMPICS, false)).isEqualTo(DayMedia.NONE);
    assertThat(lookup(6).dayMedia(OLYMPICS, true)).isEqualTo(DayMedia.NONE);

    assertThat(commons.getAllServeEvents()).isEmpty();
  }

  @Test
  void missingCategoryIsEmptyAndNotAFailure() {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{\"batchcomplete\":true}")));

    assertThat(lookup(6).dayMedia(LocalDate.of(1995, 3, 15), false)).isEqualTo(DayMedia.NONE);
  }

  @Test
  void unavailableOrRateLimitedCommonsIsAFailure() {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
    assertThat(lookup(6).dayMedia(OLYMPICS, false)).isEqualTo(DayMedia.FAILED);

    commons.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson("{\"error\":{\"code\":\"ratelimited\"}}")));
    assertThat(lookup(6).dayMedia(OLYMPICS, false)).isEqualTo(DayMedia.FAILED);
  }

  @Test
  void droppedFilesAreNotAFailureAndOnlyTheirCountsAreLogged(CapturedOutput output) {
    category(
        List.of(
            page(1, "Synthetic map.png", "1988-09-17", "image/png"),
            page(2, "Synthetic kept.jpg", "1988-09-17")));
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CommonsMediaLookup.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    DayMedia media;
    try {
      media = lookup(6).dayMedia(OLYMPICS, false);
    } finally {
      logger.setLevel(before);
    }

    assertThat(media.failed()).isFalse();
    assertThat(ids(media)).containsExactly("commons-2");
    assertThat(output.getAll())
        .contains("Day photos dropped: {MIME=1}")
        .doesNotContain("Synthetic map", "1988-09-17", "File:");
  }

  @Test
  void slowCommonsEndsAtTheDeadlineWithOneWarning(CapturedOutput output) {
    commons.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"batchcomplete\":true}").withFixedDelay(4_000)));

    long start = System.nanoTime();
    DayMedia media = lookup(6).dayMedia(OLYMPICS, false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(media).isEqualTo(DayMedia.FAILED);
    assertThat(took).isLessThan(DEADLINE.plusSeconds(2));
    assertThat(output.getAll().lines().filter(l -> l.contains("Wikimedia call failed")))
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).contains("source=commons title=commons day category"));
  }
}
