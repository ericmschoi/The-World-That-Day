package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The client bean is the wikitext cache wrapped around the Action API client, wired to the shared
 * Wikimedia client and the Boot JSON mapper.
 */
class KoWikiClientContextTest extends WikimediaWireMockTest {

  @Autowired private KoWikiClient client;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
  }

  @Test
  void clientBeanReadsTheRecordedYearPage() {
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

    KoWikiPage page = client.fetch("1988년");

    assertThat(client).isInstanceOf(CachingKoWikiClient.class);
    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.revision()).isEqualTo(42640701L);
    assertThat(requestCount()).isEqualTo(3);
  }
}
