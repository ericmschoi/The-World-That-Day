package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Plan M3a1c T5 (7): a whole day with the database really down (its container stopped, as in
 * CachingKoWikiClientDatabaseDownTest). Both pages come from Wikipedia, each page costs one failed
 * cache read and no write (T4), and the day returns inside {@code wikimedia.kowiki-day-deadline}.
 * The extra property gives this class its own context and container; DirtiesContext closes them.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-timeout=1000",
      "wikimedia.kowiki-day-deadline=5s"
    })
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventSourceDatabaseDownTest extends WikimediaWireMockTest {

  private static final LocalDate OPENING = LocalDate.of(1988, 9, 17);

  @Autowired private KoWikiEventSource source;
  @Autowired private PostgreSQLContainer postgres;

  private static long lines(CapturedOutput output, String text) {
    return Pattern.compile(Pattern.quote(text)).matcher(output.getAll()).results().count();
  }

  @Test
  void stoppedDatabaseStillGivesBothPagesInsideTheDeadline(CapturedOutput output) {
    KoWikiEventSourceContextTest.stubOpeningDay();
    postgres.stop();

    long start = System.nanoTime();
    KoWikiDayResult result = source.find(OPENING);
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::title, KoWikiDocStatus::state)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("September 17", State.FRESH),
            org.assertj.core.groups.Tuple.tuple("1988", State.FRESH));
    assertThat(result.events()).isNotEmpty();
    assertThat(result.events().getFirst().wikidataQid()).isEqualTo("Q8470");
    assertThat(result.degraded()).isFalse();
    assertThat(lines(output, "Wiki section cache read failed")).isEqualTo(2);
    assertThat(output.getAll())
        .contains("Wiki section cache read failed: title=September 17")
        .contains("Wiki section cache read failed: title=1988");
    assertThat(lines(output, "Wiki section cache write failed")).isZero();
  }
}
