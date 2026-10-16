package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3b T1 (10) and (11): the recorded day category responses of the three demo dates ({@code
 * fixtures/wikimedia/commons/}, default language) through the client and the mapper, with the
 * application's excluded credits. Values are the recording's (see the README there).
 */
class CommonsRecordedDaysTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final List<String> CREDITS =
      List.of(
          "연합뉴스",
          "Yonhap",
          "뉴시스",
          "Newsis",
          "AP",
          "Associated Press",
          "Reuters",
          "AFP",
          "Getty",
          "EPA");

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final CommonsClient client = client();
  private final CommonsFileMapper mapper = mapper();

  /** One expected item: page ID, license, capture date (null when unknown) and relation. */
  private record Expected(
      long pageId, String license, String capturedAt, Kind kind, String authorPrefix) {}

  private CommonsClient client() {
    WikimediaProperties properties =
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
    return new ActionApiCommonsClient(
        new WikimediaHttpClient(properties, clock),
        new CommonsProperties(URI.create(commons.baseUrl() + PATH)),
        JSON);
  }

  private CommonsFileMapper mapper() {
    MediaProperties properties = new MediaProperties(List.of(), List.of(), List.of(), CREDITS);
    return new CommonsFileMapper(
        new MediaFilter(properties), new MediaNormalizer(properties), clock);
  }

  private static String fixture(LocalDate date) throws IOException {
    String name = "/fixtures/wikimedia/commons/day-category-" + date + ".json";
    try (InputStream in = CommonsRecordedDaysTest.class.getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private List<MediaItem> day(LocalDate date) throws IOException {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(fixture(date))));
    Pages pages = client.dayCategory(date, Duration.ofSeconds(2));
    assertThat(pages.failed()).isFalse();
    return pages.pages().stream()
        .map(page -> mapper.map(page, "image/jpeg"::equals, "country:KR", date))
        .peek(result -> assertThat(result.drop()).as("dropped").isNull())
        .map(result -> result.kept().orElseThrow())
        .toList();
  }

  private static void assertItems(List<MediaItem> items, List<Expected> expected) {
    assertThat(items).hasSameSizeAs(expected);
    for (int i = 0; i < items.size(); i++) {
      MediaRecord r = items.get(i).record();
      Expected e = expected.get(i);
      assertThat(r.id()).isEqualTo("commons-" + e.pageId());
      assertThat(r.displayMode()).isEqualTo(DisplayMode.IMAGE);
      assertThat(r.license()).isEqualTo(e.license());
      assertThat(r.licenseStatus()).isEqualTo(LicenseStatus.FREE);
      assertThat(r.capturedAt()).as(r.id()).isEqualTo(e.capturedAt());
      assertThat(r.datePrecision()).isEqualTo(e.capturedAt() == null ? null : DatePrecision.DAY);
      assertThat(items.get(i).dateRelation().kind()).as(r.id()).isEqualTo(e.kind());
      assertThat(r.attribution())
          .startsWith(e.authorPrefix() + " · ")
          .endsWith(" · " + e.license() + " · Wikimedia Commons");
      assertThat(URI.create(r.url()).getHost()).isEqualTo("upload.wikimedia.org");
      assertThat(URI.create(r.thumbnailUrl()).getHost()).isEqualTo("thumb.wikimedia.org");
      assertThat(r.thumbnailUrl()).contains("/960px-");
      assertThat(r.thumbnailWidth()).isEqualTo(640);
      assertThat(r.placeRef()).isEqualTo("country:KR");
      ObjectNode written = JSON.valueToTree(items.get(i));
      written.remove("date_relation");
      assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), written)).isEmpty();
    }
  }

  @Test
  void openingDayGivesFiveSameDayPhotosButJose5IsUnknown() throws IOException {
    List<MediaItem> items = day(LocalDate.of(1988, 9, 17));

    String usaf = "Ken Hackman, U.S. Air Force";
    assertItems(
        items,
        List.of(
            new Expected(4196553, "Public domain", "1988-09-17", Kind.SAME_DAY, usaf),
            new Expected(4929997, "Public domain", "1988-09-17", Kind.SAME_DAY, usaf),
            new Expected(4930021, "Public domain", "1988-09-17", Kind.SAME_DAY, usaf),
            // two values in DateTimeOriginal: shown, never presented as that day
            new Expected(26546944, "CC BY-SA 3.0", null, Kind.UNKNOWN, "Jose Quinaliza"),
            new Expected(
                112453227,
                "Public domain",
                "1988-09-17",
                Kind.SAME_DAY,
                "Unknown (Comitetul Olimpic si Sportiv Roman)")));
  }

  @Test
  void quarterFinalDayGivesOneKoglPhoto() throws IOException {
    List<MediaItem> items = day(LocalDate.of(2002, 6, 22));

    assertItems(
        items,
        List.of(new Expected(129150318, "KOGL Type 1", "2002-06-22", Kind.SAME_DAY, "문덕관 기증")));
    assertThat(items.getFirst().record().licenseUrl())
        .isEqualTo("http://www.kogl.or.kr/info/licenseType1.do");
  }

  @Test
  void dayWithoutACategoryIsEmptyAndNotAFailure() throws IOException {
    assertThat(day(LocalDate.of(1995, 3, 15))).isEmpty();
  }
}
