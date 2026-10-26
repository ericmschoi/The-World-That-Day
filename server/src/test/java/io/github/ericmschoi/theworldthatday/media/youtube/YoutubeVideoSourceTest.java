package io.github.ericmschoi.theworldthatday.media.youtube;

import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.CHANNEL;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.SECOND_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeClient.Response;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a5a T5 (1): the YouTube source with a fake client (no HTTP at all), a real {@link
 * YoutubeQuota} on a {@link MutableClock} and the T4 mapper. Items are the synthetic ones of {@code
 * fixtures/youtube/videos-1986-09-20.json}; IDs such as {@code Abc123xyz_9} are made up. The 2026
 * instants are quota clock values, not story dates; the event date is the 1986-09-20 sample.
 */
@Timeout(30)
class YoutubeVideoSourceTest {

  private static final LocalDate DAY = LocalDate.of(1986, 9, 20);
  private static final Duration READ_TIMEOUT = Duration.ofMillis(400);
  private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");
  private static final String GAMES = "1986년 아시안 게임";

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-06-10T19:00:00Z"), ZoneOffset.UTC);
  private final FakeClient client = new FakeClient();
  private YoutubeQuota quota;

  private static EventContext event(char n, String title) {
    return new EventContext(
        "kowiki-1986-09-20-0000000" + n, DAY, title, null, "지어낸 문장이다.", null, "country:KR");
  }

  private static final EventContext A = event('a', GAMES);
  private static final EventContext B = event('b', "시험 문서 나");
  private static final EventContext C = event('c', "시험 문서 다");

  private YoutubeVideoSource source(List<String> channels, int searchBudget, int unitBudget) {
    return source(channels, searchBudget, unitBudget, READ_TIMEOUT);
  }

  private YoutubeVideoSource source(
      List<String> channels, int searchBudget, int unitBudget, Duration readTimeout) {
    YoutubeProperties properties =
        new YoutubeProperties(
            YoutubePropertiesTest.FAKE_KEY,
            URI.create("http://localhost:9/youtube/v3"),
            channels,
            searchBudget,
            unitBudget,
            25,
            Duration.ofSeconds(2),
            List.of(YoutubeItems.RETROSPECTIVE));
    quota = spy(new YoutubeQuota(properties, clock));
    return new YoutubeVideoSource(
        properties,
        client,
        quota,
        YoutubeItems.mapper(channels, "http://localhost:5173", List.of(), List.of("youtube")),
        new MediaLookupProperties(6, readTimeout));
  }

  private YoutubeVideoSource source() {
    return source(List.of(CHANNEL), 80, 8000);
  }

  private static List<String> ids(SourceResult result, EventContext event) {
    return result.byEvent().get(event.eventId()).stream()
        .map(Candidate::item)
        .map(item -> item.record().id())
        .toList();
  }

  private void neverReserved(Bucket bucket) {
    verify(quota, never()).reserve(eq(bucket), anyInt());
    verify(quota, never()).reserveOn(eq(bucket), anyInt());
  }

  // ---- nothing to reserve ----

  @Test
  void eventsWithoutTitlesAreOkWithNoReservationAndNoRequest() {
    YoutubeVideoSource source = source();
    List<EventContext> untitled = List.of(event('a', null), event('b', " "), event('c', " "));

    SourceResult result = source.find(DAY, untitled, false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(result.byEvent()).hasSize(3).allSatisfy((id, list) -> assertThat(list).isEmpty());
    neverReserved(Bucket.SEARCH);
    neverReserved(Bucket.UNITS);
    assertThat(client.searches).isEmpty();
    assertThat(client.lists).isEmpty();
  }

  @Test
  void aSearchWithoutIdsIsOkWithNoUnitReservationAndNoVideosCall() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Ok<>(List.of());

    SourceResult result = source.find(DAY, List.of(A), false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(ids(result, A)).isEmpty();
    assertThat(client.searches).containsExactly(CHANNEL + " " + GAMES);
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(1);
    neverReserved(Bucket.UNITS);
    assertThat(client.lists).isEmpty();
  }

  // ---- reservations ----

  @Test
  void the80thSearchIsSentAndThe81stIsSkippedWithoutARequest() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Ok<>(List.of());
    assertThat(quota.reserve(Bucket.SEARCH, 79)).isTrue();

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.OK);
    SourceResult skipped = source.find(DAY, List.of(A), false);

