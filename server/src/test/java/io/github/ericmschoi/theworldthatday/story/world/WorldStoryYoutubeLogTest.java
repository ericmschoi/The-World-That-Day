package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.item;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a5a T7 (6): the log of a YouTube lookup, DEBUG included, never holds the key, the search
 * words (the event title), a channel ID, the date or a video ID. The one exception of M3a5a T4
 * decision 2 (a), a DEBUG line with the video ID, is gone (chore-m3-followups T2): {@code
 * MediaNormalizer} now writes {@code Media dropped: source=youtube rule=R<n>}. The flows: a new
 * answer with a broken item, the same answer from the cache, a failed search, a failed videos call,
 * a 403 {@code quotaExceeded} and an unreadable 403. Synthetic 1986-09-20 sample.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryYoutubeLogTest extends YoutubeWorldTest {

  private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

  private static String searchOf(String... ids) {
    ObjectNode search = JSON.createObjectNode().put("kind", "youtube#searchListResponse");
    ArrayNode items = search.putArray("items");
    for (String id : ids) {
      items
          .addObject()
          .put("kind", "youtube#searchResult")
          .putObject("id")
          .put("kind", "youtube#video")
          .put("videoId", id);
    }
    return search.toString();
  }

  private static String videosWithABrokenItem() {
    ObjectNode broken = item("Abc123xyz_0");
    broken.put("id", "Abc123xyz_5");
    ((ObjectNode) broken.path("snippet")).put("channelTitle", "　"); // R1: no source name
    ObjectNode answer = JSON.createObjectNode().put("kind", "youtube#videoListResponse");
    answer.putArray("items").add(item("Abc123xyz_0")).add(broken);
    return answer.toString();
  }

  private void nextPacificDay() {
    Instant midnight =
        clock.instant().atZone(PACIFIC).toLocalDate().plusDays(1).atStartOfDay(PACIFIC).toInstant();
    clock.advance(Duration.between(clock.instant(), midnight));
  }

  @Test
  void noLogLineHoldsTheKeyTheTitleTheChannelTheDateOrAVideoId(CapturedOutput output)
      throws Exception {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger("io.github.ericmschoi.theworldthatday");
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      stubGamesDay();

      // a new answer with one item broken by rule R1, then the same answer from the cache
      stubSearch(okJson(searchOf("Abc123xyz_0", "Abc123xyz_5")));
      stubVideos(okJson(videosWithABrokenItem()));
      JsonNode fresh = world("1986-09-20");
      assertThat(youtubeItems(fresh)).hasSize(1);
      clock.advance(Duration.ofHours(1));
      assertThat(world("1986-09-20")).isEqualTo(fresh);

      // a failed search, a failed videos call, a 403 quotaExceeded and an unreadable 403
      jdbc.execute("TRUNCATE event_media_search_cache");
      stubSearch(aResponse().withStatus(500));
      assertThat(world("1986-09-20").path("partial").asBoolean()).isTrue();

      stubSearch(okJson(YoutubeItems.fixture("search-1986-09-20.json")));
      stubVideos(aResponse().withStatus(503));
      assertThat(world("1986-09-20").path("partial").asBoolean()).isTrue();

      stubSearch(
          aResponse().withStatus(403).withBody(YoutubeItems.fixture("error-quota-exceeded.json")));
      assertThat(world("1986-09-20").path("partial").asBoolean()).isFalse();

      nextPacificDay();
      stubSearch(aResponse().withStatus(403).withBody("<html>synthetic forbidden</html>"));
      assertThat(world("1986-09-20").path("partial").asBoolean()).isTrue();
    } finally {
      logger.setLevel(before);
    }

    List<String> lines = output.getAll().lines().toList();
    assertThat(lines).as("DEBUG was on").anyMatch(l -> l.contains("YouTube videos dropped"));
    assertThat(lines)
        .as("the failures are logged")
        .anyMatch(l -> l.contains("YouTube call failed: call=search cause=status 500"))
        .anyMatch(l -> l.contains("YouTube call failed: call=videos cause=status 503"))
        .anyMatch(l -> l.contains("YouTube call failed: call=search cause=quotaExceeded"))
        .anyMatch(l -> l.contains("YouTube call failed: call=search cause=status 403"));

    assertThat(lines)
        .as("the dropped item is logged by source and rule only, at DEBUG")
        .anyMatch(
            l -> l.contains(" DEBUG ") && l.contains("Media dropped: source=youtube rule=R1"));
    assertThat(lines).as("no line holds a video ID").noneMatch(l -> l.contains("Abc123xyz"));

    assertThat(String.join("\n", lines))
        .doesNotContain(
            "test-key-not-real",
            "Abc123xyz",
            YoutubeItems.CHANNEL,
            YoutubeItems.SECOND_CHANNEL,
            "UCsynthetic",
            GAMES,
            "아시안",
            "1986-09-20",
            "1986년",
            "KTV",
            "x-goog-api-key",
            "key=");
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }
}
