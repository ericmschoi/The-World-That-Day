package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.commonsVideo;
import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.youtubeVideo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a4 T2: the accuracy rules. Rows 1 to 7 of the plan's "점수 확인" table and the T2 boundary
 * cases are synthetic candidates; the newsreel and KTV titles are the strings written in {@code
 * docs/research/video-sources.md}, every file name, video ID and address is made up. Dates are
 * public historical event dates and the leap day 2000-02-29; event sentences are invented.
 */
@ExtendWith(OutputCaptureExtension.class)
class VideoAccuracyTest {

  static final List<String> COMMEMORATION =
      List.of(
          "재현",
          "기념",
          "기념식",
          "주년",
          "다시 보는",
          "추모",
          "anniversary",
          "reenactment",
          "montage",
          "highlights",
          "comparison",
          "memorial",
          "commemoration",
          "tribute");
  static final List<String> OTHER_STAGE =
      List.of(
          "발사",
          "launch",
          "리허설",
          "rehearsal",
          "예행연습",
          "카운트다운",
          "countdown",
          "훈련",
          "training",
          "준비",
          "preparation");
  static final List<String> STOPWORDS =
      List.of("the", "a", "an", "of", "in", "on", "at", "and", "for", "to", "및");

  private static final VideoAccuracy PLAN = accuracy(60, 600);

  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final LocalDate OLYMPICS_DAY = LocalDate.of(1988, 9, 17);
  private static final LocalDate ASIAN_GAMES_DAY = LocalDate.of(1986, 9, 20);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private static final EventContext APOLLO =
      new EventContext(
          "kowiki-1969-07-20-00000001",
          MOON_DAY,
          "아폴로 11호",
          "Apollo 11",
          "시험 문장: 우주인이 달에 내렸다.",
          "Q43653",
          "world");
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
  private static final EventContext NEUTRAL = leapEvent("시험 사건", null, "지어낸 문장이다.");

  private static VideoAccuracy accuracy(int minScore, int maxDuration) {
    return new VideoAccuracy(
        new VideoSearchProperties(minScore, 3, maxDuration, COMMEMORATION, OTHER_STAGE, STOPWORDS));
  }

  private static EventContext leapEvent(String title, String englishLabel, String summary) {
    return new EventContext(
        "kowiki-2000-02-29-00000001", LEAP_DAY, title, englishLabel, summary, null, "country:KR");
  }

  private static CaptureTime day(String date) {
    return new CaptureTime(date, DatePrecision.DAY);
  }

  private static Candidate commons(
      String title,
      String description,
      CaptureTime original,
      CaptureTime itemCapture,
      EventLink link,
      Basis basis,
      LocalDate requested) {
    return new Candidate(
        commonsVideo("commons-900001", 95.0, itemCapture, requested),
        title,
        description,
        original,
        List.of(),
        link,
        false,
        false,
        basis);
  }

  private static Candidate newsreel(
      String videoId, String title, LocalDate issue, double seconds, LocalDate requested) {
    return new Candidate(
        youtubeVideo(
            videoId,
            seconds,
            new CaptureTime(String.valueOf(issue.getYear()), DatePrecision.APPROX),
            requested),
        title,
        null,
        null,
        List.of(issue),
        EventLink.NONE,
        true,
        true,
        Basis.DAEHAN_NEWS);
  }

  /** Not free, not official, no event link: the score is only dates, words and penalties. */
  private static Candidate plain(
      String title, String description, CaptureTime original, List<LocalDate> explicit) {
    return new Candidate(
        youtubeVideo("CCCCCCCCCCC", 120.0, null, LEAP_DAY),
        title,
        description,
        original,
        explicit,
        EventLink.NONE,
        false,
        false,
        Basis.YOUTUBE_OFFICIAL);
  }

  private static Candidate row1() {
    return commons(
        "AP11 FINAL APPROACH.ogv",
        "Apollo 11",
        day("1969-07-20"),
        day("1969-07-20"),
        EventLink.P10,
        Basis.COMMONS_P10,
        MOON_DAY);
  }

  private static Candidate row1WithDuration(Double seconds) {
    Candidate row = row1();
    return new Candidate(
        commonsVideo("commons-900001", seconds, day("1969-07-20"), MOON_DAY),
        row.title(),
        row.description(),
        row.originalCapture(),
        row.explicitDates(),
        row.eventLink(),
        row.official(),
        row.dateTextExcluded(),
        row.basis());
  }

