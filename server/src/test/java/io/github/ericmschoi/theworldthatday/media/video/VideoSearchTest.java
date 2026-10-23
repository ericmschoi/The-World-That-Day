package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.LEAP_DAY;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.commons;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.ok;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.search;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.youtube;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a4 T6 (4), (6), (8) and (11) on the search itself: which events each source is asked for,
 * the choice (safeguard 4 order, score, basis, ID, one per event, the same video once), English
 * labels per source, and failure isolation on a short deadline. Fake sources and synthetic
 * candidates on the leap day; event titles and sentences are invented. No HTTP.
 */
@ExtendWith(OutputCaptureExtension.class)
class VideoSearchTest {

  private static final Duration DEADLINE = Duration.ofMillis(400);
  private static final String E1 = "kowiki-2000-02-29-0000000a";
  private static final String E2 = "kowiki-2000-02-29-0000000b";
  private static final String TITLE = "알파 행사 영상";

  private Level before;

  @BeforeEach
  void debugOn() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(VideoSearch.class);
    before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
  }

  @AfterEach
  void debugOff() {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(VideoSearch.class)).setLevel(before);
  }

  private static EventTarget target(String id, String title) {
    return new EventTarget(id, title, "country:KR", "지어낸 문장이다.", "Q999999901");
  }

  private static final List<EventTarget> TWO = List.of(target(E1, "알파 행사"), target(E2, "알파 행사"));

  private static Videos run(VideoSearch search, List<EventTarget> targets, boolean dayPathsOff) {
    try (Deadline deadline = Deadline.after(DEADLINE)) {
      return search.start(deadline, LEAP_DAY, targets, dayPathsOff).await();
    }
  }

  private static Videos run(EventVideoSource... sources) {
    return run(search(3, sources), TWO, false);
  }

  private static FakeVideoSource source(String key, Map<String, List<Candidate>> byEvent) {
    return new FakeVideoSource(key, events -> ok(byEvent));
  }

  private static Selection selection(Videos videos, String eventId) {
    return videos.byEvent().get(eventId).selection();
  }

  // 70: dated the same year, own category, free; no event words
  private static Candidate free70(long n) {
    return commons(
        n, "Example footage", day("2000-01-15"), EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
  }

  // 90: own category, event words, free
  private static Candidate free90(long n, EventLink link, Basis basis) {
    return commons(n, TITLE, null, link, basis);
  }

  // 130: P10, same day, event words, free
  private static Candidate free130(long n) {
    return commons(n, TITLE, day("2000-02-29"), EventLink.P10, Basis.COMMONS_P10);
  }

  // ---- (4) who is asked ----

  @Test
  void sourcesAreAskedForTheFirstMaxEventsTargetsWithTheirValues() {
    FakeVideoSource a = source("alpha", Map.of());
    FakeVideoSource b = source("beta", Map.of());
    List<EventTarget> four =
        List.of(
            target("e1", "시험 문서 1"),
            target("e2", "시험 문서 2"),
            target("e3", "시험 문서 3"),
            target("e4", "시험 문서 4"));

    run(search(3, a, b), four, false);

    for (FakeVideoSource source : List.of(a, b)) {
      assertThat(source.asked).hasSize(1);
      assertThat(source.asked.getFirst())
          .containsExactly(
              new EventContext(
                  "e1", LEAP_DAY, "시험 문서 1", null, "지어낸 문장이다.", "Q999999901", "country:KR"),
              new EventContext(
                  "e2", LEAP_DAY, "시험 문서 2", null, "지어낸 문장이다.", "Q999999901", "country:KR"),
              new EventContext(
                  "e3", LEAP_DAY, "시험 문서 3", null, "지어낸 문장이다.", "Q999999901", "country:KR"));
      assertThat(source.dayPathsOff).containsExactly(false);
    }
  }

  @Test
  void maxEventsZeroOrNoTargetAsksNoSource() {
    FakeVideoSource a = source("alpha", Map.of(E1, List.of(free130(900001))));

    Videos off = run(search(0, a), TWO, false);
    Videos none = run(search(3, a), List.of(), false);

    assertThat(a.asked).isEmpty();
    assertThat(off).isEqualTo(new Videos(Map.of(), false));
    assertThat(none).isEqualTo(new Videos(Map.of(), false));
  }

  @ParameterizedTest(name = "dayPathsOff {0} reaches every source")
  @ValueSource(booleans = {true, false})
  void dayPathsOffReachesEverySource(boolean off) {
    FakeVideoSource a = source("alpha", Map.of());
    FakeVideoSource b = source("beta", Map.of());

    run(search(3, a, b), TWO, off);

    assertThat(a.dayPathsOff).containsExactly(off);
    assertThat(b.dayPathsOff).containsExactly(off);
  }

  // ---- (6) the choice ----

  @Test
  void aFreeVideoBeatsAHigherScoringOfficialOneWhichBeatsTheRest() {
    Candidate official140 =
        youtube("AAAAAAAAAAA", TITLE, day("2000-02-29"), EventLink.CATEGORY_DIRECT, true);
    Candidate other120 =
        youtube("BBBBBBBBBBB", TITLE, day("2000-02-29"), EventLink.CATEGORY_DIRECT, false);

    Videos withFree =
        run(
            source("alpha", Map.of(E1, List.of(official140, other120))),
            source("beta", Map.of(E1, List.of(free70(900001)))));
    Videos withoutFree = run(source("alpha", Map.of(E1, List.of(other120, official140))));

    assertThat(withFree.byEvent().get(E1).record().id()).isEqualTo("commons-900001");
    assertThat(selection(withFree, E1)).isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 70));
    assertThat(withoutFree.byEvent().get(E1).record().id()).isEqualTo("youtube-AAAAAAAAAAA");
    assertThat(selection(withoutFree, E1)).isEqualTo(new Selection(Basis.YOUTUBE_OFFICIAL, 140));
    assertThat(withFree.failed()).isFalse();
  }

  @Test
  void inOneTierTheScoreThenTheBasisThenTheIdDecides() {
    Videos byScore =
        run(
            source(
                "alpha",
                Map.of(
                    E1,
                    List.of(
                        free90(900001, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY),
                        free130(900002)))));
    Videos byBasis =
        run(
            source(
                "alpha",
                Map.of(
                    E1,
                    List.of(
                        free90(900001, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY),
                        free90(900002, EventLink.P10, Basis.COMMONS_P10)))));
    Videos byId =
        run(
            source(
                "alpha",
                Map.of(
                    E1,
                    List.of(
                        free90(900009, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY),
                        free90(900003, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY)))));

    assertThat(byScore.byEvent().get(E1).record().id()).isEqualTo("commons-900002");
    assertThat(byBasis.byEvent().get(E1).record().id()).isEqualTo("commons-900002");
    assertThat(selection(byBasis, E1)).isEqualTo(new Selection(Basis.COMMONS_P10, 90));
    assertThat(byId.byEvent().get(E1).record().id()).isEqualTo("commons-900003");
  }

  @Test
  void theSameVideoFromTwoSourcesCountsOnceAsTheStrongerOne() {
    Videos higher =
        run(
            source(
                "alpha",
                Map.of(E1, List.of(free90(900001, EventLink.P10, Basis.COMMONS_CATEGORY)))),
            source("beta", Map.of(E1, List.of(free130(900001)))));
    Videos tie =
        run(
            source(
                "alpha",
                Map.of(
                    E1,
                    List.of(
                        free90(900001, EventLink.CATEGORY_DIRECT, Basis.COMMONS_DATE_CATEGORY)))),
            source(
                "beta",
                Map.of(
                    E1,
                    List.of(free90(900001, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY)))));

    assertThat(higher.byEvent()).hasSize(1);
    assertThat(selection(higher, E1)).isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(selection(tie, E1)).isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 90));
  }

  @Test
  void aVideoChosenForAnEarlierEventGoesToTheNextCandidateLater() {
    Candidate shared = free130(900001);
    Candidate next = free90(900002, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);

    Videos videos = run(source("alpha", Map.of(E1, List.of(shared), E2, List.of(shared, next))));
    Videos onlyShared = run(source("alpha", Map.of(E1, List.of(shared), E2, List.of(shared))));

    assertThat(videos.byEvent().get(E1).record().id()).isEqualTo("commons-900001");
    assertThat(videos.byEvent().get(E2).record().id()).isEqualTo("commons-900002");
    assertThat(onlyShared.byEvent()).containsOnlyKeys(E1);
  }

  @Test
  void rejectedCandidatesAreNeverAttachedAndOnlyCountedByReasonAtDebug(CapturedOutput output) {
    Candidate noMatch =
        commons(900001, "Example footage", null, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
    Candidate launch = commons(900002, TITLE + " launch", null, EventLink.NONE, Basis.COMMONS_P10);

    Videos videos = run(source("alpha", Map.of(E1, List.of(noMatch, launch))));

    assertThat(videos.byEvent()).isEmpty();
    assertThat(videos.failed()).isFalse();
    assertThat(output.getAll()).contains("Video candidates rejected: {NO_MATCH=1, LOW_SCORE=1}");
  }

  @Test
  void aSourcesEnglishLabelsAreUsedForItsOwnCandidatesOnly() {
    Candidate english =
        commons(
            900001, "Beta Parade footage", null, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
    Map<String, String> labels = Map.of(E1, "Beta Parade");

    Videos labelled =
        run(
            new FakeVideoSource(
                "alpha",
                events -> new SourceResult(Map.of(E1, List.of(english)), labels, Status.OK)));
    Videos unlabelled = run(source("alpha", Map.of(E1, List.of(english))));
    Videos otherSource =
        run(
            new FakeVideoSource("alpha", events -> new SourceResult(Map.of(), labels, Status.OK)),
            source("beta", Map.of(E1, List.of(english))));

    assertThat(selection(labelled, E1)).isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 90));
    assertThat(unlabelled.byEvent()).isEmpty();
    assertThat(otherSource.byEvent()).isEmpty();
  }

  @Test
  void candidatesForAnEventNotAskedAreIgnored() {
    Videos videos =
        run(source("alpha", Map.of("kowiki-2000-02-29-0000000f", List.of(free130(900001)))));

    assertThat(videos.byEvent()).isEmpty();
  }

  // ---- (8) failure isolation ----

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Video search failed"))
        .map(l -> l.substring(l.indexOf("Video search failed")))
        .toList();
  }

  private static FakeVideoSource good() {
    return source("beta", Map.of(E1, List.of(free130(900001))));
  }

  @Test
  void aFailedSourceIsEmptyAndPartialWithoutAnotherWarn(CapturedOutput output) {
    FakeVideoSource failed =
        new FakeVideoSource(
            "alpha",
            events ->
                new SourceResult(Map.of(E2, List.of(free130(900002))), Map.of(), Status.FAILED));

    Videos videos = run(failed, good());

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output)).isEmpty();
  }

  @Test
  void aThrowingSourceIsOneWarnLineWithItsClass(CapturedOutput output) {
    FakeVideoSource throwing =
        new FakeVideoSource(
            "alpha",
            events -> {
              throw new IllegalStateException("synthetic");
            });

    Videos videos = run(throwing, good());

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=IllegalStateException");
  }

  @Test
  void aSourceAnsweringNothingIsOneWarnLineWithItsClass(CapturedOutput output) {
    Videos videos = run(new FakeVideoSource("alpha", events -> null), good());

    assertThat(videos.failed()).isTrue();
    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=NullPointerException");
  }

  @Test
  void aSilentSourceEndsWithinOneWindowWithOneDeadlineLine(CapturedOutput output) {
    FakeVideoSource silent = new FakeVideoSource("alpha", events -> FakeVideoSource.silent());

    long start = System.nanoTime();
    Videos videos = run(silent, good());
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took)
        .isLessThanOrEqualTo(DEADLINE.plus(Deadline.CANCEL_GRACE).plus(Duration.ofMillis(500)));
    assertThat(took).isGreaterThanOrEqualTo(DEADLINE);
    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=deadline");
  }

  @Test
  void aSourceEndingFailedWhenTheDeadlineStopsItAddsNoLine(CapturedOutput output) {
    FakeVideoSource late =
        new FakeVideoSource(
            "alpha",
            events -> {
              try {
                Thread.sleep(60_000);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // its own read timed out: it logged
              }
              return new SourceResult(Map.of(), Map.of(), Status.FAILED);
            });

    Videos videos = run(late, good());

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output)).isEmpty();
  }

  @Test
  void aSkippedQuotaIsNotAFailure(CapturedOutput output) {
    FakeVideoSource skipped =
        new FakeVideoSource(
            "youtube", events -> new SourceResult(Map.of(), Map.of(), Status.QUOTA_SKIPPED));

    Videos videos = run(skipped, good());

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isFalse();
    assertThat(failures(output)).isEmpty();
  }

  @Test
  void noCandidateIsNotAFailure() {
    Videos videos = run(source("alpha", Map.of()), source("beta", Map.of(E1, List.of())));

    assertThat(videos).isEqualTo(new Videos(Map.of(), false));
  }

  @Test
  void anInterruptedCallerFailsWithoutAWarn(CapturedOutput output) {
    FakeVideoSource silent = new FakeVideoSource("alpha", events -> FakeVideoSource.silent());

    Thread.currentThread().interrupt();
    Videos videos;
    try {
      videos = run(silent);
    } finally {
      assertThat(Thread.interrupted()).isTrue();
    }

    assertThat(videos.failed()).isTrue();
    assertThat(failures(output)).isEmpty();
  }

  // ---- (11) logs ----

  @Test
  void logsHoldNoDateTitleVideoIdOrFullEventId(CapturedOutput output) {
    Candidate noMatch =
        commons(900007, "Example footage", null, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
    run(
        new FakeVideoSource(
            "alpha",
            events -> {
              throw new IllegalStateException("synthetic");
            }),
        new FakeVideoSource("beta", events -> FakeVideoSource.silent()),
        source(
            "gamma",
            Map.of(
                E1,
                List.of(
                    noMatch,
                    youtube("AAAAAAAAAAA", TITLE, day("2000-02-29"), EventLink.NONE, true)))));

    assertThat(output.getAll())
        .contains("Video search failed", "Video candidates rejected")
        .doesNotContain(
            "2000-02-29",
            "kowiki-2000",
            "0000000a",
            "알파",
            "Example footage",
            "commons-900007",
            "AAAAAAAAAAA",
            "Q999999901");
  }

  @Test
  void theChosenItemIsTheCandidatesItemWithItsSelection() {
    Candidate best = free130(900001);

    MediaItem chosen = run(source("alpha", Map.of(E1, List.of(best)))).byEvent().get(E1);

    assertThat(chosen.record()).isSameAs(best.item().record());
    assertThat(chosen.dateRelation()).isEqualTo(best.item().dateRelation());
    assertThat(chosen.selection()).isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(best.item().selection()).isNull();
  }
}
