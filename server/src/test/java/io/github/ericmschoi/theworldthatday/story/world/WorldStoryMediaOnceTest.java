package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
 * Plan M3a3c T0 (3) through the whole application: the day category request is sent once with
 * {@code media.lookup-deadline} as its read timeout (decision 1 (a)), so a Commons answer later
 * than the deadline is exactly one Commons request (M3a3b: up to two), and the answer is still 200
 * with the events and {@code partial: true}. No network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryMediaOnceTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MediaLookupProperties mediaLookup;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryMediaOnceTest.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
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

  private static void stubOpeningDayEvents() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("wikimedia/kowiki/pageprops-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"))));
  }

  private static long requests(String path) {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(path))
        .filter(event -> !isVideoSearch(event))
        .count();
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  @Test
  void commonsLaterThanTheDeadlineIsOneRequestAndAPartialAnswer(CapturedOutput output)
      throws Exception {
    stubOpeningDayEvents();
    stubNoVideos(); // the late day category below is the photo path's
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(
                okJson(read("wikimedia/commons/day-category-1988-09-17.json"))
                    .withFixedDelay(8_000)));

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(requests(COMMONS_PATH)).isEqualTo(1);
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.path("day_media").isEmpty()).isTrue();
    // media.lookup-deadline + 1s grace + 1s slack
    assertThat(took).isLessThan(mediaLookup.lookupDeadline().plusSeconds(2));
    assertThat(
            output
                .getAll()
                .lines()
                .filter(l -> l.contains("Wikimedia call failed") && l.contains("source=commons")))
        .hasSize(1);
  }

  @Test
  void quickCommonsIsStillOneRequestAndNotPartial() throws Exception {
    stubOpeningDayEvents();
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("wikimedia/commons/day-category-1988-09-17.json"))));

    JsonNode root = ok("1988-09-17");

    assertThat(requests(COMMONS_PATH)).isEqualTo(1);
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(root.path("day_media").size()).isPositive();
  }
}
