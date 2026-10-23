package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
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
 * Plan M3a4 T6 (1) and (12): this PR has no video source bean, the context still starts with the
 * search wired, and the 1988-09-17 demo answer carries no {@code selection} key, no video and no
 * {@code link_only} item, with no video failure line. Recorded fixtures and WireMock only.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryNoVideoSourceTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String KOWIKI = "fixtures/wikimedia/kowiki/";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectProvider<EventVideoSource> sources;
  @Autowired private VideoSearch search;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryNoVideoSourceTest.class.getClassLoader().getResourceAsStream(path)) {
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

  @Test
  void theDemoAnswerHasNoSelectionNoVideoAndNoLinkOnlyItem(CapturedOutput output) throws Exception {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read(KOWIKI + "pageprops-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(SparqlWikidataSignalSourceTest.fixture("sparql-1988-09-17.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("fixtures/wikimedia/commons/day-category-1988-09-17.json"))));

    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", "1988-09-17")).andReturn().getResponse();
    JsonNode root = JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));

    assertThat(sources.stream()).isEmpty();
    assertThat(search).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.path("day_media").isEmpty()).isFalse();
    assertThat(root.findValues("selection")).isEmpty();
    assertThat(root.findValuesAsString("type")).doesNotContain("video");
    assertThat(root.findValuesAsString("display_mode")).doesNotContain("link_only");
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(output.getAll()).doesNotContain("Video search failed");
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }
}
