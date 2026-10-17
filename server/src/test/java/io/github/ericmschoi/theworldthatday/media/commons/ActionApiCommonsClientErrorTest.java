package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3c T0 (3) and (4) on the Commons client: any {@code error} is a failure with or without a
 * code (PR #17 review recommendation 2), the log line names only the code, never the API's {@code
 * info} text, and the day category request is sent once (decision 1 (a)). Responses are synthetic.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiCommonsClientErrorTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final Duration LOOKUP = Duration.ofSeconds(2);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final CommonsClient client =
      new ActionApiCommonsClient(
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
                  URI.create(commons.baseUrl() + "/w/api.php")),
              clock),
          new CommonsProperties(URI.create(commons.baseUrl() + PATH)),
          JsonMapper.builder().build());

  private static void answer(String body) {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(body)));
  }

  /** The failure lines, from the message on (without the logger's own prefix). */
  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Wikimedia call failed"))
        .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
        .toList();
  }

  @ParameterizedTest(name = "{0} is a failure")
  @ValueSource(
      strings = {
        "{\"error\":{\"info\":\"x\"}}",
        "{\"error\":\"x\"}",
        "{\"error\":{\"code\":{\"info\":\"x\"}}}",
        "{\"error\":{\"code\":\" \",\"info\":\"x\"}}",
        "{\"error\":null}",
        "{\"error\":{\"info\":\"x\"},\"query\":{\"pages\":[]}}"
      })
  void errorWithoutACodeIsAFailureLoggedWithoutItsText(String body, CapturedOutput output) {
    answer(body);

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons day category cause=api error");
  }

  @Test
  void errorWithACodeNamesOnlyTheCode(CapturedOutput output) {
    answer("{\"error\":{\"code\":\"badvalue\",\"info\":\"x\"}}");

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons day category"
                + " cause=api error badvalue");
  }

  @Test
  void ratelimitedStillBlocksTheHost() {
    answer("{\"error\":{\"code\":\"ratelimited\",\"info\":\"x\"}}");

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();
    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(1);
  }

  @Test
  void answerWithoutAnErrorKeyIsStillASuccess() {
    answer("{\"batchcomplete\":true,\"warnings\":{\"main\":{\"warnings\":\"x\"}}}");

    assertThat(client.dayCategory(LocalDate.of(1995, 3, 15), LOOKUP).failed()).isFalse();
  }

  @Test
  void lateAnswerIsOneRequestWithOneWarning(CapturedOutput output) {
    commons.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"batchcomplete\":true}").withFixedDelay(1_500)));

    assertThat(client.dayCategory(LEAP_DAY, Duration.ofMillis(200)).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).as("no retry on the media path").hasSize(1);
    assertThat(failures(output)).hasSize(1);
  }

  @Test
  void unavailableWithoutRetryAfterIsOneRequest() {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(1);
  }
}
