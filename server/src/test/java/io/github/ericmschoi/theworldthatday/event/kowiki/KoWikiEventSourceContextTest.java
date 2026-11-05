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

  /**
   * The synthetic opening day 1988-09-17 (a public event date, M3-P13): {@code September 17} and
   * {@code 1988} from {@code enwiki/synthetic/} and their link lookup.
   */
  static void stubOpeningDay() {
    KoWikiSourceFixture.stubPage(wikimedia, "September 17", "synthetic/synthetic-09-17");
    KoWikiSourceFixture.stubPage(wikimedia, "1988", "synthetic/synthetic-1988");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("synthetic/synthetic-pageprops-1988-09-17.json"))));
  }

  @Test
  void secondCallReadsBothPagesFromTheCache() {
    stubOpeningDay();
    LocalDate opening = LocalDate.of(1988, 9, 17);

    KoWikiDayResult first = source.find(opening);
    int afterFirst = requestCount();
    KoWikiDayResult second = source.find(opening);

    assertThat(source).isInstanceOf(TwoPageKoWikiEventSource.class);
    assertThat(first.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(afterFirst).as("2 per page + 1 link lookup").isEqualTo(5);
    assertThat(second.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.CACHED);
    assertThat(requestCount() - afterFirst).as("only the link lookup").isEqualTo(1);
    assertThat(second.events()).isEqualTo(first.events());
    assertThat(second.events().getFirst().wikidataQid()).isEqualTo("Q8470");
    assertThat(second.degraded()).isFalse();
  }
}