    assertThat(skipped.status()).isEqualTo(Status.QUOTA_SKIPPED);
    assertThat(skipped.byEvent()).isEmpty();
    assertThat(client.searches).hasSize(1);
  }

  @Test
  void threeEventsWith78SearchesUsedSendNothing() {
    YoutubeVideoSource source = source();
    assertThat(quota.reserve(Bucket.SEARCH, 78)).isTrue();

    assertThat(source.find(DAY, List.of(A, B, C), false).status()).isEqualTo(Status.QUOTA_SKIPPED);
    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(78);
    assertThat(client.searches).isEmpty();
  }

  @Test
  void eachEventIsSearchedInEachChannelAndAllSearchesAreReservedAtOnce() {
    YoutubeVideoSource source = source(List.of(CHANNEL, SECOND_CHANNEL), 80, 8000);
    client.search = (channel, q) -> new Response.Ok<>(List.of());

    source.find(DAY, List.of(A, B, event('c', "")), false);

    verify(quota).reserveOn(Bucket.SEARCH, 4);
    assertThat(client.searches)
        .containsExactlyInAnyOrder(
            CHANNEL + " " + GAMES,
            SECOND_CHANNEL + " " + GAMES,
            CHANNEL + " 시험 문서 나",
            SECOND_CHANNEL + " 시험 문서 나");
  }

  @Test
  void seventyFiveIdsAreTwoVideosCallsOfOneUnitEach() {
    YoutubeVideoSource source = source();
    Map<String, Integer> offset = Map.of(GAMES, 0, "시험 문서 나", 25, "시험 문서 다", 50);
    client.search =
        (channel, q) ->
            new Response.Ok<>(
                IntStream.range(0, 25)
                    .mapToObj(i -> "Abc123xy%03d".formatted(offset.get(q) + i))
                    .toList());
    client.videos = ids -> new Response.Ok<>(List.of());

    SourceResult result = source.find(DAY, List.of(A, B, C), false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(client.lists).extracting(List::size).containsExactlyInAnyOrder(50, 25);
    assertThat(quota.used(Bucket.UNITS)).isEqualTo(2);
    verify(quota).reserveOn(Bucket.UNITS, 2);
  }

  @Test
  void anIdFoundForTwoEventsIsAskedOnce() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Ok<>(List.of("Abc123xyz_0", "Abc123xyz_2"));
    client.videos = ids -> new Response.Ok<>(List.of());

    source.find(DAY, List.of(A, B), false);

    assertThat(client.lists).containsExactly(List.of("Abc123xyz_0", "Abc123xyz_2"));
  }

  @Test
  void noUnitLeftSkipsAfterTheSearch() {
    YoutubeVideoSource source = source(List.of(CHANNEL), 80, 0);
    client.search = (channel, q) -> new Response.Ok<>(List.of("Abc123xyz_0"));

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.QUOTA_SKIPPED);
    assertThat(client.lists).isEmpty();
  }

  // ---- outcomes ----

  @Test
  void aFailedSearchIsFailedWithoutMarkingTheQuota() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Failed<>();

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.FAILED);
    assertThat(client.lists).isEmpty();
    verify(quota, never()).exhausted(eq(Bucket.SEARCH), any(LocalDate.class));
    verify(quota, never()).exhausted(Bucket.SEARCH);
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
  }

  @Test
  void aSearchQuotaAnswerMarksSearchSpentAndSkipsTheVideosCall() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.QuotaExceeded<>();

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.QUOTA_SKIPPED);
    assertThat(client.lists).isEmpty();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.reserve(Bucket.UNITS, 1)).isTrue();
  }

  /** Decision of T5 (1): a failure wins (partial), and the quota answer still marks the bucket. */
  @Test
  void aQuotaAnswerMixedWithAFailureIsFailedAndMarksSearchSpent() {
    YoutubeVideoSource source = source();
    client.search =
        (channel, q) -> q.equals(GAMES) ? new Response.QuotaExceeded<>() : new Response.Failed<>();

    assertThat(source.find(DAY, List.of(A, B), false).status()).isEqualTo(Status.FAILED);
    assertThat(client.lists).isEmpty();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
  }

  @Test
  void aVideosQuotaAnswerMarksUnitsSpent() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Ok<>(List.of("Abc123xyz_0"));
    client.videos = ids -> new Response.QuotaExceeded<>();

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.QUOTA_SKIPPED);
    assertThat(quota.reserve(Bucket.UNITS, 1)).isFalse();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isTrue();
  }

  @Test
  void aFailedVideosCallIsFailed() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> new Response.Ok<>(List.of("Abc123xyz_0"));
    client.videos = ids -> new Response.Failed<>();

    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.FAILED);
    assertThat(quota.reserve(Bucket.UNITS, 1)).isTrue();
  }

  @Test
  void aClientExceptionIsThrownToTheSearch() {
    YoutubeVideoSource source = source();
    client.search =
        (channel, q) -> {
          throw new IllegalStateException("synthetic");
        };

    assertThatIllegalStateException().isThrownBy(() -> source.find(DAY, List.of(A), false));
  }

  /**
   * M3a5 review recommendation 1: the 403 of a request sent before the reset is about the old day.
   */
  @Test
  void aQuotaAnswerArrivingAfterPacificMidnightLeavesTheNewDayOpen() {
    YoutubeVideoSource source = source();
    // from 12:00 PDT to 23:59:59 PDT of 2026-06-10 (Pacific midnight is 2026-06-11T07:00Z)
    clock.advance(Duration.ofHours(12).minusSeconds(1));
    client.search =
        (channel, q) -> {
          clock.advance(Duration.ofSeconds(1)); // the answer arrives at 00:00:00 of 06-11
          return new Response.QuotaExceeded<>();
        };

    assertThat(clock.instant().atZone(PACIFIC).toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    assertThat(source.find(DAY, List.of(A), false).status()).isEqualTo(Status.QUOTA_SKIPPED);

    assertThat(clock.instant().atZone(PACIFIC).toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 11));
    assertThat(quota.reserve(Bucket.SEARCH, 1)).as("the new day is not marked spent").isTrue();
  }

  @Test
  void aQuotaAnswerOnTheSameDayStillMarksIt() {
    YoutubeVideoSource source = source();
    client.search =
        (channel, q) -> {
          clock.advance(Duration.ofHours(4)); // 16:00 Pacific, same day
          return new Response.QuotaExceeded<>();
        };

    source.find(DAY, List.of(A), false);

    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
  }

  // ---- candidates ----

  @Test
  void eachEventGetsItsCandidatesInItsSearchOrderAndIdsMissingFromVideosAreDropped() {
    YoutubeVideoSource source = source();
    client.search =
        (channel, q) ->
            new Response.Ok<>(
                q.equals(GAMES)
                    ? List.of("Abc123xyz_9", "Abc123xyz_2", "Abc123xyz_0")
                    : List.of("Abc123xyz_0"));
    // answered in another order, with an item nobody asked for
    client.videos =
        ids ->
            new Response.Ok<>(
                List.of(
                    YoutubeItems.item("Abc123xyz_0"),
                    YoutubeItems.item("Abc123xyz_4"),
                    YoutubeItems.item("Abc123xyz_2")));
    EventContext abroad =
        new EventContext(B.eventId(), DAY, B.title(), null, B.summary(), null, "world");

    SourceResult result = source.find(DAY, List.of(A, abroad), false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(result.englishLabels()).isEmpty();
    assertThat(client.lists).containsExactly(List.of("Abc123xyz_9", "Abc123xyz_2", "Abc123xyz_0"));
    assertThat(ids(result, A)).containsExactly("youtube-Abc123xyz_2", "youtube-Abc123xyz_0");
    assertThat(ids(result, abroad)).containsExactly("youtube-Abc123xyz_0");
    assertThat(result.byEvent().get(abroad.eventId()).getFirst().item().record().placeRef())
        .isEqualTo("world");
    assertThat(result.byEvent().get(A.eventId()))
        .allSatisfy(c -> assertThat(c.origin()).isNotNull());
  }

  @Test
  void theKeyIsYoutube() {
    assertThat(source().key()).isEqualTo("youtube");
  }

  /** T5 (2): without a key (missing, empty or blank) the source is not registered. */
  @ParameterizedTest
  @CsvSource(
      value = {"<missing>, false", "'', false", "'   ', false", "test-key-not-real, true"},
      nullValues = "<missing>")
  void theSourceIsRegisteredOnlyWithAKey(String key, boolean registered) {
    MockEnvironment environment = new MockEnvironment();
    if (key != null) {
      environment.setProperty("youtube.api-key", key);
    }
    ConditionContext context = mock(ConditionContext.class);
    when(context.getEnvironment()).thenReturn(environment);

    assertThat(new YoutubeVideoSource.KeySet().matches(context, null)).isEqualTo(registered);
  }

  // ---- time and interrupts ----

  @Test
  void aSearchLongerThanTheLookupDeadlineEndsInTimeAsFailed() {
    YoutubeVideoSource source = source();
    client.search = (channel, q) -> client.silent();

    long start = System.nanoTime();
    SourceResult result = source.find(DAY, List.of(A, B), false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(result.status()).isEqualTo(Status.FAILED);
    assertThat(took).isLessThan(READ_TIMEOUT.plus(Deadline.CANCEL_GRACE));
    assertThat(client.interrupted).as("the inner calls were stopped").hasSize(2);
  }

  @Test
  void anInterruptedSearchClosesItsInnerCalls() throws Exception {
    YoutubeVideoSource patient = source(List.of(CHANNEL), 80, 8000, Duration.ofSeconds(10));
    client.search = (channel, q) -> client.silent();
    AtomicReference<SourceResult> seen = new AtomicReference<>();
    AtomicBoolean flagKept = new AtomicBoolean();
    Thread caller =
        Thread.ofVirtual()
            .start(
                () -> {
                  seen.set(patient.find(DAY, List.of(A), false));
                  flagKept.set(Thread.currentThread().isInterrupted());
                });
    while (client.searches.isEmpty()) {
      Thread.sleep(10);
    }

    long start = System.nanoTime();
    caller.interrupt();
    caller.join(Duration.ofSeconds(5));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(caller.isAlive()).isFalse();
    assertThat(took).isLessThan(Duration.ofMillis(500));
    assertThat(seen.get().status()).isEqualTo(Status.FAILED);
    assertThat(flagKept.get()).as("the interrupt flag is kept").isTrue();
    for (int i = 0; i < 50 && client.interrupted.isEmpty(); i++) {
      Thread.sleep(10);
    }
    assertThat(client.interrupted).as("the inner call was stopped").hasSize(1);
  }

  /** Records the calls; answers through the functions the test sets. */
  private static final class FakeClient implements YoutubeClient {

    final List<String> searches = new CopyOnWriteArrayList<>();
    final List<List<String>> lists = new CopyOnWriteArrayList<>();
    final List<String> interrupted = new CopyOnWriteArrayList<>();
    BiFunction<String, String, Response<List<String>>> search =
        (channel, q) -> {
          throw new AssertionError("no search expected");
        };
    Function<List<String>, Response<List<JsonNode>>> videos =
        ids -> {
          throw new AssertionError("no videos call expected");
        };

    @Override
    public Response<List<String>> search(String channelId, String q) {
      searches.add(channelId + " " + q);
      return search.apply(channelId, q);
    }

    @Override
    public Response<List<JsonNode>> videos(List<String> ids) {
      lists.add(List.copyOf(ids));
      return videos.apply(ids);
    }

    /** Waits until interrupted, then fails like the real client. */
    <T> Response<T> silent() {
      try {
        Thread.sleep(Duration.ofSeconds(20));
      } catch (InterruptedException e) {
        interrupted.add(Thread.currentThread().getName());
        Thread.currentThread().interrupt();
      }
      return new Response.Failed<>();
    }
  }
}
