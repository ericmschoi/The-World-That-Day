package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a4 T1 (1): what a {@link Candidate} requires and what it fills in. Plan M3a4b T0 (2): a
 * capture that breaks rule R8 and a {@code null} stated date are refused when it is built.
 */
class CandidateTest {

  private static final MediaItem ITEM = VideoCandidates.commonsVideo("commons-900001", 95.0, null);

  @Test
  void itemEventLinkAndBasisAreRequired() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Candidate(
                    null, "t", null, null, null, EventLink.P10, false, false, Basis.COMMONS_P10))
        .withMessage("item");
    assertThatNullPointerException()
        .isThrownBy(
            () -> new Candidate(ITEM, "t", null, null, null, null, false, false, Basis.COMMONS_P10))
        .withMessage("eventLink");
    assertThatNullPointerException()
        .isThrownBy(
            () -> new Candidate(ITEM, "t", null, null, null, EventLink.P10, false, false, null))
        .withMessage("basis");
  }

  @Test
  void missingDatesAndCaptureAreEmptyAndUnknown() {
    Candidate candidate =
        new Candidate(
            ITEM, null, null, null, null, EventLink.NONE, false, false, Basis.COMMONS_SEARCH);

    assertThat(candidate.explicitDates()).isEmpty();
    assertThat(candidate.originalCapture()).isSameAs(CaptureTime.UNKNOWN);
  }

  @Test
  void explicitDatesAreCopied() {
    List<LocalDate> dates = new ArrayList<>(List.of(LocalDate.of(2000, 2, 29)));
    Candidate candidate =
        new Candidate(
            ITEM, null, null, null, dates, EventLink.NONE, true, false, Basis.DAEHAN_NEWS);
    dates.add(LocalDate.of(2000, 3, 1));

    assertThat(candidate.explicitDates()).containsExactly(LocalDate.of(2000, 2, 29));
  }

  private static Candidate withCapture(CaptureTime capture) {
    return new Candidate(
        ITEM, "t", null, capture, List.of(), EventLink.P10, false, false, Basis.COMMONS_P10);
  }

  @ParameterizedTest(name = "[{0}] {1} is refused")
  @CsvSource(
      nullValues = "none",
      value = {
        "1969-7-20, DAY",
        "1969-07-20, YEAR",
        "none, DAY",
        "1969-02-30, DAY",
        "1900-02-29, DAY",
        "1969, none",
        "1969-07-20, MONTH",
        "1969, CURRENT"
      })
  void aCaptureBreakingRuleR8IsRefusedWithoutItsValue(String at, DatePrecision precision) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> withCapture(new CaptureTime(at, precision)))
        .withMessage("originalCapture breaks rule R8");
  }

  @ParameterizedTest(name = "[{0}] {1} is kept")
  @CsvSource(
      nullValues = "none",
      value = {
        "2000-02-29, DAY",
        "2000-02, MONTH",
        "2000, YEAR",
        "2000, APPROX",
        "none, CURRENT",
        "none, none"
      })
  void aCaptureFollowingRuleR8IsKept(String at, DatePrecision precision) {
    CaptureTime capture = new CaptureTime(at, precision);

    assertThat(withCapture(capture).originalCapture()).isEqualTo(capture);
  }

  @Test
  void aNullStatedDateIsRefused() {
    List<LocalDate> dates = Arrays.asList(LocalDate.of(2000, 2, 29), null);

    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Candidate(
                    ITEM, null, null, null, dates, EventLink.NONE, true, false, Basis.DAEHAN_NEWS));
  }
}
