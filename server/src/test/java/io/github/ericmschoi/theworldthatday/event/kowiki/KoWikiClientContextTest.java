package io.github.ericmschoi.theworldthatday.event.kowiki;

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
    KoWikiSourceFixture.stubRecorded(wikimedia, "1988");

    KoWikiPage page = client.fetch("1988");

    assertThat(client).isInstanceOf(CachingKoWikiClient.class);
    assertThat(page.state()).isEqualTo(State.FRESH);
    assertThat(page.revision()).isEqualTo(1382686554L);
    assertThat(requestCount()).as("section list + Events section").isEqualTo(2);
    assertThat(page.eventsWikitext()).isEqualTo(KoWikiFixtures.wikitext("1988-events.json"));
    assertThat(page.cultureWikitext()).isNull();
  }
}
