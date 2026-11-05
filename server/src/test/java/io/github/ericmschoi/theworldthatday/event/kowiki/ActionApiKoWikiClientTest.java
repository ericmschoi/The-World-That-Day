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
import io.github.ericmschoi.theworldthatday.wikimedia.ActionApiErrorsTest;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1 T2 (4)~(5): requests per page, pinned revision, missing pages, failures. Plan M3a1b T1
 * (2): the API error {@code ratelimited} blocks the host. Plan C1 S3a T2 (2)~(4), (6): English
 * Wikipedia, {@code prop=tocdata|revid}, one level-2 {@code Events} section.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiKoWikiClientTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";

  /**
   * No test here is about timeouts, and several count requests: a timed-out first attempt would add
   * a retry. 2 seconds instead of 300 ms keeps a stalled machine from failing them (plan M3a1b T3).
   */
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final KoWikiClient client = client();

  private KoWikiClient client() {
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
    return new ActionApiKoWikiClient(
        new WikimediaHttpClient(properties, clock), properties, KoWikiFixtures.JSON, clock);
  }

  /** Stubs the section list of {@code title} and its Events section, from fixtures. */
  static void stubPage(String title, String prefix) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("tocdata|revid"))
            .willReturn(okJson(read(prefix + "-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("prop", equalTo("wikitext"))
            .willReturn(okJson(read(prefix + "-events.json"))));
  }

  private static List<ServeEvent> requests() {
    return wikimedia.getAllServeEvents().reversed(); // oldest first
  }

  private static String param(ServeEvent event, String name) {
    var value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  @Test
  void datePageReadsOnlyTheEventsSectionOfOneRevision() {
    stubPage("July 17", "07-17");

    KoWikiPage page = client.fetch("July 17");

    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.revision()).isEqualTo(1375524665L);
    assertThat(page.fetchedAt()).isEqualTo(clock.instant());
    assertThat(page.eventsWikitext()).isEqualTo(wikitext("07-17-events.json"));
    assertThat(page.cultureWikitext()).isNull();
    List<ServeEvent> requests = requests();
    assertThat(requests).hasSize(2);
    assertThat(param(requests.getFirst(), "page")).isEqualTo("July 17");
    assertThat(param(requests.getFirst(), "prop")).isEqualTo("tocdata|revid");
    assertThat(param(requests.getFirst(), "redirects")).isEqualTo("1");
    assertThat(param(requests.get(1), "section")).isEqualTo("1");
    assertThat(param(requests.get(1), "oldid")).isEqualTo("1375524665");
    assertThat(param(requests.get(1), "prop")).isEqualTo("wikitext");
    assertThat(requests).extracting(event -> param(event, "prop")).doesNotContain("sections|revid");
    assertThat(requests)
        .extracting(event -> event.getRequest().getUrl())
        .allSatisfy(url -> assertThat(url).doesNotContain("prop=sections"));
  }

  @Test
  void yearPageWithPronunciationFirstReadsEventsAtIndexTwo() {
    stubPage("2010", "2010");

    KoWikiPage page = client.fetch("2010");

    assertThat(page.revision()).isEqualTo(1382559665L);
    assertThat(param(requests().getFirst(), "page")).isEqualTo("2010");
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "2");
    assertThat(page.eventsWikitext()).isEqualTo(wikitext("2010-events.json"));
  }

  @Test
  void leapDayPageSkipsALevel3EventsHeadingAndPronunciation() {
    stubPage("February 29", "synthetic/synthetic-02-29");

    KoWikiPage page = client.fetch("February 29");

    assertThat(page.revision()).isEqualTo(100L);
    assertThat(page.eventsWikitext()).contains("Test event A");
    assertThat(page.cultureWikitext()).isNull();
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "3");
    assertThat(param(requests().get(1), "oldid")).isEqualTo("100");
  }

  @Test
  void pageWithoutALevel2EventsSectionIsOneRequestWithoutEvents() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(read("synthetic/synthetic-no-events-tocdata.json"))));

    KoWikiPage page = client.fetch("February 29");

    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.revision()).isEqualTo(102L);
    assertThat(page.eventsWikitext()).isNull();
    assertThat(page.cultureWikitext()).isNull();
    assertThat(requests()).hasSize(1);
  }

  @Test
  void missingTitleIsAbsentWithoutRetry() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    KoWikiPage page = client.fetch("February 29");

    assertThat(page).isEqualTo(new KoWikiPage("February 29", State.ABSENT, null, null, null, null));
    assertThat(requests()).hasSize(1);
  }

  @Test
  void failedEventsSectionRequestMakesThePageUnavailable() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(okJson(read("07-17-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiPage page = client.fetch("July 17");

    assertThat(page)
        .isEqualTo(new KoWikiPage("July 17", State.UNAVAILABLE, null, null, null, null));
    assertThat(requests()).hasSize(3); // list, Events + one retry
  }

  @Test
  void missingEventsSectionAnswerStopsThePage() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(okJson(read("07-17-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(aResponse().withStatus(404)));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "1");
  }

  @Test
  void rateLimitedSectionListMakesThePageUnavailable() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "5")));

    assertThat(client.fetch("1976").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).hasSize(1);
  }

  @Test
  void otherApiErrorsAndBrokenJsonAreUnavailable() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(okJson("{\"error\":{\"code\":\"internal_api_error\"}}")));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("1976"))
            .willReturn(okJson("{\"parse\":")));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(client.fetch("1976").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).hasSize(2);
  }

  @Test
  void ratelimitedErrorBlocksTheHostLikeA429WithoutRetry(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).as("HTTP 200 + ratelimited is not retried").hasSize(1);
    assertThat(output.getAll())
        .contains("Wikimedia call failed: source=enwiki title=July 17 cause=api error ratelimited");

    clock.advance(Duration.ofSeconds(29));
    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).as("host still blocked at 29s").hasSize(1);

    clock.advance(Duration.ofSeconds(2));
    wikimedia.resetAll();
    stubPage("July 17", "07-17");
    assertThat(client.fetch("July 17").state()).isEqualTo(State.FRESH);
    assertThat(requests()).as("sent again at 31s").hasSize(2);
  }

  @Test
  void ratelimitedSectionRequestStopsThePageAndBlocksOtherPagesOfTheHost() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(okJson(read("07-17-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(client.fetch("1976").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "1");
  }

  @ParameterizedTest(name = "code [{0}] is not logged")
  @MethodSource("io.github.ericmschoi.theworldthatday.wikimedia.ActionApiErrorsTest#unsafeCodes")
  void unsafeErrorCodeNeverShapesTheLogLine(String code, CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(ActionApiErrorsTest.errorWithCode(code).toString())));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(output.getAll()).contains("title=July 17 cause=api error\n");
    if (!code.isEmpty()) {
      assertThat(output.getAll()).doesNotContain(code);
    }
    assertThat(requests()).hasSize(1);
  }

  @Test
  void errorObjectWithoutACodeIsAFailureEvenWithARevision(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"error\":{\"info\":\"x\"},\"parse\":{\"revid\":5}}")));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(output.getAll()).contains("title=July 17 cause=api error\n");
    assertThat(requests()).hasSize(1);
  }

  /** Plan C1 S3a T2 (6): a failure names the source and the page, never a date or wikitext. */
  @Test
  void failureLogNamesEnwikiAndThePageOnly(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(okJson(read("07-17-tocdata.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(okJson("{\"parse\":{\"title\":\"July 17\",\"revid\":1375524665}}")));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.UNAVAILABLE);
    String log = output.getAll();
    assertThat(log)
        .contains("Wikimedia call failed: source=enwiki title=July 17 cause=no wikitext");
    assertThat(log).doesNotContain("source=kowiki").doesNotContain("1976-07-17");
  }

  @Test
  void missingTitleLogsNoFailure(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    assertThat(client.fetch("July 17").state()).isEqualTo(State.ABSENT);
    assertThat(output.getAll()).doesNotContain("Wikimedia call failed");
  }
}
