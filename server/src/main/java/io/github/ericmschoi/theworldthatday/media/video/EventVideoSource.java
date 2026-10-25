package io.github.ericmschoi.theworldthatday.media.video;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One video source (plan M3a4, adapters from M3a4b on): searched once per date for all of its
 * events, on the shared media deadline. Its read timeouts stay within {@code
 * media.lookup-deadline}.
 */
public interface EventVideoSource {

  /** The lowercase source key of logs and the blocklist: "commons", "youtube", "daehan_news". */
  String key();

  /**
   * Candidates by event ID for {@code events} (at most {@code media.video.max-events}, never a
   * {@code tragic} one). {@code dayPathsOff}: no date-wide path (such as a date category) may be
   * used, on a tragic day or a day with an unavailable Wikipedia page.
   */
  SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff);

  /**
   * Candidates by event ID, the English labels the source read by event ID (used for its own
   * candidates' keywords), and how the search went.
   */
  record SourceResult(
      Map<String, List<Candidate>> byEvent, Map<String, String> englishLabels, Status status) {

    /** Copies the candidate lists too; a {@code null} list or candidate is refused. */
    public SourceResult {
      byEvent =
          byEvent == null
              ? Map.of()
              : byEvent.entrySet().stream()
                  .collect(
                      Collectors.toUnmodifiableMap(Entry::getKey, e -> List.copyOf(e.getValue())));
      englishLabels = englishLabels == null ? Map.of() : Map.copyOf(englishLabels);
      Objects.requireNonNull(status, "status");
    }
  }

  /**
   * {@code FAILED}: the adapter already logged a WARN line, the answer is partial. {@code
   * QUOTA_SKIPPED}: the daily quota is spent (M3a5); not a failure.
   */
  enum Status {
    OK,
    FAILED,
    QUOTA_SKIPPED
  }
}
