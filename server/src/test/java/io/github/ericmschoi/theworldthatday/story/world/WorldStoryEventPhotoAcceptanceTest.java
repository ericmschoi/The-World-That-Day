package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
 * Plan M3a3d T2 (1): the T1 criteria that only the lookup or the service tested so far, now through
 * the whole application against WireMock. The synthetic "photo attaches" day with the same file in
 * the day category (demo table row 5, T1 (6)), the event path failures that keep the day photos (T1
 * (7)), decision 2 (b) with the date page down and with an absent year page (T1 (14)), one silent
 * media path at a time within one window (T1 (9)) and the DEBUG lines (T1 (15)). Synthetic article
 * titles and file names on the leap day 2000-02-29, recorded answers on the demo date 1988-09-17.
 * No request is left unmatched (T2 (7)); no network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryEventPhotoAcceptanceTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final int SILENT_MS = 12_000;
  private static final String LEAP_DAY = "2000-02-29";
  private static final String TITLE = "시험 문서 가";
  private static final String FILE = "Synthetic test photo.jpg";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MediaLookupProperties mediaLookup;
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
        WorldStoryEventPhotoAcceptanceTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/" + path)) {
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

  private static void stubDayCategory(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("generator", equalTo("categorymembers"))
            .willReturn(response));
  }

  private static void stubFileBatch(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("titles", matching(".+"))
            .willReturn(response));
  }

  /** The demo date 1988-09-17 with its recorded pages, signals and day category (5 photos). */
  private static void stubOpeningDay() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    stubDayCategory(okJson(read("wikimedia/commons/day-category-1988-09-17.json")));
  }

  /**
   * A leap day with one event only: 시험 문서 가 (Q1, in Korea, no tragic signal), so the day is not a
   * tragic day and its day photos are looked up. The year page does not exist (ABSENT), which
   * neither turns the day photos off nor makes the answer partial.
   */
  private static void stubOneEventLeapDay() {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo("2월 29일"))
            .willReturn(
                okJson(
                    """
                    {"parse":{"title":"2월 29일","pageid":1,"revid":100,"redirects":[],
                     "sections":[{"toclevel":1,"level":"2","line":"사건","number":"1",
                     "index":"1","anchor":"사건"}]}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("oldid", equalTo("100"))
            .withQueryParam("section", equalTo("1"))
            .willReturn(
                okJson(
                    """
                    {"parse":{"title":"2월 29일","pageid":1,"revid":100,
                     "wikitext":"== 사건 ==\\n* [[2000년]] - [[시험 문서 가|시험 사건 가]]가 열렸다."}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo("2000년"))
            .willReturn(okJson(read("wikimedia/kowiki/synthetic/synthetic-missingtitle.json"))));
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

  /** A passing synthetic Commons JPEG page (no GPS) titled {@code File:<fileName>}. */
  private static String filePage(long id, String fileName, String taken) {
    String path = "0/00/" + fileName.replace(' ', '_');
    return """
        {"pageid":%d,"ns":6,"title":"File:%s","imageinfo":[{
         "url":"https://upload.wikimedia.org/wikipedia/commons/%s",
         "descriptionurl":"https://commons.wikimedia.org/wiki/File:%s",
         "thumburl":"https://upload.wikimedia.org/wikipedia/commons/thumb/%s/640px-%s",
         "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
         "extmetadata":{"Artist":{"value":"Synthetic Author"},
          "LicenseShortName":{"value":"CC BY-SA 4.0"},"DateTimeOriginal":{"value":"%s"}}}]}"""
        .formatted(
            id,
            fileName,
            path,
            fileName.replace(' ', '_'),
            path,
            fileName.replace(' ', '_'),
            taken);
  }

  private static String pages(String... pages) {
    return "{\"batchcomplete\":true,\"query\":{\"pages\":[" + String.join(",", pages) + "]}}";
  }

  /** PageImages gives 시험 문서 가 the lead image {@link #FILE}. */
  private static void leadImage() {
    stubPageImages(
        okJson(
            pages(
                "{\"pageid\":1,\"ns\":0,\"title\":\"%s\",\"pageimage\":\"%s\"}"
                    .formatted(TITLE, FILE))));
  }

  // ---- calls and counts ----

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("\"link_only\"", ".svg");
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
        .filter(event -> !isVideoSearch(event))
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

  private static JsonNode onlyEvent(JsonNode root) {
    assertThat(root.path("events").size()).isEqualTo(1);
    JsonNode event = root.path("events").get(0);
    assertThat(event.path("title").asString()).isEqualTo(TITLE);
    assertThat(event.path("tone").asString()).isEqualTo("normal");
    return event;
  }

  private static void assertEveryEventMediaEmpty(JsonNode root) {
    assertThat(root.path("events").isEmpty()).isFalse();
    root.path("events")
        .forEach(
            event -> {
              assertThat(event.path("media").isArray()).isTrue();
              assertThat(event.path("media").isEmpty()).isTrue();
            });
  }

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Wikimedia call failed"))
        .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
        .toList();
  }

  private Duration oneWindow() {
    return mediaLookup.lookupDeadline().plus(Deadline.CANCEL_GRACE).plusSeconds(1);
  }

  // ---- T2 (4): the demo media table, event slots included ----

  /** One row of the T2 (4) table: position, event, order and the fields the plan names. */
  private static String row(String position, String event, int order, JsonNode item) {
    JsonNode relation = item.path("date_relation");
    return String.join(
        " | ",
        position,
        event,
        Integer.toString(order),
        item.path("id").asString(),
        item.path("display_mode").asString(),
        item.path("license").asString("null"),
        item.path("license_status").asString(),
        item.path("captured_at").asString("null"),
        relation.path("kind").asString() + "/" + relation.path("years").asInt(),
        item.path("place_ref").asString("null"));
  }

  /** Every media item of the answer as a row; an event without media is one "(empty)" row. */
  private static List<String> mediaTable(JsonNode root) {
    List<String> rows = new ArrayList<>();
    JsonNode events = root.path("events");
    for (int n = 0; n < events.size(); n++) {
      JsonNode event = events.get(n);
      String position = "events[" + n + "]";
      String title = event.path("title").asString();
      if (event.path("media").isEmpty()) {
        rows.add(position + " | " + title + " | (empty)");
      }
      for (int i = 0; i < event.path("media").size(); i++) {
        rows.add(row(position, title, i + 1, event.path("media").get(i)));
      }
    }
    JsonNode day = root.path("day_media");
    for (int i = 0; i < day.size(); i++) {
      rows.add(row("day_media", "-", i + 1, day.get(i)));
    }
    return rows;
  }

  @Test
  void demoMediaTableHasEmptyEventSlotsAndTheM3a3bDayPhotos() throws Exception {
    stubOpeningDay();
    stubPageImages(okJson(read("wikimedia/kowiki/pageimages-1988-09-17.json")));
    stubFileBatch(okJson(read("wikimedia/commons/files-1988-09-17.json")));
    List<String> opening = mediaTable(ok("1988-09-17"));
    wikimedia.resetRequests();

    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("wikimedia/kowiki/pageprops-2002-06-22.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));
    stubDayCategory(okJson(read("wikimedia/commons/day-category-2002-06-22.json")));
    stubPageImages(okJson(read("wikimedia/kowiki/pageimages-2002-06-22.json")));
    List<String> quarterFinal = mediaTable(ok("2002-06-22"));
    wikimedia.resetRequests();

    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");
    stubDayCategory(okJson(read("wikimedia/commons/day-category-1995-03-15.json")));
    List<String> noEvents = mediaTable(ok("1995-03-15"));

    System.out.println("M3a3d T2 (4) 1988-09-17:\n" + String.join("\n", opening));
    System.out.println("M3a3d T2 (4) 2002-06-22:\n" + String.join("\n", quarterFinal));
    System.out.println("M3a3d T2 (4) 1995-03-15:\n" + String.join("\n", noEvents));

    // decision 6: no event photo on any demo date; the day photos are those of M3a3b
    String pd = " | image | Public domain | free | 1988-09-17 | same_day/0 | country:KR";
    assertThat(opening)
        .containsExactly(
            "events[0] | 1988년 하계 올림픽 | (empty)",
            "events[1] | KBS 1TV | (empty)",
            "events[2] | KBS 2TV | (empty)",
            "day_media | - | 1 | commons-4930021" + pd,
            "day_media | - | 2 | commons-4196553" + pd,
            "day_media | - | 3 | commons-112453227" + pd,
            "day_media | - | 4 | commons-4929997" + pd,
            "day_media | - | 5 | commons-26546944 | image | CC BY-SA 3.0 | free | null | unknown/0"
                + " | country:KR");
    assertThat(quarterFinal)
        .containsExactly(
            "events[0] | 2002년 FIFA 월드컵 | (empty)",
            "day_media | - | 1 | commons-129150318 | image | KOGL Type 1 | free | 2002-06-22"
                + " | same_day/0 | country:KR");
    assertThat(noEvents).isEmpty();
  }

  // ---- demo table row 5 and T1 (6): the photo leaves the day photos ----

  @Test
  void photoOnItsEventLeavesTheSameFileOutOfTheDayPhotos() throws Exception {
    stubOneEventLeapDay();
    leadImage();
    stubFileBatch(okJson(pages(filePage(900001, FILE, "1998"))));
    stubDayCategory(
        okJson(
            pages(
                filePage(900001, FILE, "1998"),
                filePage(900002, "Synthetic day photo.jpg", LEAP_DAY))));

    JsonNode root = ok(LEAP_DAY);

    JsonNode event = onlyEvent(root);
    assertThat(ids(event.path("media"))).containsExactly("commons-900001");
    JsonNode photo = event.path("media").get(0);
    assertThat(photo.path("display_mode").asString()).isEqualTo("image");
    assertThat(photo.path("place_ref").asString()).as("the event's scope").isEqualTo("country:KR");
    assertThat(photo.path("date_relation").path("kind").asString()).isEqualTo("years_apart");
    assertThat(photo.path("date_relation").path("years").asInt()).isEqualTo(-2);
    assertThat(ids(root.path("day_media")))
        .as("the event photo is not repeated in day_media")
        .containsExactly("commons-900002");
    assertThat(root.path("partial").asBoolean()).as("an absent year page is not partial").isFalse();
    assertThat(askedTitles()).containsExactly(TITLE);
    assertThat(dayCategoryRequests()).isEqualTo(1);
    assertThat(fileBatchRequests()).isEqualTo(1);
  }

  // ---- T1 (7): a failed event path keeps the day photos ----

  static Stream<Arguments> eventPathFailures() {
    return Stream.of(
        Arguments.of(
            "PageImages error without a code",
            okJson("{\"error\":{\"info\":\"x\"}}"),
            "source=kowiki title=kowiki pageimages of 3 cause=api error"),
        Arguments.of(
            "file batch 503",
            okJson(read("wikimedia/kowiki/pageimages-1988-09-17.json")),
            "source=commons title=commons files of 1 cause=status 503"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("eventPathFailures")
  void failedEventPathKeepsTheDayPhotosAndMarksThePartial(
      String name, ResponseDefinitionBuilder lead, String warning, CapturedOutput output)
      throws Exception {
    stubOpeningDay();
    stubPageImages(lead);
    stubFileBatch(aResponse().withStatus(503));

    JsonNode root = ok("1988-09-17");

    assertEveryEventMediaEmpty(root);
    assertThat(ids(root.path("day_media")))
        .containsExactly(
            "commons-4930021",
            "commons-4196553",
            "commons-112453227",
            "commons-4929997",
            "commons-26546944");
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(failures(output)).containsExactly("Wikimedia call failed: " + warning);
  }

  // ---- T1 (14): decision 2 (b) ----

  @Test
  void unavailableDatePageTurnsTheDayPhotosOffButStillAsksForEventPhotos() throws Exception {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo("9월 17일"))
            .willReturn(aResponse().withStatus(503)));
    stubPage("1988년", "1988");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"));
    stubDayCategory(okJson(read("wikimedia/commons/day-category-1988-09-17.json")));

    JsonNode root = ok("1988-09-17");

    assertThat(root.path("events").isEmpty()).as("the year page still has the opening").isFalse();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(dayCategoryRequests()).isZero();
    assertThat(pageImagesRequests()).hasSize(1);
    assertThat(askedTitles()).contains("1988년 하계 올림픽");
    assertThat(root.path("partial").asBoolean()).isTrue();
  }

  // ---- T1 (9): one silent path at a time, within one window ----

  @Test
  void silentPageImagesKeepsTheDayPhotosWithinOneWindow() throws Exception {
    stubOpeningDay();
    stubPageImages(okJson(NO_PAGE_IMAGES).withFixedDelay(SILENT_MS));

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took).isLessThanOrEqualTo(oneWindow());
    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").size()).isEqualTo(5);
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(fileBatchRequests()).isZero();
  }

  @Test
  void silentDayCategoryKeepsTheEventPhotoWithinOneWindow() throws Exception {
    stubOneEventLeapDay();
    leadImage();
    stubFileBatch(okJson(pages(filePage(900001, FILE, "1998"))));
    stubDayCategory(
        okJson(pages(filePage(900002, "Synthetic day photo.jpg", LEAP_DAY)))
            .withFixedDelay(SILENT_MS));

    long start = System.nanoTime();
    JsonNode root = ok(LEAP_DAY);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took).isLessThanOrEqualTo(oneWindow());
    assertThat(ids(onlyEvent(root).path("media"))).containsExactly("commons-900001");
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isTrue();
  }

  // ---- T1 (15): DEBUG lines carry reason counts and hashes only ----

  @Test
  void debugLinesCarryNoDateTitleFileNameOrFullEventId(CapturedOutput output) throws Exception {
    stubOneEventLeapDay();
    leadImage();
    stubFileBatch(okJson(pages(filePage(900001, FILE, "1990")))); // ten years apart: dropped
    stubDayCategory(
        okJson(
            pages(
                filePage(900002, "Synthetic day photo.jpg", LEAP_DAY),
                filePage(900003, "Synthetic logo.svg", LEAP_DAY)
                    .replace("image/jpeg", "image/svg+xml"))));
    List<Logger> loggers =
        Stream.of("io.github.ericmschoi.theworldthatday.media", WorldStoryService.class.getName())
            .map(name -> (Logger) LoggerFactory.getLogger(name))
            .toList();
    List<Level> before = loggers.stream().map(Logger::getLevel).toList();
    loggers.forEach(logger -> logger.setLevel(Level.DEBUG));
    JsonNode root;
    try {
      root = ok(LEAP_DAY);
    } finally {
      for (int i = 0; i < loggers.size(); i++) {
        loggers.get(i).setLevel(before.get(i));
      }
    }

    assertThat(onlyEvent(root).path("media").isEmpty()).isTrue();
    assertThat(ids(root.path("day_media"))).containsExactly("commons-900002");
    assertThat(root.path("partial").asBoolean()).isFalse();
    String all = output.getAll();
    assertThat(all).contains("Event photos dropped: {YEARS=1}", "Day photos dropped: {MIME=1}");
    List<String> mediaLines = // the message only: the line prefix holds the log timestamp
        all.lines()
            .filter(l -> l.contains("photos dropped") || l.contains("Wikimedia call failed"))
            .map(l -> l.substring(l.indexOf(" : ") + 3))
            .toList();
    assertThat(mediaLines).isNotEmpty();
    assertThat(mediaLines)
        .allSatisfy(line -> assertThat(ISO_DATE.matcher(line).find()).as(line).isFalse());
    assertThat(all)
        .doesNotContain("Synthetic", "시험 문서", "시험 사건", "File:", "kowiki-2000-02-29", "2000-02-29");
  }
}
