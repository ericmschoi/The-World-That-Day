package io.github.ericmschoi.theworldthatday.media.video;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes the {@code youtube} rows of the search cache 29 days after they were fetched (plan M3a5
 * "캐시와 30일"): YouTube's terms keep API data at most 30 days (III.E.4.d), and the cache otherwise
 * only replaces a row when its date is asked again. Runs when the application is ready, then every
 * hour (29 days and 1 hour stay under 30 days), whether YouTube is on or not. A database failure
 * logs one WARN line with the exception class; the next run tries again.
 */
@Component
class SearchCachePurge {

  static final String SOURCE = "youtube";
  static final Duration MAX_AGE = Duration.ofDays(29);

  private static final Logger log = LoggerFactory.getLogger(SearchCachePurge.class);

  private final EventMediaSearchCacheRepository repository;
  private final Clock clock;

  SearchCachePurge(EventMediaSearchCacheRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  void purge() {
    try {
      int rows = repository.deleteFetchedUpTo(SOURCE, clock.instant().minus(MAX_AGE));
      log.debug("Media search cache purged: source={} rows={}", SOURCE, rows);
    } catch (RuntimeException e) {
      log.warn(
          "Media search cache purge failed: source={} cause={}",
          SOURCE,
          e.getClass().getSimpleName());
    }
  }
}
