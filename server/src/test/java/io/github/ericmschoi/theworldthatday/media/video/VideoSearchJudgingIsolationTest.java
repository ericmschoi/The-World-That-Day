package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.LEAP_DAY;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.commons;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.ok;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a4b T0 (1) and (2) on the search itself: judging is wrapped per source, so a judge that
 * throws or a source answer that breaks the input checks leaves only that source empty, with one
 * WARN line naming the source, the event count and the exception class. Fake sources, synthetic
 * candidates on the leap day 2000-02-29 and invented event titles. No HTTP.
 */
@ExtendWith(OutputCaptureExtension.class)
class VideoSearchJudgingIsolationTest {

  private static final Duration DEADLINE = Duration.ofMillis(400);
  private static final String E1 = "kowiki-2000-02-29-0000000a";
  private static final String E2 = "kowiki-2000-02-29-0000000b";
  private static final String TITLE = "알파 행사 영상";

  private static final List<EventTarget> TWO =
      List.of(
          new EventTarget(E1, "알파 행사", "country:KR", "지어낸 문장이다.", "Q999999901"),
          new EventTarget(E2, "알파 행사", "country:KR", "지어낸 문장이다.", "Q999999902"));

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

  private static Videos run(VideoSearch search) {
    try (Deadline deadline = Deadline.after(DEADLINE)) {
      return search.start(deadline, LEAP_DAY, TWO, false).await();
    }
  }

  // 130: P10, same day, event words, free
  private static Candidate free130(long n) {
    return commons(n, TITLE, day("2000-02-29"), EventLink.P10, Basis.COMMONS_P10);
  }

  private static Candidate boom(long n) {
    return commons(
        n, ThrowingVideoAccuracy.BOOM, day("2000-02-29"), EventLink.P10, Basis.COMMONS_P10);
  }

  // NO_MATCH: no date, no event words
  private static Candidate noMatch(long n) {
    return commons(n, "Example footage", null, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
  }

  /** beta: the free 130 video for event 1. */
  private static FakeVideoSource good() {
    return new FakeVideoSource("beta", events -> ok(Map.of(E1, List.of(free130(900001)))));
  }

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Video search failed"))
        .map(l -> l.substring(l.indexOf("Video search failed")))
        .toList();
  }

  // ---- (1) judging isolation ----

  @Test
  void aCandidateThatMakesTheJudgeThrowEmptiesOnlyItsSource(CapturedOutput output) {
    // alpha's good video for event 2 and its rejection are lost with it; beta stays.
    FakeVideoSource alpha =
        new FakeVideoSource(
            "alpha",
            events ->
                ok(
                    Map.of(
                        E1, List.of(noMatch(900002), boom(900003)),
                        E2, List.of(free130(900004)))));

    Videos videos = run(ThrowingVideoAccuracy.search(3, alpha, good()));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.byEvent().get(E1).record().id()).isEqualTo("commons-900001");
    assertThat(videos.byEvent().get(E1).selection())
        .isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=IllegalStateException");
    assertThat(output.getAll()).doesNotContain("Video candidates rejected");
  }

  @Test
  void aJudgeThatThrowsWhilePreparingAnEventEmptiesOnlyItsSource(CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource(
            "alpha",
            events ->
                new SourceResult(
                    Map.of(E2, List.of(free130(900002))),
                    Map.of(E2, ThrowingVideoAccuracy.BOOM),
                    Status.OK));

    Videos videos = run(ThrowingVideoAccuracy.search(3, good(), alpha));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=IllegalStateException");
  }

  @Test
  void theSameJudgeWithoutTheMarkerFailsNothing(CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource("alpha", events -> ok(Map.of(E2, List.of(free130(900002)))));

    Videos videos = run(ThrowingVideoAccuracy.search(3, alpha, good()));

    assertThat(videos.byEvent()).containsOnlyKeys(E1, E2);
    assertThat(videos.failed()).isFalse();
    assertThat(failures(output)).isEmpty();
  }

  // ---- (2) input checks fail the source's own task ----

  @ParameterizedTest(name = "[{0}] {1} fails alpha only")
  @CsvSource(
      nullValues = "none",
      value = {"1969-02-30, DAY", "1969-7-20, DAY", "1969, none", "none, DAY"})
  void aCandidateBreakingRuleR8FailsOnlyItsSource(
      String at, DatePrecision precision, CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource(
            "alpha",
            events ->
                ok(
                    Map.of(
                        E2,
                        List.of(
                            commons(
                                900002,
                                TITLE,
                                new CaptureTime(at, precision),
                                EventLink.P10,
                                Basis.COMMONS_P10)))));

    Videos videos = run(FakeVideoSource.search(3, alpha, good()));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly(
            "Video search failed: source=alpha events=2 cause=IllegalArgumentException");
    assertThat(output.getAll()).doesNotContain("1969");
  }

  @Test
  void aNullCandidateFailsOnlyItsSource(CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource(
            "alpha", events -> ok(Map.of(E2, Arrays.asList(free130(900002), null))));

    Videos videos = run(FakeVideoSource.search(3, alpha, good()));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=NullPointerException");
  }

  @Test
  void aNullListFailsOnlyItsSource(CapturedOutput output) {
    Map<String, List<Candidate>> byEvent = new HashMap<>();
    byEvent.put(E2, null);
    FakeVideoSource alpha = new FakeVideoSource("alpha", events -> ok(byEvent));

    Videos videos = run(FakeVideoSource.search(3, good(), alpha));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=NullPointerException");
  }

  @Test
  void aNullStatedDateFailsOnlyItsSource(CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource(
            "alpha",
            events ->
                ok(
                    Map.of(
                        E2,
                        List.of(
                            new Candidate(
                                free130(900002).item(),
                                TITLE,
                                null,
                                null,
                                Arrays.asList(LEAP_DAY, null),
                                EventLink.NONE,
                                true,
                                false,
                                Basis.DAEHAN_NEWS)))));

    Videos videos = run(FakeVideoSource.search(3, alpha, good()));

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(failures(output))
        .containsExactly("Video search failed: source=alpha events=2 cause=NullPointerException");
  }

  // ---- logs ----

  @Test
  void failureLinesHoldNoDateTitleVideoIdOrEventId(CapturedOutput output) {
    FakeVideoSource alpha =
        new FakeVideoSource("alpha", events -> ok(Map.of(E1, List.of(boom(900007)))));
    FakeVideoSource gamma =
        new FakeVideoSource(
            "gamma", events -> ok(Map.of(E2, Arrays.asList(free130(900008), null))));

    run(ThrowingVideoAccuracy.search(3, alpha, gamma, good()));

    assertThat(failures(output)).hasSize(2);
    assertThat(output.getAll())
        .doesNotContain(
            "2000-02-29",
            "kowiki-2000",
            "0000000a",
            "알파",
            ThrowingVideoAccuracy.BOOM,
            "synthetic",
            "commons-900007",
            "commons-900008",
            "Q999999901");
  }
}
