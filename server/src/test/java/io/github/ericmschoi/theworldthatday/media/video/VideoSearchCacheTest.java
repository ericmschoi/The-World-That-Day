package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.LEAP_DAY;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Plan M3a4c T3 (1) to (5): the cache flow of {@link VideoSearch} with fake sources, on the
 * Testcontainers table (or a fake repository for database failures). Synthetic candidates on the
 * leap day with made-up file names; event titles are invented. No HTTP.
 */
@ExtendWith(OutputCaptureExtension.class)
class VideoSearchCacheTest extends WikimediaWireMockTest {

  private static final Duration TTL = Duration.ofDays(7);
  private static final Duration DEADLINE = Duration.ofMillis(400);
  private static final String E1 = "kowiki-2000-02-29-0000000a";
  private static final String E2 = "kowiki-2000-02-29-0000000b";
  private static final String TITLE = "알파 행사 영상";
  private static final List<EventTarget> ONE = List.of(target(E1));
  private static final List<EventTarget> TWO = List.of(target(E1), target(E2));

  @Autowired private EventMediaSearchCacheRepository repository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyCache() {
    jdbc.execute("TRUNCATE event_media_search_cache");
  }

  private static EventTarget target(String id) {
    return new EventTarget(id, "알파 행사", "country:KR", "지어낸 문장이다.", "Q999999901");
  }

  /** A Commons candidate with its origin (only those are stored). */
  private static Candidate stored(long n, String title, CaptureTime capture, EventLink link) {
    Basis basis = link == EventLink.P10 ? Basis.COMMONS_P10 : Basis.COMMONS_CATEGORY;
    Candidate c = FakeVideoSource.commons(n, title, capture, link, basis);
    return new Candidate(
        c.item(),
        c.title(),
        null,
        c.originalCapture(),
        List.of(),
        link,
        false,
        false,
        basis,
        new MediaOrigin(
            "commons", "Example test video " + n + ".webm", "Example Author", null, null));
  }

  // 130: P10, same day, event words, free
  private static final Candidate C130 = stored(900001, TITLE, day("2000-02-29"), EventLink.P10);
  // 90: own category, event words, free
  private static final Candidate C90 = stored(900002, TITLE, null, EventLink.CATEGORY_DIRECT);

  private SearchCache cache(EventMediaSearchCacheRepository store, Map<String, Duration> ttl) {
    MediaProperties media = new MediaProperties(List.of(), List.of());
    return new SearchCache(
        store,
        new SearchCacheFormat(new MediaFilter(media), VideoCandidates.NORMALIZER),
        new SearchCacheProperties(ttl),
        clock);
  }

  private SearchCache cache() {
    return cache(repository, Map.of("alpha", TTL, "beta", TTL));
  }

  private static VideoSearch search(SearchCache cache, int minScore, EventVideoSource... sources) {
    VideoSearchProperties p = FakeVideoSource.properties(3);
    VideoSearchProperties properties =
        new VideoSearchProperties(
            minScore, 3, 600, p.commemorationWords(), p.otherStageWords(), p.stopwords());
    return new VideoSearch(List.of(sources), new VideoAccuracy(properties), properties, cache);
  }

  private static VideoSearch search(SearchCache cache, EventVideoSource... sources) {
    return search(cache, 60, sources);
  }

  private static Videos run(VideoSearch search, List<EventTarget> targets, boolean dayPathsOff) {
    try (Deadline deadline = Deadline.after(DEADLINE)) {
      return search.start(deadline, LEAP_DAY, targets, dayPathsOff).await();
    }
  }

  private static FakeVideoSource alpha(Map<String, List<Candidate>> byEvent) {
    return new FakeVideoSource("alpha", events -> ok(byEvent));
  }

  private long rows() {
    return jdbc.queryForObject("SELECT count(*) FROM event_media_search_cache", Long.class);
  }

  private static List<String> cacheLines(CapturedOutput output) {
    return output.getAll().lines().filter(l -> l.contains("Media search cache")).toList();
  }

  // ---- (2) read, (4) write ----

