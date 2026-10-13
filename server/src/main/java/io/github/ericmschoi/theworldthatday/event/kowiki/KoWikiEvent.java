package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One event line. {@code text} is the parser output unchanged. {@code linkTitles} are canonical
 * titles (redirects followed), the first being the main link; {@code wikidataQid} belongs to the
 * main link and is {@code null} when it has none or links could not be resolved. {@code
 * retrievedAt} is when the page was read from Wikipedia (for a cached page, the cache row's time).
 */
public record KoWikiEvent(
    String id,
    LocalDate date,
    String sourceTitle,
    String sourceSection,
    long revision,
    String text,
    List<String> linkTitles,
    String wikidataQid,
    Instant retrievedAt) {

  public KoWikiEvent {
    linkTitles = List.copyOf(linkTitles);
  }
}
