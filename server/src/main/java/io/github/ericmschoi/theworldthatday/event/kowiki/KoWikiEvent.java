package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One event line. {@code text} is the parser output unchanged. {@code dateKind} says whether the
 * line is of the day or starts a range there, {@code rangeEnd} is the range's last day (else {@code
 * null}, plan C1 S3a). {@code linkTitles} are canonical titles (redirects followed), the first
 * being the main link; {@code wikidataQid} belongs to the main link and is {@code null} when it has
 * none or links could not be resolved. {@code retrievedAt} is when the page was read from Wikipedia
 * (for a cached page, the cache row's time).
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
    Instant retrievedAt,
    DateKind dateKind,
    LocalDate rangeEnd) {

  public KoWikiEvent {
    linkTitles = List.copyOf(linkTitles);
    dateKind = dateKind == null ? DateKind.DAY : dateKind;
    rangeEnd = dateKind == DateKind.RANGE_START ? rangeEnd : null;
  }

  /** An event of the day itself. */
  public KoWikiEvent(
      String id,
      LocalDate date,
      String sourceTitle,
      String sourceSection,
      long revision,
      String text,
      List<String> linkTitles,
      String wikidataQid,
      Instant retrievedAt) {
    this(
        id,
        date,
        sourceTitle,
        sourceSection,
        revision,
        text,
        linkTitles,
        wikidataQid,
        retrievedAt,
        DateKind.DAY,
        null);
  }
}
