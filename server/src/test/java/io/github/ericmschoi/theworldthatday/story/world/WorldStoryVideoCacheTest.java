package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.FOOTAGE;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.LINKS;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.NO_PAGES;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.event;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.read;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubLeapDay;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubLeapDayVideo;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubPage;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubPageprops;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubSignals;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubVideoLinks;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubVideos;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.videoRequests;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4c T3 (7) and (9) through the whole application with the cache on ({@code
 * media.search-cache.ttl.commons: 7d}, this class's own context) against WireMock (no network): a
 * second call of the same date sends no video request and answers the same, a day without videos is
 * cached too, and a failed or late search stores nothing. The synthetic leap day and the recorded
 * 1988-09-17 demo date; synthetic titles, files and made-up QIDs.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "media.search-cache.ttl.commons=7d")
class WorldStoryVideoCacheTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private SearchCacheProperties cacheProperties;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    jdbc.execute("TRUNCATE event_media_search_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  private String call(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("link_only\"");
    assertThat(JSON.readTree(body).findValuesAsString("display_mode")).doesNotContain("link_only");
    return body;
  }

  private List<Map<String, Object>> rows() {
    return jdbc.queryForList(
        "SELECT source, event_date, event_id, day_paths_off, result FROM event_media_search_cache");
  }

  private static void stubDemoDay() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("fixtures/wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSignals(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("fixtures/wikimedia/commons/day-category-1988-09-17.json"))));
    WorldStoryCommonsSourceNoVideosTest.stubRecordedNoVideos();
  }

  @Test
  void theCacheIsOnInThisContext() {
    assertThat(cacheProperties.ttl("commons")).contains(Duration.ofDays(7));
  }

  @Test
  void theLeapDayAsksNoVideoTheSecondTimeAndAnswersTheSame(CapturedOutput output) throws Exception {
    stubLeapDay(false);
    stubLeapDayVideo();

    String first = call("2000-02-29");
    assertThat(videoRequests()).hasSize(2);
    wikimedia.resetRequests();
    String second = call("2000-02-29");

    assertThat(videoRequests()).isEmpty();
    assertThat(second).isEqualTo(first);
    JsonNode video = event(JSON.readTree(second), "시험 문서 가").path("media").get(1);
    assertThat(video.path("id").asString()).isEqualTo("commons-900002");
    assertThat(video.path("selection").path("score").asInt()).isEqualTo(130);
    assertThat(rows())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("source", "commons");
              assertThat(row.get("event_date").toString()).isEqualTo("2000-02-29");
              // the untitled leap day lines show as tragic: the day paths are off
              assertThat(row).containsEntry("day_paths_off", true);
              assertThat((String) row.get("result"))
                  .contains("Synthetic test event footage.webm")
                  .doesNotContain("\"selection\"");
            });
    // an hour later the photos are read again, the video keeps its first retrieved_at
    clock.advance(Duration.ofHours(1));
    wikimedia.resetRequests();
    JsonNode later = event(JSON.readTree(call("2000-02-29")), "시험 문서 가").path("media").get(1);
    assertThat(videoRequests()).isEmpty();
    assertThat(later).isEqualTo(video);
    assertThat(output.getAll()).doesNotContain("Media search cache", "Video search failed");
  }

  @Test
  void theDemoDayWithoutVideosIsCachedToo() throws Exception {
    stubDemoDay();

    String first = call("1988-09-17");
    assertThat(videoRequests()).isNotEmpty();
    wikimedia.resetRequests();
    String second = call("1988-09-17");

    assertThat(videoRequests()).isEmpty();
    assertThat(second).isEqualTo(first);
    assertThat(JSON.readTree(second).findValues("selection")).isEmpty();
    assertThat(rows())
        .isNotEmpty()
        .allSatisfy(
            row -> {
              assertThat(row).containsEntry("day_paths_off", false);
              assertThat((String) row.get("result")).contains("\"candidates\":[]");
            });
  }

  @Test
  void aTragicEventGetsNoRow() throws Exception {
    stubLeapDay(true);
    stubLeapDayVideo();

    call("2000-02-29");

    assertThat(rows()).hasSize(1);
  }

  @Test
  void aFailedCommonsStoresNothingAndTheNextCallAsksAgain() throws Exception {
    stubLeapDay(false);
    stubVideoLinks(okJson(LINKS));
    stubVideos("generator", aResponse().withStatus(503));
    stubVideos("titles", aResponse().withStatus(503));

    JsonNode failed = JSON.readTree(call("2000-02-29"));
    assertThat(failed.path("partial").asBoolean()).isTrue();
    assertThat(rows()).isEmpty();

    stubVideos("generator", okJson(NO_PAGES));
    stubVideos("titles", okJson(FOOTAGE));
    wikimedia.resetRequests();
    JsonNode next = JSON.readTree(call("2000-02-29"));

    assertThat(videoRequests()).hasSize(2);
    assertThat(event(next, "시험 문서 가").path("media").findValuesAsString("type"))
        .containsExactly("photo", "video");
    assertThat(rows()).hasSize(1);
  }

  @Test
  void aLateWikidataAnswerStoresNothingAndTheNextCallAsksAgain() throws Exception {
    stubLeapDay(false);
    // later than media.lookup-deadline (3s)
    stubVideoLinks(okJson(LINKS).withFixedDelay(4_000));
    stubVideos("generator", okJson(NO_PAGES));
    stubVideos("titles", okJson(FOOTAGE));

    assertThat(JSON.readTree(call("2000-02-29")).path("partial").asBoolean()).isTrue();
    assertThat(rows()).isEmpty();

    stubVideoLinks(okJson(LINKS));
    wikimedia.resetRequests();
    call("2000-02-29");

    assertThat(videoRequests()).hasSize(2);
    assertThat(rows()).hasSize(1);
  }

  @Test
  void theDayWithoutEventsReadsAndWritesNothing() throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");

    call("1995-03-15");

    assertThat(videoRequests()).isEmpty();
    assertThat(rows()).isEmpty();
  }

  // ---- plan M3a4c T5: request counts per source, the TTL edge and the other demo date ----

  private static long wikidataVideoRequests() {
    return videoRequests().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(SPARQL_PATH))
        .count();
  }

  private static long commonsVideoRequests() {
    return videoRequests().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(COMMONS_PATH))
        .count();
  }

  @Test
  void theLeapDayRequestsPerSourceDropToZeroOnTheSecondCall() throws Exception {
    stubLeapDay(false);
    stubLeapDayVideo();

    call("2000-02-29");
    // one Wikidata link query and one Commons file batch; the date categories are off that day
    assertThat(wikidataVideoRequests()).isEqualTo(1);
    assertThat(commonsVideoRequests()).isEqualTo(1);
    wikimedia.resetRequests();
    call("2000-02-29");

    assertThat(wikidataVideoRequests()).isZero();
    assertThat(commonsVideoRequests()).isZero();
  }

  @Test
  void theDemoDayRequestsPerSourceDropToZeroOnTheSecondCall() throws Exception {
    stubDemoDay();

    call("1988-09-17");
    long wikidata = wikidataVideoRequests();
    long commons = commonsVideoRequests();
    assertThat(wikidata).isEqualTo(1);
    assertThat(commons).isGreaterThanOrEqualTo(1);
    wikimedia.resetRequests();
    call("1988-09-17");

    assertThat(wikidataVideoRequests()).isZero();
    assertThat(commonsVideoRequests()).isZero();
  }

  @Test
  void theQuarterFinalDayIsCachedToo() throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("fixtures/wikimedia/kowiki/pageprops-2002-06-22.json"));
    stubSignals(SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));

    String first = call("2002-06-22");
    assertThat(videoRequests()).isNotEmpty();
    assertThat(rows())
        .hasSize(1)
        .allSatisfy(row -> assertThat(row).containsEntry("source", "commons"));
    wikimedia.resetRequests();
    String second = call("2002-06-22");

    assertThat(videoRequests()).isEmpty();
    assertThat(second).isEqualTo(first);
  }

  @Test
  void aRowIsStaleExactlyAtFetchedAtPlusTtlThroughTheWholeApplication() throws Exception {
    stubLeapDay(false);
    stubLeapDayVideo();
    call("2000-02-29");
    Duration ttl = cacheProperties.ttl("commons").orElseThrow();

    clock.advance(ttl.minusSeconds(1));
    wikimedia.resetRequests();
    call("2000-02-29");
    assertThat(videoRequests()).isEmpty();

    clock.advance(Duration.ofSeconds(1));
    wikimedia.resetRequests();
    call("2000-02-29");
    assertThat(videoRequests()).hasSize(2);
    assertThat(rows()).hasSize(1); // replaced, not added
  }

  @Test
  void aCachedDateAnswersWithoutWaitingForTheSlowVideoSearch() throws Exception {
    stubLeapDay(false);
    stubVideoLinks(okJson(LINKS).withFixedDelay(1_500)); // slow, but inside media.lookup-deadline
    stubVideos("generator", okJson(NO_PAGES));
    stubVideos("titles", okJson(FOOTAGE));

    long start = System.nanoTime();
    String first = call("2000-02-29");
    Duration firstTime = Duration.ofNanos(System.nanoTime() - start);
    start = System.nanoTime();
    String second = call("2000-02-29");
    Duration secondTime = Duration.ofNanos(System.nanoTime() - start);

    System.out.println(
        "M3a4c T5 world section, leap day with a 1.5 s Wikidata video query (stubs): first "
            + firstTime.toMillis()
            + " ms, cached second call "
            + secondTime.toMillis()
            + " ms");
    assertThat(firstTime).isGreaterThanOrEqualTo(Duration.ofMillis(1_500));
    assertThat(secondTime).isLessThan(firstTime);
    assertThat(second).isEqualTo(first);
  }
}
