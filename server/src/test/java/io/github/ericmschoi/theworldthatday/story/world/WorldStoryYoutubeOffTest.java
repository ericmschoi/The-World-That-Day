package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeProperties;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeVideoSource;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Plan M3a5a T5 (2) in the tests' shared context, whose key is empty: the YouTube source is not
 * registered, so the video search has only Commons, and the synthetic 1986-09-20 sample (whose clip
 * the YouTube tests show) answers without any YouTube call, quota use, cache row or item.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryYoutubeOffTest extends WikimediaWireMockTest {

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private YoutubeProperties youtube;
  @Autowired private YoutubeQuota quota;
  @Autowired private ObjectProvider<EventVideoSource> sources;

  @Test
  void withoutAKeyNothingOfYoutubeRuns(CapturedOutput output) throws Exception {
    jdbc.execute("TRUNCATE wiki_section_cache");
    jdbc.execute("TRUNCATE event_media_search_cache");
    YoutubeWorldTest.stubGamesDay();
    stubNoVideos();
    int searchesBefore = quota.used(Bucket.SEARCH);

    MockHttpServletResponse response =
        MockMvcBuilders.webAppContextSetup(context)
            .build()
            .perform(get("/api/story/world").param("date", "1986-09-20"))
            .andReturn()
            .getResponse();

    assertThat(youtube.enabled()).isFalse();
    assertThat(context.getBeanNamesForType(YoutubeVideoSource.class)).isEmpty();
    assertThat(sources.orderedStream().map(EventVideoSource::key)).containsExactly("commons");
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString(StandardCharsets.UTF_8).toLowerCase())
        .doesNotContain("youtube");
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(searchesBefore);
    assertThat(quota.used(Bucket.UNITS)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM event_media_search_cache", Long.class))
        .isZero();
    assertThat(output.getAll()).doesNotContain("YouTube call failed");
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }
}
