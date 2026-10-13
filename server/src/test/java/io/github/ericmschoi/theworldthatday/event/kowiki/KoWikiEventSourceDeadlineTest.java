package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.PATH;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.pagepropsRequests;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.params;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubRecorded;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Plan M3a1c T1 (4): one date never takes longer than {@code wikimedia.kowiki-day-deadline} (test
 * values: deadline 1 s, read timeout 300 ms), with stalls made by WireMock delays or by a page
 * client that blocks like a hung database.
 */
class KoWikiEventSourceDeadlineTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate OPENING = LocalDate.of(1988, 9, 17);
  private static final Duration DEADLINE = Duration.ofSeconds(1);
  private static final Duration READ_TIMEOUT = Duration.ofMillis(300);
  private static final Duration BOUND = Duration.ofMillis(1500);
  private static final int STALL_MILLIS = 3000;

  private final CountDownLatch hungDatabase = new CountDownLatch(1);

  @AfterEach
  void releaseHungCalls() {
    hungDatabase.countDown();
  }

  private static void stallEverything() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok().withFixedDelay(STALL_MILLIS)));
  }

  private static <T> T timed(Supplier<T> call, List<Duration> elapsed) {
    long start = System.nanoTime();
    T result = call.get();
    elapsed.add(Duration.ofNanos(System.nanoTime() - start));
    return result;
  }

  @Test
  void stalledHostReturnsInTimeAndTheYearPageIsNotRequested() {
    stallEverything();
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, READ_TIMEOUT, DEADLINE);
    List<Duration> elapsed = new ArrayList<>();

    KoWikiDayResult result = timed(() -> parts.source().find(OPENING), elapsed);

    assertThat(elapsed.getFirst()).isLessThan(BOUND);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.UNAVAILABLE);
    assertThat(result.allUnavailable()).isTrue();
    assertThat(params(wikimedia, "page"))
        .as("date page attempt and its retry only")
        .containsOnly("9월 17일")
        .doesNotContain("1988년");
  }

  @Test
  void deadlineCutsAReadTimeoutLongerThanItself() {
    stallEverything();
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), DEADLINE);
    List<Duration> elapsed = new ArrayList<>();

    KoWikiDayResult result = timed(() -> parts.source().find(OPENING), elapsed);

    assertThat(elapsed.getFirst()).isLessThan(BOUND);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.UNAVAILABLE);
    assertThat(params(wikimedia, "page")).doesNotContain("1988년");
  }

  @Test
  void stalledYearPageStillReturnsTheDatePageEvents() {
    stubRecorded(wikimedia, "9월 17일");
    stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("1988년"))
            .willReturn(ok().withFixedDelay(STALL_MILLIS)));
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, READ_TIMEOUT, DEADLINE);
    List<Duration> elapsed = new ArrayList<>();

    KoWikiDayResult result = timed(() -> parts.source().find(OPENING), elapsed);

    assertThat(elapsed.getFirst()).isLessThan(BOUND);
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.FRESH, State.UNAVAILABLE);
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(event -> assertThat(event.sourceTitle()).isEqualTo("9월 17일"));
    assertThat(result.degraded()).isTrue();
  }

  @Test
  void stalledLinkLookupKeepsTheEventsAndMarksTheLookupFailed() {
    stubRecorded(wikimedia, "9월 17일");
    stubRecorded(wikimedia, "1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(ok().withFixedDelay(STALL_MILLIS)));
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), DEADLINE);
    List<Duration> elapsed = new ArrayList<>();

    KoWikiDayResult result = timed(() -> parts.source().find(OPENING), elapsed);

    assertThat(elapsed.getFirst()).isLessThan(BOUND);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(result.events()).isNotEmpty();
    assertThat(result.events()).extracting(KoWikiEvent::wikidataQid).containsOnlyNulls();
    assertThat(result.linkResolutionFailed()).isTrue();
    assertThat(result.degraded()).isTrue();
    assertThat(pagepropsRequests(wikimedia)).hasSize(1);
  }

  @Test
  void stalledDatePageUsesTheCachedYearPageWithoutRequestingIt() {
    stallEverything();
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, READ_TIMEOUT, DEADLINE);
    KoWikiPage staleYear =
        new KoWikiPage(
            "1988년",
            State.STALE,
            42640701L,
            KoWikiFixtures.wikitext("1988-events.json"),
            KoWikiFixtures.wikitext("1988-culture.json"),
            parts.clock.instant().minus(Duration.ofDays(8)));
    List<String> cachedCalls = new ArrayList<>();
    KoWikiClient client =
        new KoWikiClient() {
          @Override
          public KoWikiPage fetch(String title) {
            return parts.pages.fetch(title);
          }

          @Override
          public KoWikiPage cached(String title) {
            cachedCalls.add(title);
            return staleYear;
          }
        };

    KoWikiDayResult result = parts.source(client).find(OPENING);

    assertThat(cachedCalls).containsExactly("1988년");
    assertThat(params(wikimedia, "page")).containsOnly("9월 17일");
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.UNAVAILABLE, State.STALE);
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(
            event -> {
              assertThat(event.sourceTitle()).isEqualTo("1988년");
              assertThat(event.retrievedAt()).isEqualTo(staleYear.fetchedAt());
            });
    assertThat(result.degraded()).isTrue();
    assertThat(result.allUnavailable()).isFalse();
  }

  @Test
  void hungPageClientSuchAsAStalledDatabaseIsCutAtTheDeadline() {
    KoWikiSourceFixture parts = new KoWikiSourceFixture(wikimedia, READ_TIMEOUT, DEADLINE);
    List<String> fetched = new ArrayList<>();
    KoWikiClient hung =
        title -> {
          fetched.add(title);
          try {
            hungDatabase.await(); // blocks like a connection wait on a hung database
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return KoWikiPage.unavailable(title);
        };
    List<Duration> elapsed = new ArrayList<>();

    KoWikiDayResult result = timed(() -> parts.source(hung).find(OPENING), elapsed);

    assertThat(elapsed.getFirst()).isLessThan(BOUND);
    assertThat(fetched).containsExactly("9월 17일");
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.UNAVAILABLE);
    assertThat(result.allUnavailable()).isTrue();
    assertThat(wikimedia.getAllServeEvents()).isEmpty();
  }
}
