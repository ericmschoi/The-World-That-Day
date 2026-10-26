package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeDataApiClient;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeProperties;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a5 T7 (10): the API is unchanged by this PR. The YouTube parts (client, quota) exist but
 * nothing calls them, even when a key is set (a fake one here, the address still local, so no call
 * could leave the host) and {@code youtube} is an allowed embed provider. The world answer holds no
 * {@code youtube} anywhere, no {@code video_embed}, no {@code youtube_official} basis, and the
 * client and the quota are never touched. M3a5a connects them and changes this on purpose. Demo
 * date 2002-06-22 (nothing found), the demo date without events 1995-03-15, and the synthetic leap
 * day with a Commons video found, so the video path really runs.
 */
@TestPropertySource(properties = "youtube.api-key=test-key-not-real")
class WorldStoryYoutubeNotWiredTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private YoutubeProperties youtubeSettings;
  @MockitoSpyBean private YoutubeDataApiClient youtubeClient;
  @MockitoSpyBean private YoutubeQuota youtubeQuota;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private String world(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return response.getContentAsString(StandardCharsets.UTF_8);
  }

  private void assertYoutubeUntouched(String body) {
    assertThat(youtubeSettings.enabled()).as("the key is set, the source could run").isTrue();
    assertThat(body.toLowerCase()).doesNotContain("youtube");
    JsonNode root = JSON.readTree(body);
    assertThat(root.findValuesAsString("display_mode")).doesNotContain("video_embed");
    assertThat(root.findValuesAsString("provider")).doesNotContain("youtube");
    assertThat(root.findValuesAsString("basis")).doesNotContain("youtube_official");
    verifyNoInteractions(youtubeClient, youtubeQuota);
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void theQuarterFinalDayAsksNothingOfYoutube() throws Exception {
    WorldStoryCommonsVideoTest.stubPage("6월 22일", "06-22");
    WorldStoryCommonsVideoTest.stubPage("2002년", "2002");
    WorldStoryCommonsVideoTest.stubPageprops(
        WorldStoryCommonsVideoTest.read("fixtures/wikimedia/kowiki/pageprops-2002-06-22.json"));
    WorldStoryCommonsVideoTest.stubSignals(
        SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));
    stubNoVideos();

    assertYoutubeUntouched(world("2002-06-22"));
  }

  @Test
  void theDayWithoutEventsAsksNothingOfYoutube() throws Exception {
    WorldStoryCommonsVideoTest.stubPage("3월 15일", "03-15");
    WorldStoryCommonsVideoTest.stubPage("1995년", "1995");

    assertYoutubeUntouched(world("1995-03-15"));
  }

  @Test
  void aDayWithACommonsVideoStillAsksNothingOfYoutube() throws Exception {
    WorldStoryCommonsVideoTest.stubLeapDay(false);
    WorldStoryCommonsVideoTest.stubLeapDayVideo();

    String body = world("2000-02-29");

    assertThat(JSON.readTree(body).findValuesAsString("type")).contains("video");
    assertYoutubeUntouched(body);
  }
}
