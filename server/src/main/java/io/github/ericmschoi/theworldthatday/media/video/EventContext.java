package io.github.ericmschoi.theworldthatday.media.video;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One event a video source searches for (plan M3a4): {@code title} is the Korean title of its main
 * link, {@code englishLabel} the English label of the event's Wikidata item ({@code null} when
 * unknown), {@code summary} the event text as written.
 */
public record EventContext(
    String eventId,
    LocalDate date,
    String title,
    String englishLabel,
    String summary,
    String wikidataQid,
    String placeRef) {

  public EventContext {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(date, "date");
  }
}