  private static Candidate row2(String title) {
    return commons(
        title,
        null,
        day("2019-07-21"),
        null,
        EventLink.CATEGORY_DIRECT,
        Basis.COMMONS_CATEGORY,
        MOON_DAY);
  }

  private static Candidate row7() {
    return newsreel(
        "BBBBBBBBBBB",
        "대한뉴스 제 736호-사람이 달에 내리다-아폴로 11호 제1신-",
        LocalDate.of(1969, 7, 26),
        298,
        MOON_DAY);
  }

  // ---- (2) the "점수 확인" table, rows 1 to 7, and the T2 boundary cases ----

  static Stream<Arguments> scoreTable() {
    return Stream.of(
        arguments("1 AP11 FINAL APPROACH (P10)", APOLLO, row1(), 130, null),
        arguments(
            "2 Historic Apollo 11 Moonwalk Footage (late capture is no material)",
            APOLLO,
            row2("Historic Apollo 11 Moonwalk Footage"),
            90,
            null),
        arguments(
            "3 Jack King's Apollo 11 Launch Commentary (other stage)",
            APOLLO,
            commons(
                "Jack King's Apollo 11 Launch Commentary",
                null,
                day("2015-06-12"),
                null,
                EventLink.CATEGORY_DIRECT,
                Basis.COMMONS_CATEGORY,
                MOON_DAY),
            50,
            Reject.LOW_SCORE),
        arguments(
            "4 Nixon speaking with astronauts on the Moon (date category)",
            APOLLO,
            commons(
                "Nixon speaking with astronauts on the Moon",
                "Apollo 11",
                day("1969-07-20"),
                day("1969-07-20"),
                EventLink.NONE,
                Basis.COMMONS_DATE_CATEGORY,
                MOON_DAY),
            80,
            null),
        arguments(
            "5 Mónica Mihánovich experiencia (category only)",
            APOLLO,
            commons(
                "Mónica Mihánovich experiencia",
                null,
                null,
                null,
                EventLink.CATEGORY_DIRECT,
                Basis.COMMONS_CATEGORY,
                MOON_DAY),
            60,
            Reject.NO_MATCH),
        arguments(
            "6 대한뉴스 제 1715호",
            OLYMPICS,
            newsreel("AAAAAAAAAAA", "대한뉴스 제 1715호-서울 올림픽 개막", OLYMPICS_DAY, 576, OLYMPICS_DAY),
            90,
            null),
        arguments("7 대한뉴스 제 736호 (issue six days later)", APOLLO, row7(), 70, null));
  }

  static Stream<Arguments> boundaryCases() {
    return Stream.of(
        arguments(
            "1711 rehearsal: other stage -40",
            OLYMPICS,
            newsreel("DDDDDDDDDDD", "대한뉴스 제 1711호-개.폐회식 (종합)예행연습", null, OLYMPICS_DAY),
            -20,
            Reject.NO_MATCH),
        arguments(
            "spaced 카운트 다운: other stage -40",
            ASIAN_GAMES,
            newsreel("EEEEEEEEEEE", "카운트 다운", null, ASIAN_GAMES_DAY),
            -20,
            Reject.NO_MATCH),
        arguments(
            "line 7 event, line 2 video: no event words",
            leapEvent("서울 지하철 7호선", null, "지어낸 문장이다."),
            newsreel("FFFFFFFFFFF", "서울 지하철 2호선 개통", null, LEAP_DAY),
            20,
            Reject.NO_MATCH),
        arguments(
            "line 1 event, line 11 video: no event words",
            leapEvent("서울 지하철 1호선", null, "지어낸 문장이다."),
            newsreel("GGGGGGGGGGG", "서울 지하철 11호선 개통", null, LEAP_DAY),
            20,
            Reject.NO_MATCH),
        arguments(
            "발사 in the invented event title: no penalty",
            leapEvent("시험 위성 발사", null, "지어낸 문장이다."),
            commons(
                "시험 위성 발사 장면",
                null,
                day("2000-02-29"),
                day("2000-02-29"),
                EventLink.CATEGORY_DIRECT,
                Basis.COMMONS_CATEGORY,
                LEAP_DAY),
            130,
            null),
        arguments("601 seconds", APOLLO, row1WithDuration(601.0), 130, Reject.DURATION),
        arguments("600 seconds", APOLLO, row1WithDuration(600.0), 130, null),
        arguments("unknown length", APOLLO, row1WithDuration(null), 130, Reject.DURATION),
        arguments(
            "another year in the title -40",
            APOLLO,
            row2("Historic Apollo 11 Moonwalk Footage 2019"),
            50,
            Reject.LOW_SCORE),
        arguments(
            "English label null: Korean words only",
            new EventContext(
                APOLLO.eventId(), MOON_DAY, APOLLO.title(), null, APOLLO.summary(), null, "world"),
            row1(),
            100,
            null),
        arguments(
            "capture the day after the event", APOLLO, lateCapture(), 130, Reject.LATE_CAPTURE));
  }

