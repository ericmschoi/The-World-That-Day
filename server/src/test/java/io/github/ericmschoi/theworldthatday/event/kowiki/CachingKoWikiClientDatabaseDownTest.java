package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Review M3a1b required 1: with the database really down (container stopped), the cache path fails
 * with transaction errors, yet {@code fetch} still returns the Wikipedia result and never throws.
 * The extra property gives this class its own context and container (as in
 * HealthEndpointDatabaseDownTest); DirtiesContext closes them afterwards.
 */
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=1000")
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class CachingKoWikiClientDatabaseDownTest extends WikimediaWireMockTest {

  @Autowired private KoWikiClient client;
  @Autowired private PostgreSQLContainer postgres;

  private static void stubYearPage() {
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo("1988년"))
            .willReturn(okJson(read("1988-sections.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(okJson(read("1988-events.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("section", equalTo("2"))
            .willReturn(okJson(read("1988-culture.json"))));
  }

  @Test
  void stoppedDatabaseOnlyCostsTheCache(CapturedOutput output) {
    postgres.stop();

    stubYearPage();
    KoWikiPage fresh = client.fetch("1988년");

    assertThat(fresh.state()).isEqualTo(State.FRESH);
    assertThat(fresh.revision()).isEqualTo(42640701L);
    assertThat(fresh.eventsWikitext()).isEqualTo(wikitext("1988-events.json"));
    assertThat(fresh.fetchedAt()).isEqualTo(clock.instant());
    assertThat(output.getAll())
        .contains("Wiki section cache read failed: title=1988년")
        .contains("Wiki section cache write failed: title=1988년");

    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(404)));
    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);

    wikimedia.resetAll();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));
    assertThat(client.fetch("2월 30일").state()).isEqualTo(State.ABSENT);
  }
}
