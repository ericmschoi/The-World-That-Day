package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a4c T1 (1) to (3): the V3 table {@code event_media_search_cache} and its repository, on
 * the Testcontainers database. Dates are demo inputs and public event dates or boundaries; event
 * IDs and results are synthetic.
 */
class EventMediaSearchCacheRepositoryTest extends WikimediaWireMockTest {

  private static final String COMMONS = "commons";
  private static final LocalDate DEMO = LocalDate.of(1988, 9, 17);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final String EVENT = "kowiki-1988-09-17-0a1b2c3d";
  private static final String OTHER_EVENT = "kowiki-1988-09-17-4e5f6a7b";
  private static final Instant FETCHED = Instant.parse("2026-10-24T01:02:03.123456Z");

  @Autowired private EventMediaSearchCacheRepository repository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Environment environment;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE event_media_search_cache");
  }

  @Test
  void v3IsAppliedAndTheContextValidatesTheEntity() {
    Integer applied =
        jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success",
            Integer.class);

    assertThat(applied).isEqualTo(1);
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(repository.count()).isZero();
  }

  @Test
  void tableHoldsOnlyTheCacheKeyResultAndTimeAndNothingAboutTheRequester() {
    List<Map<String, Object>> columns =
        jdbc.queryForList(
            "SELECT column_name, data_type, is_nullable FROM information_schema.columns"
                + " WHERE table_name = 'event_media_search_cache' ORDER BY ordinal_position");

    assertThat(columns)
        .extracting(column -> column.get("column_name"))
        .containsExactly(
            "id", "source", "event_date", "event_id", "day_paths_off", "result", "fetched_at");
    assertThat(columns)
        .extracting(column -> column.get("data_type"))
        .containsExactly(
            "bigint",
            "character varying",
            "date",
            "character varying",
            "boolean",
            "text",
            "timestamp with time zone");
    assertThat(columns).allSatisfy(column -> assertThat(column).containsEntry("is_nullable", "NO"));
  }

  @Test
  void uniqueKeyIsSourceDateEventAndDayPaths() {
    List<String> key =
        jdbc.queryForList(
            "SELECT k.column_name FROM information_schema.table_constraints c"
                + " JOIN information_schema.key_column_usage k"
                + " ON k.constraint_name = c.constraint_name AND k.table_name = c.table_name"
                + " WHERE c.table_name = 'event_media_search_cache' AND c.constraint_type = 'UNIQUE'"
                + " ORDER BY k.ordinal_position",
            String.class);

    assertThat(key).containsExactly("source", "event_date", "event_id", "day_paths_off");
  }

  @Test
  void upsertStoresEveryFieldAndTheEntityReadsThemBack() {
    String result = "{\"version\":1,\"englishLabel\":\"서울 올림픽\",\"candidates\":[]}";

    repository.upsert(COMMONS, DEMO, EVENT, false, result, FETCHED);

    assertThat(repository.find(COMMONS, DEMO, false, List.of(EVENT)))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.source()).isEqualTo(COMMONS);
              assertThat(row.eventDate()).isEqualTo(DEMO);
              assertThat(row.eventId()).isEqualTo(EVENT);
              assertThat(row.dayPathsOff()).isFalse();
              assertThat(row.result()).isEqualTo(result);
              assertThat(row.fetchedAt()).isEqualTo(FETCHED);
            });
  }

  @Test
  void upsertOfTheSameKeyReplacesTheResultAndTheTimeAndKeepsOneRow() {
    repository.upsert(COMMONS, DEMO, EVENT, false, "{\"version\":1}", FETCHED);
    repository.upsert(COMMONS, DEMO, EVENT, false, "{\"version\":2}", FETCHED.plusSeconds(60));

    assertThat(rowCount()).isEqualTo(1);
    assertThat(repository.find(COMMONS, DEMO, false, List.of(EVENT)))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.result()).isEqualTo("{\"version\":2}");
              assertThat(row.fetchedAt()).isEqualTo(FETCHED.plusSeconds(60));
            });
  }

  @Test
  void eachKeyPartMakesAnotherRow() {
    repository.upsert(COMMONS, DEMO, EVENT, false, "a", FETCHED);
    repository.upsert(COMMONS, DEMO, EVENT, true, "b", FETCHED);
    repository.upsert(COMMONS, DEMO, OTHER_EVENT, false, "c", FETCHED);
    repository.upsert(COMMONS, DEMO.plusDays(1), EVENT, false, "d", FETCHED);
    repository.upsert("example", DEMO, EVENT, false, "e", FETCHED);

    assertThat(rowCount()).isEqualTo(5);
  }

  @Test
  void findReturnsOnlyTheAskedSourceDateDayPathsAndEvents() {
    repository.upsert(COMMONS, DEMO, EVENT, false, "asked", FETCHED);
    repository.upsert(COMMONS, DEMO, OTHER_EVENT, false, "asked too", FETCHED);
    repository.upsert(COMMONS, DEMO, "kowiki-1988-09-17-8c9d0e1f", false, "not asked", FETCHED);
    repository.upsert(COMMONS, DEMO, EVENT, true, "day paths off", FETCHED);
    repository.upsert(COMMONS, DEMO.minusDays(1), EVENT, false, "day before", FETCHED);
    repository.upsert("example", DEMO, EVENT, false, "other source", FETCHED);

    assertThat(repository.find(COMMONS, DEMO, false, List.of(EVENT, OTHER_EVENT)))
        .extracting(EventMediaSearchCache::result)
        .containsExactlyInAnyOrder("asked", "asked too");
    assertThat(repository.find(COMMONS, DEMO, true, List.of(EVENT, OTHER_EVENT)))
        .extracting(EventMediaSearchCache::result)
        .containsExactly("day paths off");
  }

  @Test
  void findWithoutEventsOrWithoutRowsIsEmpty() {
    repository.upsert(COMMONS, DEMO, EVENT, false, "stored", FETCHED);

    assertThat(repository.find(COMMONS, DEMO, false, List.of())).isEmpty();
    assertThat(repository.find(COMMONS, DEMO, false, List.of(OTHER_EVENT))).isEmpty();
    assertThat(repository.find(COMMONS, LEAP_DAY, false, List.of(EVENT))).isEmpty();
  }

  @Test
  void leapDayAndTheDaysAroundItAreSeparateDates() {
    String leapEvent = "kowiki-2000-02-29-0a1b2c3d";
    repository.upsert(COMMONS, LEAP_DAY.minusDays(1), leapEvent, false, "02-28", FETCHED);
    repository.upsert(COMMONS, LEAP_DAY, leapEvent, false, "02-29", FETCHED);
    repository.upsert(COMMONS, LEAP_DAY.plusDays(1), leapEvent, false, "03-01", FETCHED);
    repository.upsert(COMMONS, LEAP_DAY.minusYears(1), leapEvent, false, "1999-02-28", FETCHED);

    assertThat(repository.find(COMMONS, LEAP_DAY, false, List.of(leapEvent)))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.eventDate()).isEqualTo(LocalDate.of(2000, 2, 29));
              assertThat(row.result()).isEqualTo("02-29");
            });
  }

  @Test
  void aLargeResultWithKoreanTextIsStoredWhole() {
    String result = "{\"description\":\"" + "아폴로 11호 Apollo 11 ".repeat(4_000) + "\"}";

    repository.upsert(COMMONS, LocalDate.of(1969, 7, 20), EVENT, false, result, FETCHED);

    assertThat(repository.find(COMMONS, LocalDate.of(1969, 7, 20), false, List.of(EVENT)))
        .singleElement()
        .extracting(EventMediaSearchCache::result)
        .isEqualTo(result);
  }

  @Test
  void racingWritesOfOneKeyNeverFailAndKeepOneRow() throws Exception {
    int writers = 8;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(writers);
    try {
      List<Future<?>> writes = new ArrayList<>();
      for (int i = 1; i <= writers; i++) {
        int writer = i;
        writes.add(
            pool.submit(
                () -> {
                  start.await();
                  repository.upsert(
                      COMMONS, DEMO, EVENT, false, "w" + writer, FETCHED.plusSeconds(writer));
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> write : writes) {
        write.get(30, TimeUnit.SECONDS); // throws if any write failed
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(rowCount()).isEqualTo(1);
    EventMediaSearchCache row = repository.find(COMMONS, DEMO, false, List.of(EVENT)).getFirst();
    int writer = Integer.parseInt(row.result().substring(1));
    assertThat(writer).isBetween(1, writers);
    assertThat(row.fetchedAt()).isEqualTo(FETCHED.plusSeconds(writer));
  }

  private long rowCount() {
    return jdbc.queryForObject("SELECT count(*) FROM event_media_search_cache", Long.class);
  }
}
