package io.github.ericmschoi.theworldthatday.story.world;

import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubLeapDay;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.stubLeapDayVideo;
import static io.github.ericmschoi.theworldthatday.story.world.WorldStoryCommonsVideoTest.videoRequests;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.github.ericmschoi.theworldthatday.media.video.SearchCacheProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Plan M3a4c T3 (7) and (8): in the shared test context the cache is off ({@code
 * media.search-cache.ttl.commons: 0}), so every call searches again and nothing is stored. The
 * synthetic leap day against WireMock (no network).
 */
class WorldStoryVideoCacheOffTest extends WikimediaWireMockTest {

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private SearchCacheProperties cacheProperties;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    jdbc.execute("TRUNCATE event_media_search_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void withTtlZeroEveryCallSearchesAndNothingIsStored() throws Exception {
    stubLeapDay(false);
    stubLeapDayVideo();

    for (int i = 0; i < 2; i++) {
      wikimedia.resetRequests();
      int status =
          mvc.perform(get("/api/story/world").param("date", "2000-02-29"))
              .andReturn()
              .getResponse()
              .getStatus();
      assertThat(status).isEqualTo(200);
      assertThat(videoRequests()).hasSize(2);
    }

    assertThat(cacheProperties.ttl("commons")).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM event_media_search_cache", Long.class))
        .isZero();
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }
}
