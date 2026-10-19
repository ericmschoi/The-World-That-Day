package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Plan M3a3d T1 through the whole application against WireMock: the demo table with the recorded
 * PageImages and Commons answers (no demo date gets an event photo, decision 6), the synthetic day
 * where a photo attaches (every key, the schema, the place), tragic days, failures and decision 2
 * (b). Synthetic article titles and file names; dates are demo dates and the leap day. No network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryEventPhotoTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  /** Plan M3a3d T2 (7): every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryEventPhotoTest.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
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
  private static void stubPage(String title, String prefix) {
    String kowiki = "wikimedia/kowiki/";
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

  private static void unavailablePage(String title) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(aResponse().withStatus(503)));
  }

  private static void stubPageprops(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  private static void stubSparql(String body) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(okJson(body)));
  }

  /** Priority 1, like the default, so it wins over the pageprops stub on action=query. */
  private static void stubPageImages(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("prop", equalTo("pageimages"))
            .atPriority(1)
            .willReturn(response));
  }

  private static void stubDayCategory(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("generator", equalTo("categorymembers"))
            .willReturn(okJson(body)));
  }

  private static void stubFileBatch(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("titles", matching(".+"))
            .willReturn(response));
  }

  private static void stubOpeningDay() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    stubDayCategory(read("wikimedia/commons/day-category-1988-09-17.json"));
  }

  /** The leap day pages; only 시험 문서 가 (Q1, in Korea, no tragic signal) is a normal event. */
  private static void stubLeapDay() {
    stubPage("2월 29일", "synthetic/synthetic-02-29");
    stubPage("2000년", "synthetic/synthetic-2000");
    stubPageprops(
        """
        {"batchcomplete":true,"query":{"pages":[{"pageid":1,"ns":0,"title":"시험 문서 가",
         "pageprops":{"wikibase_item":"Q1"}}]}}""");
    stubSparql(
        """
        {"head":{"vars":["item","tragicClass","deaths","country"]},"results":{"bindings":[
         {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q1"},
          "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}}]}}""");
  }

  private static final String SYNTHETIC_FILE =
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

  private static void synthPhotoLeadImage() {
    stubPageImages(
        okJson(
            "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,"
                + "\"title\":\"시험 문서 가\",\"pageimage\":\"Synthetic_test_photo.jpg\"}]}}"));
  }

  // ---- calls and counts ----

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("link_only\"", ".svg");
    JsonNode root = JSON.readTree(body);
    List<JsonNode> items = new ArrayList<>();
    root.path("events").forEach(event -> event.path("media").forEach(items::add));
    root.path("day_media").forEach(items::add);
    for (JsonNode item : items) {
      ObjectNode stored = (ObjectNode) item.deepCopy();
      stored.remove("date_relation");
      assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), stored)).isEmpty();
    }
    return root;
  }

  private static String param(ServeEvent event, String name) {
    QueryParameter value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  private static List<ServeEvent> served(String path) {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(path + "?"))
        .toList();
  }

  private static List<ServeEvent> pageImagesRequests() {
    return served(KOWIKI_PATH).stream()
        .filter(event -> "pageimages".equals(param(event, "prop")))
        .toList();
  }

  private static List<String> askedTitles() {
    return pageImagesRequests().stream()
        .flatMap(event -> Arrays.stream(param(event, "titles").split("\\|")))
        .toList();
  }

  private static long dayCategoryRequests() {
    return served(COMMONS_PATH).stream().filter(e -> param(e, "generator") != null).count();
  }

  private static long fileBatchRequests() {
    return served(COMMONS_PATH).stream().filter(e -> param(e, "titles") != null).count();
  }

  private static List<String> ids(JsonNode items) {
    List<String> ids = new ArrayList<>();
    items.forEach(item -> ids.add(item.path("id").asString()));
    return ids;
  }

  private static void assertEveryEventMediaEmpty(JsonNode root) {
    root.path("events")
        .forEach(
            event -> {
              assertThat(event.path("media").isArray()).isTrue();
              assertThat(event.path("media").isEmpty()).isTrue();
            });
  }

  // ---- demo table (recorded answers; decision 6: no event photo on any demo date) ----

  @Test
  void openingDayAsksPageImagesOnceAndDropsTheSvgLogo() throws Exception {
    stubOpeningDay();
    stubPageImages(okJson(read("wikimedia/kowiki/pageimages-1988-09-17.json")));
    stubFileBatch(okJson(read("wikimedia/commons/files-1988-09-17.json")));

    JsonNode root = ok("1988-09-17");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertEveryEventMediaEmpty(root);
    assertThat(ids(root.path("day_media")))
        .containsExactly(
            "commons-4930021",
            "commons-4196553",
            "commons-112453227",
            "commons-4929997",
            "commons-26546944");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(pageImagesRequests()).hasSize(1);
    assertThat(askedTitles()).containsExactly("1988년 하계 올림픽", "KBS 1TV", "KBS 2TV");
    assertThat(served(COMMONS_PATH)).hasSize(2);
    assertThat(dayCategoryRequests()).isEqualTo(1);
    assertThat(fileBatchRequests()).isEqualTo(1);
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void quarterFinalDayHasNoLeadImageAndNoFileBatch() throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("wikimedia/kowiki/pageprops-2002-06-22.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));
    stubDayCategory(read("wikimedia/commons/day-category-2002-06-22.json"));
    stubPageImages(okJson(read("wikimedia/kowiki/pageimages-2002-06-22.json")));

    JsonNode root = ok("2002-06-22");

    assertEveryEventMediaEmpty(root);
    assertThat(ids(root.path("day_media"))).containsExactly("commons-129150318");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(askedTitles()).containsExactly("2002년 FIFA 월드컵");
    assertThat(served(COMMONS_PATH)).hasSize(1);
    assertThat(fileBatchRequests()).isZero();
  }

  @Test
  void dayWithoutEventsAsksNoPageImages() throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");
    stubDayCategory(read("wikimedia/commons/day-category-1995-03-15.json"));

    JsonNode root = ok("1995-03-15");

    assertThat(root.path("notice").path("code").asString()).isEqualTo("NO_EVENTS_FOUND");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(pageImagesRequests()).isEmpty();
    assertThat(served(COMMONS_PATH)).hasSize(1);
  }

  // ---- synthetic: a photo attaches (T1 (2), (13)) ----

  @Test
  void attachedPhotoHasEveryKeyAndTheEventsPlace(CapturedOutput output) throws Exception {
    stubLeapDay();
    synthPhotoLeadImage();
    stubFileBatch(okJson(SYNTHETIC_FILE));

    JsonNode root = ok("2000-02-29");

    JsonNode event = null;
    for (JsonNode candidate : root.path("events")) {
      if ("시험 문서 가".equals(candidate.path("title").asString(null))) {
        event = candidate;
      }
    }
    assertThat(event).isNotNull();
    assertThat(event.path("tone").asString()).isEqualTo("normal");
    assertThat(event.path("media").size()).isEqualTo(1);
    JsonNode photo = event.path("media").get(0);
    // MediaRecords.STORED_KEYS (26) plus date_relation, as for the day photos
    assertThat(new HashSet<>(photo.propertyNames())).isEqualTo(WorldStoryDayMediaTest.ITEM_KEYS);
    for (String key :
        List.of("video_sources", "duration_seconds", "embed_url", "provider", "render", "lat")) {
      assertThat(photo.get(key).isNull()).as(key).isTrue();
    }
    assertThat(photo.path("id").asString()).isEqualTo("commons-900001");
    assertThat(photo.path("display_mode").asString()).isEqualTo("image");
    assertThat(photo.path("place_ref").asString()).isEqualTo("country:KR");
    assertThat(photo.path("captured_at").asString()).isEqualTo("1998");
    assertThat(photo.path("date_relation").path("kind").asString()).isEqualTo("years_apart");
    assertThat(photo.path("date_relation").path("years").asInt()).isEqualTo(-2);
    for (JsonNode other : root.path("events")) {
      if (other != event) {
        assertThat(other.path("media").isEmpty()).as("only the normal event").isTrue();
      }
    }
    assertThat(askedTitles()).containsExactly("시험 문서 가");
    assertThat(output.getAll()).doesNotContain("Synthetic", "시험 문서", "File:");
  }

  // ---- tragic days (T1 (4)) ----

  /** 시험 문서 가 is tragic by its Wikidata class; the other leap day events have no title. */
  @Test
  void tragicEventIsNeverAskedAndTheDayHasNoDayPhotos() throws Exception {
    stubLeapDay();
    stubSparql(
        """
        {"head":{"vars":["item","tragicClass","deaths","country"]},"results":{"bindings":[
         {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q1"},
          "tragicClass":{"type":"uri","value":"http://www.wikidata.org/entity/Q3839081"},
          "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q884"}}]}}""");
    synthPhotoLeadImage();
    stubFileBatch(okJson(SYNTHETIC_FILE));
    stubDayCategory("{\"batchcomplete\":true}");

    JsonNode root = ok("2000-02-29");

    assertThat(root.path("events").findValuesAsString("tone")).containsOnly("tragic");
    assertEveryEventMediaEmpty(root);
    assertThat(pageImagesRequests()).as("no title of a tragic event is asked").isEmpty();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(served(COMMONS_PATH)).isEmpty();
  }

  // ---- failures (T1 (7)) ----

  @Test
  void failedPageImagesEmptiesOnlyTheEventPhotos() throws Exception {
    stubOpeningDay();
    stubPageImages(aResponse().withStatus(503));

    JsonNode root = ok("1988-09-17");

    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").size()).isEqualTo(5);
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(fileBatchRequests()).isZero();
  }

  @Test
  void fileBatchWithoutAQueryEmptiesOnlyTheEventPhotos() throws Exception {
    stubOpeningDay();
    stubPageImages(okJson(read("wikimedia/kowiki/pageimages-1988-09-17.json")));
    stubFileBatch(okJson("{\"batchcomplete\":true}"));

    JsonNode root = ok("1988-09-17");

    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").size()).isEqualTo(5);
    assertThat(root.path("partial").asBoolean()).isTrue();
  }

  // ---- decision 2 (b) (T1 (14)) ----

  @Test
  void unavailableYearPageTurnsTheDayPhotosOffButStillAsksForEventPhotos() throws Exception {
    stubPage("9월 17일", "09-17");
    unavailablePage("1988년");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    stubDayCategory(read("wikimedia/commons/day-category-1988-09-17.json"));

    JsonNode root = ok("1988-09-17");

    assertThat(root.path("events").isEmpty()).isFalse();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(dayCategoryRequests()).isZero();
    assertThat(pageImagesRequests()).hasSize(1);
    assertThat(root.path("partial").asBoolean()).isTrue();
  }

  @Test
  void unavailableDatePageTurnsTheDayPhotosOff() throws Exception {
    unavailablePage("9월 17일");
    stubPage("1988년", "1988");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    stubDayCategory(read("wikimedia/commons/day-category-1988-09-17.json"));

    JsonNode root = ok("1988-09-17");

    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(dayCategoryRequests()).isZero();
    assertThat(root.path("partial").asBoolean()).isTrue();
  }

  // ---- logs (T1 (15)) ----

  @Test
  void eventPhotoWarningCarriesNoDateTitleOrFileName(CapturedOutput output) throws Exception {
    stubLeapDay();
    synthPhotoLeadImage();
    stubFileBatch(aResponse().withStatus(503));

    JsonNode root = ok("2000-02-29");

    assertThat(root.path("partial").asBoolean()).isTrue();
    List<String> lines =
        output
            .getAll()
            .lines()
            .filter(l -> l.contains("Wikimedia call failed") && l.contains("commons files of"))
            .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
            .toList();
    assertThat(lines)
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons files of 1 cause=status 503");
    assertThat(lines).allSatisfy(l -> assertThat(ISO_DATE.matcher(l).find()).isFalse());
    assertThat(output.getAll()).doesNotContain("Synthetic", "시험 문서", "File:", "kowiki-2000-02-29");
  }
}
