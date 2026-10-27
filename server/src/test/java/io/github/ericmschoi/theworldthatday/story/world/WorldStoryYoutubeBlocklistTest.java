package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeVideoMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a5a T5 (8): the blocklist's {@code source: youtube} entries take a video out by {@code id}
 * and every video of a {@code channel}, both in a new answer and in one restored from the cache
 * (the row is replaced with candidates made without the blocklist, as if stored before the entries
 * were added). Without them the retrospective edit (90) would be shown, or else the clip (60).
 */
@TestPropertySource(
    properties = {
      "youtube.allowed-channels=" + YoutubeItems.CHANNEL + "," + YoutubeItems.SECOND_CHANNEL,
      "media.blocklist[0].source=youtube",
      "media.blocklist[0].id=Abc123xyz_0",
      "media.blocklist[1].source=youtube",
      "media.blocklist[1].channel=" + YoutubeItems.SECOND_CHANNEL
    })
class WorldStoryYoutubeBlocklistTest extends YoutubeWorldTest {

  private static final LocalDate DAY = LocalDate.of(1986, 9, 20);

  @Autowired private SearchCacheFormat format;

  private static final String SEARCH_BOTH =
      """
      {"kind":"youtube#searchListResponse","items":[
       {"kind":"youtube#searchResult","id":{"kind":"youtube#video","videoId":"Abc123xyz_0"}},
       {"kind":"youtube#searchResult","id":{"kind":"youtube#video","videoId":"Abc123xyz_4"}}]}""";

  @Test
  void blockedIdsAndChannelsStayOutOfNewAndRestoredAnswers() throws Exception {
    stubGamesDay();
    stubSearch(okJson(SEARCH_BOTH));
    stubVideos(okJson(YoutubeItems.fixture("videos-1986-09-20.json")));

    JsonNode fresh = world("1986-09-20");

    assertThat(requests(SEARCH)).hasSize(2); // one per allowed channel
    assertThat(youtubeItems(fresh)).isEmpty();
    assertThat(youtubeRows()).hasSize(1);

    String eventId = (String) youtubeRows().getFirst().get("event_id");
    YoutubeVideoMapper unblocked =
        YoutubeItems.mapper(
            List.of(YoutubeItems.CHANNEL, YoutubeItems.SECOND_CHANNEL),
            "http://localhost:5173",
            List.of(),
            List.of("youtube"));
    EventContext event = new EventContext(eventId, DAY, GAMES, null, null, null, "country:KR");
    List<Candidate> stored =
        Stream.of("Abc123xyz_0", "Abc123xyz_4")
            .map(id -> unblocked.map(YoutubeItems.item(id), event).candidate())
            .toList();
    assertThat(stored).doesNotContainNull();
    jdbc.update(
        "UPDATE event_media_search_cache SET result = ? WHERE source = 'youtube'",
        format.write(null, stored).orElseThrow());
    wikimedia.resetRequests();

    JsonNode restored = world("1986-09-20");

    assertThat(requests(YOUTUBE)).as("answered from the cache").isEmpty();
    assertThat((String) youtubeRows().getFirst().get("result"))
        .contains("Abc123xyz_0", "Abc123xyz_4");
    assertThat(youtubeItems(restored)).isEmpty();
  }
}
