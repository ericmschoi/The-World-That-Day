package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3b T1 (2), (3) and (9): the day category query, empty and failed answers, the per-call
 * read timeout and log lines without a date, category or file name. Responses are synthetic.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiCommonsClientTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final Duration LOOKUP = Duration.ofSeconds(2);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private static final String TWO_PAGES =
      """
      {"batchcomplete": true, "query": {"pages": [
        {"pageid": 11, "ns": 6, "title": "File:Synthetic b.jpg", "imageinfo": [{"mime": "image/jpeg"}]},
        {"pageid": 12, "ns": 6, "title": "File:Synthetic a.jpg", "imageinfo": [{"mime": "image/jpeg"}]}
      ]}}
      """;

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final WikimediaHttpClient http = http();
  private final CommonsClient client =
      new ActionApiCommonsClient(
          http,
          new CommonsProperties(URI.create(commons.baseUrl() + PATH)),
          JsonMapper.builder().build());

  private WikimediaHttpClient http() {
    WikimediaProperties properties =
        new WikimediaProperties(
            WikimediaWireMockTest.TEST_USER_AGENT,
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(commons.baseUrl() + "/w/api.php"));
    return new WikimediaHttpClient(properties, clock);
  }

  private static void answer(String body) {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(body)));
  }

  private static Map<String, String> params(ServeEvent event) {
    Map<String, String> params = new LinkedHashMap<>();
    event
        .getRequest()
        .getQueryParams()
        .forEach((name, value) -> params.put(name, value.firstValue()));
    return params;
  }

  @Test
  void dayCategoryAsksForTheFilesOfTheDateWithTheirFileInfo() {
    answer(TWO_PAGES);

    Pages pages = client.dayCategory(LEAP_DAY, LOOKUP);

    assertThat(pages.failed()).isFalse();
    assertThat(pages.pages()).extracting(p -> p.path("pageid").asInt()).containsExactly(11, 12);
    ServeEvent request = commons.getAllServeEvents().getFirst();
    assertThat(commons.getAllServeEvents()).hasSize(1);
    assertThat(request.getRequest().getUrl()).startsWith(PATH + "?");
    assertThat(request.getRequest().getHeader("User-Agent"))
        .isEqualTo(WikimediaWireMockTest.TEST_USER_AGENT)
        .doesNotContain("@");
    assertThat(params(request))
        .containsExactlyInAnyOrderEntriesOf(
            Map.ofEntries(
                Map.entry("action", "query"),
                Map.entry("generator", "categorymembers"),
                Map.entry("gcmtitle", "Category:South Korea photographs taken on 2000-02-29"),
                Map.entry("gcmtype", "file"),
                Map.entry("gcmlimit", "20"),
                Map.entry("prop", "imageinfo"),
                Map.entry("iiprop", "url|mime|size|extmetadata"),
                Map.entry("iiurlwidth", "640"),
                Map.entry("format", "json"),
                Map.entry("formatversion", "2")));
  }

  @Test
  void categoryThatDoesNotExistIsEmptyAndNotAFailure() {
    answer("{\"batchcomplete\":true}");

    Pages pages = client.dayCategory(LocalDate.of(1995, 3, 15), LOOKUP);

    assertThat(pages.failed()).isFalse();
    assertThat(pages.pages()).isEmpty();
  }

  @Test
  void apiErrorIsAFailure() {
    answer("{\"error\":{\"code\":\"badvalue\",\"info\":\"synthetic\"}}");

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();
  }

  @Test
  void ratelimitedErrorBlocksTheHost() {
    answer("{\"error\":{\"code\":\"ratelimited\",\"info\":\"synthetic\"}}");

    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();
    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(1); // the second call is never sent
  }

  @Test
  void bodyThatIsNotAnObjectWithPagesIsAFailure() {
    answer("<html>synthetic</html>");
    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    answer("[]");
    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();

    answer("{\"query\":{\"pages\":{\"11\":{}}}}");
    assertThat(client.dayCategory(LEAP_DAY, LOOKUP).failed()).isTrue();
  }

  @Test
  void eachCallUsesTheGivenReadTimeout() {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(TWO_PAGES).withFixedDelay(1_500)));

    long start = System.nanoTime();
    Pages pages = client.dayCategory(LEAP_DAY, Duration.ofMillis(200));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(pages.failed()).isTrue();
    // two 200 ms attempts; the client's own 2 s read timeout would have waited for the answer
    assertThat(took).isLessThan(Duration.ofMillis(1_400));
  }

  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  @Test
  void failureLogsCarryOnlyTheLabel(CapturedOutput output) {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
    client.dayCategory(LEAP_DAY, LOOKUP);
    answer("not json");
    client.dayCategory(LEAP_DAY, LOOKUP);
    answer("{\"error\":{\"code\":\"badvalue\"}}");
    client.dayCategory(LEAP_DAY, LOOKUP);

    String log = output.getAll();
    assertThat(log.lines().filter(l -> l.contains("Wikimedia call failed")))
        .hasSize(3)
        .allSatisfy(
            line -> {
              String message = line.substring(line.indexOf("Wikimedia call failed"));
              assertThat(message).contains("source=commons title=commons day category");
              assertThat(DATE.matcher(message).find()).as(message).isFalse();
            });
    assertThat(log).doesNotContain("File:", "Category:", "photographs taken", "2000-02-29");
  }
}
