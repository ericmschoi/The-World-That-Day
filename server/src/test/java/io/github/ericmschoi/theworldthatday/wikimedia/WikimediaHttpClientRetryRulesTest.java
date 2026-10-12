package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Retry and 429 rules that the T1 tests leave implicit (plan M3a1 "외부 호출 공통 규칙", T4): only timeouts
 * and 502/503/504 are retried, other statuses and broken connections are not, a 429 blocks only its
 * own host, and the disabled-source WARN comes from construction (startup), not from calls.
 */
@ExtendWith(OutputCaptureExtension.class)
class WikimediaHttpClientRetryRulesTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/w/api.php";
  private static final String USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  private WikimediaHttpClient client(String userAgent) {
    return new WikimediaHttpClient(
        new WikimediaProperties(
            userAgent,
            Duration.ofSeconds(2),
            Duration.ofMillis(300),
            1,
            Duration.ofSeconds(120),
            URI.create(wikimedia.baseUrl() + PATH)),
        clock);
  }

  private static URI uri(String host) {
    return URI.create(
        "http://" + host + ":" + wikimedia.getPort() + PATH + "?action=parse&page=x&format=json");
  }

  private static WikimediaResponse call(WikimediaHttpClient client) {
    return client.get("kowiki", "1988년", uri("localhost"));
  }

  private static int requestCount() {
    return wikimedia.getAllServeEvents().size();
  }

  @ParameterizedTest(name = "status {0} is not retried")
  @ValueSource(ints = {400, 403, 500, 501})
  void nonRetryableStatusesAreSentOnce(int status) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(status)));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Unavailable(Reason.HTTP_ERROR));
    assertThat(requestCount()).isEqualTo(1);
  }

  @ParameterizedTest(name = "status {0} then 200 succeeds on the retry")
  @ValueSource(ints = {502, 504})
  void otherRetryableStatusesThenOkSucceed(int status) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(status))
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
  void readTimeoutThenOkSucceedsOnTheRetry() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("slow")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(ok("{\"late\":true}").withFixedDelay(1500))
            .willSetStateTo("fast"));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("slow")
            .whenScenarioStateIs("fast")
            .willReturn(ok("{\"parse\":{}}")));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Ok("{\"parse\":{}}"));
    assertThat(requestCount()).isEqualTo(2);
  }

  @Test
  void brokenConnectionIsNotRetriedAndDoesNotThrow() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThat(call(client(USER_AGENT))).isEqualTo(new Unavailable(Reason.IO_ERROR));
    assertThat(requestCount()).isEqualTo(1);
  }

  @Test
  void tooManyRequestsBlocksOnlyThatHost() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withHeader("Host", containing("localhost"))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).withHeader("Host", containing("127.0.0.1")).willReturn(ok("{}")));
    WikimediaHttpClient client = client(USER_AGENT);

    assertThat(client.get("kowiki", "1988년", uri("localhost")))
        .isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(client.get("commons", "1988년", uri("127.0.0.1"))).isEqualTo(new Ok("{}"));
    assertThat(client.get("kowiki", "1988년", uri("localhost")))
        .isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).as("blocked host gets no second request").isEqualTo(2);
  }

  @Test
  void disabledWarningIsLoggedOnceAtConstructionWithoutAnyCall(CapturedOutput output) {
    client("");

    assertThat(output.getAll().lines().filter(line -> line.contains("WIKIMEDIA_USER_AGENT")))
        .singleElement()
        .satisfies(line -> assertThat(line).contains("WARN"));
    assertThat(requestCount()).isZero();
  }

  @Test
  void configuredClientLogsNoDisabledWarning(CapturedOutput output) {
    client(USER_AGENT);

    assertThat(output.getAll()).doesNotContain("WIKIMEDIA_USER_AGENT").doesNotContain(USER_AGENT);
  }
}
