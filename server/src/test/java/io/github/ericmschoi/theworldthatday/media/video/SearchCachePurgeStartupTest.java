package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * Plan M3a5a T5 (5): the purge runs once as soon as the application is ready, then every hour, also
 * with YouTube off (this context has the tests' empty key). Two {@code youtube} rows are written
 * after the schema exists and before the application is ready (this class's own context and
 * database); only the one older than 29 days is gone when the first test starts.
 */
@Import(SearchCachePurgeStartupTest.Rows.class)
class SearchCachePurgeStartupTest extends WikimediaWireMockTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private YoutubeProperties youtube;
  @Autowired private List<ScheduledTaskHolder> schedules;

  @TestConfiguration(proxyBeanMethods = false)
  static class Rows {
    @Bean
    SmartInitializingSingleton oldAndNewYoutubeRows(
        EventMediaSearchCacheRepository repository, Clock clock) {
      return () -> {
        Instant now = clock.instant();
        LocalDate day = LocalDate.of(1986, 9, 20);
        repository.upsert(
            "youtube",
            day,
            "kowiki-1986-09-20-0000000a",
            false,
            "{}",
            now.minus(Duration.ofDays(30)));
        repository.upsert("youtube", day, "kowiki-1986-09-20-0000000b", false, "{}", now);
      };
    }
  }

  @Test
  void theOldRowIsGoneAtStartupWithYoutubeOff() {
    assertThat(youtube.enabled()).isFalse();
    assertThat(jdbc.queryForList("SELECT event_id FROM event_media_search_cache", String.class))
        .containsExactly("kowiki-1986-09-20-0000000b");
  }

  @Test
  void itRunsAgainEveryHour() {
    List<IntervalTask> purges =
        schedules.stream()
            .flatMap(holder -> holder.getScheduledTasks().stream())
            .map(ScheduledTask::getTask)
            .filter(task -> task.toString().contains("SearchCachePurge.purge"))
            .map(IntervalTask.class::cast)
            .toList();

    assertThat(purges)
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofHours(1));
              assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofHours(1));
            });
  }
}
