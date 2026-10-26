package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a5a T5 (4), the quota rows of the M3a5 plan "데모·표본 날짜" table, through the whole
 * application: a refused reservation or a 403 {@code quotaExceeded} skips YouTube only (no request,
 * not partial, no cache row) until the next Pacific midnight; an unreadable 403 is a failure. The
 * 2026 instants of the quota clock are not story dates; the event day is the 1986-09-20 sample.
 */
class WorldStoryYoutubeQuotaTest extends YoutubeWorldTest {

  private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

  private void assertSkipped(JsonNode root) {
    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(youtubeItems(root)).isEmpty();
    assertThat(youtubeRows()).isEmpty();
  }

  @Test
  void the80thSearchIsSentAndAfterItTheDayIsSkipped() throws Exception {
    stubGamesDay();
    stubGamesVideos();
    assertThat(quota.reserve(Bucket.SEARCH, 79)).isTrue();

    JsonNode sent = world("1986-09-20");
    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(youtubeItems(sent)).hasSize(1);

    jdbc.execute("TRUNCATE event_media_search_cache");
    wikimedia.resetRequests();
    assertSkipped(world("1986-09-20"));
  }

  @Test
  void threeEventsWith78SearchesUsedSearchNone() throws Exception {
    stubDay(
        "9월 20일",
        "1986년",
        300,
        List.of(
            GAMES_LINE,
            "* [[1986년]] - [[시험 문서 나|시험 행사 나]]가 열렸다.",
            "* [[1986년]] - [[시험 문서 다|시험 행사 다]]가 열렸다."),
        new Link(GAMES, "Q9001", false),
        new Link("시험 문서 나", "Q9005", false),
        new Link("시험 문서 다", "Q9006", false));
    stubGamesVideos();
    assertThat(quota.reserve(Bucket.SEARCH, 78)).isTrue();

    assertSkipped(world("1986-09-20"));
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(78);
  }

  @Test
  void aQuotaAnswerSkipsYoutubeUntilPacificMidnight() throws Exception {
    stubGamesDay();
    stubSearch(
        aResponse().withStatus(403).withBody(YoutubeItems.fixture("error-quota-exceeded.json")));

    JsonNode refused = world("1986-09-20");
    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).isEmpty();
    assertThat(refused.path("partial").asBoolean()).isFalse();
    assertThat(youtubeRows()).isEmpty();

    Instant midnight =
        clock.instant().atZone(PACIFIC).toLocalDate().plusDays(1).atStartOfDay(PACIFIC).toInstant();
    clock.advance(Duration.between(clock.instant(), midnight).minusSeconds(1));
    wikimedia.resetRequests();
    assertSkipped(world("1986-09-20"));

    clock.advance(Duration.ofSeconds(1));
    stubGamesVideos();
    wikimedia.resetRequests();
    JsonNode nextDay = world("1986-09-20");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(youtubeItems(nextDay)).hasSize(1);
    assertThat(youtubeRows()).hasSize(1);
  }

  @Test
  void anUnreadable403IsAFailureThatMarksNothingAndIsAskedAgain() throws Exception {
    stubGamesDay();
    stubSearch(aResponse().withStatus(403).withBody("<html>synthetic forbidden</html>"));

    JsonNode failed = world("1986-09-20");
    assertThat(failed.path("partial").asBoolean()).isTrue();
    assertThat(youtubeRows()).isEmpty();

    stubSearch(okJson(YoutubeItems.fixture("search-empty.json")));
    wikimedia.resetRequests();
    JsonNode next = world("1986-09-20");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(next.path("partial").asBoolean()).isFalse();
    assertThat(youtubeRows()).hasSize(1);
  }
}
