package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Plan M3a3c T3 (4) and (6): the event photo parts (PageImages, the Commons file batch) are not
 * wired yet, so {@code GET /api/story/world} answers the three demo dates as in M3a3b (every event
 * {@code media} empty, the same {@code day_media} ids in the same order, not partial), sends no
 * PageImages request and exactly one Commons request (the day category, 1995-03-15 included). The
 * world section's worst time with every source slow stays within the M3a3b T3 measurement (about
 * 11.7s) plus 0.5s. Recorded fixtures and WireMock only; no network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryEventPhotoPartsRegressionTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** M3a3b T3 (7) measured worst time with stubs, plus the 0.5s the plan allows. */
  private static final Duration WORST_BOUND = Duration.ofMillis(11_700 + 500);

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryEventPhotoPartsRegressionTest.class
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

  /** Section list by title, then the level-2 사건 and 문화 sections, each after {@code delayMs}. */
  private static void stubPage(String title, String prefix, int delayMs) {
    String kowiki = "wikimedia/kowiki/";
    JsonNode parse = JSON.readTree(read(kowiki + prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(kowiki + prefix + "-sections.json")).withFixedDelay(delayMs)));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(
                    okJson(read(kowiki + prefix + "-" + kind + ".json")).withFixedDelay(delayMs)));
      }
    }
  }

  private static void stubPageprops(String file) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("wikimedia/kowiki/" + file))));
  }

  private static void stubSparql(String file, int delayMs) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(
                okJson(SparqlWikidataSignalSourceTest.fixture(file)).withFixedDelay(delayMs)));
  }

  private static void stubCommons(String file, int delayMs) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("wikimedia/commons/" + file)).withFixedDelay(delayMs)));
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));
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

  private static long pageImagesRequests() {
    return served(KOWIKI_PATH).stream()
        .filter(
            event -> "pageimages".equals(param(event, "prop")) || param(event, "piprop") != null)
        .count();
  }

  /** The one Commons request is the day category, never a file batch ({@code titles=File:…}). */
  private static void assertOneDayCategoryRequest() {
    List<ServeEvent> commons = served(COMMONS_PATH);
    assertThat(commons).hasSize(1);
    ServeEvent only = commons.getFirst();
    assertThat(param(only, "generator")).isEqualTo("categorymembers");
    assertThat(param(only, "titles")).isNull();
    assertThat(param(only, "iiextmetadatalanguage")).isNull();
  }

  private static List<String> ids(JsonNode items) {
    List<String> ids = new ArrayList<>();
    items.forEach(item -> ids.add(item.path("id").asString()));
    return ids;
  }

  private static void assertEveryEventMediaEmpty(JsonNode root) {
    assertThat(root.path("events").isArray()).isTrue();
    root.path("events")
        .forEach(
            event -> {
              assertThat(event.has("media")).as(event.path("title").asString()).isTrue();
              assertThat(event.path("media").isArray()).isTrue();
              assertThat(event.path("media").isEmpty()).as(event.path("title").asString()).isTrue();
            });
  }

  // ---- T3 (4): no regression on the demo dates ----

  @Test
  void openingDayAnswersAsInM3a3b() throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 0);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 0);
    stubCommons("day-category-1988-09-17.json", 0);

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
    assertThat(pageImagesRequests()).isZero();
    assertOneDayCategoryRequest();
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void quarterFinalDayAnswersAsInM3a3b() throws Exception {
    stubPage("6월 22일", "06-22", 0);
    stubPage("2002년", "2002", 0);
    stubPageprops("pageprops-2002-06-22.json");
    stubSparql("sparql-2002-06-22.json", 0);
    stubCommons("day-category-2002-06-22.json", 0);

    JsonNode root = ok("2002-06-22");

    assertThat(root.path("events").isEmpty()).isFalse();
    assertEveryEventMediaEmpty(root);
    assertThat(ids(root.path("day_media"))).containsExactly("commons-129150318");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(pageImagesRequests()).isZero();
    assertOneDayCategoryRequest();
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void dayWithoutEventsStillAsksTheDayCategoryOnce() throws Exception {
    stubPage("3월 15일", "03-15", 0);
    stubPage("1995년", "1995", 0);
    stubCommons("day-category-1995-03-15.json", 0);

    JsonNode root = ok("1995-03-15");

    assertThat(root.path("events").isArray()).isTrue();
    assertThat(root.path("events").isEmpty()).isTrue();
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(pageImagesRequests()).isZero();
    assertOneDayCategoryRequest();
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void demoRequestsLogNoEventPhotoLookup(CapturedOutput output) throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 0);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 0);
    stubCommons("day-category-1988-09-17.json", 0);
    ok("1988-09-17");

    assertThat(output.getAll())
        .doesNotContain("kowiki pageimages of", "commons files of")
        .doesNotContain("KBS_2_logo", "KBS 2 logo");
  }

  // ---- T3 (6): worst time with every source slow, no retry on the media path ----

  /**
   * Same stubs as the M3a3b worst time test in {@code WorldStoryDayMediaAcceptanceTest}: date page
   * at once, year page requests just under the read timeout, Wikidata and Commons never in time.
   * Now the day category is sent exactly once, and the whole answer stays within the M3a3b
   * measurement plus 0.5s. The time is printed for the T3 report.
   */
  @Test
  void worstTimeStaysWithinTheM3a3bMeasurementAndCommonsIsAskedOnce() throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 1_200);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 12_000);
    stubCommons("day-category-1988-09-17.json", 12_000);

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println("M3a3c T3 (6) world section worst time (stubs): " + took.toMillis() + " ms");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(served(COMMONS_PATH)).as("no retry on the media path").hasSize(1);
    assertThat(pageImagesRequests()).isZero();
    assertThat(took).isLessThanOrEqualTo(WORST_BOUND);
  }
}
