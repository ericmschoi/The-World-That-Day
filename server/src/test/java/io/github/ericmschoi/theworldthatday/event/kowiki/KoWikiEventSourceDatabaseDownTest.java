package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
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
    KoWikiEventSourceContextTest.stubRecorded("9월 17일");
    KoWikiEventSourceContextTest.stubRecorded("1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("pageprops-1988-09-17.json"))));
    postgres.stop();

    long start = System.nanoTime();
    KoWikiDayResult result = source.find(OPENING);
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::title, KoWikiDocStatus::state)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("9월 17일", State.FRESH),
            org.assertj.core.groups.Tuple.tuple("1988년", State.FRESH));
    assertThat(result.events()).isNotEmpty();
    assertThat(result.events().getFirst().wikidataQid()).isEqualTo("Q8470");
    assertThat(result.degraded()).isFalse();
    assertThat(lines(output, "Wiki section cache read failed")).isEqualTo(2);
    assertThat(output.getAll())
        .contains("Wiki section cache read failed: title=9월 17일")
        .contains("Wiki section cache read failed: title=1988년");
    assertThat(lines(output, "Wiki section cache write failed")).isZero();
  }
}
