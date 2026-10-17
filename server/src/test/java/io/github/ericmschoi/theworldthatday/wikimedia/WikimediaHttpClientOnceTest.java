package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Ok;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Reason;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Unavailable;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a3c T0 (3), decision 1 (a): the media path's call is sent once. A late answer and a 503
 * without Retry-After are one request and one WARN line each; a 429 still blocks the host. The
 * retrying overload is covered, unchanged, by {@code WikimediaHttpClient*Test}.
 */
@ExtendWith(OutputCaptureExtension.class)
class WikimediaHttpClientOnceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/commons/w/api.php";
  private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  private final WikimediaHttpClient client =
      new WikimediaHttpClient(
          new WikimediaProperties(
              WikimediaWireMockTest.TEST_USER_AGENT,
              Duration.ofSeconds(2),
              Duration.ofSeconds(2),
              1,
              Duration.ofSeconds(120),
              3,
              Duration.ofDays(7),
              Duration.ofSeconds(5),
              URI.create(wikimedia.baseUrl() + "/w/api.php")),
          clock);

  private static URI uri() {
    return URI.create(wikimedia.baseUrl() + PATH + "?action=query&format=json");
  }

  private WikimediaResponse once() {
    return client.getOnce("commons", "commons day category", uri(), READ_TIMEOUT);
  }

  private static int requestCount() {
    return wikimedia.getAllServeEvents().size();
  }

  private static List<String> warnings(CapturedOutput output) {
    return output.getAll().lines().filter(l -> l.contains("Wikimedia call failed")).toList();
  }

  @Test
  void answerLaterThanTheReadTimeoutIsOneRequestAndOneWarning(CapturedOutput output) {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(ok("{\"late\":true}").withFixedDelay(1_500)));

    long start = System.nanoTime();
    WikimediaResponse response = once();
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(response).isEqualTo(new Unavailable(Reason.TIMEOUT));
    assertThat(requestCount()).isEqualTo(1);
    assertThat(took).as("one attempt, not two").isLessThan(Duration.ofMillis(1_000));
    assertThat(warnings(output))
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).contains("source=commons title=commons day category"));
  }

  @Test
  void unavailableWithoutRetryAfterIsOneRequestAndOneWarning(CapturedOutput output) {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThat(once()).isEqualTo(new Unavailable(Reason.HTTP_ERROR));
    assertThat(requestCount()).isEqualTo(1);
    assertThat(warnings(output)).hasSize(1).allSatisfy(l -> assertThat(l).contains("status 503"));
  }

  @Test
  void tooManyRequestsStillBlocksTheHost() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));

    assertThat(once()).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(once()).isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requestCount()).as("the second call is never sent").isEqualTo(1);
  }

  @Test
  void quickAnswerIsReturnedWithTheUserAgent() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(ok("{\"batchcomplete\":true}")));

    assertThat(once()).isEqualTo(new Ok("{\"batchcomplete\":true}"));
    assertThat(wikimedia.getAllServeEvents().getFirst().getRequest().getHeader("User-Agent"))
        .isEqualTo(WikimediaWireMockTest.TEST_USER_AGENT);
  }
}
