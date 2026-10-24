package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
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
 * and {@code dateTextExcluded} that dates written in the title or description are not used.
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
    Selection.Basis basis) {

  public Candidate {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(eventLink, "eventLink");
    Objects.requireNonNull(basis, "basis");
    originalCapture = originalCapture == null ? CaptureTime.UNKNOWN : originalCapture;
    explicitDates = explicitDates == null ? List.of() : List.copyOf(explicitDates);
  }

  /** How the video is tied to the event: Wikidata {@code P10}, its own category, or neither. */
  public enum EventLink {
    P10,
    CATEGORY_DIRECT,
    NONE
  }
}
