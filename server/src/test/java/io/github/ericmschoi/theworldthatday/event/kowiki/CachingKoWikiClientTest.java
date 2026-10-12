package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.DATE;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a1b T2 (2)~(9): the wikitext cache in front of the Action API client, against WireMock and
 * the Testcontainers database (V2 table {@code wiki_section_cache}). Time moves with the test
 * clock; the TTL is {@code wikimedia.cache-ttl} from application.yml (7 days).
 */
class CachingKoWikiClientTest extends WikimediaWireMockTest {

  private static final String SITE = "localhost"; // host of the WireMock kowiki-api-url
  private static final String LEAP_PREFIX = "synthetic/synthetic-02-29";
  private static final String NEXT_PREFIX = "synthetic/synthetic-02-29-next";

  @Autowired private KoWikiClient client;
  @Autowired private WikiSectionCacheRepository repository;
  @Autowired private WikitextEventParser parser;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
  }

  /** Stubs a page from fixtures: section list by title, sections by revid and index. */
  private static void stubPage(String title, String prefix) {
    long revid = KoWikiFixtureReadme.revid(prefix + "-sections.json");
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("sections|revid"))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    for (String[] kind : List.of(new String[] {"사건", "events"}, new String[] {"문화", "culture"})) {
      String index = KoWikiFixtureReadme.sectionIndex(prefix, kind[0]);
      if (index != null) {
        wikimedia.stubFor(
            get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(Long.toString(revid)))
                .withQueryParam("section", equalTo(index))
                .willReturn(okJson(read(prefix + "-" + kind[1] + ".json"))));
      }
    }
  }

  private static void stubRecorded(String title) {
    stubPage(title, KoWikiFixtureReadme.DEMO_PAGES.get(title));
  }

  private List<Map<String, Object>> rows(String title) {
    return jdbc.queryForList(
        "SELECT * FROM wiki_section_cache WHERE site = ? AND page_title = ?", SITE, title);
  }

  private static Instant fetchedAt(Map<String, Object> row) {
    Object value = row.get("fetched_at");
    return value instanceof OffsetDateTime time
        ? time.toInstant()
        : ((java.sql.Timestamp) value).toInstant();
  }

  private void expireCache() {
    clock.advance(properties.cacheTtl().plusSeconds(1));
  }

  @Test
  void firstReadIsFreshAndStoredThenCachedWithoutRequests() {
    stubRecorded("9월 17일");

    KoWikiPage first = client.fetch("9월 17일");

    assertThat(first.state()).isEqualTo(State.FRESH);
    assertThat(first.revision()).isEqualTo(42604413L);
    assertThat(requestCount()).isEqualTo(3);
    assertThat(rows("9월 17일"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("revid", 42604413L);
              assertThat(row).containsEntry("events_wikitext", wikitext("09-17-events.json"));
              assertThat(row).containsEntry("culture_wikitext", wikitext("09-17-culture.json"));
              assertThat(fetchedAt(row)).isEqualTo(first.fetchedAt()).isEqualTo(clock.instant());
            });

    clock.advance(properties.cacheTtl().minusSeconds(1));
    KoWikiPage second = client.fetch("9월 17일");

    assertThat(second.state()).isEqualTo(State.CACHED);
    assertThat(requestCount()).as("no request inside the TTL").isEqualTo(3);
    assertThat(second).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
  }

  @Test
  void expiredCacheIsReadAgainAndAFailureServesTheOldCopyAsStale() {
    stubRecorded("9월 17일");
    KoWikiPage first = client.fetch("9월 17일");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    KoWikiPage stale = client.fetch("9월 17일");

    assertThat(requestCount()).as("asked again: first try + one retry").isEqualTo(2);
    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
    assertThat(fetchedAt(rows("9월 17일").getFirst())).isEqualTo(first.fetchedAt());
  }

  @Test
  void ratelimitedAfterTheTtlAlsoServesTheOldCopyAsStale() {
    stubRecorded("1988년");
    KoWikiPage first = client.fetch("1988년");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    KoWikiPage stale = client.fetch("1988년");

    assertThat(requestCount()).isEqualTo(1);
    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale.revision()).isEqualTo(first.revision());
    assertThat(stale.eventsWikitext()).isEqualTo(first.eventsWikitext());
  }

  @Test
  void expiredCacheWithANewRevisionIsReplaced() {
    stubPage("2월 29일", LEAP_PREFIX);
    KoWikiPage first = client.fetch("2월 29일");
    expireCache();
    wikimedia.resetAll();
    stubPage("2월 29일", NEXT_PREFIX);

    KoWikiPage next = client.fetch("2월 29일");

    assertThat(first.revision()).isEqualTo(100L);
    assertThat(next.state()).isEqualTo(State.FRESH);
    assertThat(next.revision()).isEqualTo(101L);
    assertThat(next.fetchedAt()).isEqualTo(clock.instant()).isAfter(first.fetchedAt());
    assertThat(rows("2월 29일"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("revid", 101L);
              assertThat(row)
                  .containsEntry("events_wikitext", wikitext(NEXT_PREFIX + "-events.json"));
              assertThat(row.get("culture_wikitext"))
                  .as("section gone in the new revision")
                  .isNull();
              assertThat(fetchedAt(row)).isEqualTo(next.fetchedAt());
            });
  }

  @Test
  void failureWithoutACacheIsUnavailableAndStoresNothing() {
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    KoWikiPage page = client.fetch("9월 17일");

    assertThat(page).isEqualTo(KoWikiPage.unavailable("9월 17일"));
    assertThat(rows("9월 17일")).isEmpty();
  }

  @Test
  void tableHoldsOnlyTheEventsAndCultureSectionsAndNothingAboutTheRequester() {
    for (String title : KoWikiFixtureReadme.DEMO_PAGES.keySet()) {
      stubRecorded(title);
      client.fetch(title);
    }

    List<String> columns =
        jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns"
                + " WHERE table_name = 'wiki_section_cache' ORDER BY ordinal_position",
            String.class);
    assertThat(columns)
        .containsExactly(
            "id",
            "site",
            "page_title",
            "revid",
            "events_wikitext",
            "culture_wikitext",
            "fetched_at");
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM wiki_section_cache");
    assertThat(rows).hasSize(6);
    for (Map<String, Object> row : rows) {
      String prefix = KoWikiFixtureReadme.DEMO_PAGES.get((String) row.get("page_title"));
      assertThat(row).containsEntry("events_wikitext", wikitext(prefix + "-events.json"));
      assertThat(row).containsEntry("culture_wikitext", wikitext(prefix + "-culture.json"));
      assertThat((String) row.get("events_wikitext"))
          .startsWith("== 사건 ==")
          .doesNotContainPattern("==\\s*(탄생|사망|기념일)\\s*==");
      assertThat((String) row.get("culture_wikitext"))
          .startsWith("== 문화 ==")
          .doesNotContainPattern("==\\s*(탄생|사망|기념일)\\s*==");
    }
  }

  @Test
  void savingTheSameTitleTwiceKeepsOneRow() {
    Instant now = clock.instant();

    repository.upsert(SITE, "9월 17일", 1L, "== 사건 ==", null, now);
    repository.upsert(SITE, "9월 17일", 2L, "== 사건 ==\n* 시험", "== 문화 ==", now.plusSeconds(1));

    assertThat(rows("9월 17일"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("revid", 2L);
              assertThat(row).containsEntry("culture_wikitext", "== 문화 ==");
              assertThat(fetchedAt(row)).isEqualTo(now.plusSeconds(1));
            });
  }

  @Test
  void sameTitleOnAnotherSiteIsAnotherRow() {
    Instant now = clock.instant();

    repository.upsert(SITE, "9월 17일", 1L, null, null, now);
    repository.upsert("other.example.test", "9월 17일", 1L, null, null, now);

    assertThat(jdbc.queryForObject("SELECT count(*) FROM wiki_section_cache", Long.class))
        .isEqualTo(2L);
  }

  @Test
  void missingPageIsNotCachedAndAskedAgain() {
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    assertThat(client.fetch("2월 30일").state()).isEqualTo(State.ABSENT);
    assertThat(rows("2월 30일")).isEmpty();
    assertThat(client.fetch("2월 30일").state()).isEqualTo(State.ABSENT);
    assertThat(requestCount()).as("asked again on the next read").isEqualTo(2);
  }

  @Test
  void demoSeoulOlympicsPagesAreCachedAndStillParseToTheOpening() {
    LocalDate day = LocalDate.of(1988, 9, 17);
    stubRecorded("9월 17일");
    stubRecorded("1988년");

    assertThat(client.fetch("9월 17일").state()).isEqualTo(State.FRESH);
    assertThat(client.fetch("1988년").state()).isEqualTo(State.FRESH);
    int requests = requestCount();
    KoWikiPage datePage = client.fetch("9월 17일");
    KoWikiPage yearPage = client.fetch("1988년");

    assertThat(datePage.state()).isEqualTo(State.CACHED);
    assertThat(yearPage.state()).isEqualTo(State.CACHED);
    assertThat(requestCount()).isEqualTo(requests).isEqualTo(6);
    assertThat(parser.parse(datePage.eventsWikitext(), DATE, day).events())
        .extracting(ParsedEvent::text)
        .containsExactly("제24회 서울 하계 올림픽 개막.");
    assertThat(parser.parse(yearPage.eventsWikitext(), YEAR, day).events())
        .extracting(ParsedEvent::text)
        .containsExactly("1988 제24회 서울 올림픽이 개막하였다.");
  }

  @Test
  void demoQuarterFinalLineSurvivesTheCacheAndTheHtmlRule() {
    stubRecorded("6월 22일");
    client.fetch("6월 22일");

    KoWikiPage cached = client.fetch("6월 22일");

    assertThat(cached.state()).isEqualTo(State.CACHED);
    assertThat(parser.parse(cached.cultureWikitext(), DATE, LocalDate.of(2002, 6, 22)).events())
        .singleElement()
        .satisfies(event -> assertThat(event.text()).contains("스페인"));
  }

  @Test
  void demoEmptyDayPagesAreStoredAndGiveNoEvents() {
    LocalDate day = LocalDate.of(1995, 3, 15);
    stubRecorded("3월 15일");
    stubRecorded("1995년");

    KoWikiPage datePage = client.fetch("3월 15일");
    KoWikiPage yearPage = client.fetch("1995년");

    assertThat(rows("3월 15일")).hasSize(1);
    assertThat(rows("1995년")).hasSize(1);
    assertThat(datePage.state()).isEqualTo(State.FRESH);
    assertThat(yearPage.state()).isEqualTo(State.FRESH);
    assertThat(parser.parse(datePage.eventsWikitext(), DATE, day).events()).isEmpty();
    assertThat(parser.parse(datePage.cultureWikitext(), DATE, day).events()).isEmpty();
    assertThat(parser.parse(yearPage.eventsWikitext(), YEAR, day).events()).isEmpty();
    assertThat(parser.parse(yearPage.cultureWikitext(), YEAR, day).events()).isEmpty();
  }

  @Test
  void ttlComesFromApplicationYml() {
    assertThat(properties.cacheTtl()).isEqualTo(Duration.ofDays(7));
  }
}
