package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a1c T1b: with no database connection to be had (every pooled connection held, the same
 * wait as an unreachable or hung database), each cache call fails after the production {@code
 * spring.datasource.hikari.connection-timeout}, and the day still returns both pages read from
 * Wikipedia inside {@code wikimedia.kowiki-day-deadline}: only the cache is lost, not the section.
 */
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventSourceDatabaseExhaustedTest extends WikimediaWireMockTest {

  private static final LocalDate OPENING = LocalDate.of(1988, 9, 17);

  @Autowired private KoWikiEventSource source;
  @Autowired private KoWikiClient client;
  @Autowired private HikariDataSource dataSource;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
  }

  /** Takes every connection of the pool; the caller closes them. */
  private List<Connection> exhaustPool() throws SQLException {
    List<Connection> held = new ArrayList<>();
    for (int i = 0; i < dataSource.getMaximumPoolSize(); i++) {
      held.add(dataSource.getConnection());
    }
    return held;
  }

  private static void release(List<Connection> held) throws SQLException {
    for (Connection connection : held) {
      connection.close();
    }
  }

  @Test
  void poolUsesTheProductionTimeouts() {
    assertThat(dataSource.getConnectionTimeout()).isEqualTo(1000);
    assertThat(dataSource.getValidationTimeout())
        .isEqualTo(500)
        .isLessThan(dataSource.getConnectionTimeout());
    assertThat(Duration.ofMillis(dataSource.getConnectionTimeout()).multipliedBy(2))
        .as(
            "one read per page (the write is skipped after a failed read), two pages, inside the"
                + " day deadline")
        .isLessThan(properties.kowikiDayDeadline());
  }

  @Test
  void cacheReadWithoutAConnectionFailsAfterTheConnectionTimeout() throws SQLException {
    List<Connection> held = exhaustPool();
    try {
      long start = System.nanoTime();
      KoWikiPage page = client.cached("9월 17일");
      Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

      assertThat(page).isEqualTo(KoWikiPage.unavailable("9월 17일"));
      assertThat(elapsed)
          .isGreaterThanOrEqualTo(Duration.ofMillis(900))
          .isLessThan(Duration.ofMillis(1500));
    } finally {
      release(held);
    }
  }

  @Test
  void dayStillReturnsBothWikipediaPagesInsideTheDeadline(CapturedOutput output)
      throws SQLException {
    KoWikiEventSourceContextTest.stubRecorded("9월 17일");
    KoWikiEventSourceContextTest.stubRecorded("1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("pageprops-1988-09-17.json"))));

    List<Connection> held = exhaustPool();
    KoWikiDayResult result;
    Duration elapsed;
    try {
      long start = System.nanoTime();
      result = source.find(OPENING);
      elapsed = Duration.ofNanos(System.nanoTime() - start);
    } finally {
      release(held);
    }

    assertThat(elapsed).isLessThan(properties.kowikiDayDeadline());
    assertThat(elapsed)
        .as("T4: one connection wait per page (2 x 1 s), the write is skipped")
        .isLessThan(Duration.ofMillis(3500));
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(result.events()).isNotEmpty();
    assertThat(result.events().getFirst().wikidataQid()).isEqualTo("Q8470");
    assertThat(result.degraded()).isFalse();
    assertThat(output.getAll())
        .contains("Wiki section cache read failed: title=9월 17일")
        .contains("Wiki section cache read failed: title=1988년")
        .as("T4: no write after a failed read")
        .doesNotContain("Wiki section cache write failed");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM wiki_section_cache", Long.class))
        .as("nothing could be stored")
        .isZero();
  }
}
