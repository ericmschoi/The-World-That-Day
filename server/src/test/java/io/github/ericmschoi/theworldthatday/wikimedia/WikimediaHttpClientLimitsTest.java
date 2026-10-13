package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Ok;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Reason;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Unavailable;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1b T1 (2)~(3): at most {@code max-concurrent-requests} Wikimedia requests in flight
 * across every host, a request that gets no slot within the read timeout is never sent, and the
 * host-block entry point used for the Action API error {@code ratelimited}.
 */
@ExtendWith(OutputCaptureExtension.class)
class WikimediaHttpClientLimitsTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";
  private static final String USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";
  private static final int DELAY_MS = 200;

  /**
   * For tests that count requests but are not about the read timeout: a timed-out first attempt on
   * a stalled machine would add a retry (plan M3a1b T3 flaky-test check).
   */
  private static final Duration NOT_UNDER_TEST = Duration.ofSeconds(2);

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  private WikimediaHttpClient client(Duration readTimeout, int maxConcurrent) {
    return new WikimediaHttpClient(
        new WikimediaProperties(
            USER_AGENT,
            Duration.ofSeconds(2),
            readTimeout,
            1,
            Duration.ofSeconds(120),
            maxConcurrent,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(wikimedia.baseUrl() + PATH)),
        clock);
  }

  private static URI uri(String host) {
    return URI.create(
        "http://" + host + ":" + wikimedia.getPort() + PATH + "?action=parse&page=x&format=json");
  }

  private static int requestCount() {
    return wikimedia.getAllServeEvents().size();
  }

  /** Most requests WireMock was serving at once, from receive times and the fixed delay. */
  private static int maxInFlight() {
    List<long[]> intervals = new ArrayList<>();
    wikimedia
        .getAllServeEvents()
        .forEach(
            event -> {
              long received = event.getRequest().getLoggedDate().getTime();
              intervals.add(new long[] {received, received + DELAY_MS});
            });
    int max = 0;
    for (long[] interval : intervals) {
      long at = interval[0];
      int inFlight =
          (int) intervals.stream().filter(other -> other[0] <= at && at < other[1]).count();
      max = Math.max(max, inFlight);
    }
    return max;
  }

  @Test
  void sixSimultaneousCallsOnTwoHostsNeverHaveMoreThanThreeInFlight() throws Exception {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}").withFixedDelay(DELAY_MS)));
    WikimediaHttpClient client = client(Duration.ofSeconds(2), 3);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(6);
    try {
      List<Future<WikimediaResponse>> calls = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
        String host = i % 2 == 0 ? "localhost" : "127.0.0.1"; // the limit is shared by hosts
        calls.add(
            pool.submit(
                () -> {
                  start.await();
                  return client.get("kowiki", "9월 17일", uri(host));
                }));
      }
      long begin = System.nanoTime();
      start.countDown();
      for (Future<WikimediaResponse> call : calls) {
        assertThat(call.get(10, TimeUnit.SECONDS)).isEqualTo(new Ok("{}"));
      }
      Duration elapsed = Duration.ofNanos(System.nanoTime() - begin);

      assertThat(requestCount()).isEqualTo(6);
      assertThat(maxInFlight()).isLessThanOrEqualTo(3);
      assertThat(elapsed).as("two rounds of three").isGreaterThanOrEqualTo(Duration.ofMillis(400));
      assertThat(client.permits().availablePermits()).isEqualTo(3);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void noFreeSlotWithinTheReadTimeoutSendsNothing() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    WikimediaHttpClient client = client(Duration.ofMillis(300), 3);
    int taken = client.permits().drainPermits();

    long begin = System.nanoTime();
    WikimediaResponse response = client.get("kowiki", "9월 17일", uri("localhost"));
    Duration waited = Duration.ofNanos(System.nanoTime() - begin);

    assertThat(taken).isEqualTo(3);
    assertThat(response).isEqualTo(new Unavailable(Reason.BUSY));
    assertThat(waited)
        .as("waits for the read timeout")
        .isGreaterThanOrEqualTo(Duration.ofMillis(300));
    assertThat(requestCount()).as("never reaches WireMock").isZero();

    client.permits().release(taken);
    assertThat(client.get("kowiki", "9월 17일", uri("localhost"))).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).isEqualTo(1);
  }

  @Test
  void retryTakesASlotAgainWithOneSlot() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("recovered"));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs("recovered")
            .willReturn(ok("{}")));
    WikimediaHttpClient client = client(NOT_UNDER_TEST, 1);

    assertThat(client.get("kowiki", "9월 17일", uri("localhost"))).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).isEqualTo(2);
    assertThat(client.permits().availablePermits()).isEqualTo(1);
  }

  static Stream<Arguments> outcomes() {
    return Stream.of(
        Arguments.of("200", ok("{}")),
        Arguments.of("404", aResponse().withStatus(404)),
        Arguments.of("429", aResponse().withStatus(429)),
        Arguments.of("503 twice", aResponse().withStatus(503)),
        Arguments.of("timeout twice", ok("{}").withFixedDelay(600)),
        Arguments.of("connection reset", aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("outcomes")
  void everyOutcomeGivesTheSlotBack(String name, ResponseDefinitionBuilder response) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(response));
    WikimediaHttpClient client = client(Duration.ofMillis(300), 3);

    client.get("kowiki", "9월 17일", uri("localhost"));

    assertThat(client.permits().availablePermits()).isEqualTo(3);
  }

  @Test
  void hostBlockedWhileWaitingForASlotIsNotSent() throws Exception {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    WikimediaHttpClient client = client(Duration.ofSeconds(5), 1);
    client.permits().acquire();
    CompletableFuture<WikimediaResponse> waiting =
        CompletableFuture.supplyAsync(() -> client.get("kowiki", "9월 17일", uri("localhost")));
    while (!client.permits().hasQueuedThreads()) {
      Thread.onSpinWait();
    }

    client.blockHost(uri("localhost"));
    client.permits().release();

    assertThat(waiting.get(5, TimeUnit.SECONDS)).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).isZero();
  }

  @Test
  void blockHostSkipsOnlyThatHostForThirtySeconds() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    WikimediaHttpClient client = client(NOT_UNDER_TEST, 3);

    client.blockHost(uri("localhost"));

    assertThat(client.get("kowiki", "9월 17일", uri("localhost")))
        .isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(client.get("commons", "9월 17일", uri("127.0.0.1"))).isEqualTo(new Ok("{}"));
    clock.advance(Duration.ofSeconds(29));
    assertThat(client.get("kowiki", "9월 17일", uri("localhost")))
        .isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).isEqualTo(1);

    clock.advance(Duration.ofSeconds(2));
    assertThat(client.get("kowiki", "9월 17일", uri("localhost"))).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).isEqualTo(2);
  }

  @Test
  void blockHostIgnoresAUriWithoutHost() {
    WikimediaHttpClient client = client(NOT_UNDER_TEST, 3);

    client.blockHost(null);
    client.blockHost(URI.create("/w/api.php"));

    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    assertThat(client.get("kowiki", "9월 17일", uri("localhost"))).isEqualTo(new Ok("{}"));
  }

  @Test
  void interruptWhileWaitingForASlotIsNotBusyAndKeepsTheFlag(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    WikimediaHttpClient client = client(Duration.ofSeconds(2), 3);
    client.permits().drainPermits();

    Thread.currentThread().interrupt();
    WikimediaResponse response = client.get("kowiki", "9월 17일", uri("localhost"));
    boolean stillInterrupted = Thread.interrupted(); // also clears it for the next test

    assertThat(response).isEqualTo(new Unavailable(Reason.INTERRUPTED));
    assertThat(stillInterrupted).isTrue();
    assertThat(requestCount()).isZero();
    assertThat(output.getAll())
        .contains("interrupted while waiting for a request slot")
        .doesNotContain("no free request slot");
  }
}
