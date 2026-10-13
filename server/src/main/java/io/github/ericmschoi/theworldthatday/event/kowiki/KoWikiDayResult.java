package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of {@link KoWikiEventSource#find}. {@code events} keep the input order (date page 사건, 문화,
 * then year page 사건, 문화); {@code docs} are the date page and the year page, in that order. {@code
 * mergedCount} is the number of duplicates removed, {@code linkResolutionFailed} is true when links
 * had to be resolved and the lookup failed or ran out of time.
 */
public record KoWikiDayResult(
    LocalDate date,
    List<KoWikiEvent> events,
    List<KoWikiDocStatus> docs,
    int skippedLines,
    int mergedCount,
    boolean linkResolutionFailed) {

  public KoWikiDayResult {
    events = List.copyOf(events);
    docs = List.copyOf(docs);
  }

  /** Both pages are {@code UNAVAILABLE}. Missing pages ({@code ABSENT}) are not a failure. */
  public boolean allUnavailable() {
    return docs.stream().allMatch(doc -> doc.state() == State.UNAVAILABLE);
  }

  /** A page is {@code UNAVAILABLE} or {@code STALE}, or the link lookup failed. */
  public boolean degraded() {
    return linkResolutionFailed
        || docs.stream()
            .anyMatch(doc -> doc.state() == State.UNAVAILABLE || doc.state() == State.STALE);
  }
}
