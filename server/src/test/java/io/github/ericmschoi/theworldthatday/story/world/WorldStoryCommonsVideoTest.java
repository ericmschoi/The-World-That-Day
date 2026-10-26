package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a4b T3 (6), (9) and (10) through the whole application with the Commons video source
 * against WireMock (no network): the recorded quarter-final day finds no video, the day without
 * events asks nothing, the synthetic leap day gets a photo then a video, a tragic event is never
 * asked, and a failed Commons or a late Wikidata answer empties only the videos. Synthetic article
 * titles, files and made-up QIDs; the dates are a demo date, the leap day and a day without events.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryCommonsVideoTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String VIDEO = "fixtures/wikimedia/commons/video/";
  private static final String APP_LOGGER = "io.github.ericmschoi.theworldthatday";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;
  private Level before;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    Logger app = (Logger) LoggerFactory.getLogger(APP_LOGGER);
    before = app.getLevel();
    app.setLevel(Level.DEBUG);
  }

  /** Every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    ((Logger) LoggerFactory.getLogger(APP_LOGGER)).setLevel(before);
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  static String read(String path) {
    try (InputStream in =
        WorldStoryCommonsVideoTest.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // ---- stubs ----

  /** Section list by title, then the level-2 사건 and 문화 sections by revid and index. */
  static void stubPage(String title, String prefix) {
    String kowiki = "fixtures/wikimedia/kowiki/";
    JsonNode parse = JSON.readTree(read(kowiki + prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(kowiki + prefix + "-sections.json"))));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(okJson(read(kowiki + prefix + "-" + kind + ".json"))));
      }
    }
  }

  static void stubPageprops(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  /** The tone and scope signals (the video query is told apart by its {@code ?video}). */
  static void stubSignals(String body) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(okJson(body)));
  }

  static void stubVideoLinks(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .withQueryParam("query", containing("?video"))
            .atPriority(1)
            .willReturn(response));
  }

  /** A Commons video request: {@code generator} (date category or category search) or files. */
  static void stubVideos(String parameter, ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("videoinfo"))
            .withQueryParam(parameter, matching(".+"))
            .atPriority(1)
            .willReturn(response));
  }

  static final String NO_PAGES = "{\"batchcomplete\":true}";

  /**
   * The leap day: 시험 문서 가 (Q1, in Korea) and, when {@code withTragic}, 시험 문서 나 (Q2, a disaster by
   * its Wikidata class) in the 2000 lines of the 2월 29일 page. The untitled lines (2000년 page, 문화)
   * have no signal and show as tragic, so the day paths (date categories) are off.
   */
  static void stubLeapDay(boolean withTragic) {
    stubPage("2월 29일", "synthetic/synthetic-02-29");
    stubPage("2000년", "synthetic/synthetic-2000");
    String second = withTragic ? "\\n* [[2000년]] - [[시험 문서 나|시험 사건 나]]가 일어났다." : "";
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("oldid", equalTo("100"))
            .withQueryParam("section", equalTo("2"))
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"parse\":{\"title\":\"2월 29일\",\"pageid\":1,\"revid\":100,\"wikitext\":"
                        + "\"== 사건 ==\\n* [[2000년]] - [[시험 문서 가|시험 사건 가]]가 열렸다."
                        + second
                        + "\"}}")));
    stubPageprops(
        """
        {"batchcomplete":true,"query":{"pages":[
         {"pageid":1,"ns":0,"title":"시험 문서 가","pageprops":{"wikibase_item":"Q1"}},
         {"pageid":2,"ns":0,"title":"시험 문서 나","pageprops":{"wikibase_item":"Q2"}}]}}""");
    stubSignals(
        """
        {"head":{"vars":["item","tragicClass","deaths","country"]},"results":{"bindings":[
         {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q1"},
          "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}},
         {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q2"},
          "tragicClass":{"type":"uri","value":"http://www.wikidata.org/entity/Q3839081"},
          "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}}]}}""");
    // the lead image of 시험 문서 가 and its file (the photo path: prop=imageinfo)
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("prop", equalTo("pageimages"))
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,"
                        + "\"title\":\"시험 문서 가\",\"pageimage\":\"Synthetic_test_photo.jpg\"}]}}")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("imageinfo"))
            .withQueryParam("titles", matching(".+"))
            .willReturn(okJson(PHOTO)));
  }

  static final String PHOTO =
      """
      {"batchcomplete":true,"query":{
       "normalized":[{"fromencoded":false,"from":"File:Synthetic_test_photo.jpg",
        "to":"File:Synthetic test photo.jpg"}],
       "pages":[{"pageid":900001,"ns":6,
       "title":"File:Synthetic test photo.jpg","imageinfo":[{
        "url":"https://upload.wikimedia.org/wikipedia/commons/0/00/Synthetic_test_photo.jpg",
        "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_test_photo.jpg",
        "thumburl":"https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Synthetic_test_photo.jpg/640px-Synthetic_test_photo.jpg",
        "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
        "extmetadata":{"Artist":{"value":"Synthetic Author"},
         "LicenseShortName":{"value":"CC BY-SA 4.0"},"DateTimeOriginal":{"value":"1998"}}}]}]}}""";

  /** Q1's P10 file, an English label, no category. */
  static final String LINKS =
      """
      {"results":{"bindings":[{"item":{"value":"http://www.wikidata.org/entity/Q1"},
       "video":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Synthetic%20test%20event%20footage.webm"},
       "label":{"value":"Synthetic test event"}}]}}""";

  /** The P10 file: free, filmed on the leap day. */
  static final String FOOTAGE =
      """
      {"batchcomplete":true,"query":{"pages":[{"pageid":900002,"ns":6,
       "title":"File:Synthetic test event footage.webm","videoinfo":[{
        "url":"https://upload.wikimedia.org/wikipedia/commons/b/bc/Synthetic_test_event_footage.webm",
        "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_test_event_footage.webm",
        "duration":60,"mime":"video/webm","mediatype":"VIDEO","height":360,
        "extmetadata":{"Artist":{"value":"Synthetic Author"},"LicenseShortName":{"value":"CC0"},
         "DateTimeOriginal":{"value":"2000-02-29"}}}]}]}}""";

  static void stubLeapDayVideo() {
    stubVideoLinks(okJson(LINKS));
    stubVideos("generator", okJson(NO_PAGES));
    stubVideos("titles", okJson(FOOTAGE));
  }

  // ---- calls and counts ----

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("link_only\"");
    assertThat(JSON.readTree(body).findValuesAsString("display_mode")).doesNotContain("link_only");
    JsonNode root = JSON.readTree(body);
    List<JsonNode> items = new ArrayList<>();
    root.path("events").forEach(event -> event.path("media").forEach(items::add));
    root.path("day_media").forEach(items::add);
    for (JsonNode item : items) {
      ObjectNode stored = (ObjectNode) item.deepCopy();
      stored.remove("date_relation");
      stored.remove("selection");
      assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), stored)).isEmpty();
    }
    return root;
  }

  static List<ServeEvent> videoRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(WikimediaWireMockTest::isVideoSearch)
        .toList();
  }

  static List<ServeEvent> videoRequests(String parameter) {
    return videoRequests().stream()
        .filter(e -> e.getRequest().queryParameter(parameter).isPresent())
        .toList();
  }

  static JsonNode event(JsonNode root, String title) {
    for (JsonNode event : root.path("events")) {
      if (title.equals(event.path("title").asString(null))) {
        return event;
      }
    }
    throw new AssertionError("no event " + title);
  }

  private static void assertNoValueLogged(CapturedOutput output, JsonNode root) {
    List<String> values =
        new ArrayList<>(
            List.of(
                "2000-02-29",
                "2002-06-22",
                "Q1",
                "Q2",
                "Synthetic",
                "Special:FilePath",
                "Category:",
                "File:",
                "Videos taken on",
                "2002 FIFA World Cup"));
    root.path("events").forEach(event -> values.add(event.path("id").asString()));
    assertThat(output.getAll()).doesNotContain(values.toArray(String[]::new));
  }

  // ---- (6) demo dates: recorded, no video ----

  @Test
  void theQuarterFinalDayFindsNoCommonsVideo(CapturedOutput output) throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("fixtures/wikimedia/kowiki/pageprops-2002-06-22.json"));
    stubSignals(SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));
    stubVideoLinks(okJson(SparqlWikidataSignalSourceTest.fixture("video-links-2002-06-22.json")));
    stubVideos("gcmtitle", okJson(read(VIDEO + "date-category-2002-06-22.json")));
    stubVideos("gsrsearch", okJson(read(VIDEO + "category-2002-fifa-world-cup.json")));

    JsonNode root = ok("2002-06-22");

    assertThat(root.path("events").size()).isEqualTo(1);
    assertThat(root.findValuesAsString("type")).doesNotContain("video");
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(output.getAll()).doesNotContain("Video search failed", "Wikimedia call failed");
    assertThat(videoRequests()).hasSize(3);
    assertThat(videoRequests("gcmtitle")).hasSize(1);
    assertThat(videoRequests("gsrsearch")).hasSize(1);
    assertNoValueLogged(output, root);
  }

  @Test
  void theDayWithoutEventsAsksNoVideo() throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");

    JsonNode root = ok("1995-03-15");

    assertThat(root.path("notice").path("code").asString()).isEqualTo("NO_EVENTS_FOUND");
    assertThat(videoRequests()).isEmpty();
    assertThat(wikimedia.getAllServeEvents())
        .noneMatch(e -> e.getRequest().getUrl().startsWith(SPARQL_PATH));
  }

  // ---- (6) the synthetic leap day: a photo, then its video ----

  @Test
  void theLeapDayEventShowsItsPhotoThenItsP10Video(CapturedOutput output) throws Exception {
    stubLeapDay(false);
    stubLeapDayVideo();

    JsonNode root = ok("2000-02-29");

    JsonNode media = event(root, "시험 문서 가").path("media");
    assertThat(media.findValuesAsString("type")).containsExactly("photo", "video");
    JsonNode video = media.get(1);
    assertThat(video.path("id").asString()).isEqualTo("commons-900002");
    assertThat(video.path("display_mode").asString()).isEqualTo("video_file");
    assertThat(video.path("captured_at").asString()).isEqualTo("2000-02-29");
    assertThat(video.path("date_precision").asString()).isEqualTo("day");
    assertThat(video.path("place_ref").asString()).isEqualTo("country:KR");
    assertThat(video.path("date_relation").path("kind").asString()).isEqualTo("same_day");
    assertThat(video.path("selection").path("basis").asString()).isEqualTo("commons_p10");
    assertThat(video.path("selection").path("score").asInt()).isEqualTo(130);
    assertThat(media.get(0).has("selection")).isFalse();
    assertThat(root.path("partial").asBoolean()).isFalse();
    // the untitled leap day lines show as tragic, so no date category: links, then the P10 file
    assertThat(videoRequests()).hasSize(2);
    assertThat(videoRequests("generator")).isEmpty();
    assertNoValueLogged(output, root);
  }

  // ---- (6) a tragic event on the day ----

  @Test
  void aTragicEventIsNeverAskedAndItsDayHasNoDateCategory(CapturedOutput output) throws Exception {
    stubLeapDay(true);
    stubLeapDayVideo();

    JsonNode root = ok("2000-02-29");

    JsonNode tragic = event(root, "시험 문서 나");
    assertThat(tragic.path("tone").asString()).isEqualTo("tragic");
    assertThat(tragic.path("media").isArray()).isTrue();
    assertThat(tragic.path("media").isEmpty()).isTrue();
    assertThat(event(root, "시험 문서 가").path("media").findValuesAsString("type"))
        .containsExactly("photo", "video");
    assertThat(videoRequests("query"))
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getRequest().queryParameter("query").firstValue())
                    .contains("wd:Q1")
                    .doesNotContain("wd:Q2"));
    assertThat(videoRequests("generator")).isEmpty();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertNoValueLogged(output, root);
  }

  // ---- (9) one source fails alone ----

  @Test
  void aFailedCommonsLeavesOnlyTheVideosEmpty(CapturedOutput output) throws Exception {
    stubLeapDay(false);
    stubVideoLinks(okJson(LINKS));
    stubVideos("generator", aResponse().withStatus(503));
    stubVideos("titles", aResponse().withStatus(503));

    JsonNode root = ok("2000-02-29");

    assertThat(event(root, "시험 문서 가").path("media").findValuesAsString("type"))
        .containsExactly("photo");
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(output.getAll().lines().filter(l -> l.contains(" WARN ")).toList())
        .singleElement()
        .satisfies(l -> assertThat(l).contains("title=commons video files of 1 cause=status 503"));
    assertThat(output.getAll()).doesNotContain("Video search failed");
    assertNoValueLogged(output, root);
  }

  @Test
  void aLateWikidataAnswerLeavesOnlyTheVideosEmpty(CapturedOutput output) throws Exception {
    stubLeapDay(false);
    // later than media.lookup-deadline (3s), the read timeout of the request and of the lookup
    stubVideoLinks(okJson(LINKS).withFixedDelay(4_000));
    stubVideos("generator", okJson(NO_PAGES));
    stubVideos("titles", okJson(FOOTAGE));

    JsonNode root = ok("2000-02-29");

    assertThat(event(root, "시험 문서 가").path("media").findValuesAsString("type"))
        .containsExactly("photo");
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(videoRequests("titles")).isEmpty();
    assertNoValueLogged(output, root);
  }
}