  /** Row 1 whose item the adapter dated the day after the event (it should have used notAfter). */
  private static Candidate lateCapture() {
    return commons(
        "AP11 FINAL APPROACH.ogv",
        "Apollo 11",
        day("1969-07-20"),
        day("1969-07-21"),
        EventLink.P10,
        Basis.COMMONS_P10,
        MOON_DAY);
  }

  private static Candidate newsreel(
      String videoId, String title, LocalDate issue, LocalDate requested) {
    return new Candidate(
        youtubeVideo(videoId, 300.0, null, requested),
        title,
        null,
        null,
        issue == null ? List.of() : List.of(issue),
        EventLink.NONE,
        true,
        true,
        Basis.DAEHAN_NEWS);
  }

  @ParameterizedTest(name = "row {0}: {3}, {4}")
  @MethodSource("scoreTable")
  void scoreTableRowsOneToSeven(
      String row, EventContext event, Candidate candidate, int score, Reject reject) {
    assertThat(PLAN.judge(event, candidate)).isEqualTo(new Judgement(score, reject));
  }

  @ParameterizedTest(name = "{0}: {3}, {4}")
  @MethodSource("boundaryCases")
  void boundaryCasesOfT2(
      String name, EventContext event, Candidate candidate, int score, Reject reject) {
    assertThat(PLAN.judge(event, candidate)).isEqualTo(new Judgement(score, reject));
  }

  @Test
  void rowSevenFallsBelowAThresholdOf71() {
    assertThat(accuracy(71, 600).judge(APOLLO, row7()))
        .isEqualTo(new Judgement(70, Reject.LOW_SCORE));
  }

  // ---- (1) pure ----

  @Test
  void judgingIsRepeatableLeavesTheCandidateAndLogsNothing(CapturedOutput output) {
    Candidate candidate = row1();
    Candidate copy = row1();

    Judgement first = PLAN.judge(APOLLO, candidate);
    Judgement second = PLAN.judge(APOLLO, candidate);

    assertThat(second).isEqualTo(first);
    assertThat(candidate).isEqualTo(copy);
    assertThat(candidate.item()).isEqualTo(copy.item());
    assertThat(output.getAll()).doesNotContain("아폴로", "AP11", "kowiki-1969", "1969-07-20");
    assertThat(Arrays.stream(VideoAccuracy.class.getDeclaredFields()).map(Field::getType))
        .noneMatch(type -> type.getName().startsWith("org.slf4j"));
  }

  // ---- (3) date materials ----

  private static int dateScore(CaptureTime original, List<LocalDate> explicit) {
    return PLAN.judge(NEUTRAL, plain("영상", null, original, explicit)).score();
  }

  @ParameterizedTest(name = "capture {0} ({1}) on 2000-02-29: {2}")
  @CsvSource({
    "2000-02-29, DAY, 40",
    "2000-02-28, DAY, 20",
    "2000-02-01, DAY, 20",
    "2000-01-31, DAY, 10",
    "1999-02-28, DAY, 0",
    "1999-12-31, DAY, 0",
    "2000-02, MONTH, 20",
    "2000-01, MONTH, 10",
    "2000, YEAR, 10",
    "1999, YEAR, 0",
    "2000, APPROX, 0",
    "2000-03-01, DAY, 0",
    "2000-12-31, DAY, 0",
    "2000-03, MONTH, 0",
    "2001, YEAR, 0"
  })
  void originalCaptureScoresSameDayMonthYearAndNothingWhenLater(
      String at, DatePrecision precision, int score) {
    assertThat(dateScore(new CaptureTime(at, precision), List.of())).isEqualTo(score);
  }

  @Test
  void theClosestOfSeveralStatedDatesCountsAndLaterStatedDatesStillCount() {
    assertThat(
            dateScore(
                null,
                List.of(
                    LocalDate.of(1999, 2, 28), LocalDate.of(2000, 1, 5), LocalDate.of(2000, 2, 3))))
        .isEqualTo(20);
    assertThat(dateScore(null, List.of(LocalDate.of(2000, 3, 7)))).isEqualTo(10);
    assertThat(dateScore(null, List.of(LocalDate.of(2000, 3, 7), LEAP_DAY))).isEqualTo(40);
    assertThat(dateScore(null, List.of(LocalDate.of(2001, 2, 28)))).isZero();
  }

