package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.commonsVideo;
import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.youtubeVideo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Plan M3a4 T5 (4), (7): dates written in the title or description as the first date material, rows
 * 8 and 9 of the "점수 확인" table and the T5 boundary cases, and PM decision 3 (b), the apostrophe
 * inside a word. Titles are the strings written in {@code docs/research/video-sources.md};
 * descriptions, labels and event sentences are invented; video IDs are made up. Public historical
 * event dates and 2000-02-29 only. No HTTP.
 */
class VideoAccuracyTextDatesTest {

  private static final VideoAccuracy PLAN =
      new VideoAccuracy(
          new VideoSearchProperties(
              60,
              3,
              600,
              VideoAccuracyTest.COMMEMORATION,
              VideoAccuracyTest.OTHER_STAGE,
              VideoAccuracyTest.STOPWORDS));

  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final LocalDate OLYMPICS_DAY = LocalDate.of(1988, 9, 17);
  private static final LocalDate ASIAN_GAMES_DAY = LocalDate.of(1986, 9, 20);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private static final EventContext ASIAN_GAMES =
      new EventContext(
          "kowiki-1986-09-20-00000001",
          ASIAN_GAMES_DAY,
          "1986년 아시안 게임",
          null,
          "시험 문장: 서울에서 개막했다.",
          null,
          "country:KR");
  private static final EventContext OLYMPICS =
      new EventContext(
          "kowiki-1988-09-17-00000001",
          OLYMPICS_DAY,
          "1988년 하계 올림픽",
          null,
          "시험 문장: 서울에서 개막했다.",
          null,
          "country:KR");
  private static final EventContext APOLLO =
      new EventContext(
          "kowiki-1969-07-20-00000001",
          MOON_DAY,
          "아폴로 11호",
          "Apollo 11",
          "시험 문장: 우주인이 달에 내렸다.",
          "Q43653",
          "world");

  /** An official channel video, not free, no event link, dated by nothing but its text. */
  private static Candidate official(
      String videoId,
      String title,
      String description,
      double seconds,
      boolean dateTextExcluded,
      Basis basis,
      LocalDate requested) {
    return new Candidate(
        youtubeVideo(videoId, seconds, null, requested),
        title,
        description,
        null,
        List.of(),
        EventLink.NONE,
        true,
        dateTextExcluded,
        basis);
  }

  private static Candidate row8(boolean dateTextExcluded) {
    return official(
        "JJJJJJJJJJJ",
        "1986년 서울아시안게임 개막식",
        null,
        300,
        dateTextExcluded,
        Basis.YOUTUBE_OFFICIAL,
        ASIAN_GAMES_DAY);
  }

  private static Candidate row9(boolean dateTextExcluded) {
    return official(
        "KKKKKKKKKKK",
        "1986년 9월 20일 오늘 #제10회 #아시안게임 #개막",
        null,
        300,
        dateTextExcluded,
        Basis.YOUTUBE_OFFICIAL,
        ASIAN_GAMES_DAY);
  }

  /** Row 6 without its stated issue date: only the description can date it. */
  private static Candidate row6(String description, boolean dateTextExcluded) {
    return official(
        "AAAAAAAAAAA",
        "대한뉴스 제 1715호-서울 올림픽 개막",
        description,
        576,
        dateTextExcluded,
        Basis.DAEHAN_NEWS,
        OLYMPICS_DAY);
  }

  // ---- (4) rows 8 and 9, and the T5 boundary cases ----

  static Stream<Arguments> textDateCases() {
    return Stream.of(
        // year 10 + event words 30 + official 20: exactly the threshold.
        arguments("8 1986년 서울아시안게임 개막식", ASIAN_GAMES, row8(false), 60, null),
        // same day 40 + event words 30 + official 20.
        arguments("9 1986년 9월 20일 오늘", ASIAN_GAMES, row9(false), 90, null),
        // event words 30 + official 20, the written date not used.
        arguments(
            "row 6, 제작일 in the description, excluded",
            OLYMPICS,
            row6("제작일: 1988-09-17", true),
            50,
            Reject.LOW_SCORE),
        arguments(
            "row 6, 제작일 in the description", OLYMPICS, row6("제작일: 1988-09-17", false), 90, null),
        arguments(
            "row 6, English ordinal in the description",
            OLYMPICS,
            row6("17th September 1988", false),
            90,
            null),
        arguments(
            "row 9 with its title date excluded", ASIAN_GAMES, row9(true), 50, Reject.LOW_SCORE),
        arguments(
            "row 8 with its title year excluded", ASIAN_GAMES, row8(true), 50, Reject.LOW_SCORE));
  }

  @ParameterizedTest(name = "row {0}: {3}, {4}")
  @MethodSource("textDateCases")
  void writtenDatesAreTheFirstDateMaterial(
      String row, EventContext event, Candidate candidate, int score, Reject reject) {
    assertThat(PLAN.judge(event, candidate)).isEqualTo(new Judgement(score, reject));
  }

  /** Not free, not official, no event link and no event words: the score is the date only. */
  private static int writtenDateScore(String title, String description) {
    EventContext event =
        new EventContext(
            "kowiki-2000-02-29-00000001", LEAP_DAY, "시험 사건", null, "지어낸 문장이다.", null, "country:KR");
    Candidate candidate =
        new Candidate(
            youtubeVideo("CCCCCCCCCCC", 120.0, null, LEAP_DAY),
            title,
            description,
            null,
            List.of(),
            EventLink.NONE,
            false,
            false,
            Basis.YOUTUBE_OFFICIAL);
    return PLAN.judge(event, candidate).score();
  }

