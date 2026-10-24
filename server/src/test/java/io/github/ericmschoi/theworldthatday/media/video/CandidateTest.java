package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plan M3a4 T1 (1): what a {@link Candidate} requires and what it fills in. */
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
}