  @Test
  void twoMaterialsGiveTheHigherScoreOnlyAndNoMaterialGivesNothing() {
    assertThat(dateScore(day("2000-02-10"), List.of(LEAP_DAY))).isEqualTo(40);
    assertThat(dateScore(day("2000-02-29"), List.of(LocalDate.of(2000, 1, 1)))).isEqualTo(40);
    assertThat(dateScore(day("2000-03-01"), List.of(LocalDate.of(2000, 2, 1)))).isEqualTo(20);
    assertThat(dateScore(CaptureTime.UNKNOWN, List.of())).isZero();
  }

  // ---- (4) event words ----

  private static int wordScore(EventContext event, String title, String description) {
    return PLAN.judge(event, plain(title, description, null, null)).score();
  }

  @Test
  void koreanWordsMatchIgnoringSpaces() {
    assertThat(wordScore(ASIAN_GAMES, "1986년 서울아시안게임 개막식", null)).isEqualTo(30);
    assertThat(wordScore(ASIAN_GAMES, "개막식", "아시 안게 임")).isEqualTo(30);
  }

  @Test
  void englishLabelWordsMatchIgnoringCase() {
    assertThat(wordScore(APOLLO, "HISTORIC APOLLO 11 FOOTAGE", null)).isEqualTo(30);
    assertThat(wordScore(APOLLO, "apollo 11", null)).isEqualTo(30);
    assertThat(wordScore(APOLLO, "Apollo11", null)).isZero();
  }

  @ParameterizedTest(name = "{0} ↔ {1}: {2}")
  @CsvSource({
    "시험 위성 관측 장면, 위성, 0",
    "시험 위성 관측 장면, 위성 관측, 30",
    "시험 위성 관측 장면, 시험 위성 관측, 30",
    "개막식(서울), 서울 개막식, 30",
    "올림픽 (서울·부산), 부산 올림픽, 30",
    "아폴로 11호, 아폴로 8호, 0",
    "아폴로 11호, 아폴로 111호, 0",
    "서울 지하철 1호선, 서울 지하철 11호선 개통, 0",
    "서울 지하철 1호선, 지하철 1호선 개통, 30",
    "2000년, 2000년 영상, 0",
    "2000 및, 2000 및 영상, 0"
  })
  void halfTheKeywordsAndEveryNumberAreNeeded(String eventTitle, String title, int score) {
    assertThat(wordScore(leapEvent(eventTitle, null, "지어낸 문장이다."), title, null)).isEqualTo(score);
  }

  @Test
  void englishKeywordsAlsoNeedHalfAndTheirNumbers() {
    EventContext label = leapEvent("시험 사건", "Synthetic Line 7 Opening", "지어낸 문장이다.");
    assertThat(wordScore(label, "line 7 footage", null)).isEqualTo(30);
    assertThat(wordScore(label, "synthetic opening line", null)).isZero();
    assertThat(wordScore(label, "line 77 opening", null)).isZero();
    assertThat(wordScore(leapEvent("시험 사건", "The 2000", "지어낸 문장이다."), "the 2000", null)).isZero();
  }

  @Test
  void withoutAnEnglishLabelOnlyKoreanWordsCount() {
    EventContext koreanOnly =
        new EventContext("e", MOON_DAY, "아폴로 11호", null, APOLLO.summary(), null, "world");
    assertThat(PLAN.judge(koreanOnly, plain("Apollo 11 footage", null, null, null)).score())
        .isZero();
    assertThat(PLAN.judge(koreanOnly, plain("아폴로 11호 영상", null, null, null)).score()).isEqualTo(30);
  }

  // ---- (5) penalties ----

  private static int penalty(EventContext event, String title, String description) {
    return PLAN.judge(event, plain(title, description, null, null)).score();
  }

  @ParameterizedTest(name = "title \"{0}\": {1}")
  @CsvSource({
    "영상 1999, -40",
    "1999년 영상, -40",
    "영상 2000, 0",
    "영상 2099, -40",
    "영상 1900, -40",
    "영상 2100, 0",
    "영상 1899, 0",
    "영상 12000, 0",
    "영상 19990, 0",
    "대한뉴스 제 1715호, 0",
    "지하철 7호선, 0"
  })
  void anotherYearCountsInTheTitleOnly(String title, int score) {
    assertThat(penalty(NEUTRAL, title, null)).isEqualTo(score);
    assertThat(penalty(NEUTRAL, "영상", title)).isZero();
  }

