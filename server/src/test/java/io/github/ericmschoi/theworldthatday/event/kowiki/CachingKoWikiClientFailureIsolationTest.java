package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.Fault;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a1b T3: failures the T2 tests do not cover. A timeout, a dropped connection or a host
 * still blocked by an earlier rate limit falls back to the expired row ({@code STALE}) as a value,
 * never an exception; saves that race on one title keep one row (T2 (6), "even when two race").
 */
@ExtendWith(OutputCaptureExtension.class)
class CachingKoWikiClientFailureIsolationTest extends WikimediaWireMockTest {

  private static final String SITE = "localhost"; // host of the WireMock kowiki-api-url
  private static final String TITLE = "9월 17일";

  @Autowired private KoWikiClient client;
  @Autowired private WikimediaHttpClient http;
  @Autowired private WikiSectionCacheRepository repository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE wiki_section_cache");
  }

  private void stubRecordedDemoPage() {
    String prefix = KoWikiFixtureReadme.DEMO_PAGES.get(TITLE);
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(TITLE))
            .willReturn(okJson(read(prefix + "-sections.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("section", equalTo(KoWikiFixtureReadme.sectionIndex(prefix, "사건")))
            .willReturn(okJson(read(prefix + "-events.json"))));
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("section", equalTo(KoWikiFixtureReadme.sectionIndex(prefix, "문화")))
            .willReturn(okJson(read(prefix + "-culture.json"))));
  }

  /** Reads the demo page once (FRESH, stored), then lets the row expire and clears WireMock. */
  private KoWikiPage cachedThenExpired() {
    stubRecordedDemoPage();
    KoWikiPage first = client.fetch(TITLE);
    assertThat(first.state()).isEqualTo(State.FRESH);
    clock.advance(properties.cacheTtl().plusSeconds(1));
    wikimedia.resetAll();
    return first;
  }

  private long rowCount() {
    Long count =
        jdbc.queryForObject(
            "SELECT count(*) FROM wiki_section_cache WHERE site = ? AND page_title = ?",
            Long.class,
            SITE,
            TITLE);
    return count == null ? 0 : count;
  }

  @Test
  void timeoutAfterTheTtlServesTheOldCopyAsStaleAndLogsNoUrl(CapturedOutput output) {
    KoWikiPage first = cachedThenExpired();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(
                okJson("{}").withFixedDelay((int) properties.readTimeout().toMillis() * 4)));

    KoWikiPage stale = client.fetch(TITLE);

    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
    assertThat(output.getAll())
        .contains("source=kowiki title=9월 17일 cause=HttpTimeoutException")
        .doesNotContain(wikimedia.baseUrl())
        .doesNotContain("oldid=");
  }

  @Test
  void droppedConnectionAfterTheTtlServesTheOldCopyAsStale() {
    KoWikiPage first = cachedThenExpired();
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    KoWikiPage stale = client.fetch(TITLE);

    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale).usingRecursiveComparison().ignoringFields("state").isEqualTo(first);
  }

  @Test
  void hostStillBlockedAfterTheTtlServesTheOldCopyWithoutARequest() {
    KoWikiPage first = cachedThenExpired();
    stubRecordedDemoPage();
    http.blockHost(properties.kowikiApiUrl());

    KoWikiPage stale = client.fetch(TITLE);

    assertThat(stale.state()).isEqualTo(State.STALE);
    assertThat(stale.revision()).isEqualTo(first.revision());
    assertThat(requestCount()).as("a blocked host gets no request").isZero();
  }

  @Test
  void timeoutWithoutACacheIsUnavailableAndStoresNothing() {
    wikimedia.stubFor(
        get(urlPathEqualTo(KOWIKI_PATH))
            .willReturn(
                okJson("{}").withFixedDelay((int) properties.readTimeout().toMillis() * 4)));

    KoWikiPage page = client.fetch(TITLE);

    assertThat(page).isEqualTo(KoWikiPage.unavailable(TITLE));
    assertThat(rowCount()).isZero();
  }

  @Test
  void racingSavesOfOneTitleNeverFailAndKeepOneRow() throws Exception {
    int writers = 8;
    Instant now = clock.instant();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(writers);
    try {
      List<Future<?>> saves = new ArrayList<>();
      for (int i = 1; i <= writers; i++) {
        long revid = i;
        saves.add(
            pool.submit(
                () -> {
                  start.await();
                  repository.upsert(SITE, TITLE, revid, "== 사건 ==", null, now.plusSeconds(revid));
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> save : saves) {
        save.get(30, TimeUnit.SECONDS); // throws if any save failed
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(rowCount()).isEqualTo(1);
    Long revid =
        jdbc.queryForObject(
            "SELECT revid FROM wiki_section_cache WHERE site = ? AND page_title = ?",
            Long.class,
            SITE,
            TITLE);
    assertThat(revid).isBetween(1L, (long) writers);
  }

  @Test
  void racingFirstReadsOfOneTitleAreAllFreshAndKeepOneRow() throws Exception {
    stubRecordedDemoPage();
    int readers = 3; // one per request slot, so no read waits for a slot
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(readers);
    try {
      List<Future<KoWikiPage>> reads = new ArrayList<>();
      for (int i = 0; i < readers; i++) {
        reads.add(
            pool.submit(
                () -> {
                  start.await();
                  return client.fetch(TITLE);
                }));
      }
      start.countDown();
      for (Future<KoWikiPage> read : reads) {
        assertThat(read.get(30, TimeUnit.SECONDS).state()).isEqualTo(State.FRESH);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(rowCount()).isEqualTo(1);
  }
}
