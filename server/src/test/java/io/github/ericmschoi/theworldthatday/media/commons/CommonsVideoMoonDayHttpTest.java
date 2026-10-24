package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearchProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T4 (4) and (6): the sample date 1969-07-20 (Apollo 11, a public historical event) with
 * the real Wikidata and Commons video clients, the source and the video search, against WireMock
 * and the recorded fixtures. The unit tests of the source use fake clients; this one counts what
 * goes over HTTP. The row for the report is printed.
 */
class CommonsVideoMoonDayHttpTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String SPARQL = WikimediaWireMockTest.SPARQL_PATH;
  private static final String COMMONS = WikimediaWireMockTest.COMMONS_PATH;
  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final String EVENT = "kowiki-1969-07-20-00000001";
  private static final Duration LOOKUP = Duration.ofSeconds(3);
  private static final EventTarget MOON =
      new EventTarget(EVENT, "아폴로 11호", "world", "지어낸 문장: 달에 내렸다.", "Q43653");

  /** Wikidata without the P10 file: the Apollo 11 category only. */
  private static final String LINKS_WITHOUT_P10 =
      """
      {"results":{"bindings":[{"item":{"value":"http://www.wikidata.org/entity/Q43653"},
       "p373":{"value":"Apollo 11"},"label":{"value":"Apollo 11"}}]}}""";

  /** No request may leave WireMock: every one was answered by a stub. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  private static VideoSearch search() {
    MutableClock clock =
        new MutableClock(Instant.parse("2026-10-24T21:15:44Z"), ClockConfig.SERVICE_ZONE);
    WikimediaHttpClient http =
        new WikimediaHttpClient(
            new WikimediaProperties(
                WikimediaWireMockTest.TEST_USER_AGENT,
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                1,
                Duration.ofSeconds(120),
                3,
                Duration.ofDays(7),
                Duration.ofSeconds(5),
                URI.create(wikimedia.baseUrl() + "/w/api.php")),
            clock);
    JsonMapper json = JsonMapper.builder().build();
    MediaProperties media =
        new MediaProperties(List.of(), List.of(), List.of(), List.of("Associated Press"));
    CommonsVideoSource source =
        new CommonsVideoSource(
            new SparqlWikidataVideoLinks(
                http,
                new WikidataProperties(
                    URI.create(wikimedia.baseUrl() + SPARQL),
                    Duration.ofSeconds(5),
                    List.of("Q3839081")),
                json),
            new ActionApiCommonsVideoClient(
                http, new CommonsProperties(URI.create(wikimedia.baseUrl() + COMMONS)), json),
            new CommonsFileMapper(new MediaFilter(media), new MediaNormalizer(media), clock),
            new MediaLookupProperties(6, LOOKUP, 5));
    VideoSearchProperties properties = FakeVideoSource.properties(3);
    return new VideoSearch(List.of(source), new VideoAccuracy(properties), properties);
  }

  private static void stubRecorded(String links) {
    wikimedia.stubFor(get(urlPathEqualTo(SPARQL)).willReturn(okJson(links)));
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS))
            .withQueryParam("gcmtitle", matching("Category:Videos taken on 1969-07-20"))
            .willReturn(
                okJson(ActionApiCommonsVideoClientTest.fixture("date-category-1969-07-20.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS))
            .withQueryParam("titles", matching("File:AP11 FINAL APPROACH\\.ogv"))
            .willReturn(
                okJson(ActionApiCommonsVideoClientTest.fixture("p10-files-1969-07-20.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS))
            .withQueryParam("gsrsearch", matching("incategory:\"Apollo 11\" filetype:video"))
            .willReturn(
                okJson(ActionApiCommonsVideoClientTest.fixture("category-apollo-11.json"))));
  }

  private static Videos run(boolean dayPathsOff) {
    try (Deadline deadline = Deadline.after(LOOKUP)) {
      return search().start(deadline, MOON_DAY, List.of(MOON), dayPathsOff).await();
    }
  }

  private static long asked(String parameter) {
    return wikimedia.getAllServeEvents().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(COMMONS))
        .filter(e -> e.getRequest().queryParameter(parameter).isPresent())
        .count();
  }

  private static long wikidata() {
    return wikimedia.getAllServeEvents().stream()
        .map(ServeEvent::getRequest)
        .filter(r -> r.getUrl().startsWith(SPARQL))
        .count();
  }

  @Test
  void theMoonLandingDayIsAskedInFourRequestsAndGetsItsP10Video() {
    stubRecorded(SparqlWikidataSignalSourceTest.fixture("video-links-1969-07-20.json"));

    Videos videos = run(false);

    MediaItem video = videos.byEvent().get(EVENT);
    assertThat(videos.failed()).isFalse();
    assertThat(video.record().id()).isEqualTo("commons-7366097");
    assertThat(video.record().displayMode().name()).isEqualTo("VIDEO_FILE");
    assertThat(video.selection()).isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(video.record().capturedAt()).isEqualTo("1969-07-20");
    assertThat(video.record().datePrecision().name()).isEqualTo("DAY");
    assertThat(video.dateRelation().kind()).isEqualTo(DateRelation.Kind.SAME_DAY);
    assertThat(video.record().videoSources().getFirst().mime()).isEqualTo("video/webm");
    // Wikidata 1 + date category 1 + P10 file batch 1 + the Apollo 11 category 1
    assertThat(wikimedia.getAllServeEvents()).hasSize(4);
    assertThat(wikidata()).isEqualTo(1);
    assertThat(asked("gcmtitle")).isEqualTo(1);
    assertThat(asked("titles")).isEqualTo(1);
    assertThat(asked("gsrsearch")).isEqualTo(1);
    System.out.println(
        "M3a4b T4 table | 1969-07-20 | adapter+VideoSearch over HTTP (recorded) | video "
            + video.record().id()
            + " | "
            + video.selection()
            + " | "
            + video.record().capturedAt()
            + " | "
            + video.dateRelation().kind()
            + " | requests "
            + wikimedia.getAllServeEvents().size());
  }

  @Test
  void withoutP10TheFileBatchIsNotSentAndTheCategoryVideoWins() {
    stubRecorded(LINKS_WITHOUT_P10);

    Videos videos = run(false);

    MediaItem video = videos.byEvent().get(EVENT);
    assertThat(videos.failed()).isFalse();
    assertThat(video.record().id()).isEqualTo("commons-71615");
    assertThat(video.selection()).isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 130));
    assertThat(wikimedia.getAllServeEvents()).hasSize(3);
    assertThat(asked("titles")).isZero();
    System.out.println(
        "M3a4b T4 table | 1969-07-20 without P10 | adapter+VideoSearch over HTTP (synthetic Wikidata)"
            + " | video "
            + video.record().id()
            + " | "
            + video.selection()
            + " | "
            + video.record().capturedAt()
            + " | "
            + video.dateRelation().kind()
            + " | requests "
            + wikimedia.getAllServeEvents().size());
  }

  @Test
  void dayPathsOffDropsTheDateCategoryRequestOnly() {
    stubRecorded(SparqlWikidataSignalSourceTest.fixture("video-links-1969-07-20.json"));

    Videos videos = run(true);

    assertThat(videos.byEvent().get(EVENT).selection())
        .isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(wikimedia.getAllServeEvents()).hasSize(3);
    assertThat(asked("gcmtitle")).isZero();
  }
}
