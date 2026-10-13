package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

/**
 * Plan M3a2 T4 (1): checks of T2/T3 that the backend tests cover only in parts, run here through
 * the whole application (controller, service, real Wikipedia source with the cache, real SPARQL
 * client, repository curation file) against WireMock. The tone and scope table for an uncurated
 * event with a synthetic item (Q1~Q6 of {@code synthetic-signals.json}), Wikidata failing for an
 * uncurated event, error bodies that never echo hostile input, and logs of whole requests at DEBUG
 * that hold no event text, ISO date or full event ID. No network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryAcceptanceTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String KOWIKI = "fixtures/wikimedia/kowiki/";

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
        WorldStoryAcceptanceTest.class.getClassLoader().getResourceAsStream(KOWIKI + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Section list by title, then the level-2 사건 and 문화 sections by revid and index. */
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

  /**
   * The synthetic leap-day pages: "시험 사건 가가 열렸다." links 시험 문서 가 (date page 사건), "시험 사건 나." (date
   * page 문화) and "시험 사건 다." (year page 사건) have no link. 시험 문서 가 resolves to {@code qid}.
   */
  private static void stubLeapDay(String qid) {
    stubPage("2월 29일", "synthetic/synthetic-02-29");
    stubPage("2000년", "synthetic/synthetic-2000");
    stubPageprops(
        """
        {"batchcomplete":true,"query":{"pages":[{"pageid":1,"ns":0,"title":"시험 문서 가",
         "pageprops":{"wikibase_item":"%s"}}]}}"""
            .formatted(qid));
  }

  private static void stubSyntheticSparql() {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(
                okJson(
                    SparqlWikidataSignalSourceTest.fixture("synthetic/synthetic-signals.json"))));
  }

  private MockHttpServletResponse call(String date) throws Exception {
    return mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response = call(date);
    assertThat(response.getStatus()).isEqualTo(200);
    return JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  private static List<String> column(JsonNode events, String field) {
    List<String> values = new ArrayList<>();
    events.forEach(event -> values.add(event.path(field).asString(null)));
    return values;
  }

  @ParameterizedTest(name = "{0} -> {1}/{2}, scope {3}")
  @CsvSource({
    "Q1, tragic, rule:class, unknown",
    "Q2, tragic, rule:deaths, unknown",
    "Q3, normal, rule:no-signal, unknown", // deaths 0 is no signal
    "Q7, normal, rule:no-signal, unknown", // unreadable death toll is no signal
    "Q4, normal, rule:no-signal, domestic",
    "Q5, normal, rule:no-signal, world",
    "Q6, normal, rule:no-signal, unknown"
  })
  void uncuratedEventGetsToneAndScopeFromTheRealWikidataPath(
      String qid, String tone, String basis, String scope) throws Exception {
    stubLeapDay(qid);
    stubSyntheticSparql();

    JsonNode root = ok("2000-02-29");

    JsonNode linked = null;
    for (JsonNode event : root.path("events")) {
      if ("시험 문서 가".equals(event.path("title").asString(null))) {
        linked = event;
      }
    }
    assertThat(linked).isNotNull();
    assertThat(linked.path("wikidata_qid").asString()).isEqualTo(qid);
    assertThat(linked.path("tone").asString()).isEqualTo(tone);
    assertThat(linked.path("tone_basis").asString()).isEqualTo(basis);
    assertThat(linked.path("scope").asString()).isEqualTo(scope);
    assertThat(root.path("events"))
        .as("lines without a link have no QID: conservative default")
        .filteredOn(event -> event.path("title").isNull())
        .hasSize(2)
        .allSatisfy(
            event -> {
              assertThat(event.path("tone").asString()).isEqualTo("tragic");
              assertThat(event.path("tone_basis").asString()).isEqualTo("default");
              assertThat(event.path("scope").asString()).isEqualTo("unknown");
            });
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(column(root.path("events"), "tone"))
        .as("normal events first, then tragic")
        .isSortedAccordingTo((a, b) -> Boolean.compare(a.equals("tragic"), b.equals("tragic")));
  }

  @Test
  void wikidataDownMakesAnUncuratedEventConservativeAndThePagePartial() throws Exception {
    stubLeapDay("Q6");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(aResponse().withStatus(503)));

    JsonNode root = ok("2000-02-29");

    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(column(root.path("events"), "tone")).containsOnly("tragic");
    assertThat(column(root.path("events"), "tone_basis")).containsOnly("default");
    assertThat(root.path("events").get(0).path("wikidata_qid").asString()).isEqualTo("Q6");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<script>alert(1)</script>",
        "1988-09-17 서울",
        "1988-09-17,서울특별시",
        "서울대학교병원",
        "9999-99-99",
        "2002-06-22T00:00:00Z",
        "1988-09-17\nX-Injected: 1",
        "%27%22%3E"
      })
  void errorBodiesAndLogsNeverEchoTheInput(String input, CapturedOutput output) throws Exception {
    MockHttpServletResponse response = call(input);
    String body = response.getContentAsString(StandardCharsets.UTF_8);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    JsonNode problem = JSON.readTree(body);
    assertThat(problem.path("code").asString()).isIn("DATE_FORMAT", "DATE_NOT_EXIST");
    assertThat(problem.path("instance").asString()).isEqualTo("/api/story/world");
    for (String part : input.split("[\\s,]+")) {
      if (part.length() < 3) {
        continue; // too short to tell an echo from ordinary text
      }
      assertThat(body).as("body never echoes [%s]", part).doesNotContain(part);
      assertThat(output.getAll()).as("400s are not logged").doesNotContain(part);
    }
    assertThat(body).doesNotContain("script").doesNotContain("서울");
    assertThat(wikimedia.getAllServeEvents()).as("no source call for a bad date").isEmpty();
  }

  @Test
  void duplicatedDateParameterIsAFormatErrorWithoutEcho() throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", "1988-09-17", "2002-06-22"))
            .andReturn()
            .getResponse();
    String body = response.getContentAsString(StandardCharsets.UTF_8);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(JSON.readTree(body).path("code").asString()).isEqualTo("DATE_FORMAT");
    assertThat(body).doesNotContain("1988").doesNotContain("2002");
  }

  @Test
  void unavailableSourceBodyAndLogsHoldNoDate(CapturedOutput output) throws Exception {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    MockHttpServletResponse response = call("1988-09-17");
    String body = response.getContentAsString(StandardCharsets.UTF_8);

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(JSON.readTree(body).path("code").asString()).isEqualTo("UPSTREAM_UNAVAILABLE");
    assertThat(body).doesNotContain("1988").doesNotContain("09-17").doesNotContain("9월");
    assertThat(output.getAll()).doesNotContain("1988-09-17").doesNotContain("kowiki-1988");
  }

  @Test
  void wholeDemoRequestsAtDebugLogNoEventTextIsoDateOrFullId(CapturedOutput output)
      throws Exception {
    atDebug(
        () -> {
          stubPage("9월 17일", "09-17");
          stubPage("1988년", "1988");
          stubPageprops(read("pageprops-1988-09-17.json"));
          wikimedia.stubFor(
              WireMock.get(urlPathEqualTo(SPARQL_PATH))
                  .willReturn(
                      okJson(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"))));
          JsonNode opening = ok("1988-09-17");
          assertThat(opening.path("events").size()).isEqualTo(3);

          wikimedia.resetAll();
          stubPage("6월 22일", "06-22");
          stubPage("2002년", "2002");
          stubPageprops(read("pageprops-2002-06-22.json"));
          wikimedia.stubFor(
              WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(aResponse().withStatus(503)));
          JsonNode match = ok("2002-06-22");
          assertThat(match.path("partial").asBoolean()).isTrue();
          return null;
        });

    String logs = output.getAll();
    assertThat(logs).as("the run logged at DEBUG").contains("DEBUG");
    assertThat(logs)
        .doesNotContain("1988-09-17")
        .doesNotContain("2002-06-22")
        .doesNotContain("kowiki-19")
        .doesNotContain("kowiki-20")
        .doesNotContain("올림픽 개막")
        .doesNotContain("광고")
        .doesNotContain("스페인")
        .doesNotContain("승부차기");
  }

  private static <T> T atDebug(ThrowingSupplier<T> body) throws Exception {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    try {
      return body.get();
    } finally {
      app.setLevel(before);
    }
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
