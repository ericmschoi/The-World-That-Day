package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan C1 S3a T7 (2): the "demo expectation table" (S3a column) through the whole path, one row per
 * demo day, from the recorded English pages served by WireMock. {@link WorldStoryDemoTest} fixes
 * the tone, scope and text of four of the five days; this class adds the 2010-02-12 response (the
 * only demo day the response-level tests did not reach) and the table's counts and {@code
 * date_kind} / {@code range_end} columns for all five. No network.
 */
class WorldStoryDemoTableTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String FIXTURES = "fixtures/wikimedia/enwiki/";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryDemoTableTest.class.getClassLoader().getResourceAsStream(FIXTURES + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void stubDay(
      String date, String dateTitle, String datePrefix, String yearTitle, String yearPrefix) {
    WikiPageStubs.stubPage(wikimedia, dateTitle, datePrefix);
    WikiPageStubs.stubPage(wikimedia, yearTitle, yearPrefix);
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("pageprops-" + date + ".json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(read("wikidata-signals-" + date + ".json"))));
  }

  private JsonNode world(String date) throws Exception {
    String body =
        mvc.perform(get("/api/story/world").param("date", date))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return JSON.readTree(body);
  }

  @Test
  void vancouverDayIsTheOlympicsRangeStartInCanadaEndingFebruary28() throws Exception {
    stubDay("2010-02-12", "February 12", "02-12", "2010", "2010");

    JsonNode root = world("2010-02-12");

    assertThat(root.path("events").size()).isEqualTo(1);
    JsonNode event = root.path("events").get(0);
    assertThat(event.path("id").asString()).startsWith("enwiki-2010-02-12-");
    assertThat(event.path("date").asString()).isEqualTo("2010-02-12");
    assertThat(event.path("date_kind").asString()).isEqualTo("range_start");
    assertThat(event.path("range_end").asString()).isEqualTo("2010-02-28");
    assertThat(event.path("summary").asString())
        .isEqualTo("The 2010 Winter Olympics are held in Vancouver and Whistler, Canada.");
    assertThat(event.path("summary_lang").asString()).isEqualTo("en");
    assertThat(event.path("source_title").asString()).isEqualTo("2010");
    assertThat(event.path("source_section").asString()).isEqualTo("Events");
    assertThat(event.path("source_url").asString()).isEqualTo("https://en.wikipedia.org/wiki/2010");
    assertThat(event.path("wikidata_qid").asString()).isEqualTo("Q9674");
    assertThat(event.path("scope").asString()).isEqualTo("domestic");
    assertThat(event.path("tone").asString()).isEqualTo("normal");
    assertThat(event.path("tone_basis").asString()).isEqualTo("rule:no-signal");
    assertThat(root.path("day_media").isEmpty()).as("D7: no day photos in S3a").isTrue();
    assertThat(root.path("notice").isNull()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
  }

  /** Columns: date, date page, its prefix, year page, its prefix, events, kinds, range ends. */
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "1988-02-13, February 13, 02-13, 1988, 1988, 1, range_start, 1988-02-28",
    "1976-07-17, July 17, 07-17, 1976, 1976, 3, day|day|day, ",
    "2010-02-12, February 12, 02-12, 2010, 2010, 1, range_start, 2010-02-28",
    "1980-04-12, April 12, 04-12, 1980, 1980, 3, day|day|day, ",
    "1969-07-20, July 20, 07-20, 1969, 1969, 3, day|day|day, "
  })
  void demoTableCountsAndDateKinds(
      String date,
      String dateTitle,
      String datePrefix,
      String yearTitle,
      String yearPrefix,
      int count,
      String kinds,
      String rangeEnd)
      throws Exception {
    stubDay(date, dateTitle, datePrefix, yearTitle, yearPrefix);

    JsonNode root = world(date);

    assertThat(root.path("events").size()).isEqualTo(count);
    List<String> actualKinds = new ArrayList<>();
    for (JsonNode event : root.path("events")) {
      actualKinds.add(event.path("date_kind").asString());
      if (rangeEnd == null || rangeEnd.isBlank()) {
        assertThat(event.path("range_end").isNull()).as("day events have no range_end").isTrue();
      } else {
        assertThat(event.path("range_end").asString()).isEqualTo(rangeEnd);
      }
    }
    assertThat(String.join("|", actualKinds)).isEqualTo(kinds);
    assertThat(root.path("day_media").isEmpty()).as("D7: no day photos in S3a").isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
  }
}