  @Test
  void eachListCostsOnce() {
    assertThat(penalty(NEUTRAL, "기념 추모 영상", "anniversary tribute")).isEqualTo(-30);
    assertThat(penalty(NEUTRAL, "발사 리허설", "launch rehearsal")).isEqualTo(-40);
    assertThat(penalty(NEUTRAL, "다시보는 영상", "launch")).isEqualTo(-70);
  }

  @ParameterizedTest(name = "\"{0}\": {1}")
  @CsvSource({
    "카운트 다운, -40",
    "카 운 트 다 운, -40",
    "카운트다운, -40",
    "Launch day, -40",
    "LAUNCH, -40",
    "relaunch, 0",
    "launches, 0",
    "launch-pad, -40",
    "Montage, -30",
    "다시 보는 영상, -30"
  })
  void hangulWordsIgnoreSpacesAndLatinWordsAreWholeWords(String title, int score) {
    assertThat(penalty(NEUTRAL, title, null)).isEqualTo(score);
  }

  @Test
  void anOtherStageWordInTheEventIsNoPenaltyButACommemorationWordStillIs() {
    assertThat(penalty(leapEvent("시험 위성 발사", null, "지어낸 문장이다."), "발사 영상", null)).isZero();
    assertThat(penalty(leapEvent("시험 사건", null, "시험 위성을 발 사했다."), "발사 영상", null)).isZero();
    assertThat(penalty(leapEvent("시험 사건", null, "Synthetic launch."), "launch", null)).isZero();
    assertThat(penalty(leapEvent("시험 사건", null, "Synthetic launch."), "발사 영상", null))
        .isEqualTo(-40);
    assertThat(penalty(leapEvent("시험 사건", null, "기념식이 열렸다."), "기념 영상", null)).isEqualTo(-30);
  }

  // ---- (6) required rules, one failing at a time ----

  private static Candidate commonsAt(
      Basis basis, EventLink link, CaptureTime original, String title, boolean official) {
    return new Candidate(
        commonsVideo("commons-900001", 95.0, null, LEAP_DAY),
        title,
        null,
        original,
        List.of(),
        link,
        official,
        false,
        basis);
  }

  private static final EventContext SATELLITE = leapEvent("시험 위성 관측", null, "지어낸 문장이다.");

  @Test
  void eachRequiredRuleRejectsAlone() {
    // 1: the event link and a free license reach 60, but there is no date and no event word.
    assertThat(
            PLAN.judge(
                SATELLITE,
                commonsAt(Basis.COMMONS_CATEGORY, EventLink.CATEGORY_DIRECT, null, "영상", false)))
        .isEqualTo(new Judgement(60, Reject.NO_MATCH));
    // 2: a date category without event words.
    assertThat(
            PLAN.judge(
                SATELLITE,
                commonsAt(
                    Basis.COMMONS_DATE_CATEGORY, EventLink.NONE, day("2000-02-29"), "영상", true)))
        .isEqualTo(new Judgement(70, Reject.BASIS_RULE));
    // 2: a search hit with event words but only the same month.
    assertThat(
            PLAN.judge(
                SATELLITE,
                commonsAt(Basis.COMMONS_SEARCH, EventLink.NONE, day("2000-02-01"), "위성 관측", false)))
        .isEqualTo(new Judgement(60, Reject.BASIS_RULE));
    assertThat(
            PLAN.judge(
                SATELLITE,
                commonsAt(Basis.COMMONS_SEARCH, EventLink.NONE, day("2000-02-29"), "위성 관측", false)))
        .isEqualTo(new Judgement(80, null));
    // 3, 4 and 5.
    assertThat(PLAN.judge(APOLLO, row1WithDuration(600.5)).reject()).isEqualTo(Reject.DURATION);
    assertThat(PLAN.judge(APOLLO, row2("Historic Apollo 11 Moonwalk Footage 2019")))
        .isEqualTo(new Judgement(50, Reject.LOW_SCORE));
    assertThat(PLAN.judge(APOLLO, lateCapture()))
        .isEqualTo(new Judgement(130, Reject.LATE_CAPTURE));
  }

