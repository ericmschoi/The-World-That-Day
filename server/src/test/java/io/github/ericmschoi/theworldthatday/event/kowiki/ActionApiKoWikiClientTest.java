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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1 T2 (4)~(5): requests per page, pinned revision, missing pages, failures. Plan M3a1b T1
 * (2): the API error {@code ratelimited} blocks the host.
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

  /** Stubs the section list of {@code title} and the given sections, all from fixtures. */
  static void stubPage(String title, String prefix, String... sections) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("sections|revid"))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    for (String section : sections) {
      String[] indexAndName = section.split(":");
      wikimedia.stubFor(
          get(urlPathEqualTo(PATH))
              .withQueryParam("prop", equalTo("wikitext"))
              .withQueryParam("section", equalTo(indexAndName[0]))
              .willReturn(okJson(read(prefix + "-" + indexAndName[1] + ".json"))));
    }
  }

  private static List<ServeEvent> requests() {
    return wikimedia.getAllServeEvents().reversed(); // oldest first
  }

  private static String param(ServeEvent event, String name) {
    var value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  @Test
  void datePageReadsOnlyTheEventsAndCultureSectionsOfOneRevision() {
    stubPage("9월 17일", "09-17", "1:events", "2:culture");

    KoWikiPage page = client.fetch("9월 17일");

    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.revision()).isEqualTo(42604413L);
    assertThat(page.fetchedAt()).isEqualTo(clock.instant());
    assertThat(page.eventsWikitext()).isEqualTo(wikitext("09-17-events.json"));
    assertThat(page.cultureWikitext()).isEqualTo(wikitext("09-17-culture.json"));
    List<ServeEvent> requests = requests();
    assertThat(requests).hasSize(3);
    assertThat(param(requests.getFirst(), "page")).isEqualTo("9월 17일");
    assertThat(param(requests.getFirst(), "redirects")).isEqualTo("1");
    assertThat(requests.subList(1, 3))
        .extracting(event -> param(event, "section"))
        .containsExactly("1", "2");
    assertThat(requests.subList(1, 3))
        .extracting(event -> param(event, "oldid"))
        .containsOnly("42604413");
  }

  @Test
  void yearPageNeverRequestsBirthOrDeathSections() {
    stubPage("1988년", "1988", "1:events", "2:culture");

    KoWikiPage page = client.fetch("1988년");

    assertThat(page.revision()).isEqualTo(42640701L);
    assertThat(param(requests().getFirst(), "page")).isEqualTo("1988년");
    assertThat(requests())
        .extracting(event -> param(event, "section"))
        .containsOnly(null, "1", "2");
    assertThat(requests()).hasSize(3);
  }

  @Test
  void leapDayPageUsesTheSectionIndexesFromTheList() {
    stubPage("2월 29일", "synthetic/synthetic-02-29", "2:events", "3:culture");

    KoWikiPage page = client.fetch("2월 29일");

    assertThat(page.revision()).isEqualTo(100L);
    assertThat(page.eventsWikitext()).contains("시험 사건 가");
    assertThat(page.cultureWikitext()).contains("시험 사건 나");
    assertThat(requests())
        .extracting(event -> param(event, "section"))
        .containsExactly(null, "2", "3");
    assertThat(requests().subList(1, 3))
        .extracting(event -> param(event, "oldid"))
        .containsOnly("100");
  }

  @Test
  void missingSectionIsNotRequestedAndNestedCultureIsIgnored() {
    stubPage("2000년", "synthetic/synthetic-2000", "1:events");

    KoWikiPage page = client.fetch("2000년");

    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.eventsWikitext()).contains("시험 사건 다");
    assertThat(page.cultureWikitext()).isNull();
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "1");
  }

  @Test
  void missingTitleIsAbsentWithoutRetry() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    KoWikiPage page = client.fetch("2월 29일");

    assertThat(page).isEqualTo(new KoWikiPage("2월 29일", State.ABSENT, null, null, null, null));
    assertThat(requests()).hasSize(1);
  }

  @Test
  void failedSectionRequestMakesThePageUnavailable() {
    stubPage("9월 17일", "09-17", "1:events");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("2"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiPage page = client.fetch("9월 17일");

    assertThat(page).isEqualTo(new KoWikiPage("9월 17일", State.UNAVAILABLE, null, null, null, null));
    assertThat(requests()).hasSize(4); // list, events, culture + one retry
  }

  @Test
  void failedEventsSectionStopsBeforeTheCultureSection() {
    stubPage("9월 17일", "09-17", "2:culture");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(aResponse().withStatus(404)));

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "1");
  }

  @Test
  void rateLimitedSectionListMakesThePageUnavailable() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "5")));

    assertThat(client.fetch("1988년").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).hasSize(1);
  }

  @Test
  void otherApiErrorsAndBrokenJsonAreUnavailable() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("9월 17일"))
            .willReturn(okJson("{\"error\":{\"code\":\"internal_api_error\"}}")));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("1988년"))
            .willReturn(okJson("{\"parse\":")));

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(client.fetch("1988년").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).hasSize(2);
  }

  @Test
  void ratelimitedErrorBlocksTheHostLikeA429WithoutRetry(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).as("HTTP 200 + ratelimited is not retried").hasSize(1);
    assertThat(output.getAll()).contains("title=9월 17일 cause=api error ratelimited");

    clock.advance(Duration.ofSeconds(29));
    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).as("host still blocked at 29s").hasSize(1);

    clock.advance(Duration.ofSeconds(2));
    wikimedia.resetAll();
    stubPage("9월 17일", "09-17", "1:events", "2:culture");
    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.FRESH);
    assertThat(requests()).as("sent again at 31s").hasSize(3);
  }

  @Test
  void ratelimitedSectionRequestStopsThePageAndBlocksOtherPagesOfTheHost() {
    stubPage("9월 17일", "09-17", "2:culture");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("section", equalTo("1"))
            .willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(client.fetch("1988년").state()).isEqualTo(State.UNAVAILABLE);
    assertThat(requests()).extracting(event -> param(event, "section")).containsExactly(null, "1");
  }
}
