package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3c T1 (3) on the Commons file batch: the read timeout is the caller's, and a late answer
 * is one request (no retry) and one WARN line with the label only. The client's own read timeout
 * (2s) would have waited for the answer. Synthetic file names.
 */
@ExtendWith(OutputCaptureExtension.class)
class CommonsFilesReadTimeoutTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;

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

  @Test
  void lateFileBatchIsOneRequestWithTheGivenReadTimeout(CapturedOutput output) {
    commons.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                okJson("{\"batchcomplete\":true,\"query\":{\"pages\":[]}}").withFixedDelay(1_500)));

    long start = System.nanoTime();
    Files files = client.files(List.of("Synthetic a.jpg"), Duration.ofMillis(200));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(files).isEqualTo(new Files(Map.of(), true));
    assertThat(commons.getAllServeEvents()).as("no retry on the media path").hasSize(1);
    assertThat(took).as("one 200 ms attempt").isLessThan(Duration.ofMillis(1_000));
    List<String> failures =
        output
            .getAll()
            .lines()
            .filter(l -> l.contains("Wikimedia call failed"))
            .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
            .toList();
    assertThat(failures)
        .hasSize(1)
        .allSatisfy(l -> assertThat(l).contains("source=commons title=commons files of 1 cause="));
    assertThat(output.getAll()).doesNotContain("Synthetic a", "File:");
  }
}
