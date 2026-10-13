package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

/** A database error only costs the cache: the page is still read from Wikipedia. */
@ExtendWith(OutputCaptureExtension.class)
class CachingKoWikiClientDatabaseFailureTest {

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
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

  private void databaseDown() {
    DataAccessResourceFailureException down = new DataAccessResourceFailureException("down");
    when(repository.findBySiteAndPageTitle(anyString(), anyString())).thenThrow(down);
    doThrow(down)
        .when(repository)
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
  }

  @Test
  void freshPageIsReturnedWhenTheDatabaseFails(CapturedOutput output) {
    databaseDown();
    KoWikiPage fresh = new KoWikiPage("9월 17일", State.FRESH, 1L, "== 사건 ==", null, clock.instant());
    when(delegate.fetch("9월 17일")).thenReturn(fresh);

    assertThat(client.fetch("9월 17일")).isEqualTo(fresh);
    assertThat(output.getAll())
        .contains("Wiki section cache read failed: title=9월 17일")
        .as("T4: no write after a failed read")
        .doesNotContain("Wiki section cache write failed");
    verify(repository, never())
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
  }

  @Test
  void failedReadWithoutAWorkingDatabaseIsUnavailable() {
    databaseDown();
    when(delegate.fetch("9월 17일")).thenReturn(KoWikiPage.unavailable("9월 17일"));

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
  }

  @Test
  void freshPageTimeIsCutToWhatTheDatabaseKeeps() {
    Instant precise = Instant.parse("2026-01-01T00:00:00.123456789Z");
    when(delegate.fetch("9월 17일"))
        .thenReturn(new KoWikiPage("9월 17일", State.FRESH, 1L, null, null, precise));

    assertThat(client.fetch("9월 17일").fetchedAt())
        .isEqualTo(Instant.parse("2026-01-01T00:00:00.123456Z"));
  }

  @Test
  void unreachableDatabaseTransactionErrorsAreCaughtToo() {
    CannotCreateTransactionException down = new CannotCreateTransactionException("no connection");
    when(repository.findBySiteAndPageTitle(anyString(), anyString())).thenThrow(down);
    doThrow(down)
        .when(repository)
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
    KoWikiPage fresh = new KoWikiPage("9월 17일", State.FRESH, 1L, null, null, clock.instant());
    when(delegate.fetch("9월 17일")).thenReturn(fresh);

    assertThat(client.fetch("9월 17일")).isEqualTo(fresh);
  }

  @Test
  void failedDeleteOfAMissingPageStillReturnsAbsent(CapturedOutput output) {
    WikiSectionCache row = mock(WikiSectionCache.class);
    when(row.fetchedAt()).thenReturn(clock.instant().minus(Duration.ofDays(8)));
    when(repository.findBySiteAndPageTitle("localhost", "2월 29일")).thenReturn(Optional.of(row));
    doThrow(new CannotCreateTransactionException("no connection"))
        .when(repository)
        .deleteBySiteAndPageTitle("localhost", "2월 29일");
    when(delegate.fetch("2월 29일")).thenReturn(KoWikiPage.absent("2월 29일"));

    assertThat(client.fetch("2월 29일")).isEqualTo(KoWikiPage.absent("2월 29일"));
    assertThat(output.getAll()).contains("Wiki section cache delete failed: title=2월 29일");
  }

  @Test
  void missingPageWithoutARowDeletesNothing() {
    when(repository.findBySiteAndPageTitle(anyString(), anyString())).thenReturn(Optional.empty());
    when(delegate.fetch("2월 29일")).thenReturn(KoWikiPage.absent("2월 29일"));

    assertThat(client.fetch("2월 29일").state()).isEqualTo(State.ABSENT);
    verify(repository, never()).deleteBySiteAndPageTitle(anyString(), anyString());
  }
}
