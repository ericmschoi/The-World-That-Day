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
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a1 T2 (4) for every recorded demo page, not only {@code 9월 17일} and {@code 1988년}: one
 * section-list request plus one request per level-2 "사건"/"문화" section, pinned to the listed revid,
 * and nothing else. Any other request gets a 404 from a catch-all stub so it would show up as a
 * failed page.
 */
class KoWikiRecordedPagesClientTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";

  private static KoWikiClient client() {
    WikimediaProperties properties =
        new WikimediaProperties(
            "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)",
            Duration.ofSeconds(2),
            Duration.ofMillis(300),
            1,
            Duration.ofSeconds(120),
            3,
            URI.create(wikimedia.baseUrl() + PATH));
    MutableClock clock =
        new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
    return new ActionApiKoWikiClient(
        new WikimediaHttpClient(properties, clock), properties, KoWikiFixtures.JSON);
  }

  private static String param(ServeEvent event, String name) {
    var value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"9월 17일", "6월 22일", "3월 15일", "1988년", "2002년", "1995년"})
  void recordedPageIsReadWithExactlyItsEventsAndCultureSections(String title) {
    String prefix = KoWikiFixtureReadme.DEMO_PAGES.get(title);
    long revid = KoWikiFixtureReadme.revid(prefix + "-sections.json");
    String eventsIndex = KoWikiFixtureReadme.sectionIndex(prefix, "사건");
    String cultureIndex = KoWikiFixtureReadme.sectionIndex(prefix, "문화");
    assertThat(eventsIndex).as("recorded 사건 index").isNotNull();
    assertThat(cultureIndex).as("recorded 문화 index").isNotNull();

    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).atPriority(10).willReturn(aResponse().withStatus(404)));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .atPriority(1)
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("sections|revid"))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    for (String[] section :
        List.of(new String[] {eventsIndex, "events"}, new String[] {cultureIndex, "culture"})) {
      wikimedia.stubFor(
          get(urlPathEqualTo(PATH))
              .atPriority(1)
              .withQueryParam("oldid", equalTo(Long.toString(revid)))
              .withQueryParam("prop", equalTo("wikitext"))
              .withQueryParam("section", equalTo(section[0]))
              .willReturn(okJson(read(prefix + "-" + section[1] + ".json"))));
    }

    KoWikiPage page = client().fetch(title);

    assertThat(page.state()).isEqualTo(State.FOUND);
    assertThat(page.title()).isEqualTo(title);
    assertThat(page.revision()).isEqualTo(revid);
    assertThat(page.revision())
        .as("README revid")
        .isEqualTo(KoWikiFixtureReadme.rows().get(prefix + "-sections.json").revid());
    assertThat(page.eventsWikitext()).isEqualTo(wikitext(prefix + "-events.json"));
    assertThat(page.cultureWikitext()).isEqualTo(wikitext(prefix + "-culture.json"));

    List<ServeEvent> requests = wikimedia.getAllServeEvents().reversed();
    assertThat(requests).hasSize(3);
    assertThat(param(requests.getFirst(), "page")).isEqualTo(title);
    assertThat(param(requests.getFirst(), "section")).isNull();
    assertThat(requests.subList(1, 3))
        .extracting(event -> param(event, "section"))
        .containsExactly(eventsIndex, cultureIndex);
    assertThat(requests.subList(1, 3))
        .extracting(event -> param(event, "oldid"))
        .containsOnly(Long.toString(revid));
    assertThat(requests.subList(1, 3))
        .extracting(event -> param(event, "page"))
        .as("wikitext requests are pinned by oldid, not by title")
        .containsOnlyNulls();
    assertThat(requests)
        .allSatisfy(event -> assertThat(event.getResponse().getStatus()).isEqualTo(200));
  }
}
