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
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
 * Plan M3a2 T2 (3), (4), (5), (6): the whole path (controller, service, the real Korean Wikipedia
 * source with the wikitext cache) against the recorded M3a1/M3a1c fixtures and the synthetic
 * leap-day pages, served by WireMock. No network.
 */
class WorldStoryDemoTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String FIXTURES = "fixtures/wikimedia/kowiki/";

  private static final Set<String> TOP_KEYS =
      Set.of("date", "events", "day_media", "notice", "partial");
  private static final Set<String> EVENT_KEYS =
      Set.of(
          "id",
          "date",
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

  /** Section list by title, then the 사건 and 문화 sections by revid and index, if present. */
  private static void stubPage(String title, String prefix) {
    JsonNode parse = JSON.readTree(read(prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(okJson(read(prefix + "-" + kind + ".json"))));
      }
    }
  }

  private static void stubPageprops(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  private static void stubOpeningDay() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("pageprops-1988-09-17.json"));
    stubSparql(sparqlFixture("sparql-1988-09-17.json"));
  }

  private static void stubSparql(String body) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(okJson(body)));
  }

  private static List<ServeEvent> sparqlRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(SPARQL_PATH))
        .toList();
  }

  private static long wikipediaRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(KOWIKI_PATH))
        .count();
  }

  private static long pagepropsRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(KOWIKI_PATH))
        .filter(event -> event.getRequest().queryParameter("action").containsValue("query"))
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

  @Test
  void openingDayReturnsTheThreeEventsAsWritten() throws Exception {
    stubOpeningDay();

    JsonNode root = world("1988-09-17");
    List<KoWikiEvent> expected = source.find(LocalDate.of(1988, 9, 17)).events();

    assertThat(root.path("date").asString()).isEqualTo("1988-09-17");
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("notice").isNull()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    JsonNode events = root.path("events");
    assertThat(events.size()).isEqualTo(3).isEqualTo(expected.size());

    List<List<Object>> rows = new ArrayList<>();
    for (JsonNode event : events) {
      rows.add(
          List.of(
              event.path("source_title").asString(),
              event.path("source_section").asString(),
              event.path("source_revision").asLong(),
              event.path("title").asString(),
              event.path("wikidata_qid").asString()));
    }
    assertThat(rows)
        .containsExactly(
            List.of("9월 17일", "사건", 42604413L, "1988년 하계 올림픽", "Q8470"),
            List.of("1988년", "문화", 42640701L, "KBS 1TV", "Q777278"),
            List.of("1988년", "문화", 42640701L, "KBS 2TV", "Q624509"));
    assertThat(events.get(0).path("id").asString()).isEqualTo("kowiki-1988-09-17-875c69fa");
    assertThat(events.get(0).path("summary").asString()).contains("서울 하계 올림픽");
    assertThat(events.get(1).path("summary").asString()).isEqualTo("KBS 1TV의 채널에서 광고 24개를 때려갔다.");
    assertThat(events.get(2).path("summary").asString()).isEqualTo("KBS 2TV의 채널에서 광고 37개를 때려갔다.");
    assertThat(events.get(0).path("source_url").asString())
        .isEqualTo("https://ko.wikipedia.org/wiki/9%EC%9B%94_17%EC%9D%BC");
    assertThat(events.get(1).path("source_url").asString())
        .isEqualTo("https://ko.wikipedia.org/wiki/1988%EB%85%84");
    assertThat(events.get(1).path("related_links").get(0).path("url").asString())
        .isEqualTo("https://ko.wikipedia.org/wiki/KBS_1TV");

    for (int i = 0; i < 3; i++) {
      JsonNode event = events.get(i);
      KoWikiEvent source = expected.get(i);
      assertThat(event.path("id").asString()).isEqualTo(source.id());
      assertThat(event.path("summary").asString()).as("text unchanged").isEqualTo(source.text());
      assertThat(event.path("summary_modified").asBoolean()).isFalse();
      assertThat(event.path("text_license").asString()).isEqualTo("CC BY-SA 4.0");
      assertThat(event.path("media").isArray()).isTrue();
      assertThat(event.path("media").isEmpty()).isTrue();
      assertThat(event.path("retrieved_at").asString())
          .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
          .isEqualTo(
              source.retrievedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
      assertThat(event.path("tone").asString()).isEqualTo("normal");
      assertThat(event.path("tone_basis").asString()).isEqualTo("curated");
      assertThat(event.path("scope").asString()).isEqualTo("domestic");
    }
  }

  @Test
  void dayWithoutEventsIsNoEventsFoundWithoutALinkLookup() throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");

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
    stubPage("2월 29일", "synthetic/synthetic-02-29");
    stubPage("2000년", "synthetic/synthetic-2000");
    stubPageprops("{\"batchcomplete\":true,\"query\":{\"pages\":[]}}");

    JsonNode root = world("2000-02-29");

    assertThat(root.path("date").asString()).isEqualTo("2000-02-29");
    assertThat(root.path("partial").asBoolean()).isFalse();
    List<String> summaries = new ArrayList<>();
    root.path("events").forEach(event -> summaries.add(event.path("summary").asString()));
    assertThat(summaries).containsExactly("시험 사건 가가 열렸다.", "시험 사건 나.", "시험 사건 다.");
    assertThat(root.path("events"))
        .allSatisfy(event -> assertThat(event.path("date").asString()).isEqualTo("2000-02-29"));
  }

  @Test
  void failedLinkLookupIsPartialWithoutQids() throws Exception {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    JsonNode root = world("1988-09-17");

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
  void openingDayAsksWikidataOnceAndWikipediaNoMoreThanTheSourceDoes() throws Exception {
    stubOpeningDay();

    JsonNode root = world("1988-09-17");
    long apiWikipedia = wikipediaRequests();
    List<ServeEvent> sparql = sparqlRequests();
    wikimedia.resetRequests();
    jdbc.execute("TRUNCATE wiki_section_cache");
    source.find(LocalDate.of(1988, 9, 17));

    assertThat(sparql).hasSize(1);
    assertThat(sparql.getFirst().getRequest().getHeader("User-Agent")).isEqualTo(TEST_USER_AGENT);
    assertThat(sparql.getFirst().getRequest().queryParameter("query").firstValue())
        .contains("VALUES ?item { wd:Q8470 wd:Q777278 wd:Q624509 }");
    assertThat(apiWikipedia)
        .as("pages and one pageprops lookup, as KoWikiEventSource.find alone")
        .isEqualTo(wikipediaRequests())
        .isEqualTo(7);
    assertThat(root.path("partial").asBoolean()).isFalse();
  }

  @Test
  void quarterFinalDayReturnsTheCuratedMatch() throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("pageprops-2002-06-22.json"));
    stubSparql(sparqlFixture("sparql-2002-06-22.json"));

    JsonNode root = world("2002-06-22");

    JsonNode events = root.path("events");
    assertThat(events.size()).isEqualTo(1);
    JsonNode event = events.get(0);
    assertThat(event.path("id").asString()).isEqualTo("kowiki-2002-06-22-04eaf6a6");
    assertThat(event.path("source_title").asString()).isEqualTo("6월 22일");
    assertThat(event.path("source_section").asString()).isEqualTo("문화");
    assertThat(event.path("source_revision").asLong()).isEqualTo(42614972L);
    assertThat(event.path("title").asString()).isEqualTo("2002년 FIFA 월드컵");
    assertThat(event.path("wikidata_qid").asString()).isEqualTo("Q47735");
    assertThat(event.path("summary").asString()).contains("스페인");
    assertThat(event.path("tone").asString()).isEqualTo("normal");
    assertThat(event.path("tone_basis").asString()).isEqualTo("curated");
    assertThat(event.path("scope").asString()).isEqualTo("domestic");
    // Plan M3a3 demo table: no photos are attached yet, so both media slots are empty arrays.
    assertThat(event.path("media").isArray()).isTrue();
    assertThat(event.path("media").isEmpty()).isTrue();
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("notice").isNull()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(sparqlRequests()).hasSize(1);
  }

  @Test
  void wikidataDownKeepsCuratedTonesAndMarksThePagePartial() throws Exception {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("pageprops-1988-09-17.json"));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(aResponse().withStatus(503)));

    JsonNode root = world("1988-09-17");

    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.path("events"))
        .allSatisfy(event -> assertThat(event.path("tone_basis").asString()).isEqualTo("curated"));
  }
}
