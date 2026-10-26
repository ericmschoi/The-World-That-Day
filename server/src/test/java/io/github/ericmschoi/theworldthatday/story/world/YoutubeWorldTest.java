package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base of the world API tests with YouTube on (plan M3a5a T5): a fake key, the synthetic channel
 * allowed, and the YouTube address on the shared WireMock server, so no request can leave the host.
 * The {@code youtube} cache TTL is the application's 7 days; Commons stays uncached. Each test
 * starts on a new Pacific day, so the quota is fresh. Synthetic days are written inline: invented
 * event text, made-up QIDs, public historical dates.
 */
@TestPropertySource(
    properties = {
      "youtube.api-key=test-key-not-real",
      "youtube.allowed-channels=" + YoutubeItems.CHANNEL
    })
abstract class YoutubeWorldTest extends WikimediaWireMockTest {

  static final JsonMapper JSON = JsonMapper.builder().build();
  static final String YOUTUBE = "/youtube/v3";
  static final String SEARCH = YOUTUBE + "/search";
  static final String VIDEOS = YOUTUBE + "/videos";
  static final String GAMES = "1986년 아시안 게임";
  static final String GAMES_LINE = "* [[1986년]] - 서울에서 [[" + GAMES + "|제10회 아시안 게임]]이 개막했다.";
  private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

  @Autowired private WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean YoutubeQuota quota;
  private MockMvc mvc;

  @DynamicPropertySource
  static void youtubeUrl(DynamicPropertyRegistry registry) {
    registry.add("youtube.api-url", () -> wikimedia.baseUrl() + YOUTUBE);
  }

  @BeforeEach
  void freshDayAndCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    jdbc.execute("TRUNCATE event_media_search_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    Instant midnight =
        clock.instant().atZone(PACIFIC).toLocalDate().plusDays(1).atStartOfDay(PACIFIC).toInstant();
    clock.advance(Duration.between(clock.instant(), midnight));
    stubNoVideos();
  }

  /** Every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  /** The world answer of {@code date}: 200, and {@code link_only} nowhere. */
  JsonNode world(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("link_only\"");
    JsonNode root = JSON.readTree(body);
    assertThat(root.findValuesAsString("display_mode")).doesNotContain("link_only");
    return root;
  }

  /** The {@code provider: youtube} items of the answer. */
  static List<JsonNode> youtubeItems(JsonNode root) {
    return root.findParents("provider").stream()
        .filter(item -> "youtube".equals(item.path("provider").asString(null)))
        .toList();
  }

  static List<ServeEvent> requests(String path) {
    return wikimedia.getAllServeEvents().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(path))
        .toList();
  }

  List<Map<String, Object>> youtubeRows() {
    return jdbc.queryForList(
        "SELECT event_id, result, fetched_at FROM event_media_search_cache WHERE source = 'youtube'");
  }

  static void stubSearch(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(SEARCH)).willReturn(response));
  }

  static void stubVideos(ResponseDefinitionBuilder response) {
    wikimedia.stubFor(WireMock.get(urlPathEqualTo(VIDEOS)).willReturn(response));
  }

  /** The 1986-09-20 sample: the contract clip, two newsreels and an ID videos.list lacks. */
  static void stubGamesVideos() {
    stubSearch(okJson(YoutubeItems.fixture("search-1986-09-20.json")));
    stubVideos(okJson(YoutubeItems.fixture("videos-1986-09-20.json")));
  }

  /** A linked article of a synthetic day: its QID, in Korea, and tragic by its class or not. */
  record Link(String title, String qid, boolean tragic) {}

  /**
   * A synthetic day: the {@code dayTitle} page with {@code lines} under 사건 (revision {@code revid})
   * and a {@code yearTitle} page without a line of that day (revision {@code revid + 1}), the
   * links' QIDs and their signals.
   */
  static void stubDay(
      String dayTitle, String yearTitle, int revid, List<String> lines, Link... links) {
    stubPage(dayTitle, revid, "== 사건 ==\n" + String.join("\n", lines));
    stubPage(yearTitle, revid + 1, "== 사건 ==\n* [[1월 1일]] - 시험 사건 영.");
    ArrayNode pages = JSON.createArrayNode();
    ArrayNode bindings = JSON.createArrayNode();
    for (int i = 0; i < links.length; i++) {
      Link link = links[i];
      pages
          .addObject()
          .put("pageid", 9000 + i)
          .put("ns", 0)
          .put("title", link.title())
          .putObject("pageprops")
          .put("wikibase_item", link.qid());
      ObjectNode binding = bindings.addObject();
      binding.putObject("item").put("type", "uri").put("value", entity(link.qid()));
      binding.putObject("country").put("type", "uri").put("value", entity("Q884"));
      if (link.tragic()) {
        binding.putObject("tragicClass").put("type", "uri").put("value", entity("Q3839081"));
      }
    }
    ObjectNode pageprops = JSON.createObjectNode().put("batchcomplete", true);
    pageprops.putObject("query").set("pages", pages);
    WorldStoryCommonsVideoTest.stubPageprops(pageprops.toString());
    ObjectNode signals = JSON.createObjectNode();
    signals.putObject("head").putArray("vars").add("item").add("tragicClass").add("country");
    signals.putObject("results").set("bindings", bindings);
    WorldStoryCommonsVideoTest.stubSignals(signals.toString());
  }

  /** The 1986-09-20 sample with the Asian Games line only. */
  static void stubGamesDay() {
    stubDay("9월 20일", "1986년", 300, List.of(GAMES_LINE), new Link(GAMES, "Q9001", false));
  }

  private static String entity(String qid) {
    return "http://www.wikidata.org/entity/" + qid;
  }

  private static void stubPage(String title, int revid, String wikitext) {
    ObjectNode sections = JSON.createObjectNode();
    ObjectNode parse = sections.putObject("parse").put("title", title).put("pageid", revid);
    parse.put("revid", revid).putArray("redirects");
    parse
        .putArray("sections")
        .addObject()
        .put("toclevel", 1)
        .put("level", "2")
        .put("line", "사건")
        .put("number", "1")
        .put("index", "1")
        .put("anchor", "사건");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(sections.toString())));
    ObjectNode text = JSON.createObjectNode();
    text.putObject("parse")
        .put("title", title)
        .put("pageid", revid)
        .put("revid", revid)
        .put("wikitext", wikitext);
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("oldid", equalTo(Integer.toString(revid)))
            .withQueryParam("section", equalTo("1"))
            .willReturn(okJson(text.toString())));
  }
}
