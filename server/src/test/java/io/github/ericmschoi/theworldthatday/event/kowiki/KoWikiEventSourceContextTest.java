package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Spring wiring of plan M3a1c: {@link KoWikiEventSource} reads pages through the wikitext
 * cache, so a second call needs no page request; the link lookup is sent every time.
 */
class KoWikiEventSourceContextTest extends WikimediaWireMockTest {

  @Autowired private KoWikiEventSource source;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
  }

  private static void stubRecorded(String title) {
    String prefix = KoWikiFixtureReadme.DEMO_PAGES.get(title);
    long revid = KoWikiFixtureReadme.revid(prefix + "-sections.json");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    for (String[] kind : List.of(new String[] {"사건", "events"}, new String[] {"문화", "culture"})) {
      wikimedia.stubFor(
          get(urlPathEqualTo(KOWIKI_PATH))
              .withQueryParam("oldid", equalTo(Long.toString(revid)))
              .withQueryParam("section", equalTo(KoWikiFixtureReadme.sectionIndex(prefix, kind[0])))
              .willReturn(okJson(read(prefix + "-" + kind[1] + ".json"))));
    }
  }

  @Test
  void secondCallReadsBothPagesFromTheCache() {
    stubRecorded("9월 17일");
    stubRecorded("1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("pageprops-1988-09-17.json"))));
    LocalDate opening = LocalDate.of(1988, 9, 17);

    KoWikiDayResult first = source.find(opening);
    int afterFirst = requestCount();
    KoWikiDayResult second = source.find(opening);

    assertThat(source).isInstanceOf(TwoPageKoWikiEventSource.class);
    assertThat(first.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(afterFirst).as("3 per page + 1 link lookup").isEqualTo(7);
    assertThat(second.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.CACHED);
    assertThat(requestCount() - afterFirst).as("only the link lookup").isEqualTo(1);
    assertThat(second.events()).isEqualTo(first.events());
    assertThat(second.events().getFirst().wikidataQid()).isEqualTo("Q8470");
    assertThat(second.degraded()).isFalse();
  }
}
