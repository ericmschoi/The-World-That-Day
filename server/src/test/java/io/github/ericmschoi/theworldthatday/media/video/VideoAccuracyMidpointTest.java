package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.commonsVideo;
import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.youtubeVideo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a4 T3 (1): T2 criteria the backend tests left implicit. A search hit on the same day
 * without event words, the late-capture guard at month and year precision (and not at {@code
 * approx}), and the other-stage penalty of the two newsreel boundary titles measured against the
 * same candidate without the list. Titles are the strings written in {@code
 * docs/research/video-sources.md}; dates are public historical event dates and 2000-02-29; event
 * sentences are invented. No HTTP.
 */
class VideoAccuracyMidpointTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final LocalDate OLYMPICS_DAY = LocalDate.of(1988, 9, 17);
  private static final LocalDate ASIAN_GAMES_DAY = LocalDate.of(1986, 9, 20);

  private static final VideoAccuracy PLAN = accuracy(VideoAccuracyTest.OTHER_STAGE);
  private static final VideoAccuracy NO_STAGE_WORDS = accuracy(List.of());

  private static final EventContext SATELLITE =
      new EventContext(
          "kowiki-2000-02-29-00000001",
          LEAP_DAY,
          "시험 위성 관측",
          null,
          "지어낸 문장이다.",
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
  private static final EventContext ASIAN_GAMES =
      new EventContext(
          "kowiki-1986-09-20-00000001",
          ASIAN_GAMES_DAY,
          "1986년 아시안 게임",
          null,
          "시험 문장: 서울에서 개막했다.",
          null,
          "country:KR");

  private static VideoAccuracy accuracy(List<String> otherStage) {
    return new VideoAccuracy(
        new VideoSearchProperties(
            60, 3, 600, VideoAccuracyTest.COMMEMORATION, otherStage, VideoAccuracyTest.STOPWORDS));
  }

  private static CaptureTime day(String date) {
    return new CaptureTime(date, DatePrecision.DAY);
  }

  private static Candidate commons(
      Basis basis, EventLink link, String title, CaptureTime original, CaptureTime itemCapture) {
    return new Candidate(
        commonsVideo("commons-900001", 95.0, itemCapture, LEAP_DAY),
        title,
        null,
        original,
        List.of(),
        link,
        false,
        false,
        basis);
  }

  // ---- T2 (6), required rule 2 ----

  @Test
  void aSearchHitOnTheSameDayWithoutEventWordsBreaksTheBasisRule() {
    // Same day 40 + free 10; the date alone is not enough for COMMONS_SEARCH.
    assertThat(
            PLAN.judge(
                SATELLITE,
                commons(Basis.COMMONS_SEARCH, EventLink.NONE, "영상", day("2000-02-29"), null)))
        .isEqualTo(new Judgement(50, Reject.BASIS_RULE));
  }

  @Test
  void aDateCategoryWithEventWordsPassesTheBasisRule() {
    // Same day 40 + event words 30 + free 10.
    assertThat(
            PLAN.judge(
                SATELLITE,
                commons(
                    Basis.COMMONS_DATE_CATEGORY,
                    EventLink.NONE,
                    "위성 관측 영상",
                    day("2000-02-29"),
                    null)))
        .isEqualTo(new Judgement(80, null));
  }

  // ---- T2 (6), required rule 5, at every precision (honest dates, §6.1) ----

  @ParameterizedTest(name = "item captured {0} ({1}) for 2000-02-29: {2}")
  @CsvSource(
      nullValues = "none",
      value = {
        "2000-03-01, DAY, LATE_CAPTURE",
        "2000-03, MONTH, LATE_CAPTURE",
        "2001, YEAR, LATE_CAPTURE",
        "2000-02-29, DAY, none",
        "2000-02-28, DAY, none",
        "2000-02, MONTH, none",
        "2000, YEAR, none",
        "1999, YEAR, none",
        "2001, APPROX, none",
        "none, none, none"
      })
  void anItemCapturedAfterTheEventIsRejectedAtDayMonthAndYearButNotApprox(
      String at, DatePrecision precision, Reject reject) {
    // Event link 50 + same day 40 + event words 30 + free 10, whatever the item's capture time.
    Candidate candidate =
        commons(
            Basis.COMMONS_P10,
            EventLink.P10,
            "시험 위성 관측 영상",
            day("2000-02-29"),
            new CaptureTime(at, precision));

    assertThat(PLAN.judge(SATELLITE, candidate)).isEqualTo(new Judgement(130, reject));
  }

  // ---- T2 (2), boundary cases: the other-stage penalty is exactly -40 ----

  private static Candidate newsreel(String videoId, String title, LocalDate requested) {
    return new Candidate(
        youtubeVideo(videoId, 300.0, null, requested),
        title,
        null,
        null,
        List.of(),
        EventLink.NONE,
        true,
        true,
        Basis.DAEHAN_NEWS);
  }

  @Test
  void theRehearsalIssueLosesFortyForItsOtherStageWord() {
    Candidate rehearsal = newsreel("DDDDDDDDDDD", "대한뉴스 제 1711호-개.폐회식 (종합)예행연습", OLYMPICS_DAY);

    assertThat(NO_STAGE_WORDS.judge(OLYMPICS, rehearsal).score()).isEqualTo(20);
    assertThat(PLAN.judge(OLYMPICS, rehearsal).score()).isEqualTo(-20);
  }

  @Test
  void theSpacedCountdownLosesFortyForItsOtherStageWord() {
    Candidate countdown = newsreel("EEEEEEEEEEE", "카운트 다운", ASIAN_GAMES_DAY);

    assertThat(NO_STAGE_WORDS.judge(ASIAN_GAMES, countdown).score()).isEqualTo(20);
    assertThat(PLAN.judge(ASIAN_GAMES, countdown).score()).isEqualTo(-20);
  }
}
