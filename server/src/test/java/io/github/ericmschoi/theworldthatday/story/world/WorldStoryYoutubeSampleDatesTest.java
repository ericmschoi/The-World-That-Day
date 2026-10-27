package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.item;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.titled;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a5a T7 (8): the sample dates of the M3a5 plan "데모·표본 날짜" table, each with a first and a
 * second call, through the whole application (WireMock, synthetic fixtures, no network). The days
 * the other classes already take end to end are 1986-09-20 and 2002-06-22 ({@link
 * WorldStoryYoutubeWiredTest}); this class adds 1988-09-17 and 1969-07-20 (newsreel, rehearsal,
 * launch and anniversary titles that must not be attached), 2000-02-29 (a video whose title shares
 * no event word) and the second call of the three days that ask nothing (1950-06-25, 2014-04-16,
 * 1995-03-15). A day without a video stays without one; no other date is tried.
 */
class WorldStoryYoutubeSampleDatesTest extends YoutubeWorldTest {

  private static final String CLIP = "Abc123xyz_0";

  private static String searchOf(String... ids) {
    ObjectNode search = JSON.createObjectNode().put("kind", "youtube#searchListResponse");
    ArrayNode items = search.putArray("items");
    for (String id : ids) {
      ObjectNode result = items.addObject().put("kind", "youtube#searchResult");
      result.putObject("id").put("kind", "youtube#video").put("videoId", id);
    }
    return search.toString();
  }

  private static String videosOf(ObjectNode... videos) {
    ObjectNode answer = JSON.createObjectNode().put("kind", "youtube#videoListResponse");
    ArrayNode items = answer.putArray("items");
    for (ObjectNode video : videos) {
      items.add(video);
    }
    return answer.toString();
  }

  /** The second call, an hour later: no YouTube request, the same answer. */
  private void assertSecondCallFromTheCache(JsonNode first, String date, int rowsBefore)
      throws Exception {
    assertThat(youtubeRows()).hasSize(rowsBefore);
    clock.advance(Duration.ofHours(1));
    wikimedia.resetRequests();

    JsonNode second = world(date);

    assertThat(requests(YOUTUBE)).isEmpty();
    assertThat(second).isEqualTo(first);
    assertThat(youtubeRows()).hasSize(rowsBefore);
  }

  @Test
  void theOlympicsDayAttachesNoNewsreelOrRehearsalAndCachesTheAnswer() throws Exception {
    stubDay(
        "9월 17일",
        "1988년",
        600,
        List.of("* [[1988년]] - 서울에서 [[1988년 하계 올림픽|제24회 올림픽]]이 개막했다."),
        new Link("1988년 하계 올림픽", "Q9101", false));
    ObjectNode notEmbeddable = titled("Abc123xyz_3", "대한뉴스 제 1715호-서울 올림픽 개막", "제작일: 1988-09-17");
    ((ObjectNode) notEmbeddable.path("status")).put("embeddable", false);
    stubSearch(okJson(searchOf(CLIP, "Abc123xyz_2", "Abc123xyz_3", "Abc123xyz_4")));
    stubVideos(
        okJson(
            videosOf(
                titled(CLIP, "대한뉴스 제 1715호-서울 올림픽 개막", "제작일: 1988-09-17"),
                titled("Abc123xyz_2", "대한뉴스 제 1711호-올림픽 개.폐회식 (종합)예행연습", "시험용 설명"),
                notEmbeddable,
                // the fixture's second channel is not an allowed channel here
                item("Abc123xyz_4"))));

    JsonNode first = world("1988-09-17");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).hasSize(1);
    assertThat(first.path("partial").asBoolean()).isFalse();
    assertThat(first.path("events").size()).isEqualTo(1);
    assertThat(youtubeItems(first)).isEmpty();
    assertSecondCallFromTheCache(first, "1988-09-17", 1);
  }

  @Test
  void theMoonLandingDayAttachesNeitherTheLaunchNorTheAnniversary() throws Exception {
    stubDay(
        "7월 20일",
        "1969년",
        700,
        List.of("* [[1969년]] - [[아폴로 11호]]가 달에 착륙했다."),
        new Link("아폴로 11호", "Q9102", false));
    stubSearch(okJson(searchOf(CLIP, "Abc123xyz_2")));
    stubVideos(
        okJson(
            videosOf(
                titled(CLIP, "아폴로 11호 발사 [1969]", "시험용 설명"),
                titled("Abc123xyz_2", "아폴로 11호 달 착륙 50주년", "시험용 설명"))));

    JsonNode first = world("1969-07-20");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).hasSize(1);
    assertThat(first.path("partial").asBoolean()).isFalse();
    assertThat(youtubeItems(first)).isEmpty();
    assertSecondCallFromTheCache(first, "1969-07-20", 1);
  }

  @Test
  void theLeapDayAttachesAVideoWithoutAnEventWordToNothing() throws Exception {
    stubDay(
        "2월 29일",
        "2000년",
        800,
        List.of("* [[2000년]] - [[2000년 시험 행사|시험 행사]]가 열렸다."),
        new Link("2000년 시험 행사", "Q9103", false));
    stubSearch(okJson(searchOf(CLIP)));
    stubVideos(okJson(videosOf(titled(CLIP, "서울 지하철 2호선 개통", "시험용 설명"))));

    JsonNode first = world("2000-02-29");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).hasSize(1);
    assertThat(first.path("partial").asBoolean()).isFalse();
    assertThat(youtubeItems(first)).isEmpty();
    assertSecondCallFromTheCache(first, "2000-02-29", 1);
  }

  @Test
  void theWarDayAsksNothingOnTheSecondCallEither() throws Exception {
    stubDay(
        "6월 25일",
        "1950년",
        400,
        List.of("* [[1950년]] - [[시험 전쟁 문서|시험 전쟁]]이 일어났다."),
        new Link("시험 전쟁 문서", "Q9003", true));

    JsonNode first = world("1950-06-25");
    assertSecondCallFromTheCache(first, "1950-06-25", 0);
  }

  @Test
  void theFerryDayAsksNothingOnTheSecondCallEither() throws Exception {
    stubDay(
        "4월 16일",
        "2014년",
        500,
        List.of("* [[2014년]] - [[시험 사고 문서|시험 사고]]가 일어났다."),
        new Link("시험 사고 문서", "Q9004", true));

    JsonNode first = world("2014-04-16");
    assertSecondCallFromTheCache(first, "2014-04-16", 0);
  }

  @Test
  void theDayWithoutEventsAsksNothingOnTheSecondCallEither() throws Exception {
    WorldStoryCommonsVideoTest.stubPage("3월 15일", "03-15");
    WorldStoryCommonsVideoTest.stubPage("1995년", "1995");

    JsonNode first = world("1995-03-15");

    assertThat(first.path("events")).isEmpty();
    assertSecondCallFromTheCache(first, "1995-03-15", 0);
  }
}