  @ParameterizedTest(name = "title \"{0}\", description \"{1}\" on 2000-02-29: {2}")
  @CsvSource(
      nullValues = "none",
      value = {
        "2000년 2월 29일 영상, none, 40",
        "영상, 29 February 2000, 40",
        "영상, 2000-02-01, 20",
        "2000년 2월, none, 20",
        "2000년 영상, none, 10",
        // a later written date in the same year still counts, as stated dates do
        "영상, 2000-03-01, 10",
        "영상, 1999년 2월 28일, 0",
        "영상, 2001-02-28, 0",
        // the closest of several (the title's other year 1999 costs 40: 40 - 40)
        "1999년 영상 2000년, 2000-02-28 / 2000-02-29, 0",
        // not dates
        "2000-02-30 영상, 9월 20일, 0",
        "영상 2000, 2000, 0"
      })
  void theClosestWrittenDateCounts(String title, String description, int score) {
    assertThat(writtenDateScore(title, description)).isEqualTo(score);
  }

  @Test
  void aDateSplitBetweenTitleAndDescriptionIsNoDay() {
    // "2000년" ends the title and "2월 29일" opens the description: a year only.
    assertThat(writtenDateScore("영상 2000년", "2월 29일 방송")).isEqualTo(10);
  }

  private static int withOtherMaterials(String title, String original, LocalDate stated) {
    Candidate candidate =
        new Candidate(
            youtubeVideo("CCCCCCCCCCC", 120.0, null, LEAP_DAY),
            title,
            null,
            new CaptureTime(original, DatePrecision.DAY),
            List.of(stated),
            EventLink.NONE,
            false,
            false,
            Basis.YOUTUBE_OFFICIAL);
    EventContext event =
        new EventContext("e", LEAP_DAY, "시험 사건", null, "지어낸 문장이다.", null, "country:KR");
    return PLAN.judge(event, candidate).score();
  }

  @Test
  void writtenDatesJoinTheOtherMaterialsAndOnlyTheBestCounts() {
    // The written day beats a capture in the same month and a stated date in the same year.
    assertThat(withOtherMaterials("2000년 2월 29일 영상", "2000-02-03", LocalDate.of(2000, 1, 5)))
        .isEqualTo(40);
    // A written year loses to a capture on the day; the scores are not added.
    assertThat(withOtherMaterials("2000년 영상", "2000-02-29", LocalDate.of(2000, 1, 5)))
        .isEqualTo(40);
    // A later capture is still no material, whatever the text says.
    assertThat(withOtherMaterials("2000년 영상", "2000-03-01", LocalDate.of(1999, 1, 5)))
        .isEqualTo(10);
  }

  // ---- (7) PM decision 3 (b): the apostrophe does not split words ----

  private static int labelWordScore(String label, String title) {
    EventContext event =
        new EventContext("e", LEAP_DAY, "시험 사건", label, "지어낸 문장이다.", null, "country:KR");
    Candidate candidate =
        new Candidate(
            youtubeVideo("CCCCCCCCCCC", 120.0, null, LEAP_DAY),
            title,
            null,
            null,
            List.of(),
            EventLink.NONE,
            false,
            true,
            Basis.YOUTUBE_OFFICIAL);
    return PLAN.judge(event, candidate).score();
  }

  @ParameterizedTest(name = "label \"{0}\" ↔ \"{1}\": {2}")
  @CsvSource(
      delimiter = '|',
      value = {
        // {queen's, day, parade}: only "day" is found, one of three. Split at the apostrophe the
        // words were {queen, s, day, parade}, and the "s" of "Mary's" made two of four.
        "Queen's Day Parade | Mary's day | 0",
        "Queen’s Day Parade | Mary’s day | 0",
        // U+2019 and ' are the same apostrophe on either side: {mary's, day} of three.
        "Mary's Day Parade | Mary's day | 30",
        "Mary’s Day Parade | Mary's day | 30",
        "Mary's Day Parade | MARY’S DAY | 30",
        // The word with its apostrophe is one word: "Marys" is another word.
        "Marys Day Parade | Mary's day | 0"
      })
  void anApostropheStaysInsideItsWord(String label, String title, int score) {
    assertThat(labelWordScore(label, title)).isEqualTo(score);
  }

  @Test
  void rowThreeStillMatchesItsEnglishWordsAndStaysAtFifty() {
    Candidate jackKing =
        new Candidate(
            commonsVideo("commons-900001", 95.0, null, MOON_DAY),
            "Jack King's Apollo 11 Launch Commentary",
            null,
            new CaptureTime("2015-06-12", DatePrecision.DAY),
            List.of(),
            EventLink.CATEGORY_DIRECT,
            false,
            false,
            Basis.COMMONS_CATEGORY);
    EventContext noLabel =
        new EventContext(
            APOLLO.eventId(), MOON_DAY, APOLLO.title(), null, APOLLO.summary(), null, "world");

    // Event link 50 + English words {apollo, 11} 30 + free 10 - other stage 40.
    assertThat(PLAN.judge(APOLLO, jackKing)).isEqualTo(new Judgement(50, Reject.LOW_SCORE));
    // Without the label the 30 for event words is gone, so the match came from {apollo, 11}.
    assertThat(PLAN.judge(noLabel, jackKing)).isEqualTo(new Judgement(20, Reject.NO_MATCH));
  }
}
