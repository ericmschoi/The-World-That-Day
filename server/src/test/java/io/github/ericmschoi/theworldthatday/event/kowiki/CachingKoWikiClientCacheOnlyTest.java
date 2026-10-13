package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * {@link CachingKoWikiClient#cached}: the copy the event source uses when it skips the year page
 * after a stalled date page (plan M3a1c). Never sends a request.
 */
class CachingKoWikiClientCacheOnlyTest {

  private static final Instant FETCHED = Instant.parse("2026-01-01T00:00:00Z");

  private final MutableClock clock = new MutableClock(FETCHED, ClockConfig.SERVICE_ZONE);
  private final ActionApiKoWikiClient delegate = mock(ActionApiKoWikiClient.class);
  private final WikiSectionCacheRepository repository = mock(WikiSectionCacheRepository.class);
  private final CachingKoWikiClient client =
      new CachingKoWikiClient(
          delegate,
          repository,
          new WikimediaProperties(
              "",
              Duration.ofSeconds(2),
              Duration.ofSeconds(4),
              1,
              Duration.ofSeconds(120),
              3,
              Duration.ofDays(7),
              Duration.ofSeconds(5),
              URI.create("http://localhost:9/w/api.php")),
          clock);

  private void cacheRow() {
    WikiSectionCache row = mock(WikiSectionCache.class);
    when(row.revid()).thenReturn(200L);
    when(row.eventsWikitext()).thenReturn("== 사건 ==");
    when(row.cultureWikitext()).thenReturn(null);
    when(row.fetchedAt()).thenReturn(FETCHED);
    when(repository.findBySiteAndPageTitle("localhost", "2000년")).thenReturn(Optional.of(row));
  }

  @Test
  void rowInsideTheTtlIsCached() {
    cacheRow();
    clock.advance(Duration.ofDays(7).minusSeconds(1));

    assertThat(client.cached("2000년"))
        .isEqualTo(new KoWikiPage("2000년", State.CACHED, 200L, "== 사건 ==", null, FETCHED));
    verifyNoInteractions(delegate);
  }

  @Test
  void rowFromTheTtlOnIsStale() {
    cacheRow();
    clock.advance(Duration.ofDays(7));

    assertThat(client.cached("2000년").state()).isEqualTo(State.STALE);
    assertThat(client.cached("2000년").fetchedAt()).isEqualTo(FETCHED);
    verifyNoInteractions(delegate);
  }

  @Test
  void noRowOrADatabaseErrorIsUnavailable() {
    assertThat(client.cached("2000년")).isEqualTo(KoWikiPage.unavailable("2000년"));

    when(repository.findBySiteAndPageTitle(anyString(), anyString()))
        .thenThrow(new DataAccessResourceFailureException("down"));
    assertThat(client.cached("2000년")).isEqualTo(KoWikiPage.unavailable("2000년"));
    verifyNoInteractions(delegate);
  }

  @Test
  void clientWithoutACacheHasNoCopy() {
    KoWikiClient plain = title -> KoWikiPage.absent(title);

    assertThat(plain.cached("2000년")).isEqualTo(KoWikiPage.unavailable("2000년"));
  }
}
