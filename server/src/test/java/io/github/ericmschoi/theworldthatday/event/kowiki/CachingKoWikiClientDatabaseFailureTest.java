package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;

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
        .contains("Wiki section cache write failed: title=9월 17일");
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
}
