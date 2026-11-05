package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
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
 * Plan M3a1b T2 (2)~(9) and plan C1 S3a T2 (5): the wikitext cache in front of the Action API
 * client, against WireMock and the Testcontainers database (V2 table {@code wiki_section_cache}),
 * on English Wikipedia pages. Time moves with the test clock; the TTL is {@code
 * wikimedia.cache-ttl} from application.yml (7 days).
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

  /** Stubs a page from fixtures: section list by title, the Events section by revid and index. */
  private static void stubPage(String title, String prefix) {
    WikiPageStubs.stubPage(wikimedia, title, prefix);
  }

  private static void stubRecorded(String title) {
    KoWikiSourceFixture.stubRecorded(wikimedia, title);
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
    stubRecorded("July 17");

    KoWikiPage first = client.fetch("July 17");

    assertThat(first.state()).isEqualTo(State.FRESH);
    assertThat(first.revision()).isEqualTo(1375524665L);
    assertThat(requestCount()).as("section list + Events section").isEqualTo(2);
    assertThat(rows("July 17"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("site", SITE);
              assertThat(row).containsEntry("revid", 1375524665L);
              assertThat(row).containsEntry("events_wikitext", wikitext("07-17-events.json"));
              assertThat(row.get("culture_wikitext"))
                  .as("English Wikipedia has no culture section (plan C1 S3a T2 (5))")
                  .isNull();
              assertThat(fetchedAt(row)).isEqualTo(first.fetchedAt()).isEqualTo(clock.instant());
            });

    clock.advance(properties.cacheTtl().minusSeconds(1));
    KoWikiPage second = client.fetch("July 17");

    assertThat(second.state()).isEqualTo(State.CACHED);
    assertThat(requestCount()).as("no request inside the TTL").isEqualTo(2);
    assertThat(second).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
  }

  @Test
  void expiredCacheIsReadAgainAndAFailureServesTheOldCopyAsStale() {
    stubRecorded("July 17");
    KoWikiPage first = client.fetch("July 17");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    KoWikiPage stale = client.fetch("July 17");

    assertThat(requestCount()).as("asked again: first try + one retry").isEqualTo(2);
    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
    assertThat(fetchedAt(rows("July 17").getFirst())).isEqualTo(first.fetchedAt());
  }

  @Test
  void ratelimitedAfterTheTtlAlsoServesTheOldCopyAsStale() {
    stubRecorded("1976");
    KoWikiPage first = client.fetch("1976");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    KoWikiPage stale = client.fetch("1976");

    assertThat(requestCount()).isEqualTo(1);
    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale.revision()).isEqualTo(first.revision());
    assertThat(stale.eventsWikitext()).isEqualTo(first.eventsWikitext());
  }

  @Test
  void expiredCacheWithANewRevisionIsReplaced() {
    stubPage("February 29", LEAP_PREFIX);
    KoWikiPage first = client.fetch("February 29");
    expireCache();
    wikimedia.resetAll();
    stubPage("February 29", NEXT_PREFIX);

    KoWikiPage next = client.fetch("February 29");

    assertThat(first.revision()).isEqualTo(100L);
    assertThat(next.state()).isEqualTo(State.FRESH);
    assertThat(next.revision()).isEqualTo(101L);
    assertThat(next.fetchedAt()).isEqualTo(clock.instant()).isAfter(first.fetchedAt());
    assertThat(rows("February 29"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("revid", 101L);
              assertThat(row)
                  .containsEntry("events_wikitext", wikitext(NEXT_PREFIX + "-events.json"));
              assertThat(row.get("culture_wikitext"))
                  .as("no culture section on English Wikipedia")
                  .isNull();
              assertThat(fetchedAt(row)).isEqualTo(next.fetchedAt());
            });
  }

  @Test
  void failureWithoutACacheIsUnavailableAndStoresNothing() {
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    KoWikiPage page = client.fetch("July 17");

    assertThat(page).isEqualTo(KoWikiPage.unavailable("July 17"));
    assertThat(rows("July 17")).isEmpty();
  }

  @Test
  void tableHoldsOnlyTheEventsAndCultureSectionsAndNothingAboutTheRequester() {
    for (String title : EnWikiPages.RECORDED.keySet()) {
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
    assertThat(rows).hasSize(EnWikiPages.RECORDED.size()).hasSize(10);
    for (Map<String, Object> row : rows) {
      String prefix = EnWikiPages.RECORDED.get((String) row.get("page_title"));
      assertThat(row).containsEntry("site", SITE);
      assertThat(row).containsEntry("events_wikitext", wikitext(prefix + "-events.json"));
      assertThat(row.get("culture_wikitext")).isNull();
      assertThat((String) row.get("events_wikitext"))
          .startsWith("==Events==")
          .doesNotContainPattern("(?m)^==[^=].*==\\s*$(?s).*^==[^=]")
          .doesNotContainPattern("==\\s*(Births|Deaths|Holidays and observances)\\s*==");
    }
  }

  @Test
  void savingTheSameTitleTwiceKeepsOneRow() {
    Instant now = clock.instant();

    repository.upsert(SITE, "July 17", 1L, "==Events==", "==Culture==", now);
    repository.upsert(SITE, "July 17", 2L, "==Events==\n* Test event A", null, now.plusSeconds(1));

    assertThat(rows("July 17"))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("revid", 2L);
              assertThat(row).containsEntry("events_wikitext", "==Events==\n* Test event A");
              assertThat(row.get("culture_wikitext")).as("overwritten with null").isNull();
              assertThat(fetchedAt(row)).isEqualTo(now.plusSeconds(1));
            });
  }

  @Test
  void sameTitleOnAnotherSiteIsAnotherRow() {
    Instant now = clock.instant();

    repository.upsert(SITE, "July 17", 1L, null, null, now);
    repository.upsert("other.example.test", "July 17", 1L, null, null, now);

    assertThat(jdbc.queryForObject("SELECT count(*) FROM wiki_section_cache", Long.class))
        .isEqualTo(2L);
  }

  @Test
  void missingPageIsNotCachedAndAskedAgain() {
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    assertThat(client.fetch("February 30").state()).isEqualTo(State.ABSENT);
    assertThat(rows("February 30")).isEmpty();
    assertThat(client.fetch("February 30").state()).isEqualTo(State.ABSENT);
    assertThat(requestCount()).as("asked again on the next read").isEqualTo(2);
  }

  @Test
  void demoMontrealOlympicsPagesAreCachedAndStillParseToTheOpening() {
    LocalDate day = LocalDate.of(1976, 7, 17);
    stubRecorded("July 17");
    stubRecorded("1976");

    assertThat(client.fetch("July 17").state()).isEqualTo(State.FRESH);
    assertThat(client.fetch("1976").state()).isEqualTo(State.FRESH);
    int requests = requestCount();
    KoWikiPage datePage = client.fetch("July 17");
    KoWikiPage yearPage = client.fetch("1976");

    assertThat(datePage.state()).isEqualTo(State.CACHED);
    assertThat(yearPage.state()).isEqualTo(State.CACHED);
    assertThat(requestCount()).isEqualTo(requests).isEqualTo(4);
    assertThat(parser.parse(datePage.eventsWikitext(), DATE, day).events())
        .extracting(ParsedEvent::text)
        .containsExactly(
            "East Timor is annexed and becomes the 27th province of Indonesia.",
            "The opening of the Summer Olympics in Montreal is marred by 25 African teams"
                + " boycotting the games because of New Zealand's participation. Contrary to"
                + " rulings by other international sports organizations, the IOC had declined to"
                + " exclude New Zealand because of their participation in South African sporting"
                + " events during apartheid.");
    assertThat(parser.parse(yearPage.eventsWikitext(), YEAR, day).events())
        .extracting(ParsedEvent::text)
        .containsExactly(
            "The 1976 Summer Olympics begin in Montreal, Quebec, Canada.",
            "East Timor is declared the 27th province of Indonesia.");
  }

  @Test
  void demoQuarterFinalLineSurvivesTheCacheAndTheHtmlRule() {
    stubRecorded("June 22");
    client.fetch("June 22");

    KoWikiPage cached = client.fetch("June 22");

    assertThat(cached.state()).isEqualTo(State.CACHED);
    assertThat(cached.cultureWikitext()).isNull();
    assertThat(parser.parse(cached.eventsWikitext(), DATE, LocalDate.of(2002, 6, 22)).events())
        .singleElement()
        .satisfies(event -> assertThat(event.text()).contains("quarter-final A"));
  }

  @Test
  void demoEmptyDayPagesAreStoredAndGiveNoEvents() {
    LocalDate day = LocalDate.of(1995, 3, 15);
    stubRecorded("March 15");
    stubPage("1995", "synthetic/synthetic-1995");

    KoWikiPage datePage = client.fetch("March 15");
    KoWikiPage yearPage = client.fetch("1995");

    assertThat(rows("March 15")).hasSize(1);
    assertThat(rows("1995")).hasSize(1);
    assertThat(datePage.state()).isEqualTo(State.FRESH);
    assertThat(yearPage.state()).isEqualTo(State.FRESH);
    assertThat(parser.parse(datePage.eventsWikitext(), DATE, day).events()).isEmpty();
    assertThat(datePage.cultureWikitext()).isNull();
    assertThat(parser.parse(yearPage.eventsWikitext(), YEAR, day).events()).isEmpty();
    assertThat(yearPage.cultureWikitext()).isNull();
  }

  @Test
  void ttlComesFromApplicationYml() {
    assertThat(properties.cacheTtl()).isEqualTo(Duration.ofDays(7));
  }

  @Test
  void ttlBoundaryIsExclusive() {
    stubRecorded("July 17");
    client.fetch("July 17");

    clock.advance(properties.cacheTtl().minusNanos(1));
    assertThat(client.fetch("July 17").state()).as("TTL - 1 ns").isEqualTo(State.CACHED);
    assertThat(requestCount()).isEqualTo(2);

    clock.advance(Duration.ofNanos(1));
    assertThat(client.fetch("July 17").state()).as("exactly TTL").isEqualTo(State.FRESH);
    assertThat(requestCount()).isEqualTo(4);
  }

  @Test
  void rowOfAPageThatIsNowMissingIsDeletedAndNeverServedAsStale() {
    stubPage("February 29", LEAP_PREFIX);
    client.fetch("February 29");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    assertThat(client.fetch("February 29").state()).isEqualTo(State.ABSENT);
    assertThat(rows("February 29")).isEmpty();

    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));
    assertThat(client.fetch("February 29")).isEqualTo(KoWikiPage.unavailable("February 29"));
  }

  @Test
  void missingPageDeletesOnlyItsOwnRow() {
    stubPage("February 29", LEAP_PREFIX);
    stubRecorded("July 17");
    client.fetch("February 29");
    client.fetch("July 17");
    expireCache();
    wikimedia.resetAll();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    client.fetch("February 29");

    assertThat(rows("February 29")).isEmpty();
    assertThat(rows("July 17")).hasSize(1);
  }
}
