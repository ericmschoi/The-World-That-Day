package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a1 T2 (4) and plan C1 S3a T2 (1)~(3) for every recorded English demo page: one
 * section-list request plus one request for the level-2 "Events" section, pinned to the listed
 * revid, and nothing else. Any other request gets a 404 from a catch-all stub so it would show up
 * as a failed page.
 */
class KoWikiRecordedPagesClientTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";

  /**
   * No test here is about timeouts. A short read timeout let a stalled machine time out every
   * request of a run (plan M3a1b T3 flaky-test check); 2 seconds keeps a timeout from ever being
   * mistaken for a page failure.
   */
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

  private static KoWikiClient client() {
    WikimediaProperties properties =
        new WikimediaProperties(
            "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)",
            Duration.ofSeconds(2),
            READ_TIMEOUT,
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(wikimedia.baseUrl() + PATH));
    MutableClock clock =
        new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
    return new ActionApiKoWikiClient(
        new WikimediaHttpClient(properties, clock), properties, KoWikiFixtures.JSON, clock);
  }

  private static String param(ServeEvent event, String name) {
    var value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "February 13, 02-13, 1370674264, 1",
    "July 17, 07-17, 1375524665, 1",
    "February 12, 02-12, 1370674257, 1",
    "April 12, 04-12, 1378081960, 1",
    "July 20, 07-20, 1373354079, 1",
    "1988, 1988, 1382686554, 1",
    "1976, 1976, 1382023488, 1",
    "2010, 2010, 1382559665, 2",
    "1980, 1980, 1382118549, 1",
    "1969, 1969, 1380604178, 1"
  })
  void recordedPageIsReadWithExactlyItsEventsSection(
      String title, String prefix, long revid, String eventsIndex) {
    assertThat(EnWikiPages.RECORDED).containsEntry(title, prefix);
    assertThat(EnWikiPages.revid(prefix)).as("recorded revid").isEqualTo(revid);
    assertThat(EnWikiPages.eventsIndex(prefix)).as("recorded Events index").isEqualTo(eventsIndex);

    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).atPriority(10).willReturn(aResponse().withStatus(404)));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .atPriority(1)
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("tocdata|revid"))
            .willReturn(okJson(read(prefix + "-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .atPriority(1)
            .withQueryParam("oldid", equalTo(Long.toString(revid)))
            .withQueryParam("prop", equalTo("wikitext"))
            .withQueryParam("section", equalTo(eventsIndex))
            .willReturn(okJson(read(prefix + "-events.json"))));

    KoWikiPage page = client().fetch(title);

    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.title()).isEqualTo(title);
    assertThat(page.revision()).isEqualTo(revid);
    assertThat(page.eventsWikitext()).isEqualTo(wikitext(prefix + "-events.json"));
    assertThat(page.cultureWikitext()).as("English Wikipedia has no culture section").isNull();

    List<ServeEvent> requests = wikimedia.getAllServeEvents().reversed();
    assertThat(requests).hasSize(2);
    assertThat(param(requests.getFirst(), "page")).isEqualTo(title);
    assertThat(param(requests.getFirst(), "section")).isNull();
    assertThat(param(requests.get(1), "section")).isEqualTo(eventsIndex);
    assertThat(param(requests.get(1), "oldid")).isEqualTo(Long.toString(revid));
    assertThat(param(requests.get(1), "page"))
        .as("wikitext requests are pinned by oldid, not by title")
        .isNull();
    assertThat(requests)
        .allSatisfy(event -> assertThat(event.getResponse().getStatus()).isEqualTo(200));
  }
}
