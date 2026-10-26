package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat.Restored;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The video search result cache of plan M3a4c (table {@code event_media_search_cache}) for sources
 * with a {@code media.search-cache.ttl} above 0. A row is fresh while {@code fetched_at + ttl} is
 * after now; one that cannot be read counts as absent. A database failure loses the cache only,
 * with one WARN line naming the source and the exception class (never a date, event or file).
 */
@Component
public class SearchCache {

  private static final Logger log = LoggerFactory.getLogger(SearchCache.class);

  private final EventMediaSearchCacheRepository repository;
  private final SearchCacheFormat format;
  private final SearchCacheProperties properties;
  private final Clock clock;

  SearchCache(
      EventMediaSearchCacheRepository repository,
      SearchCacheFormat format,
      SearchCacheProperties properties,
      Clock clock) {
    this.repository = repository;
    this.format = format;
    this.properties = properties;
    this.clock = clock;
  }

  /** The cache of one lookup, used on the caller's thread only. */
  Session session() {
    return new Session();
  }

  final class Session {

    /** Set after a failed read: the lookup's other reads and all its writes are skipped. */
    private boolean down;

    /** The fresh, readable answers of {@code source} by event ID; none when it is not cached. */
    Map<String, Restored> read(
        String source, LocalDate date, List<String> eventIds, boolean dayPathsOff) {
      Optional<Duration> ttl = properties.ttl(source);
      if (down || ttl.isEmpty() || eventIds.isEmpty()) {
        return Map.of();
      }
      List<EventMediaSearchCache> rows;
      try {
        rows = repository.find(source, date, dayPathsOff, eventIds);
      } catch (RuntimeException e) {
        down = true;
        log.warn(
            "Media search cache read failed: source={} cause={}",
            source,
            e.getClass().getSimpleName());
        return Map.of();
      }
      Instant now = clock.instant();
      Map<String, Restored> fresh = new HashMap<>();
      for (EventMediaSearchCache row : rows) {
        if (row.fetchedAt().plus(ttl.get()).isAfter(now)) {
          format.read(row.result(), date).ifPresent(restored -> fresh.put(row.eventId(), restored));
        }
      }
      return fresh;
    }

    /** Stores one row per asked event (none for an event the format refuses). */
    void write(
        String source,
        LocalDate date,
        List<String> eventIds,
        boolean dayPathsOff,
        SourceResult result) {
      if (down || properties.ttl(source).isEmpty()) {
        return;
      }
      for (String id : eventIds) {
        Optional<String> text =
            format.write(
                result.englishLabels().get(id), result.byEvent().getOrDefault(id, List.of()));
        if (text.isEmpty()) {
          continue;
        }
        try {
          repository.upsert(source, date, id, dayPathsOff, text.get(), clock.instant());
        } catch (RuntimeException e) {
          log.warn(
              "Media search cache write failed: source={} cause={}",
              source,
              e.getClass().getSimpleName());
          return; // one line per source and lookup; the other events are not tried
        }
      }
    }
  }
}
