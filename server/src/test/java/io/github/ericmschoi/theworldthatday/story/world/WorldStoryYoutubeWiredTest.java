package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a5a T5 (3), (9) and (10) through the whole application with YouTube on (WireMock, no
 * network). Replaces M3a5 T7's {@code WorldStoryYoutubeNotWiredTest}, which by design failed once
 * the source was wired: the same three days now ask YouTube (or, without events, still do not). The
 * synthetic 1986-09-20 sample gives the contract example of the plan key for key, and its second
 * call is answered from the cache. Tragic events and a day without events send nothing to YouTube.
 */
class WorldStoryYoutubeWiredTest extends YoutubeWorldTest {

  @Autowired private MediaProperties media;

  private static JsonNode event(JsonNode root, String title) {
    return WorldStoryCommonsVideoTest.event(root, title);
  }

  private static String query(ServeEvent request, String name) {
    return request.getRequest().queryParameter(name).firstValue();
  }

  // ---- (3) end to end ----

  @Test
  void theAsianGamesDayGivesTheContractExampleAndItsSecondCallComesFromTheCache() throws Exception {
    stubGamesDay();
    stubGamesVideos();
    String retrievedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString();

    JsonNode first = world("1986-09-20");

    assertThat(first.path("events").size()).isEqualTo(1);
    JsonNode media = first.path("events").get(0).path("media");
    assertThat(media.size()).isEqualTo(1);
    ObjectNode expected =
        (ObjectNode) JSON.readTree(YoutubeItems.fixture("contract-video-embed-1986-09-20.json"));
    expected.put("retrieved_at", retrievedAt);
    assertThat(media.get(0)).isEqualTo(expected);
    assertThat(first.path("partial").asBoolean()).isFalse();
    assertThat(requests(SEARCH))
        .singleElement()
        .satisfies(
            search -> {
              assertThat(query(search, "q")).isEqualTo(GAMES);
              assertThat(query(search, "channelId")).isEqualTo(YoutubeItems.CHANNEL);
            });
    assertThat(requests(VIDEOS))
        .singleElement()
        .satisfies(
            videos ->
                assertThat(query(videos, "id"))
                    .isEqualTo("Abc123xyz_0,Abc123xyz_2,Abc123xyz_3,Abc123xyz_9"));
    assertThat(youtubeRows()).hasSize(1);

    clock.advance(Duration.ofHours(1));
    wikimedia.resetRequests();
    JsonNode second = world("1986-09-20");

    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(second).isEqualTo(first);
  }

  /** (10): the YouTube record passes R5 (allowed embed, no file URL) and R9, and the schema. */
  @Test
  void theYoutubeRecordIsAnAllowedEmbedOfTheSchema() throws Exception {
    stubGamesDay();
    stubGamesVideos();

    JsonNode item = youtubeItems(world("1986-09-20")).getFirst();

    assertThat(item.path("display_mode").asString()).isEqualTo("video_embed");
    assertThat(item.path("url").isNull()).isTrue();
    assertThat(media.allowedVideoEmbedProviders()).contains(item.path("provider").asString());
    assertThat(item.path("embed_url").asString())
        .startsWith("https://")
        .doesNotContain("autoplay", "mute");
    assertThat(item.path("source_url").asString()).startsWith("https://");
    ObjectNode stored = (ObjectNode) item.deepCopy();
    stored.remove("date_relation");
    stored.remove("selection");
    assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), stored)).isEmpty();
  }

  @Test
  void theQuarterFinalDaySearchesOnceSendsNoVideosCallAndCachesTheEmptyAnswer() throws Exception {
    WorldStoryCommonsVideoTest.stubPage("6월 22일", "06-22");
    WorldStoryCommonsVideoTest.stubPage("2002년", "2002");
    WorldStoryCommonsVideoTest.stubPageprops(
        WorldStoryCommonsVideoTest.read("fixtures/wikimedia/kowiki/pageprops-2002-06-22.json"));
    WorldStoryCommonsVideoTest.stubSignals(
        SparqlWikidataSignalSourceTest.fixture("sparql-2002-06-22.json"));
    stubSearch(okJson(YoutubeItems.fixture("search-empty.json")));

    JsonNode first = world("2002-06-22");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).isEmpty();
    verify(quota, never()).reserveOn(eq(Bucket.UNITS), anyInt());
    verify(quota, never()).reserve(eq(Bucket.UNITS), anyInt());
    assertThat(youtubeItems(first)).isEmpty();
    assertThat(youtubeRows())
        .singleElement()
        .satisfies(row -> assertThat((String) row.get("result")).contains("\"candidates\":[]"));

    wikimedia.resetRequests();
    JsonNode second = world("2002-06-22");

    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(second).isEqualTo(first);
  }

  @Test
  void aDayWithACommonsVideoAsksYoutubeTooAndKeepsTheCommonsVideo() throws Exception {
    WorldStoryCommonsVideoTest.stubLeapDay(false);
    WorldStoryCommonsVideoTest.stubLeapDayVideo();
    stubSearch(okJson(YoutubeItems.fixture("search-empty.json")));

    JsonNode root = world("2000-02-29");

    assertThat(requests(SEARCH))
        .singleElement()
        .satisfies(search -> assertThat(query(search, "q")).isEqualTo("시험 문서 가"));
    assertThat(event(root, "시험 문서 가").path("media").findValuesAsString("type"))
        .containsExactly("photo", "video");
    assertThat(youtubeItems(root)).isEmpty();
  }

  // ---- (9) nothing is asked ----

  @Test
  void aTragicEventIsNeverSearchedNextToANormalOne() throws Exception {
    stubDay(
        "9월 20일",
        "1986년",
        300,
        List.of(GAMES_LINE, "* [[1986년]] - [[시험 비극 문서|시험 비극]]이 일어났다."),
        new Link(GAMES, "Q9001", false),
        new Link("시험 비극 문서", "Q9002", true));
    stubSearch(okJson(YoutubeItems.fixture("search-empty.json")));

    world("1986-09-20");

    assertThat(requests(SEARCH))
        .singleElement()
        .satisfies(search -> assertThat(query(search, "q")).isEqualTo(GAMES));
  }

  @Test
  void theWarDayAsksNothingOfYoutube() throws Exception {
    stubDay(
        "6월 25일",
        "1950년",
        400,
        List.of("* [[1950년]] - [[시험 전쟁 문서|시험 전쟁]]이 일어났다."),
        new Link("시험 전쟁 문서", "Q9003", true));

    JsonNode root = world("1950-06-25");

    assertThat(root.path("events").size()).isEqualTo(1);
    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(youtubeRows()).isEmpty();
  }

  @Test
  void theFerryDayAsksNothingOfYoutube() throws Exception {
    stubDay(
        "4월 16일",
        "2014년",
        500,
        List.of("* [[2014년]] - [[시험 사고 문서|시험 사고]]가 일어났다."),
        new Link("시험 사고 문서", "Q9004", true));

    JsonNode root = world("2014-04-16");

    assertThat(root.path("events").size()).isEqualTo(1);
    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(youtubeRows()).isEmpty();
  }

  @Test
  void theDayWithoutEventsAsksNothingOfYoutube() throws Exception {
    WorldStoryCommonsVideoTest.stubPage("3월 15일", "03-15");
    WorldStoryCommonsVideoTest.stubPage("1995년", "1995");

    world("1995-03-15");

    assertThat(requests(YOUTUBE)).isEmpty();
    verify(quota, never()).reserveOn(eq(Bucket.SEARCH), anyInt());
  }
}
