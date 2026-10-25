package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The {@code result} text of one {@code event_media_search_cache} row (plan M3a4c): a version, the
 * event's English label and its candidates, each with the normalized {@link MediaRecord}, the
 * {@link MediaOrigin} it passed {@link MediaFilter} with and what the accuracy rules read. Only
 * addresses and text metadata: no file, no {@link MediaItem}, no selection. Reading filters and
 * normalizes every record again for the row's date, so the blocklist, the agency ban and rules R1
 * to R9 apply to stored rows too. It never logs and never throws.
 */
@Component
public class SearchCacheFormat {

  static final int VERSION = 1;

  /** Every key must be present (a {@code null} value is allowed where the field allows it). */
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  private final MediaFilter filter;
  private final MediaNormalizer normalizer;

  public SearchCacheFormat(MediaFilter filter, MediaNormalizer normalizer) {
    this.filter = filter;
    this.normalizer = normalizer;
  }

  /** One event's cached answer: its English label ({@code null} if none) and kept candidates. */
  public record Restored(String englishLabel, List<Candidate> candidates) {}

  /** The row text, or empty when it must not be stored: a candidate has no origin. */
  public Optional<String> write(String englishLabel, List<Candidate> candidates) {
    try {
      if (candidates.stream().anyMatch(candidate -> candidate.origin() == null)) {
        return Optional.empty();
      }
      List<Stored> stored = candidates.stream().map(Stored::of).toList();
      return Optional.of(JSON.writeValueAsString(new Row(VERSION, englishLabel, stored)));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  /**
   * The event's answer for {@code date}, without the candidates that are now filtered or break a
   * rule; empty when the text cannot be read (unknown version, broken JSON, a missing key, a
   * candidate that cannot be built), so the row counts as absent.
   */
  public Optional<Restored> read(String text, LocalDate date) {
    try {
      Row row = JSON.readValue(text, Row.class);
      if (row.version() != VERSION) {
        return Optional.empty();
      }
      List<Candidate> kept = new ArrayList<>();
      for (Stored stored : Objects.requireNonNull(row.candidates())) {
        restore(Objects.requireNonNull(stored), date).ifPresent(kept::add);
      }
      return Optional.of(new Restored(row.englishLabel(), List.copyOf(kept)));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  /** Throws when the stored candidate cannot be built; empty when it is filtered or dropped. */
  private Optional<Candidate> restore(Stored s, LocalDate date) {
    MediaRecord record = Objects.requireNonNull(s.record());
    MediaOrigin origin = Objects.requireNonNull(s.origin());
    EventLink link = Objects.requireNonNull(s.eventLink());
    Basis basis = Objects.requireNonNull(s.basis());
    List<LocalDate> dates = List.copyOf(s.explicitDates());
    CaptureTime capture = new CaptureTime(s.originalCapturedAt(), s.originalDatePrecision());
    if (!capture.followsRuleR8()) {
      throw new IllegalArgumentException("originalCapture breaks rule R8");
    }
    if (filter.check(record, origin).isPresent()) {
      return Optional.empty();
    }
    return normalizer
        .normalize(record, date)
        .map(
            item ->
                new Candidate(
                    item,
                    s.title(),
                    s.description(),
                    capture,
                    dates,
                    link,
                    s.official(),
                    s.dateTextExcluded(),
                    basis,
                    origin));
  }

  @JsonNaming(SnakeCaseStrategy.class)
  record Row(int version, String englishLabel, List<Stored> candidates) {}

  @JsonNaming(SnakeCaseStrategy.class)
  record Stored(
      MediaRecord record,
      MediaOrigin origin,
      String title,
      String description,
      String originalCapturedAt,
      DatePrecision originalDatePrecision,
      List<LocalDate> explicitDates,
      EventLink eventLink,
      boolean official,
      boolean dateTextExcluded,
      Basis basis) {

    static Stored of(Candidate c) {
      return new Stored(
          c.item().record(),
          c.origin(),
          c.title(),
          c.description(),
          c.originalCapture().capturedAt(),
          c.originalCapture().datePrecision(),
          c.explicitDates(),
          c.eventLink(),
          c.official(),
          c.dateTextExcluded(),
          c.basis());
    }
  }
}
