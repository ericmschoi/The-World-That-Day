package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Shared Wikimedia call rules against WireMock (plan M3a1 T1 (2)~(6)). */
@ExtendWith(OutputCaptureExtension.class)
class WikimediaHttpClientTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";
  private static final String USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  /**
   * Tests that are not about the read timeout use 2 s: a timed-out first attempt on a stalled
   * machine would add a retry to their request counts (plan M3a1b T3 flaky-test check).
   */
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

  /** The test value of plan M3a1 T1 (4), for tests that check timeout behaviour. */
  private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(300);

  private WikimediaHttpClient client(String userAgent) {
    return client(userAgent, READ_TIMEOUT);
  }

  private WikimediaHttpClient client(String userAgent, Duration readTimeout) {
    WikimediaProperties properties =
        new WikimediaProperties(
            userAgent,
            Duration.ofSeconds(2),
            readTimeout,
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(wikimedia.baseUrl() + PATH));
    return new WikimediaHttpClient(properties, clock);
  }

  private WikimediaResponse call(WikimediaHttpClient client) {
    URI uri = URI.create(wikimedia.baseUrl() + PATH + "?action=parse&page=x&format=json");
    return client.get("kowiki", "9월 17일", uri);
  }

  private static int requestCount() {
    return wikimedia.getAllServeEvents().size();
  }

  @Test
  void okReturnsTheBodyAndSendsTheConfiguredUserAgent() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{\"parse\":{}}")));

    WikimediaResponse response = call(client(USER_AGENT));

    assertThat(response).isEqualTo(new Ok("{\"parse\":{}}"));
    wikimedia.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    wikimedia.verify(
        1, getRequestedFor(urlPathEqualTo(PATH)).withHeader("User-Agent", equalTo(USER_AGENT)));
  }

  @Test
  void utf8BodyIsDecoded() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                ok("{\"title\":\"9월 17일\"}").withHeader("Content-Type", "application/json")));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Ok("{\"title\":\"9월 17일\"}"));
  }

  @Test
  void emptyUserAgentSendsNothingAndWarnsOnceWithoutTheValue(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));

    WikimediaHttpClient disabled = client("   ");
    WikimediaResponse first = call(disabled);
    WikimediaResponse second = call(disabled);

    assertThat(first).isEqualTo(new Unavailable(Reason.DISABLED));
    assertThat(second).isEqualTo(new Unavailable(Reason.DISABLED));
    assertThat(requestCount()).isZero();
    assertThat(output.getAll().lines().filter(line -> line.contains("WIKIMEDIA_USER_AGENT")))
        .singleElement()
        .satisfies(line -> assertThat(line).contains("WARN").contains("disabled"));
  }

  @Test
  void configuredUserAgentIsNeverLogged(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

    call(client(USER_AGENT));

    assertThat(output.getAll()).doesNotContain(USER_AGENT).doesNotContain("WIKIMEDIA_USER_AGENT");
  }

  @Test
  void slowResponseTimesOutAfterOneRetryWithinTwoSeconds() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}").withFixedDelay(1500)));

    long start = System.nanoTime();
    WikimediaResponse response = call(client(USER_AGENT, SHORT_READ_TIMEOUT));
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(response).isEqualTo(new Unavailable(Reason.TIMEOUT));
    assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    assertThat(requestCount()).isEqualTo(2);
    wikimedia.verify(
        2, getRequestedFor(urlPathEqualTo(PATH)).withHeader("User-Agent", equalTo(USER_AGENT)));
  }

  /** Plan M3a1c: the event source compares timeout counts to tell a stalled host. */
  @Test
  void timedOutAttemptsAreCountedPerHostAndOtherFailuresAreNot() {
    WikimediaHttpClient client = client(USER_AGENT, SHORT_READ_TIMEOUT);
    URI uri = URI.create(wikimedia.baseUrl() + PATH);
    assertThat(client.timeouts(uri)).isZero();

    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}").withFixedDelay(1500)));
    call(client);
    assertThat(client.timeouts(uri)).as("first attempt and its retry").isEqualTo(2);
    assertThat(client.timeouts(URI.create("http://other.test/w/api.php"))).isZero();
    assertThat(client.timeouts(URI.create("/relative"))).isZero();

    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
    call(client);
    assertThat(client.timeouts(uri)).as("a 503 is not a timeout").isEqualTo(2);
  }

  @Test
  void unavailableThenOkSucceedsOnTheRetry() {
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
            .willReturn(ok("{\"parse\":{}}")));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Ok("{\"parse\":{}}"));
    assertThat(requestCount()).isEqualTo(2);
  }

  @Test
  void retryableStatusTwiceGivesUpAfterOneRetry() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(502)));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Unavailable(Reason.HTTP_ERROR));
    assertThat(requestCount()).isEqualTo(2);
  }

  @Test
  void notFoundIsNotRetried() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Unavailable(Reason.HTTP_ERROR));
    assertThat(requestCount()).isEqualTo(1);
  }

  @Test
  void tooManyRequestsBlocksTheHostForRetryAfterSeconds() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));
    WikimediaHttpClient client = client(USER_AGENT);

    assertThat(call(client)).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).as("429 is not retried").isEqualTo(1);

    clock.advance(Duration.ofSeconds(59));
    assertThat(call(client)).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).as("still blocked at 59s").isEqualTo(1);

    clock.advance(Duration.ofSeconds(2));
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));
    assertThat(call(client)).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).as("sent again at 61s").isEqualTo(2);
  }

  @Test
  void retryAfterAboveTheCapIsCutTo120Seconds() {
    assertBlockedFor("999", Duration.ofSeconds(120));
  }

  @Test
  void missingRetryAfterBlocksFor30Seconds() {
    assertBlockedFor(null, Duration.ofSeconds(30));
  }

  private void assertBlockedFor(String retryAfter, Duration window) {
    var tooMany = aResponse().withStatus(429);
    if (retryAfter != null) {
      tooMany = tooMany.withHeader("Retry-After", retryAfter);
    }
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(tooMany));
    WikimediaHttpClient client = client(USER_AGENT);
    call(client);
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{}")));

    clock.advance(window.minusSeconds(1));
    assertThat(call(client)).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).isEqualTo(1);

    clock.advance(Duration.ofSeconds(2));
    assertThat(call(client)).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).isEqualTo(2);
  }

  @Test
  void failureLogIsOneLineWithTitleAndStatusButNoUrl(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

    call(client(USER_AGENT));

    assertThat(output.getAll().lines().filter(line -> line.contains("Wikimedia call failed")))
        .singleElement()
        .satisfies(
            line ->
                assertThat(line)
                    .contains("WARN")
                    .contains("source=kowiki")
                    .contains("title=9월 17일")
                    .contains("status 404")
                    .doesNotContain("action=parse")
                    .doesNotContain("localhost"));
  }
}