  @Test
  void aScoreEqualToTheThresholdIsAttachedAndOneBelowIsNot() {
    // Steps are tens, so 59 cannot be scored: the threshold moves instead.
    Candidate sixty =
        new Candidate(
            youtubeVideo("HHHHHHHHHHH", 120.0, null, LEAP_DAY),
            "영상",
            null,
            day("2000-01-15"),
            List.of(),
            EventLink.CATEGORY_DIRECT,
            false,
            false,
            Basis.COMMONS_CATEGORY);
    assertThat(PLAN.judge(SATELLITE, sixty)).isEqualTo(new Judgement(60, null));
    assertThat(accuracy(61, 600).judge(SATELLITE, sixty))
        .isEqualTo(new Judgement(60, Reject.LOW_SCORE));
  }

  @Test
  void settingsChangeTheOutcome() {
    assertThat(accuracy(60, 90).judge(APOLLO, row1()).reject()).isEqualTo(Reject.DURATION);
    assertThat(accuracy(60, 95).judge(APOLLO, row1()).reject()).isNull();

    Candidate launch =
        commons(
            "Jack King's Apollo 11 Launch Commentary",
            null,
            day("2015-06-12"),
            null,
            EventLink.CATEGORY_DIRECT,
            Basis.COMMONS_CATEGORY,
            MOON_DAY);
    VideoAccuracy noStages =
        new VideoAccuracy(
            new VideoSearchProperties(60, 3, 600, COMMEMORATION, List.of(), STOPWORDS));
    assertThat(noStages.judge(APOLLO, launch)).isEqualTo(new Judgement(90, null));

    VideoAccuracy footage =
        new VideoAccuracy(
            new VideoSearchProperties(60, 3, 600, List.of("footage"), OTHER_STAGE, STOPWORDS));
    assertThat(footage.judge(APOLLO, row2("Historic Apollo 11 Moonwalk Footage")).score())
        .isEqualTo(60);

    EventContext label = leapEvent("시험 사건", "Apollo of the Moon", "지어낸 문장이다.");
    Candidate apollo = plain("Apollo footage", null, null, null);
    assertThat(PLAN.judge(label, apollo).score()).isEqualTo(30);
    VideoAccuracy noStopwords =
        new VideoAccuracy(
            new VideoSearchProperties(60, 3, 600, COMMEMORATION, OTHER_STAGE, List.of()));
    assertThat(noStopwords.judge(label, apollo).score()).isZero();
  }

  // ---- (7) notAfter ----

  @ParameterizedTest(name = "{0} ({1}) for 2000-02-29: unknown = {2}")
  @CsvSource({
    "2000-03-01, DAY, true",
    "2001-02-28, DAY, true",
    "2000-03, MONTH, true",
    "2001, YEAR, true",
    "2000-02-29, DAY, false",
    "2000-02-28, DAY, false",
    "1999-03-01, DAY, false",
    "2000-02, MONTH, false",
    "2000-01, MONTH, false",
    "2000, YEAR, false",
    "1999, YEAR, false",
    "2001, APPROX, false"
  })
  void notAfterForgetsOnlyLaterTimes(String at, DatePrecision precision, boolean unknown) {
    CaptureTime time = new CaptureTime(at, precision);
    assertThat(VideoAccuracy.notAfter(time, LEAP_DAY))
        .isEqualTo(unknown ? CaptureTime.UNKNOWN : time);
  }

  @Test
  void notAfterKeepsUnknownAndCurrent() {
    assertThat(VideoAccuracy.notAfter(CaptureTime.UNKNOWN, LEAP_DAY)).isSameAs(CaptureTime.UNKNOWN);
    CaptureTime current = new CaptureTime(null, DatePrecision.CURRENT);
    assertThat(VideoAccuracy.notAfter(current, LEAP_DAY)).isSameAs(current);
  }

  // ---- (8) no MediaItem is built ----

  @Test
  void theItemIsTheAdaptersAndKeepsItsUnknownCapture() {
    Candidate candidate = row2("Historic Apollo 11 Moonwalk Footage");
    MediaItem before = candidate.item();

    assertThat(PLAN.judge(APOLLO, candidate).accepted()).isTrue();
    assertThat(candidate.item()).isSameAs(before);
    assertThat(before.record().capturedAt()).isNull();
    assertThat(
            Arrays.stream(VideoAccuracy.class.getDeclaredMethods())
                .filter(m -> !Modifier.isPrivate(m.getModifiers()))
                .map(java.lang.reflect.Method::getReturnType))
        .doesNotContain(MediaItem.class);
  }
}
