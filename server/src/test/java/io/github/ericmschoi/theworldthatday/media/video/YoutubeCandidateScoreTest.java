package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.CHANNEL;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.SECOND_CHANNEL;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.item;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.titled;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeVideoMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a5a T4 (2) and (6): candidates of {@link YoutubeVideoMapper} judged by {@link
 * VideoAccuracy} with the plan's word lists. The score rows of the M3a5 plan "데모·표본 날짜" table are
 * written out here from the rule table (date 40/20/10, event words 30, official 20, other stage
 * −40, commemoration −30), never read back from the code. The contract example of the M3a5a plan is
 * {@code fixtures/youtube/contract-video-embed-1986-09-20.json}. Synthetic items and invented event
 * text; the dates are public historical events.
 */
class YoutubeCandidateScoreTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final VideoAccuracy PLAN =
      new VideoAccuracy(
          new VideoSearchProperties(
              60,
              3,
              600,
              VideoAccuracyTest.COMMEMORATION,
              VideoAccuracyTest.OTHER_STAGE,
              VideoAccuracyTest.STOPWORDS));

  private static final String CLIP = "Abc123xyz_0";
  private static final EventContext ASIAN_GAMES =
      event(LocalDate.of(1986, 9, 20), "1986년 아시안 게임", null);
  private static final EventContext OLYMPICS =
      event(LocalDate.of(1988, 9, 17), "1988년 하계 올림픽", null);
  private static final EventContext APOLLO =
      event(LocalDate.of(1969, 7, 20), "아폴로 11호", "Apollo 11");

  private static final YoutubeVideoMapper MAPPER =
      YoutubeItems.mapper(
          List.of(CHANNEL, SECOND_CHANNEL), "http://localhost:5173", List.of(), List.of("youtube"));

  private static EventContext event(LocalDate date, String title, String label) {
    return new EventContext(
        "kowiki-" + date + "-00000001",
        date,
        title,
        label,
        "시험 문장: 지어낸 사건 설명이다.",
        null,
        "country:KR");
  }

  private static Candidate candidate(JsonNode item, EventContext event) {
    Candidate candidate = MAPPER.map(item, event).candidate();
    assertThat(candidate).as("kept by the mapper").isNotNull();
    return candidate;
  }

  private static ObjectNode longer(ObjectNode item) {
    ((ObjectNode) item.path("contentDetails")).put("duration", "PT10M1S");
    return item;
  }

  static Stream<Arguments> rows() {
    return Stream.of(
        // 10 same year + 30 words + 20 official
        Arguments.of("1986 clip", item(CLIP), ASIAN_GAMES, 60, null, "1986"),
        // 40 same day + 30 + 20, no capture time (R-Y3)
        Arguments.of("retrospective", item("Abc123xyz_4"), ASIAN_GAMES, 90, null, null),
        // 30 + 20 − 40 (준비), the newsreel's text dates excluded
        Arguments.of(
            "1610 preparation", item("Abc123xyz_2"), ASIAN_GAMES, 10, Reject.LOW_SCORE, null),
        // 30 + 20 − 40 (카운트 다운 = 카운트다운)
        Arguments.of(
            "1610 countdown", item("Abc123xyz_3"), ASIAN_GAMES, 10, Reject.LOW_SCORE, null),
        // 30 + 20, "제작일: 1988-09-17" not read: dated by M3a5b
        Arguments.of(
            "1715 opening",
            titled(CLIP, "대한뉴스 제 1715호-서울 올림픽 개막", "제작일: 1988-09-17"),
            OLYMPICS,
            50,
            Reject.LOW_SCORE,
            null),
        // 30 + 20 − 40 (예행연습)
        Arguments.of(
            "1711 rehearsal",
            titled(CLIP, "대한뉴스 제 1711호-올림픽 개.폐회식 (종합)예행연습", "시험용 설명"),
            OLYMPICS,
            10,
            Reject.LOW_SCORE,
            null),
        // 30 + 20 − 40 (발사); "[1969]" is the event's year and no date
        Arguments.of(
            "launch",
            titled(CLIP, "아폴로 11호 발사 [1969]", "시험용 설명"),
            APOLLO,
            10,
            Reject.LOW_SCORE,
            null),
        // 30 + 20 − 30 (주년)
        Arguments.of(
            "50th anniversary",
            titled(CLIP, "아폴로 11호 달 착륙 50주년", "시험용 설명"),
            APOLLO,
            20,
            Reject.LOW_SCORE,
            null),
        // 60 as the clip, but longer than 600 seconds
        Arguments.of("601 seconds", longer(item(CLIP)), ASIAN_GAMES, 60, Reject.DURATION, "1986"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rows")
  void theSampleRowsScoreAsThePlanTable(
      String name, JsonNode item, EventContext event, int score, Reject reject, String capturedAt) {
    Candidate candidate = candidate(item, event);

    Judgement judgement = PLAN.judge(event, candidate);

    assertThat(judgement.score()).isEqualTo(score);
    assertThat(judgement.reject()).isEqualTo(reject);
    assertThat(candidate.item().record().capturedAt()).isEqualTo(capturedAt);
  }

  @Test
  void theContractItemGivesTheContractExampleKeyForKey() {
    Candidate candidate = candidate(item(CLIP), ASIAN_GAMES);
    Judgement judgement = PLAN.judge(ASIAN_GAMES, candidate);
    JsonNode expected = JSON.readTree(YoutubeItems.fixture("contract-video-embed-1986-09-20.json"));

    JsonNode actual =
        JSON.valueToTree(
            candidate
                .item()
                .withSelection(new Selection(Basis.YOUTUBE_OFFICIAL, judgement.score())));

    assertThat(judgement.accepted()).isTrue();
    assertThat(actual).isEqualTo(expected);
    assertThat(actual.findValuesAsString("display_mode")).containsExactly("video_embed");
    assertThat(actual.path("link_only_reason").isNull()).isTrue();
  }
}
