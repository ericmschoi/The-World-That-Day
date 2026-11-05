package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventSource;
import io.github.ericmschoi.theworldthatday.event.tone.EventCuration;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a2 T2 (3), (4), (5), (6) and plan C1 S3a T4 (3)~(5): the whole path (controller, service,
 * the real English Wikipedia source with the wikitext cache, the tone rules) against the recorded
 * English demo days ({@code fixtures/wikimedia/enwiki/}, their link lookups and Wikidata signals)
 * and the synthetic pages, served by WireMock. No network. The repository curation still holds the
 * Korean entries only (removed by S3a T5), so every tone here comes from the rules.
 */
class WorldStoryDemoTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String FIXTURES = "fixtures/wikimedia/enwiki/";

  private static final Set<String> TOP_KEYS =
      Set.of("date", "events", "day_media", "notice", "partial");
  private static final Set<String> EVENT_KEYS =
      Set.of(
          "id",
          "date",
          "date_kind",
          "range_end",
          "scope",
          "title",
          "summary",
          "summary_lang",
          "summary_modified",
          "text_license",
          "text_license_url",
          "source_name",
          "source_title",
          "source_url",
          "source_section",
          "source_revision",
          "wikidata_qid",
          "tone",
          "tone_basis",
          "related_links",
          "media",
          "retrieved_at");

  @Autowired private WebApplicationContext context;
  @Autowired private KoWikiEventSource source;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EventCuration curation;
  private MockMvc mvc;

  /** Plan M3a4b T4 (6): every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    // Built from the shared context, so this class reuses the cached Spring context.
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryDemoTest.class.getClassLoader().getResourceAsStream(FIXTURES + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String sparqlFixture(String file) {
    return SparqlWikidataSignalSourceTest.fixture(file);
  }

  private static void stubPage(String title, String prefix) {
    WikiPageStubs.stubPage(wikimedia, title, prefix);
  }

  private static void stubPageprops(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  /** A recorded demo day: both pages, the link lookup and the Wikidata signals. */
  private static void stubDemoDay(String date, String dateTitle, String yearTitle) {
    stubPage(dateTitle, EN_PREFIX.get(dateTitle));
    stubPage(yearTitle, EN_PREFIX.get(yearTitle));
    stubPageprops(read("pageprops-" + date + ".json"));
    stubSparql(read("wikidata-signals-" + date + ".json"));
  }

  private static final java.util.Map<String, String> EN_PREFIX =
      java.util.Map.of(
          "July 17", "07-17",
          "1976", "1976",
          "February 13", "02-13",
          "1988", "1988",
          "April 12", "04-12",
          "1980", "1980",
          "July 20", "07-20",
          "1969", "1969");

  private static void stubMontrealDay() {
    stubDemoDay("1976-07-17", "July 17", "1976");
  }

  private static void stubSparql(String body) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(okJson(body)));
  }

  private static List<ServeEvent> sparqlRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(SPARQL_PATH))
        .filter(event -> !isVideoSearch(event))
        .toList();
  }

  /** Wikipedia requests of the event source; the event photo lookup (PageImages) is not one. */
  private static long wikipediaRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(KOWIKI_PATH))
        .filter(event -> !event.getRequest().queryParameter("prop").containsValue("pageimages"))
        .count();
  }

  private static long pagepropsRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(KOWIKI_PATH))
        .filter(event -> event.getRequest().queryParameter("action").containsValue("query"))
        .filter(event -> !event.getRequest().queryParameter("prop").containsValue("pageimages"))
        .count();
  }

  private JsonNode world(String date) throws Exception {
    String body =
        mvc.perform(get("/api/story/world").param("date", date))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode root = JSON.readTree(body);
    assertThat(names(root)).as("top-level keys").isEqualTo(TOP_KEYS);
    for (JsonNode event : root.path("events")) {
      assertThat(names(event)).as("event keys, snake_case").isEqualTo(EVENT_KEYS);
    }
    return root;
  }

  private static Set<String> names(JsonNode node) {
    return Set.copyOf(node.propertyNames());
  }

  private static List<List<Object>> rows(JsonNode events) {
    List<List<Object>> rows = new ArrayList<>();
    for (JsonNode event : events) {
      rows.add(
          List.of(
              event.path("source_title").asString(),
              event.path("source_section").asString(),
              event.path("source_revision").asLong(),
              event.path("title").asString(),
              event.path("wikidata_qid").asString(),
              event.path("tone").asString() + "/" + event.path("tone_basis").asString(),
              event.path("scope").asString()));
    }
    return rows;
  }

  @Test
  void montrealDayReturnsTheThreeEventsAsWritten() throws Exception {
    stubMontrealDay();

    JsonNode root = world("1976-07-17");
    List<KoWikiEvent> expected = source.find(LocalDate.of(1976, 7, 17)).events();

    assertThat(root.path("date").asString()).isEqualTo("1976-07-17");
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).as("D7: no day photos in S3a").isTrue();
    assertThat(root.path("notice").isNull()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    JsonNode events = root.path("events");
    assertThat(events.size()).isEqualTo(3).isEqualTo(expected.size());
    assertThat(rows(events))
        .as("the opening once (D3m), East Timor twice")
        .containsExactly(
            List.of(
                "July 17",
                "Events",
                1375524665L,
                "Timor-Leste",
                "Q574",
                "normal/rule:no-signal",
                "world"),
            List.of(
                "July 17",
                "Events",
                1375524665L,
                "1976 Summer Olympics",
                "Q8444",
                "normal/rule:no-signal",
                "domestic"),
            List.of(
                "1976",
                "Events",
                1382023488L,
                "Timor-Leste",
                "Q574",
                "normal/rule:no-signal",
                "world"));
    assertThat(events.get(1).path("summary").asString())
        .startsWith("The opening of the Summer Olympics in Montreal");
    assertThat(events.get(0).path("source_url").asString())
        .isEqualTo("https://en.wikipedia.org/wiki/July_17");
    assertThat(events.get(2).path("source_url").asString())
        .isEqualTo("https://en.wikipedia.org/wiki/1976");
    assertThat(events.get(1).path("related_links").get(0).path("url").asString())
        .isEqualTo("https://en.wikipedia.org/wiki/1976_Summer_Olympics");

    for (int i = 0; i < 3; i++) {
      JsonNode event = events.get(i);
      KoWikiEvent source = expected.get(i);
      assertThat(event.path("id").asString())
          .isEqualTo(source.id())
          .startsWith("enwiki-1976-07-17-");
      assertThat(event.path("summary").asString()).as("text unchanged").isEqualTo(source.text());
      assertThat(event.path("summary_lang").asString()).isEqualTo("en");
      assertThat(event.path("summary_modified").asBoolean()).isFalse();
      assertThat(event.path("text_license").asString()).isEqualTo("CC BY-SA 4.0");
      assertThat(event.path("source_name").asString()).isEqualTo("English Wikipedia");
      assertThat(event.path("date_kind").asString()).isEqualTo("day");
      assertThat(event.path("range_end").isNull()).isTrue();
      assertThat(event.path("media").isArray()).isTrue();
      assertThat(event.path("retrieved_at").asString())
          .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
          .isEqualTo(
              source.retrievedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
    }
  }

  @Test
  void calgaryDayIsTheOlympicsRangeStartInCanada() throws Exception {
    stubDemoDay("1988-02-13", "February 13", "1988");

    JsonNode root = world("1988-02-13");

    assertThat(root.path("events").size()).isEqualTo(1);
    JsonNode event = root.path("events").get(0);
    assertThat(event.path("id").asString()).isEqualTo("enwiki-1988-02-13-d716c4f7");
    assertThat(event.path("date").asString()).isEqualTo("1988-02-13");
    assertThat(event.path("date_kind").asString()).isEqualTo("range_start");
    assertThat(event.path("range_end").asString()).isEqualTo("1988-02-28");
    assertThat(event.path("summary").asString())
        .isEqualTo("The 1988 Winter Olympics are held in Calgary, Alberta, Canada.");
    assertThat(rows(root.path("events")))
        .containsExactly(
            List.of(
                "1988",
                "Events",
                1382686554L,
                "1988 Winter Olympics",
                "Q9658",
                "normal/rule:no-signal",
                "domestic"));
    assertThat(root.path("partial").asBoolean()).isFalse();
  }

  @Test
  void liberianCoupDayHasTheCoupOnceAndTheCrashTragicByKeyword() throws Exception {
    stubDemoDay("1980-04-12", "April 12", "1980");

    JsonNode root = world("1980-04-12");

    assertThat(rows(root.path("events")))
        .as("normal events first; the coup line's tone is left to T5 curation")
        .containsExactly(
            List.of(
                "April 12",
                "Events",
                1378081960L,
                "1980 Liberian coup d'état",
                "Q27628137",
                "normal/rule:no-signal",
                "world"),
            List.of(
                "April 12",
                "Events",
                1378081960L,
                "Terry Fox",
                "Q295053",
                "normal/rule:no-signal",
                "unknown"),
            List.of(
                "April 12",
                "Events",
                1378081960L,
                "Transbrasil Flight 303",
                "Q7833816",
                "tragic/rule:keyword",
                "world"));
  }

  @Test
  void moonLandingDayHasTheLandingOnce() throws Exception {
    stubDemoDay("1969-07-20", "July 20", "1969");

    JsonNode root = world("1969-07-20");

    assertThat(rows(root.path("events")))
        .containsExactly(
            List.of(
                "July 20",
                "Events",
                1373354079L,
                "Apollo program",
                "Q46611",
                "normal/rule:no-signal",
                "world"),
            List.of(
                "1969",
                "Events",
                1380604178L,
                "1969 Tour de France",
                "Q836884",
                "normal/rule:no-signal",
                "world"),
            List.of(
                "July 20",
                "Events",
                1373354079L,
                "Honduras",
                "Q783",
                "tragic/rule:keyword",
                "world"));
  }

  @Test
  void dayWithoutEventsIsNoEventsFoundWithoutALinkLookup() throws Exception {
    stubPage("March 15", "synthetic/synthetic-03-15");
    stubPage("1995", "synthetic/synthetic-1995");

    JsonNode root = world("1995-03-15");

    assertThat(root.path("events").isEmpty()).isTrue();
    // Plan M3a3 demo table: the media slot stays an empty array.
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("notice").path("code").asString()).isEqualTo("NO_EVENTS_FOUND");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(pagepropsRequests()).isZero();
    assertThat(sparqlRequests()).as("no QID, no SPARQL request").isEmpty();
  }

  @Test
  void leapDayReadsTheLeapDayPages() throws Exception {
    stubPage("February 29", "synthetic/synthetic-02-29");
    stubPage("2000", "synthetic/synthetic-2000");
    stubPageprops("{\"batchcomplete\":true,\"query\":{\"pages\":[]}}");

    JsonNode root = world("2000-02-29");

    assertThat(root.path("date").asString()).isEqualTo("2000-02-29");
    assertThat(root.path("partial").asBoolean()).isFalse();
    List<String> summaries = new ArrayList<>();
    root.path("events").forEach(event -> summaries.add(event.path("summary").asString()));
    assertThat(summaries).containsExactly("Test event A opens.", "Test event B.", "Test event C.");
    assertThat(root.path("events"))
        .allSatisfy(event -> assertThat(event.path("date").asString()).isEqualTo("2000-02-29"));
  }

  @Test
  void failedLinkLookupIsPartialWithoutQids() throws Exception {
    stubPage("July 17", "07-17");
    stubPage("1976", "1976");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    JsonNode root = world("1976-07-17");

    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").isEmpty()).isFalse();
    assertThat(root.path("events"))
        .allSatisfy(event -> assertThat(event.path("wikidata_qid").isNull()).isTrue());
  }

  @Test
  void defaultSettingsReadTheRepositoryCuration() {
    assertThat(curation.entries()).hasSize(4);
  }

  @Test
  void montrealDayAsksWikidataOnceAndWikipediaNoMoreThanTheSourceDoes() throws Exception {
    stubMontrealDay();

    JsonNode root = world("1976-07-17");
    long apiWikipedia = wikipediaRequests();
    List<ServeEvent> sparql = sparqlRequests();
    wikimedia.resetRequests();
    jdbc.execute("TRUNCATE wiki_section_cache");
    source.find(LocalDate.of(1976, 7, 17));

    assertThat(sparql).hasSize(1);
    assertThat(sparql.getFirst().getRequest().getHeader("User-Agent")).isEqualTo(TEST_USER_AGENT);
    assertThat(sparql.getFirst().getRequest().queryParameter("query").firstValue())
        .contains("VALUES ?item { wd:Q574 wd:Q8444 }");
    assertThat(apiWikipedia)
        .as("pages and one pageprops lookup, as KoWikiEventSource.find alone")
        .isEqualTo(wikipediaRequests())
        .as("2 per page + 1 lookup")
        .isEqualTo(5);
    assertThat(root.path("partial").asBoolean()).isFalse();
  }

  @Test
  void quarterFinalDayReturnsTheMatchOnce() throws Exception {
    stubPage("June 22", "synthetic/synthetic-06-22");
    stubPage("2002", "synthetic/synthetic-2002");
    stubPageprops(read("synthetic/synthetic-pageprops-2002-06-22.json"));
    stubSparql(sparqlFixture("sparql-2002-06-22.json"));

    JsonNode root = world("2002-06-22");

    JsonNode events = root.path("events");
    assertThat(events.size())
        .as("date and year lines merged (D3m through the redirect)")
        .isEqualTo(1);
    JsonNode event = events.get(0);
    assertThat(event.path("id").asString()).startsWith("enwiki-2002-06-22-");
    assertThat(event.path("source_title").asString()).isEqualTo("June 22");
    assertThat(event.path("source_section").asString()).isEqualTo("Events");
    assertThat(event.path("source_revision").asLong()).isEqualTo(622L);
    assertThat(event.path("title").asString()).isEqualTo("2002 FIFA World Cup");
    assertThat(event.path("wikidata_qid").asString()).isEqualTo("Q47735");
    assertThat(event.path("summary").asString()).contains("quarter-final A");
    assertThat(event.path("media").isArray()).isTrue();
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("notice").isNull()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(sparqlRequests()).hasSize(1);
  }

  @Test
  void wikidataDownMakesUncuratedTonesConservativeAndThePagePartial() throws Exception {
    stubPage("July 17", "07-17");
    stubPage("1976", "1976");
    stubPageprops(read("pageprops-1976-07-17.json"));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(aResponse().withStatus(503)));

    JsonNode root = world("1976-07-17");

    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.path("events"))
        .allSatisfy(
            event -> {
              assertThat(event.path("tone").asString()).isEqualTo("tragic");
              assertThat(event.path("tone_basis").asString()).isEqualTo("default");
            });
  }
}
