package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Wraps {@link ActionApiKoWikiClient} with the wikitext cache (plan M3a1b, table {@code
 * wiki_section_cache}). Younger than {@code wikimedia.cache-ttl}: {@code CACHED}, no request.
 * Otherwise the page is read again: success replaces the row ({@code FRESH}), failure falls back to
 * the old row ({@code STALE}) or {@code UNAVAILABLE} without one. Missing pages are not cached.
 * Only raw wikitext is stored, so parser changes never need a cache flush. A database error only
 * costs the cache: the page is still read from Wikipedia.
 */
@Primary
@Component
public class CachingKoWikiClient implements KoWikiClient {

  private static final Logger log = LoggerFactory.getLogger(CachingKoWikiClient.class);

  private final KoWikiClient delegate;
  private final WikiSectionCacheRepository repository;
  private final WikimediaProperties properties;
  private final Clock clock;

  public CachingKoWikiClient(
      ActionApiKoWikiClient delegate,
      WikiSectionCacheRepository repository,
      WikimediaProperties properties,
      Clock clock) {
    this.delegate = delegate;
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public KoWikiPage fetch(String title) {
    Optional<WikiSectionCache> cached = find(title);
    if (cached.isPresent()
        && cached.get().fetchedAt().plus(properties.cacheTtl()).isAfter(clock.instant())) {
      return page(title, cached.get(), State.CACHED);
    }
    KoWikiPage page = delegate.fetch(title);
    return switch (page.state()) {
      case FRESH -> save(page);
      case UNAVAILABLE -> cached.map(row -> page(title, row, State.STALE)).orElse(page);
      default -> page;
    };
  }

  private Optional<WikiSectionCache> find(String title) {
    try {
      return repository.findBySiteAndPageTitle(site(), title);
    } catch (DataAccessException e) {
      log.warn("Wiki section cache read failed: title={} cause={}", title, cause(e));
      return Optional.empty();
    }
  }

  /** Stores the page and returns it with the stored {@code fetched_at} (microseconds). */
  private KoWikiPage save(KoWikiPage page) {
    Instant fetchedAt = page.fetchedAt().truncatedTo(ChronoUnit.MICROS);
    try {
      repository.upsert(
          site(),
          page.title(),
          page.revision(),
          page.eventsWikitext(),
          page.cultureWikitext(),
          fetchedAt);
    } catch (DataAccessException e) {
      log.warn("Wiki section cache write failed: title={} cause={}", page.title(), cause(e));
    }
    return new KoWikiPage(
        page.title(),
        State.FRESH,
        page.revision(),
        page.eventsWikitext(),
        page.cultureWikitext(),
        fetchedAt);
  }

  private static KoWikiPage page(String title, WikiSectionCache row, State state) {
    return new KoWikiPage(
        title, state, row.revid(), row.eventsWikitext(), row.cultureWikitext(), row.fetchedAt());
  }

  private String site() {
    return properties.kowikiApiUrl().getHost();
  }

  private static String cause(Exception e) {
    return e.getClass().getSimpleName();
  }
}
