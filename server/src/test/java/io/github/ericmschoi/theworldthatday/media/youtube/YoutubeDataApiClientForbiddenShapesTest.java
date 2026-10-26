package io.github.ericmschoi.theworldthatday.media.youtube;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeClient.Response;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a5 T7 (1) and T2 (4): the shape of a 403 body is not in the official docs ([91] lists the
 * reason {@code quotaExceeded} without a body). Only {@code error.errors[].reason} equal to {@code
 * quotaExceeded} counts as quota. A body that is not JSON, JSON of another shape, or a reason that
 * is not that exact text is an ordinary {@link Response.Failed}, with one WARN line and no quota
 * mark by the caller. Synthetic bodies; WireMock only.
 */
@ExtendWith(OutputCaptureExtension.class)
class YoutubeDataApiClientForbiddenShapesTest {

  @RegisterExtension
  static final WireMockExtension youtube =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String API = "/youtube/v3";

  private final YoutubeClient client =
      YoutubeDataApiClientTest.client(URI.create(youtube.baseUrl() + API), Duration.ofMillis(400));

  @AfterEach
  void nothingLeftTheStubs() {
    assertThat(youtube.findAllUnmatchedRequests()).isEmpty();
  }

  static Stream<Arguments> bodies() {
    return Stream.of(
        Arguments.of("plain text", "quotaExceeded"),
        Arguments.of("HTML page", "<html><body>quotaExceeded</body></html>"),
        Arguments.of("truncated JSON", "{\"error\":{\"errors\":[{\"reason\":\"quotaExceeded\""),
        Arguments.of("empty JSON object", "{}"),
        Arguments.of("JSON array", "[]"),
        Arguments.of("JSON string", "\"quotaExceeded\""),
        Arguments.of("JSON null", "null"),
        Arguments.of("error is a string", "{\"error\":\"quotaExceeded\"}"),
        Arguments.of("errors is a string", "{\"error\":{\"errors\":\"quotaExceeded\"}}"),
        Arguments.of(
            "errors is an object", "{\"error\":{\"errors\":{\"reason\":\"quotaExceeded\"}}}"),
        Arguments.of("errors is empty", "{\"error\":{\"code\":403,\"errors\":[]}}"),
        Arguments.of("entry has no reason", "{\"error\":{\"errors\":[{\"domain\":\"youtube\"}]}}"),
        Arguments.of("reason is a number", "{\"error\":{\"errors\":[{\"reason\":403}]}}"),
        Arguments.of("reason is null", "{\"error\":{\"errors\":[{\"reason\":null}]}}"),
        Arguments.of("entry is null", "{\"error\":{\"errors\":[null]}}"),
        Arguments.of(
            "reason in the wrong place",
            "{\"error\":{\"reason\":\"quotaExceeded\",\"errors\":[]}}"),
        Arguments.of(
            "reason only in the message",
            "{\"error\":{\"message\":\"quotaExceeded\",\"errors\":[{\"reason\":\"forbidden\"}]}}"),
        Arguments.of(
            "another quota-like reason",
            "{\"error\":{\"errors\":[{\"reason\":\"dailyLimitExceeded\"}]}}"),
        Arguments.of(
            "reason in another case", "{\"error\":{\"errors\":[{\"reason\":\"QUOTAEXCEEDED\"}]}}"));
  }

  @ParameterizedTest(name = "403, {0}: Failed")
  @MethodSource("bodies")
  void aForbiddenAnswerThatDoesNotNameQuotaExceededIsAFailure(
      String label, String body, CapturedOutput output) {
    youtube.stubFor(
        get(urlPathEqualTo(API + "/search"))
            .willReturn(aResponse().withStatus(403).withBody(body)));
    youtube.stubFor(
        get(urlPathEqualTo(API + "/videos"))
            .willReturn(aResponse().withStatus(403).withBody(body)));

    assertThat(client.search("UCsyntheticChannelId0001", "x")).isEqualTo(new Response.Failed<>());
    assertThat(client.videos(List.of("Abc123xyz_0"))).isEqualTo(new Response.Failed<>());

    assertThat(youtube.getAllServeEvents()).as("one request per call, no retry").hasSize(2);
    assertThat(output.getAll().lines().filter(l -> l.contains("YouTube call failed: ")).toList())
        .hasSize(2)
        .allSatisfy(line -> assertThat(line).endsWith("cause=status 403"));
    assertThat(output.getAll()).doesNotContain("quotaExceeded cause", "cause=quotaExceeded");
  }
}