  @Test
  void theSecondSearchAsksNothingAndChoosesTheSame() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130), E2, List.of()));
    VideoSearch search = search(cache(), alpha);

    Videos first = run(search, TWO, false);
    Videos second = run(search, TWO, false);

    assertThat(alpha.asked).hasSize(1);
    assertThat(rows()).isEqualTo(2); // the event without candidates has its row too
    assertThat(second).isEqualTo(first);
    assertThat(second.byEvent().get(E1).selection().score()).isEqualTo(130);
    assertThat(second.failed()).isFalse();
  }

  @Test
  void aRowIsStaleExactlyAtFetchedAtPlusTtl() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130)));
    VideoSearch search = search(cache(), alpha);
    run(search, ONE, false);

    clock.advance(TTL.minusSeconds(1));
    run(search, ONE, false);
    assertThat(alpha.asked).hasSize(1);

    clock.advance(Duration.ofSeconds(1));
    run(search, ONE, false);
    assertThat(alpha.asked).hasSize(2);
    assertThat(rows()).isEqualTo(1); // replaced, not added
  }

  @Test
  void onlyTheEventsWithoutAFreshRowAreAskedAndTheAnswersAreJoined() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130), E2, List.of(C90)));
    VideoSearch search = search(cache(), alpha);
    run(search, ONE, false);

    Videos videos = run(search, TWO, false);

    assertThat(alpha.asked).hasSize(2);
    assertThat(alpha.asked.get(1)).extracting(EventContext::eventId).containsExactly(E2);
    assertThat(videos.byEvent()).containsOnlyKeys(E1, E2);
    assertThat(videos.byEvent().get(E1).record().id()).isEqualTo("commons-900001");
    assertThat(videos.byEvent().get(E2).record().id()).isEqualTo("commons-900002");
    assertThat(rows()).isEqualTo(2);
  }

  @Test
  void theSameVideoInCachedAndNewEventsIsChosenOnce() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130), E2, List.of(C130)));
    VideoSearch search = search(cache(), alpha);
    run(search, ONE, false);

    Videos videos = run(search, TWO, false);

    assertThat(videos.byEvent()).containsOnlyKeys(E1);
  }

  @Test
  void aChangedMinScoreAppliesToTheSameCachedRow() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C90)));
    SearchCache cache = cache();
    assertThat(run(search(cache, 60, alpha), ONE, false).byEvent()).containsOnlyKeys(E1);

    Videos stricter = run(search(cache, 100, alpha), ONE, false);

    assertThat(alpha.asked).hasSize(1);
    assertThat(stricter.byEvent()).isEmpty();
    assertThat(stricter.failed()).isFalse();
  }

  @Test
  void dayPathsOnAndOffAreSeparateRows() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130)));
    VideoSearch search = search(cache(), alpha);

    run(search, ONE, false);
    run(search, ONE, true);
    run(search, ONE, false);
    run(search, ONE, true);

    assertThat(alpha.asked).hasSize(2);
    assertThat(alpha.dayPathsOff).containsExactly(false, true);
    assertThat(
            jdbc.queryForList(
                "SELECT day_paths_off FROM event_media_search_cache ORDER BY day_paths_off",
                Boolean.class))
        .containsExactly(false, true);
  }

  @Test
  void anEventWithACandidateWithoutOriginIsNotStored() {
    Candidate noOrigin =
        FakeVideoSource.commons(
            900003, TITLE, null, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130, noOrigin), E2, List.of(C90)));
    VideoSearch search = search(cache(), alpha);

    Videos first = run(search, TWO, false);
    Videos second = run(search, TWO, false);

    assertThat(jdbc.queryForList("SELECT event_id FROM event_media_search_cache", String.class))
        .containsExactly(E2);
    assertThat(alpha.asked.get(1)).extracting(EventContext::eventId).containsExactly(E1);
    assertThat(second).isEqualTo(first);
  }

  // ---- (4) what is not stored ----

  private void assertNothingStored(FakeVideoSource alpha) {
    VideoSearch search = search(cache(), alpha);
    run(search, ONE, false);
    run(search, ONE, false);
    assertThat(rows()).isZero();
    assertThat(alpha.asked).hasSize(2);
  }

  @Test
  void aFailedAnswerIsNotStored() {
    assertNothingStored(
        new FakeVideoSource(
            "alpha",
            events -> new SourceResult(Map.of(E1, List.of(C130)), Map.of(), Status.FAILED)));
  }

  @Test
  void aSkippedQuotaIsNotStored() {
    assertNothingStored(
        new FakeVideoSource(
            "alpha", events -> new SourceResult(Map.of(), Map.of(), Status.QUOTA_SKIPPED)));
  }

  @Test
  void anExceptionIsNotStored() {
    assertNothingStored(
        new FakeVideoSource(
            "alpha",
            events -> {
              throw new IllegalStateException("synthetic");
            }));
  }

  @Test
  void aLateOkAnswerIsNotStored() {
    assertNothingStored(
        new FakeVideoSource(
            "alpha",
            events -> {
              try {
                Thread.sleep(DEADLINE.plusMillis(300));
              } catch (InterruptedException e) {
                // the deadline stops it; it still answers OK, late
              }
              return ok(Map.of(E1, List.of(C130)));
            }));
  }

  @Test
  void anAnswerWhoseJudgingThrowsIsNotStored() {
    Candidate boom = stored(900004, ThrowingVideoAccuracy.BOOM, null, EventLink.CATEGORY_DIRECT);
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(boom)));
    VideoSearch search =
        new VideoSearch(
            List.of(alpha), new ThrowingVideoAccuracy(3), FakeVideoSource.properties(3), cache());

    Videos videos = run(search, ONE, false);

    assertThat(videos.failed()).isTrue();
    assertThat(rows()).isZero();
  }

  @Test
  void anInterruptedCallerStoresNothing() {
    FakeCacheRepository store = new FakeCacheRepository();
    FakeVideoSource silent = new FakeVideoSource("alpha", events -> FakeVideoSource.silent());
    VideoSearch search = search(cache(store.proxy(), Map.of("alpha", TTL)), silent);

    Thread.currentThread().interrupt();
    Videos videos;
    try {
      videos = run(search, ONE, false);
    } finally {
      assertThat(Thread.interrupted()).isTrue();
    }

    assertThat(videos.failed()).isTrue();
    assertThat(store.count("upsert")).isZero();
  }

  @Test
  void cachedCandidatesStayWhenTheNewPartFails() {
    SearchCache cache = cache();
    run(search(cache, alpha(Map.of(E1, List.of(C130)))), ONE, false);
    FakeVideoSource failing =
        new FakeVideoSource(
            "alpha", events -> new SourceResult(Map.of(E2, List.of(C90)), Map.of(), Status.FAILED));

    Videos videos = run(search(cache, failing), TWO, false);

    assertThat(failing.asked.getFirst()).extracting(EventContext::eventId).containsExactly(E2);
    assertThat(videos.byEvent()).containsOnlyKeys(E1);
    assertThat(videos.failed()).isTrue();
    assertThat(jdbc.queryForList("SELECT event_id FROM event_media_search_cache", String.class))
        .containsExactly(E1);
  }

  // ---- (1) which sources use the cache ----

  @Test
  void aSourceWithTtlZeroOrNoEntryIsNeverReadNorWritten() {
    FakeCacheRepository store = new FakeCacheRepository();
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130)));
    FakeVideoSource beta = new FakeVideoSource("beta", events -> ok(Map.of(E1, List.of(C90))));
    VideoSearch search = search(cache(store.proxy(), Map.of("alpha", Duration.ZERO)), alpha, beta);

    run(search, TWO, false);
    run(search, TWO, false);

    assertThat(store.calls).isEmpty();
    assertThat(alpha.asked).hasSize(2);
    assertThat(beta.asked).hasSize(2);
  }

  @Test
  void withoutEventsNothingIsRead() {
    FakeCacheRepository store = new FakeCacheRepository();
    FakeVideoSource alpha = alpha(Map.of());

    run(search(cache(store.proxy(), Map.of("alpha", TTL)), alpha), List.of(), false);

    assertThat(store.calls).isEmpty();
    assertThat(alpha.asked).isEmpty();
  }

  // ---- (5) database failures ----

  @Test
  void aFailedReadSkipsTheOtherReadsAndEveryWriteWithOneLine(CapturedOutput output) {
    FakeCacheRepository store = new FakeCacheRepository();
    store.findThrows = new DataAccessResourceFailureException("synthetic");
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130)));
    FakeVideoSource beta = new FakeVideoSource("beta", events -> ok(Map.of(E2, List.of(C90))));
    Map<String, Duration> ttl = Map.of("alpha", TTL, "beta", TTL);

    Videos videos = run(search(cache(store.proxy(), ttl), alpha, beta), TWO, false);

    assertThat(store.calls).containsExactly("find");
    assertThat(alpha.asked.getFirst()).hasSize(2);
    assertThat(beta.asked.getFirst()).hasSize(2);
    assertThat(videos).isEqualTo(run(search(null, alpha, beta), TWO, false));
    assertThat(videos.failed()).isFalse();
    assertThat(cacheLines(output))
        .singleElement()
        .satisfies(
            l ->
                assertThat(l)
                    .contains(" WARN ")
                    .endsWith(
                        "Media search cache read failed: source=alpha"
                            + " cause=DataAccessResourceFailureException"));
    assertThat(output.getAll()).doesNotContain("2000-02-29", E1, E2, "Example test video", "Q999");
  }

  @Test
  void aFailedWriteKeepsTheAnswerWithOneLinePerSource(CapturedOutput output) {
    FakeCacheRepository store = new FakeCacheRepository();
    store.upsertThrows = new DataAccessResourceFailureException("synthetic");
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130), E2, List.of()));
    FakeVideoSource beta = new FakeVideoSource("beta", events -> ok(Map.of(E2, List.of(C90))));
    Map<String, Duration> ttl = Map.of("alpha", TTL, "beta", TTL);

    Videos videos = run(search(cache(store.proxy(), ttl), alpha, beta), TWO, false);

    assertThat(store.count("find")).isEqualTo(2);
    assertThat(store.count("upsert")).isEqualTo(2); // one try per source, not per event
    assertThat(videos).isEqualTo(run(search(null, alpha, beta), TWO, false));
    assertThat(videos.failed()).isFalse();
    assertThat(cacheLines(output))
        .map(l -> l.substring(l.indexOf("Media search cache")))
        .containsExactly(
            "Media search cache write failed: source=alpha"
                + " cause=DataAccessResourceFailureException",
            "Media search cache write failed: source=beta"
                + " cause=DataAccessResourceFailureException");
    assertThat(output.getAll()).doesNotContain("2000-02-29", E1, E2, "Example test video", "Q999");
  }

  @Test
  void theListConstructorHasNoCache() {
    FakeVideoSource alpha = alpha(Map.of(E1, List.of(C130)));
    VideoSearch search = FakeVideoSource.search(3, alpha);

    run(search, ONE, false);
    run(search, ONE, false);

    assertThat(alpha.asked).hasSize(2);
    assertThat(rows()).isZero();
  }
}
