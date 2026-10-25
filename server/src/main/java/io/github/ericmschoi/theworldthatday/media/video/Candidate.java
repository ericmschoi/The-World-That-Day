package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.Selection;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A video a source found for one event, with what {@link VideoAccuracy} scores (plan M3a4). The
 * adapter builds {@code item} through {@code MediaFilter} and {@code MediaNormalizer}, so blocking,
 * the agency ban and rules R1 to R9 are its job. {@code originalCapture} is the source's own
 * capture date field as parsed ({@code UNKNOWN} when absent), {@code explicitDates} dates the
 * source states (a newsreel issue date), {@code official} whether the channel ID is an allowed one,
 * and {@code dateTextExcluded} that dates written in the title or description are not used. A
 * capture that breaks rule R8 or a {@code null} date is refused when the adapter builds it, so it
 * fails that source's task alone (plan M3a4b T0). {@code origin} is what the adapter gave {@code
 * MediaFilter} (plan M3a4c): the result cache stores it to filter the item again when it is read,
 * and a candidate without one is never stored.
 */
public record Candidate(
    MediaItem item,
    String title,
    String description,
    CaptureTime originalCapture,
    List<LocalDate> explicitDates,
    EventLink eventLink,
    boolean official,
    boolean dateTextExcluded,
    Selection.Basis basis,
    MediaOrigin origin) {

  public Candidate {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(eventLink, "eventLink");
    Objects.requireNonNull(basis, "basis");
    originalCapture = originalCapture == null ? CaptureTime.UNKNOWN : originalCapture;
    if (!originalCapture.followsRuleR8()) {
      throw new IllegalArgumentException("originalCapture breaks rule R8"); // no value in the text
    }
    explicitDates = explicitDates == null ? List.of() : List.copyOf(explicitDates); // no null date
  }

  /** A candidate without an origin, which the result cache never stores. */
  public Candidate(
      MediaItem item,
      String title,
      String description,
      CaptureTime originalCapture,
      List<LocalDate> explicitDates,
      EventLink eventLink,
      boolean official,
      boolean dateTextExcluded,
      Selection.Basis basis) {
    this(
        item,
        title,
        description,
        originalCapture,
        explicitDates,
        eventLink,
        official,
        dateTextExcluded,
        basis,
        null);
  }

  /** How the video is tied to the event: Wikidata {@code P10}, its own category, or neither. */
  public enum EventLink {
    P10,
    CATEGORY_DIRECT,
    NONE
  }
}
