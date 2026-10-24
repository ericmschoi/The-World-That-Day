package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T2 (3) and (6): the three video request shapes (parameters checked one by one), each
 * sent once, empty and failed answers, the per-call read timeout and fixed log labels. Recorded
 * fixtures (1969-07-20, 1988-09-17 and the four event categories) and synthetic answers.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiCommonsVideoClientTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final Duration LOOKUP = Duration.ofSeconds(2);
  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final String VIPROP = "url|size|mime|mediatype|extmetadata|derivatives";

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final CommonsVideoClient client =
      new ActionApiCommonsVideoClient(
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

  /** Plan M3a4b T4 (6): every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(commons.findAllUnmatchedRequests()).isEmpty();
  }

  static String fixture(String name) {
    try (InputStream in =
        ActionApiCommonsVideoClientTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/wikimedia/commons/video/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void answer(String body) {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(body)));
  }

  /**
   * The video info parameters every request carries; the one request sent has {@code parameters}
   * query parameters in all, so nothing else is sent.
   */
  private static RequestPatternBuilder videoRequest(int parameters) {
    assertThat(commons.getAllServeEvents()).hasSize(1);
    assertThat(commons.getAllServeEvents().getFirst().getRequest().getQueryParams())
        .hasSize(parameters);
    return getRequestedFor(urlPathEqualTo(PATH))
        .withQueryParam("action", equalTo("query"))
        .withQueryParam("prop", equalTo("videoinfo"))
        .withQueryParam("viprop", equalTo(VIPROP))
        .withQueryParam("viurlwidth", equalTo("640"))
        .withQueryParam("format", equalTo("json"))
        .withQueryParam("formatversion", equalTo("2"));
  }

  private static List<String> titles(Pages pages) {
    return pages.pages().stream().map(p -> p.path("title").asString()).toList();
  }

  // ---- the three request shapes ----

  @Test
  void dateCategoryAsksForTheVideosTakenOnTheDate() {
    answer(fixture("date-category-1969-07-20.json"));

    Pages pages = client.dateCategory(MOON_DAY, LOOKUP);

    assertThat(pages.failed()).isFalse();
    assertThat(titles(pages))
        .containsExactly(
            "File:President Nixon speaking with astronauts Armstrong and Aldrin on the Moon.webm");
    assertThat(pages.pages().getFirst().path("videoinfo").get(0).path("derivatives").size())
        .isEqualTo(4);
    commons.verify(
        1,
        videoRequest(10)
            .withQueryParam("generator", equalTo("categorymembers"))
            .withQueryParam("gcmtitle", equalTo("Category:Videos taken on 1969-07-20"))
            .withQueryParam("gcmtype", equalTo("file"))
            .withQueryParam("gcmlimit", equalTo("20"))
            .withHeader("User-Agent", equalTo(WikimediaWireMockTest.TEST_USER_AGENT)));
  }

  @Test
  void categoryAsksForTheVideosDirectlyInTheCategory() {
    answer(fixture("category-apollo-11.json"));

    Pages pages = client.category("Apollo 11", LOOKUP);

    assertThat(pages.failed()).isFalse();
    assertThat(pages.pages()).hasSize(5);
    assertThat(pages.pages())
        .allSatisfy(p -> assertThat(p.path("videoinfo").get(0).has("derivatives")).isTrue());
    commons.verify(
        1,
        videoRequest(10)
            .withQueryParam("generator", equalTo("search"))
            .withQueryParam("gsrnamespace", equalTo("6"))
            .withQueryParam("gsrlimit", equalTo("10"))
            .withQueryParam("gsrsearch", equalTo("incategory:\"Apollo 11\" filetype:video")));
  }

  @Test
  void filesAsksForTheP10FilesByName() {
    answer(fixture("p10-files-1969-07-20.json"));

    Files files = client.files(List.of("AP11 FINAL APPROACH.ogv"), LOOKUP);

    assertThat(files.failed()).isFalse();
    assertThat(files.byName()).containsOnlyKeys("AP11 FINAL APPROACH.ogv");
    assertThat(files.byName().get("AP11 FINAL APPROACH.ogv").path("pageid").asLong())
        .isEqualTo(7366097);
    commons.verify(
        1, videoRequest(7).withQueryParam("titles", equalTo("File:AP11 FINAL APPROACH.ogv")));
  }

  // ---- empty answers are not failures ----

  @Test
  void recordedEmptyCategoriesAndADateWithoutACategoryAreEmptyAndNotFailures() {
    for (String name :
        List.of(
            "category-1988-summer-olympics.json",
            "category-2002-fifa-world-cup.json",
            "category-1986-asian-games.json")) {
      answer(fixture(name));
      Pages pages = client.category("Synthetic", LOOKUP);
      assertThat(pages.failed()).as(name).isFalse();
      assertThat(pages.pages()).as(name).isEmpty();
    }
    answer(fixture("date-category-1988-09-17.json"));
    assertThat(client.dateCategory(LocalDate.of(1988, 9, 17), LOOKUP))
        .isEqualTo(new Pages(List.of(), false));
  }

  @Test
  void aBlankCategoryOrNoFileNameSendsNothing() {
    assertThat(client.category(null, LOOKUP)).isEqualTo(new Pages(List.of(), false));
    assertThat(client.category(" \"\" ", LOOKUP)).isEqualTo(new Pages(List.of(), false));
    assertThat(client.files(List.of(), LOOKUP).byName()).isEmpty();

    assertThat(commons.getAllServeEvents()).isEmpty();
  }

  @Test
  void quotesInACategoryNameCannotChangeTheSearch() {
    answer("{\"batchcomplete\":true}");

    client.category("Synthetic \"x\" filetype:audio", LOOKUP);

    commons.verify(
        1,
        getRequestedFor(urlPathEqualTo(PATH))
            .withQueryParam(
                "gsrsearch", equalTo("incategory:\"Synthetic x filetype:audio\" filetype:video")));
  }

  // ---- failures ----

  @Test
  void anApiErrorIsAFailureAndRatelimitedBlocksTheHost() {
    answer("{\"error\":{\"code\":\"badvalue\",\"info\":\"synthetic\"}}");
    assertThat(client.category("Synthetic", LOOKUP).failed()).isTrue();

    answer("{\"error\":{\"code\":\"ratelimited\",\"info\":\"synthetic\"}}");
    assertThat(client.dateCategory(MOON_DAY, LOOKUP).failed()).isTrue();
    assertThat(client.files(List.of("Synthetic.webm"), LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(2); // the third call is never sent
  }

  @Test
  void aServerErrorIsSentOnceAndIsAFailure() {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(502)));

    assertThat(client.category("Synthetic", LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(1); // no retry on the media path
  }

  @Test
  void aFileAnswerWithoutPagesIsAFailure() {
    answer("{\"batchcomplete\":true}");

    assertThat(client.files(List.of("Synthetic.webm"), LOOKUP).failed()).isTrue();
  }

  @Test
  void eachCallUsesTheGivenReadTimeout() {
    commons.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"batchcomplete\":true}").withFixedDelay(1_500)));

    long start = System.nanoTime();
    Pages pages = client.category("Synthetic", Duration.ofMillis(200));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(pages.failed()).isTrue();
    assertThat(took).isLessThan(Duration.ofMillis(1_400));
  }

  @Test
  void failureLinesCarryFixedLabelsOnly(CapturedOutput output) {
    answer("not json");
    client.dateCategory(MOON_DAY, LOOKUP);
    client.category("Apollo 11", LOOKUP);
    client.files(List.of("AP11 FINAL APPROACH.ogv", "Synthetic.webm"), LOOKUP);

    assertThat(
            output
                .getAll()
                .lines()
                .filter(l -> l.contains("Wikimedia call failed"))
                .map(l -> l.substring(l.indexOf("Wikimedia call failed"))))
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons video date category"
                + " cause=invalid JSON",
            "Wikimedia call failed: source=commons title=commons video category search"
                + " cause=invalid JSON",
            "Wikimedia call failed: source=commons title=commons video files of 2"
                + " cause=invalid JSON");
    assertThat(output.getAll())
        .doesNotContain("1969", "Apollo", "AP11", "Synthetic.webm", "Category:", "File:");
  }
}
