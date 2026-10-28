package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

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

  /**
   * The task whose runnable calls {@code SearchCachePurge.purge()}. Spring wraps the {@link
   * ScheduledMethodRunnable} in a package-private outcome tracker, so it is read from the wrapper's
   * {@code runnable} field. If Spring changes that, no task is found and the test fails (never
   * passes silently).
   */
  private static boolean isThePurge(Task task) {
    return scheduledMethod(task.getRunnable())
        .filter(
            runnable ->
                runnable.getTarget() instanceof SearchCachePurge
                    && runnable.getMethod().getName().equals("purge"))
        .isPresent();
  }

  private static Optional<ScheduledMethodRunnable> scheduledMethod(Runnable runnable) {
    if (runnable instanceof ScheduledMethodRunnable method) {
      return Optional.of(method);
    }
    try {
      Field inner = runnable.getClass().getDeclaredField("runnable");
      inner.setAccessible(true);
      Object wrapped = inner.get(runnable);
      return wrapped instanceof ScheduledMethodRunnable method
          ? Optional.of(method)
          : Optional.empty();
    } catch (ReflectiveOperationException e) {
      return Optional.empty();
    }
  }

  @Test
  void itRunsAgainEveryHour() {
    List<IntervalTask> purges =
        schedules.stream()
            .flatMap(holder -> holder.getScheduledTasks().stream())
            .map(ScheduledTask::getTask)
            .filter(SearchCachePurgeStartupTest::isThePurge)
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
