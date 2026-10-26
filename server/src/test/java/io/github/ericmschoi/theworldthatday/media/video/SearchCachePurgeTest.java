package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a5a T5 (5): the 30-day purge on the Testcontainers table, timed by the test clock. A
 * {@code youtube} row goes when {@code fetched_at + 29 days <= now}; one a second younger and every
 * {@code commons} row stay. Synthetic event IDs and results; the sample date 1986-09-20.
 */
@ExtendWith(OutputCaptureExtension.class)
class SearchCachePurgeTest extends WikimediaWireMockTest {

  private static final LocalDate DAY = LocalDate.of(1986, 9, 20);

  @Autowired private SearchCachePurge purge;
  @Autowired private EventMediaSearchCacheRepository repository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE event_media_search_cache");
  }

  private void row(String source, String eventId, Instant fetchedAt) {
    repository.upsert(source, DAY, eventId, false, "{\"synthetic\":true}", fetchedAt);
  }

  @Test
  void youtubeRowsGoAt29DaysAndOthersStay() {
    Instant now = clock.instant();
    row("youtube", "kowiki-1986-09-20-0000000a", now.minus(Duration.ofDays(29)));
    row("youtube", "kowiki-1986-09-20-0000000b", now.minus(Duration.ofDays(29)).plusSeconds(1));
    row("youtube", "kowiki-1986-09-20-0000000c", now.minus(Duration.ofDays(400)));
    row("youtube", "kowiki-1986-09-20-0000000d", now);
    row("commons", "kowiki-1986-09-20-0000000a", now.minus(Duration.ofDays(400)));

    purge.purge();

    assertThat(
            jdbc.queryForList(
                "SELECT source || ' ' || event_id FROM event_media_search_cache ORDER BY 1",
                String.class))
        .containsExactly(
            "commons kowiki-1986-09-20-0000000a",
            "youtube kowiki-1986-09-20-0000000b",
            "youtube kowiki-1986-09-20-0000000d");
  }

  @Test
  void theNextRunTakesTheRowThatTurned29() {
    row(
        "youtube",
        "kowiki-1986-09-20-0000000b",
        clock.instant().minus(Duration.ofDays(29)).plusSeconds(1));
    purge.purge();
    assertThat(repository.count()).isEqualTo(1);

    clock.advance(Duration.ofSeconds(1));
    purge.purge();

    assertThat(repository.count()).isZero();
  }

  @Test
  void aDatabaseFailureLogsOneLineWithoutDetails(CapturedOutput output) {
    EventMediaSearchCacheRepository down =
        (EventMediaSearchCacheRepository)
            Proxy.newProxyInstance(
                EventMediaSearchCacheRepository.class.getClassLoader(),
                new Class<?>[] {EventMediaSearchCacheRepository.class},
                (proxy, method, args) -> {
                  throw new DataAccessResourceFailureException("synthetic");
                });

    new SearchCachePurge(down, clock).purge();

    assertThat(output.getAll().lines().filter(l -> l.contains("Media search cache purge")))
        .singleElement()
        .satisfies(
            l ->
                assertThat(l)
                    .contains(" WARN ")
                    .endsWith(
                        "Media search cache purge failed: source=youtube"
                            + " cause=DataAccessResourceFailureException"));
  }
}
