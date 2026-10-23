package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
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

  /** The event of a media target, without an English label yet. */
  public static EventContext of(EventTarget target, LocalDate date) {
    return new EventContext(
        target.eventId(),
        date,
        target.title(),
        null,
        target.summary(),
        target.wikidataQid(),
        target.placeRef());
  }

  /** This event with the English label a source read for it. */
  public EventContext withEnglishLabel(String label) {
    return new EventContext(eventId, date, title, label, summary, wikidataQid, placeRef);
  }
}
