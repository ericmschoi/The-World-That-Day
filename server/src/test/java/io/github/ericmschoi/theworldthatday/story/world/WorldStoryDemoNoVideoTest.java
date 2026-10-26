package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
 * Plan M3a4 T7 (2) and (7), the other two demo dates: with nothing found by the video search
 * (WorldStoryCommonsVideoTest runs it on the recorded answers), the 2002-06-22 and 1995-03-15
 * answers hold no {@code selection} key, no video and no {@code link_only} item, log no video
 * failure, and WireMock sees no request it has no stub for ({@link
 * WorldStoryCommonsSourceNoVideosTest} does the same for 1988-09-17). Recorded fixtures, WireMock
 * only; the absence of a video is the right answer for these dates (plan M3a4 demo table).
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryDemoNoVideoTest extends WikimediaWireMockTest {

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
        WorldStoryDemoNoVideoTest.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void stubPage(String title, String prefix) {
    JsonNode parse = JSON.readTree(read(KOWIKI + prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(KOWIKI + prefix + "-sections.json"))));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(okJson(read(KOWIKI + prefix + "-" + kind + ".json"))));
      }
    }
  }

  private JsonNode world(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  private static void assertNoVideoAnywhere(JsonNode root, CapturedOutput output) {
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.findValuesAsString("type")).doesNotContain("video");
    assertThat(root.findValuesAsString("display_mode")).doesNotContain("link_only");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(output.getAll()).doesNotContain("Video search failed");
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void theQuarterFinalDayHasNoVideoAndNoSelection(CapturedOutput output) throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read(KOWIKI + "pageprops-2002-06-22.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"))));

    JsonNode root = world("2002-06-22");

    assertThat(root.path("events").size()).isEqualTo(1);
    assertThat(root.path("events").get(0).path("media").isArray()).isTrue();
    assertNoVideoAnywhere(root, output);
  }

  @Test
  void theDayWithoutEventsHasNoVideoAndNoSelection(CapturedOutput output) throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");

    JsonNode root = world("1995-03-15");

    assertThat(root.path("events").isEmpty()).isTrue();
    assertThat(root.path("notice").path("code").asString()).isEqualTo("NO_EVENTS_FOUND");
    assertNoVideoAnywhere(root, output);
  }
}
