package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.LEAP_DAY;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.commons;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.EventJudge;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4b T0 (3): one judge prepared for an event and reused for all its candidates gives the
 * same judgements as {@link VideoAccuracy#judge(EventContext, Candidate)} for each candidate alone,
 * in any order. Synthetic candidates and invented event sentences on the leap day 2000-02-29.
 */
class VideoAccuracyForEventTest {

  private static final VideoAccuracy PLAN = new VideoAccuracy(FakeVideoSource.properties(3));

  private static EventContext event(String title, String label, String summary) {
    return new EventContext(
        "kowiki-2000-02-29-00000001", LEAP_DAY, title, label, summary, null, "world");
  }

  private static final List<EventContext> EVENTS =
      List.of(
          event("알파 위성 발사", "Alpha Satellite Launch", "Invented: the satellite launch."),
          event("알파 위성 관측", "Alpha Satellite", "지어낸 문장: 위성을 관측했다."),
          event("제2회 베타 대회", "2nd Beta Games", "지어낸 문장이다."),
          event("시험 사건", null, null));

  private static Candidate video(long n, String title, EventLink link, Basis basis) {
    return commons(n, title, day("2000-02-29"), link, basis);
  }

  private static final List<Candidate> CANDIDATES =
      List.of(
          video(900001, "Alpha Satellite launch footage", EventLink.P10, Basis.COMMONS_P10),
          video(900002, "alpha satellite’s orbit", EventLink.CATEGORY_DIRECT, Basis.COMMONS_P10),
          video(900003, "알파위성 관측 영상", EventLink.NONE, Basis.COMMONS_DATE_CATEGORY),
          video(900004, "알파 위성 발사 다시보는 영상", EventLink.P10, Basis.COMMONS_P10),
          video(900005, "제2회 베타 대회 개막", EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY),
          video(900006, "제12회 베타 대회", EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY),
          video(900007, "2nd Beta Games 1999 highlights", EventLink.NONE, Basis.COMMONS_SEARCH),
          video(900008, "Satellites", EventLink.NONE, Basis.COMMONS_SEARCH),
          commons(900009, "Example footage", null, EventLink.NONE, Basis.COMMONS_CATEGORY));

  @Test
  void aJudgeReusedForAllCandidatesMatchesJudgingEachAlone() {
    for (EventContext event : EVENTS) {
      EventJudge judge = PLAN.forEvent(event);
      for (Candidate candidate : CANDIDATES) {
        assertThat(judge.judge(candidate)).isEqualTo(PLAN.judge(event, candidate));
      }
      for (Candidate candidate : CANDIDATES.reversed()) {
        assertThat(judge.judge(candidate)).isEqualTo(PLAN.judge(event, candidate));
      }
    }
  }

  @Test
  void theReusedJudgeKeepsTheRulesOfEachCandidate() {
    EventJudge launch = PLAN.forEvent(EVENTS.get(0));
    EventJudge watch = PLAN.forEvent(EVENTS.get(1));

    // English label words as whole words, the typographic apostrophe read as "'".
    assertThat(watch.judge(CANDIDATES.get(1))).isEqualTo(new Judgement(130, null));
    // A launch video counts against an event that is not itself a launch only.
    assertThat(launch.judge(CANDIDATES.get(0))).isEqualTo(new Judgement(130, null));
    assertThat(watch.judge(CANDIDATES.get(0)).score()).isEqualTo(90);
    // Korean words ignore spaces; "다시 보는" is found in "다시보는".
    assertThat(watch.judge(CANDIDATES.get(2))).isEqualTo(new Judgement(80, null));
    assertThat(launch.judge(CANDIDATES.get(3))).isEqualTo(new Judgement(100, null));
    // A word with a digit must be found, and not inside a longer number.
    EventJudge games = PLAN.forEvent(EVENTS.get(2));
    assertThat(games.judge(CANDIDATES.get(4))).isEqualTo(new Judgement(130, null));
    assertThat(games.judge(CANDIDATES.get(5))).isEqualTo(new Judgement(100, null)); // no words
    assertThat(PLAN.forEvent(EVENTS.get(3)).judge(CANDIDATES.get(8)).reject())
        .isEqualTo(Reject.NO_MATCH);
  }
}
