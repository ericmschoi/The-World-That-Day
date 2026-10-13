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
import org.springframework.transaction.TransactionException;

/**
 * Wraps {@link ActionApiKoWikiClient} with the wikitext cache (plan M3a1b, table {@code
 * wiki_section_cache}). A row is {@code CACHED} (no request) while {@code now < fetched_at +
 * wikimedia.cache-ttl}; from exactly {@code fetched_at + cache-ttl} on, the page is read again:
 * success replaces the row ({@code FRESH}), failure falls back to the old row ({@code STALE}) or
 * {@code UNAVAILABLE} without one. Missing pages are not cached, and a row whose page is now
 * missing is deleted so it can never come back as {@code STALE}. Only raw wikitext is stored, so
 * parser changes never need a cache flush. A database error, including an unreachable database,
 * only costs the cache: the page is still read from Wikipedia and nothing is thrown.
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
    if (cached.isPresent() && isFresh(cached.get())) {
      return page(title, cached.get(), State.CACHED);
    }
    KoWikiPage page = delegate.fetch(title);
    return switch (page.state()) {
      case FRESH -> save(page);
      case UNAVAILABLE -> cached.map(row -> page(title, row, State.STALE)).orElse(page);
      case ABSENT -> {
        if (cached.isPresent()) {
          delete(title);
        }
        yield page;
      }
      default -> page;
    };
  }

  @Override
  public KoWikiPage cached(String title) {
    return find(title)
        .map(row -> page(title, row, isFresh(row) ? State.CACHED : State.STALE))
        .orElseGet(() -> KoWikiPage.unavailable(title));
  }

  private boolean isFresh(WikiSectionCache row) {
    return row.fetchedAt().plus(properties.cacheTtl()).isAfter(clock.instant());
  }

  private void delete(String title) {
    try {
      repository.deleteBySiteAndPageTitle(site(), title);
    } catch (DataAccessException | TransactionException e) {
      log.warn("Wiki section cache delete failed: title={} cause={}", title, cause(e));
    }
  }

  private Optional<WikiSectionCache> find(String title) {
    try {
      return repository.findBySiteAndPageTitle(site(), title);
    } catch (DataAccessException | TransactionException e) {
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
    } catch (DataAccessException | TransactionException e) {
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
