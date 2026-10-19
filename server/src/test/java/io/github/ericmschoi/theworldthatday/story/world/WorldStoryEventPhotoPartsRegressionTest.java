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
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Plan M3a3c T3 (4) and (6), with the event photo parts wired since M3a3d T1 (12) (c): with the
 * default PageImages answer (no lead image), {@code GET /api/story/world} answers the three demo
 * dates as in M3a3b (every event {@code media} empty, the same {@code day_media} ids in the same
 * order, not partial), sends one PageImages request on a day with events (none on 1995-03-15) and
 * exactly one Commons request (the day category; no file batch without a lead image). Plan M3a3d T2
 * (6), PR #20 review recommendation 1: the world section's worst time with every source slow
 * (PageImages included) stays within a bound computed from the settings, and the media segment
 * alone, with both media paths silent, takes one window ({@code lookup-deadline} + {@link
 * Deadline#CANCEL_GRACE} + 0.5s), never two. The measured times are printed for the report.
 * Recorded fixtures and WireMock only; no network: no request is left unmatched.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryEventPhotoPartsRegressionTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** Longer than any budget of the world section: the stub never answers in time. */
  private static final int SILENT_MS = 12_000;

  /** The slack the plan allows on top of one media window. */
  private static final Duration MEDIA_SLACK = Duration.ofMillis(500);

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private WikidataProperties wikidata;
  @Autowired private MediaLookupProperties mediaLookup;
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

  /** PageImages that answers only after {@code delayMs}; priority 1 like the default stub. */
  private static void stubPageImages(int delayMs) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("prop", equalTo("pageimages"))
            .atPriority(1)
            .willReturn(okJson(NO_PAGE_IMAGES).withFixedDelay(delayMs)));
  }

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Wikimedia call failed"))
        .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
        .toList();
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
    assertThat(pageImagesRequests()).isEqualTo(1);
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
    assertThat(pageImagesRequests()).isEqualTo(1);
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

  // ---- T3 (6) with M3a3d T2 (6): worst time and the one media window ----

  /**
   * Same stubs as the M3a3b worst time test in {@code WorldStoryDayMediaAcceptanceTest} (date page
   * at once, year page requests just under the read timeout, Wikidata and the Commons day category
   * never in time), and PageImages never in time either. The day category is still sent exactly
   * once and the whole answer stays within kowiki-day-deadline + wikidata-read-timeout + grace +
   * lookup-deadline + grace + 1s, the same formula as the M3a3b test; Wikidata and the media lookup
   * each waited their whole budget. The time is printed next to the M3a3b and M3a3c measurement.
   */
  @Test
  void worstTimeWithEverySourceSlowStaysWithinTheSettingsBoundAndCommonsIsAskedOnce()
      throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 1_200);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", SILENT_MS);
    stubCommons("day-category-1988-09-17.json", SILENT_MS);
    stubPageImages(SILENT_MS);

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println(
        "M3a3d T2 (6) world section worst time, PageImages slow too (stubs): "
            + took.toMillis()
            + " ms");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(served(COMMONS_PATH)).as("no retry on the media path").hasSize(1);
    assertThat(pageImagesRequests()).isEqualTo(1);
    Duration bound =
        properties
            .kowikiDayDeadline()
            .plus(wikidata.wikidataReadTimeout())
            .plus(Deadline.CANCEL_GRACE)
            .plus(mediaLookup.lookupDeadline())
            .plus(Deadline.CANCEL_GRACE)
            .plusSeconds(1);
    assertThat(took).isLessThanOrEqualTo(bound);
    assertThat(took)
        .as("Wikidata and the media lookup each waited their whole budget")
        .isGreaterThanOrEqualTo(wikidata.wikidataReadTimeout().plus(mediaLookup.lookupDeadline()));
  }

  /**
   * Wikipedia and Wikidata answer at once; the Commons day category and PageImages never do. Both
   * media paths share one deadline, so the whole answer, and with it the media segment, takes at
   * most lookup-deadline + grace + 0.5s, not two windows, with exactly one WARN line per path.
   */
  @Test
  void mediaSegmentAloneTakesOneWindowWhenBothPathsAreSilent(CapturedOutput output)
      throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 0);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 0);
    stubCommons("day-category-1988-09-17.json", SILENT_MS);
    stubPageImages(SILENT_MS);

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println(
        "M3a3d T2 (6) media segment, both paths silent (stubs): " + took.toMillis() + " ms");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertEveryEventMediaEmpty(root);
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(served(COMMONS_PATH)).as("no retry on the media path").hasSize(1);
    assertThat(pageImagesRequests()).isEqualTo(1);
    assertThat(took)
        .as("one media window, not two")
        .isLessThanOrEqualTo(
            mediaLookup.lookupDeadline().plus(Deadline.CANCEL_GRACE).plus(MEDIA_SLACK));
    assertThat(took)
        .as("the media lookup waited its window")
        .isGreaterThanOrEqualTo(mediaLookup.lookupDeadline());
    List<String> lines = failures(output);
    assertThat(lines).filteredOn(l -> l.contains("title=commons day category")).hasSize(1);
    assertThat(lines)
        .filteredOn(
            l ->
                l.contains("title=kowiki pageimages of 3") || l.contains("title=event photos of 3"))
        .hasSize(1);
    assertThat(lines).as("one WARN line per media path, nothing else").hasSize(2);
  }
}
