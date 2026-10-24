package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

/**
 * Plan M3a4b T4 (4) and (6), the sample-date rows of the plan's table that no other world test
 * covers: the opening-day request counts of the recorded 1988-09-17 answer, and the two tragic
 * sample dates 1950-06-25 and 2014-04-16 (public historical event dates) with invented events: the
 * tragic event's Wikidata item is never asked, no date category is asked, its {@code media} is
 * empty, and the normal event on the same day gets its video. Recorded fixtures, synthetic pages
 * and WireMock only; no request is left unmatched. Rows for the report are printed.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryCommonsVideoSampleDatesTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String KOWIKI = "fixtures/wikimedia/kowiki/";
  private static final String VIDEO = "fixtures/wikimedia/commons/video/";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryCommonsVideoSampleDatesTest.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Section list by title, then the level-2 사건 and 문화 sections; names swapped by {@code from}. */
  private static void stubPage(String title, String prefix, String from, String to) {
    String sections = read(KOWIKI + prefix + "-sections.json").replace(from, to);
    JsonNode parse = JSON.readTree(sections).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(sections)));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(
                    okJson(read(KOWIKI + prefix + "-" + kind + ".json").replace(from, to))));
      }
    }
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("link_only\"");
    return JSON.readTree(body);
  }

  private static List<ServeEvent> videoRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(WikimediaWireMockTest::isVideoSearch)
        .toList();
  }

  private static long countOf(String parameter) {
    return videoRequests().stream()
        .filter(e -> e.getRequest().queryParameter(parameter).isPresent())
        .count();
  }

  private static JsonNode event(JsonNode root, String title) {
    for (JsonNode event : root.path("events")) {
      if (title.equals(event.path("title").asString(null))) {
        return event;
      }
    }
    throw new AssertionError("no event " + title);
  }

  // ---- 1988-09-17, recorded: what the video search asks and finds ----

  @Test
  void theOpeningDayAsksItsLinksAndCategoriesAndFindsNoVideo(CapturedOutput output)
      throws Exception {
    stubPage("9월 17일", "09-17", "9월 17일", "9월 17일");
    stubPage("1988년", "1988", "1988년", "1988년");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read(KOWIKI + "pageprops-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("fixtures/wikimedia/commons/day-category-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .withQueryParam("query", containing("?video"))
            .atPriority(1)
            .willReturn(
                okJson(SparqlWikidataSignalSourceTest.fixture("video-links-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("videoinfo"))
            .withQueryParam("generator", equalTo("categorymembers"))
            .atPriority(1)
            .willReturn(okJson(read(VIDEO + "date-category-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("videoinfo"))
            .withQueryParam("gsrsearch", matching("incategory:\".+\" filetype:video"))
            .atPriority(1)
            .willReturn(okJson(read(VIDEO + "category-1988-summer-olympics.json"))));

    JsonNode root = ok("1988-09-17");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.findValuesAsString("type")).doesNotContain("video");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(output.getAll()).doesNotContain("Video search failed");
    // Wikidata once for the three events, the date category once, no P10 file batch
    assertThat(countOf("query")).isEqualTo(1);
    assertThat(countOf("gcmtitle")).isEqualTo(1);
    assertThat(countOf("titles")).isZero();
    assertThat(videoRequests().size()).isLessThanOrEqualTo(5);
    System.out.println(
        "M3a4b T4 table | 1988-09-17 | world API (recorded) | events "
            + root.path("events").size()
            + " | video none | video requests "
            + videoRequests().size()
            + " (wikidata "
            + countOf("query")
            + ", date category "
            + countOf("gcmtitle")
            + ", category search "
            + countOf("gsrsearch")
            + ")");
  }

  // ---- 1950-06-25 and 2014-04-16: a tragic event and a normal one, invented ----

  private static final String LINKS =
      """
      {"results":{"bindings":[{"item":{"value":"http://www.wikidata.org/entity/Q1"},
       "video":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Synthetic%20test%20event%20footage.webm"},
       "label":{"value":"Synthetic test event"}}]}}""";

  private static String footage(String date) {
    return """
        {"batchcomplete":true,"query":{"pages":[{"pageid":900002,"ns":6,
         "title":"File:Synthetic test event footage.webm","videoinfo":[{
          "url":"https://upload.wikimedia.org/wikipedia/commons/b/bc/Synthetic_test_event_footage.webm",
          "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_test_event_footage.webm",
          "duration":60,"mime":"video/webm","mediatype":"VIDEO","height":360,
          "extmetadata":{"Artist":{"value":"Synthetic Author"},"LicenseShortName":{"value":"CC0"},
           "DateTimeOriginal":{"value":"%s"}}}]}]}}"""
        .formatted(date);
  }

  @ParameterizedTest(name = "[{0}] {1} {2}")
  @CsvSource({"1950-06-25, 6월 25일, 1950년", "2014-04-16, 4월 16일, 2014년"})
  void aTragicEventIsNotAskedAndTheNormalOneGetsItsVideo(
      String date, String dayTitle, String yearTitle, CapturedOutput output) throws Exception {
    String year = yearTitle.replace("년", "");
    stubPage(dayTitle, "synthetic/synthetic-02-29", "2월 29일", dayTitle);
    stubPage(yearTitle, "synthetic/synthetic-2000", "2000년", yearTitle);
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("oldid", equalTo("100"))
            .withQueryParam("section", equalTo("2"))
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"parse\":{\"title\":\""
                        + dayTitle
                        + "\",\"pageid\":1,\"revid\":100,\"wikitext\":\"== 사건 ==\\n* [["
                        + yearTitle
                        + "]] - [[시험 문서 가|시험 사건 가]]가 열렸다.\\n* [["
                        + yearTitle
                        + "]] - [[시험 문서 나|시험 사건 나]]가 일어났다.\"}}")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(
                okJson(
                    """
                    {"batchcomplete":true,"query":{"pages":[
                     {"pageid":1,"ns":0,"title":"시험 문서 가","pageprops":{"wikibase_item":"Q1"}},
                     {"pageid":2,"ns":0,"title":"시험 문서 나","pageprops":{"wikibase_item":"Q2"}}]}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(
                okJson(
                    """
                    {"head":{"vars":["item","tragicClass","deaths","country"]},"results":{"bindings":[
                     {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q1"},
                      "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}},
                     {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q2"},
                      "tragicClass":{"type":"uri","value":"http://www.wikidata.org/entity/Q3839081"},
                      "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}}]}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .withQueryParam("query", containing("?video"))
            .atPriority(1)
            .willReturn(okJson(LINKS)));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("videoinfo"))
            .withQueryParam("titles", matching(".+"))
            .atPriority(1)
            .willReturn(okJson(footage(date))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("videoinfo"))
            .withQueryParam("generator", matching(".+"))
            .atPriority(1)
            .willReturn(okJson("{\"batchcomplete\":true}")));

    JsonNode root = ok(date);

    assertThat(year).isEqualTo(date.substring(0, 4));
    JsonNode tragic = event(root, "시험 문서 나");
    assertThat(tragic.path("tone").asString()).isEqualTo("tragic");
    assertThat(tragic.path("media").isArray()).isTrue();
    assertThat(tragic.path("media").isEmpty()).isTrue();
    JsonNode normal = event(root, "시험 문서 가");
    assertThat(normal.path("tone").asString()).isNotEqualTo("tragic");
    assertThat(normal.path("media").findValuesAsString("type")).containsExactly("video");
    JsonNode video = normal.path("media").get(0);
    assertThat(video.path("captured_at").asString()).isEqualTo(date);
    assertThat(video.path("date_relation").path("kind").asString()).isEqualTo("same_day");
    assertThat(video.path("selection").path("basis").asString()).isEqualTo("commons_p10");
    assertThat(
            videoRequests().stream()
                .filter(e -> e.getRequest().queryParameter("query").isPresent()))
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getRequest().queryParameter("query").firstValue())
                    .contains("wd:Q1")
                    .doesNotContain("wd:Q2"));
    assertThat(countOf("generator")).isZero();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(output.getAll()).doesNotContain("Video search failed");
    System.out.println(
        "M3a4b T4 table | "
            + date
            + " | world API (synthetic tragic + normal event) | normal event video "
            + video.path("id").asString()
            + " | "
            + video.path("selection")
            + " | "
            + video.path("captured_at").asString()
            + " | "
            + video.path("date_relation").path("kind").asString()
            + " | video requests "
            + videoRequests().size()
            + " | tragic media []");
  }
}
