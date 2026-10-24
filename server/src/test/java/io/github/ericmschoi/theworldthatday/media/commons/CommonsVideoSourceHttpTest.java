package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T3 (4), (5) and (10) with the real clients against WireMock (no network): how many
 * requests a new date takes, that the two waits run their requests side by side, and that no log
 * line (DEBUG included) names a date, category, file, QID or event ID. Synthetic answers with
 * made-up QIDs ({@code Q1}...) on the leap day 2000-02-29.
 */
@ExtendWith(OutputCaptureExtension.class)
class CommonsVideoSourceHttpTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String SPARQL = WikimediaWireMockTest.SPARQL_PATH;
  private static final String COMMONS = WikimediaWireMockTest.COMMONS_PATH;
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final String NO_PAGES = "{\"batchcomplete\":true,\"query\":{\"pages\":[]}}";
  private static final String FILE_PATH = "http://commons.wikimedia.org/wiki/Special:FilePath/";

  /** Q1 has a video and a category, Q2 and Q3 a category each. */
  private static final String LINKS =
      """
      {"results":{"bindings":[
       {"item":{"value":"http://www.wikidata.org/entity/Q1"},"video":{"value":"%sSynthetic%%20file.webm"},
        "p373":{"value":"Synthetic category one"},"label":{"value":"Synthetic one"}},
       {"item":{"value":"http://www.wikidata.org/entity/Q2"},
        "cs":{"value":"Category:Synthetic category two"}},
       {"item":{"value":"http://www.wikidata.org/entity/Q3"},
        "p373":{"value":"Synthetic category three"}}]}}"""
          .formatted(FILE_PATH);

  private static final List<EventContext> THREE =
      List.of(
          event("kowiki-2000-02-29-0000000a", "Q1"),
          event("kowiki-2000-02-29-0000000b", "Q2"),
          event("kowiki-2000-02-29-0000000c", "Q3"));

  private static EventContext event(String id, String qid) {
    return new EventContext(id, LEAP_DAY, "시험 사건", null, "지어낸 문장이다.", qid, "world");
  }

  private final CommonsVideoSource source = source();

  private static CommonsVideoSource source() {
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
            new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE));
    JsonMapper json = JsonMapper.builder().build();
    MediaProperties media = new MediaProperties(List.of(), List.of());
    return new CommonsVideoSource(
        new SparqlWikidataVideoLinks(
            http,
            new WikidataProperties(
                URI.create(wikimedia.baseUrl() + SPARQL),
                Duration.ofSeconds(5),
                List.of("Q3839081")),
            json),
        new ActionApiCommonsVideoClient(
            http, new CommonsProperties(URI.create(wikimedia.baseUrl() + COMMONS)), json),
        new CommonsFileMapper(
            new MediaFilter(media),
            new MediaNormalizer(media),
            new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE)),
        new MediaLookupProperties(6, Duration.ofSeconds(3), 5));
  }

  private Level before;

  @BeforeEach
  void debugOn() {
    Logger root = (Logger) LoggerFactory.getLogger("io.github.ericmschoi.theworldthatday");
    before = root.getLevel();
    root.setLevel(Level.DEBUG);
  }

  @AfterEach
  void debugOff() {
    ((Logger) LoggerFactory.getLogger("io.github.ericmschoi.theworldthatday")).setLevel(before);
  }

  private static void answer(int delayMillis) {
    wikimedia.stubFor(
        get(urlPathEqualTo(SPARQL)).willReturn(okJson(LINKS).withFixedDelay(delayMillis)));
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS)).willReturn(okJson(NO_PAGES).withFixedDelay(delayMillis)));
  }

  private static List<ServeEvent> commons(String parameter) {
    return wikimedia.getAllServeEvents().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(COMMONS))
        .filter(e -> e.getRequest().queryParameter(parameter).isPresent())
        .toList();
  }

  // ---- (4) requests per new date ----

  @Test
  void threeEventsOnANewDateTakeAtMostSixRequests() {
    answer(0);

    SourceResult result = source.find(LEAP_DAY, THREE, false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(wikimedia.getAllServeEvents()).hasSize(6);
    assertThat(wikimedia.getAllServeEvents())
        .filteredOn(e -> e.getRequest().getUrl().startsWith(SPARQL))
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getRequest().queryParameter("query").firstValue())
                    .isEqualTo(SparqlWikidataVideoLinks.query(List.of("Q1", "Q2", "Q3"))));
    assertThat(commons("gcmtitle")).hasSize(1);
    assertThat(commons("titles")).hasSize(1);
    assertThat(commons("gsrsearch"))
        .extracting(e -> e.getRequest().queryParameter("gsrsearch").firstValue())
        .containsExactlyInAnyOrder(
            "incategory:\"Synthetic category one\" filetype:video",
            "incategory:\"Synthetic category two\" filetype:video",
            "incategory:\"Synthetic category three\" filetype:video");
  }

  @Test
  void dayPathsOffAsksNoDateCategory() {
    answer(0);

    source.find(LEAP_DAY, THREE, true);

    assertThat(wikimedia.getAllServeEvents()).hasSize(5);
    assertThat(commons("gcmtitle")).isEmpty();
  }

  // ---- (6) 1986-09-20, recorded: no P373, the P910 sitelink category is asked ----

  @Test
  void theAsianGamesAskTheirSitelinkCategoryAndFindNoVideo() {
    wikimedia.stubFor(
        get(urlPathEqualTo(SPARQL))
            .willReturn(
                okJson(SparqlWikidataSignalSourceTest.fixture("video-links-1986-09-20.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS))
            .willReturn(
                okJson(ActionApiCommonsVideoClientTest.fixture("category-1986-asian-games.json"))));
    String id = "kowiki-1986-09-20-00000001";
    EventContext games =
        new EventContext(
            id,
            LocalDate.of(1986, 9, 20),
            "제10회 아시안 게임",
            null,
            "지어낸 문장이다.",
            "Q495611",
            "country:KR");

    SourceResult result = source.find(LocalDate.of(1986, 9, 20), List.of(games), true);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(result.byEvent()).isEqualTo(Map.of(id, List.of()));
    assertThat(result.englishLabels()).isEqualTo(Map.of(id, "1986 Asian Games"));
    assertThat(wikimedia.getAllServeEvents()).hasSize(2);
    assertThat(commons("gsrsearch"))
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getRequest().queryParameter("gsrsearch").firstValue())
                    .isEqualTo("incategory:\"1986 Asian Games\" filetype:video"));
  }

  // ---- (5) the two waits run side by side ----

  @Test
  void requestsOfEachWaitRunSideBySide() {
    answer(300);

    long start = System.nanoTime();
    SourceResult result = source.find(LEAP_DAY, THREE, false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println(
        "M3a4b T3 (5) Commons video find, 6 requests of 300 ms: " + took.toMillis() + " ms");

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(wikimedia.getAllServeEvents()).hasSize(6);
    // in order it would take 6 x 300 ms = 1.8 s; two waits (the second with 4 requests on 3 slots)
    assertThat(took).isLessThan(Duration.ofMillis(1_500));
    assertThat(took).isGreaterThanOrEqualTo(Duration.ofMillis(600));
  }

  // ---- (3) and (10): failures and logs ----

  @Test
  void aFailedCommonsRequestFailsTheSourceAndNoLineNamesItsValues(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(SPARQL)).willReturn(okJson(LINKS)));
    wikimedia.stubFor(get(urlPathEqualTo(COMMONS)).willReturn(aResponse().withStatus(503)));

    SourceResult result = source.find(LEAP_DAY, THREE, false);

    assertThat(result.status()).isEqualTo(Status.FAILED);
    assertThat(output.getAll().lines().filter(l -> l.contains("Wikimedia call failed")).toList())
        .hasSize(1)
        .allSatisfy(
            l -> assertThat(l).contains("title=commons video date category cause=status 503"));
    assertThat(output.getAll())
        .doesNotContain(
            "2000-02-29",
            "kowiki-2000",
            "0000000a",
            "Q1",
            "Q2",
            "Synthetic category",
            "Synthetic file",
            "Synthetic%20file",
            "Category:",
            "File:");
  }

  @Test
  void aSuccessfulSearchLogsNoValueEvenAtDebug(CapturedOutput output) {
    answer(0);

    source.find(LEAP_DAY, THREE, false);

    assertThat(output.getAll())
        .doesNotContain("2000-02-29", "kowiki-2000", "Q1", "Synthetic", "Category:", "File:");
  }
}
