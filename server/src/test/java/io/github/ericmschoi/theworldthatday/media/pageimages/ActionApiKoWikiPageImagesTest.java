package io.github.ericmschoi.theworldthatday.media.pageimages;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3c T1 (1), (3), (4) and (6) on the PageImages adapter: the query, strict title encoding,
 * the 50-title cap, normalization and redirects, failures, one request without a retry, log lines
 * with the label only, and the recorded Korean Wikipedia answers. Synthetic titles are made up.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiKoWikiPageImagesTest {

  @RegisterExtension
  static final WireMockExtension kowiki =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.KOWIKI_PATH;
  private static final Duration LOOKUP = Duration.ofSeconds(2);
  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  private final WikimediaProperties properties =
      new WikimediaProperties(
          WikimediaWireMockTest.TEST_USER_AGENT,
          Duration.ofSeconds(2),
          Duration.ofSeconds(2),
          1,
          Duration.ofSeconds(120),
          3,
          Duration.ofDays(7),
          Duration.ofSeconds(5),
          URI.create(kowiki.baseUrl() + PATH));
  private final KoWikiPageImages pageImages =
      new ActionApiKoWikiPageImages(
          new WikimediaHttpClient(properties, clock), properties, JsonMapper.builder().build());

  static String fixture(String name) {
    try (InputStream in =
        ActionApiKoWikiPageImagesTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/wikimedia/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void answer(String body) {
    kowiki.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(body)));
  }

  private static Map<String, String> params(ServeEvent event) {
    Map<String, String> params = new LinkedHashMap<>();
    event
        .getRequest()
        .getQueryParams()
        .forEach((name, value) -> params.put(name, value.firstValue()));
    return params;
  }

  private static List<String> requestedTitles() {
    String titles = params(kowiki.getAllServeEvents().getFirst()).get("titles");
    return Arrays.asList(titles.split("\\|", -1));
  }

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Wikimedia call failed"))
        .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
        .toList();
  }

  // ---- recorded answers ----

  @Test
  void openingDayAsksForTheFreeLeadImagesOfAllThreeTitlesInOneRequest() {
    answer(fixture("kowiki/pageimages-1988-09-17.json"));

    Optional<Map<String, String>> names =
        pageImages.imageNames(List.of("1988년 하계 올림픽", "KBS 1TV", "KBS 2TV"), LOOKUP);

    assertThat(names).contains(Map.of("KBS 2TV", "KBS_2_logo.svg"));
    assertThat(kowiki.getAllServeEvents()).hasSize(1);
    ServeEvent request = kowiki.getAllServeEvents().getFirst();
    assertThat(request.getRequest().getUrl()).startsWith(PATH + "?");
    assertThat(request.getRequest().getHeader("User-Agent"))
        .isEqualTo(WikimediaWireMockTest.TEST_USER_AGENT)
        .doesNotContain("@");
    assertThat(params(request))
        .containsExactlyInAnyOrderEntriesOf(
            Map.ofEntries(
                Map.entry("action", "query"),
                Map.entry("prop", "pageimages"),
                Map.entry("piprop", "name"),
                Map.entry("pilicense", "free"),
                Map.entry("pilimit", "50"),
                Map.entry("redirects", "1"),
                Map.entry("titles", "1988년 하계 올림픽|KBS 1TV|KBS 2TV"),
                Map.entry("format", "json"),
                Map.entry("formatversion", "2")));
  }

  @Test
  void worldCupArticleWithoutAFreeLeadImageIsEmptyAndNotAFailure() {
    answer(fixture("kowiki/pageimages-2002-06-22.json"));

    assertThat(pageImages.imageNames(List.of("2002년 FIFA 월드컵"), LOOKUP)).contains(Map.of());
    assertThat(requestedTitles()).containsExactly("2002년 FIFA 월드컵");
  }

  // ---- synthetic answers ----

  @Test
  void ampersandAndPlusStayPartOfTheTitles() {
    answer(WikimediaWireMockTest.NO_PAGE_IMAGES);

    pageImages.imageNames(List.of("Synthetic A&B", "Synthetic C+D"), LOOKUP);

    assertThat(requestedTitles()).containsExactly("Synthetic A&B", "Synthetic C+D");
    assertThat(params(kowiki.getAllServeEvents().getFirst())).doesNotContainKeys("B");
  }

  @Test
  void fiftyOneTitlesAreOneRequestWithTheFirstFifty() {
    answer(WikimediaWireMockTest.NO_PAGE_IMAGES);
    List<String> titles =
        IntStream.rangeClosed(1, 51).mapToObj(i -> "Synthetic article " + i).toList();

    assertThat(pageImages.imageNames(titles, LOOKUP)).contains(Map.of());

    assertThat(kowiki.getAllServeEvents()).hasSize(1);
    assertThat(requestedTitles()).isEqualTo(titles.subList(0, 50));
  }

  @Test
  void normalizedAndRedirectedTitlesMapBackToTheRequestedTitle() {
    answer(
        """
        {"batchcomplete": true, "query": {
          "normalized": [{"fromencoded": false, "from": "synthetic a", "to": "Synthetic a"}],
          "redirects": [{"from": "Synthetic a", "to": "Synthetic target"}],
          "pages": [
            {"pageid": 1, "ns": 0, "title": "Synthetic target", "pageimage": "Synthetic_photo.jpg"},
            {"pageid": 2, "ns": 0, "title": "Synthetic plain", "pageimage": "File:Synthetic plain.jpg"},
            {"pageid": 3, "ns": 0, "title": "Synthetic none"},
            {"ns": 0, "title": "Synthetic gone", "missing": true}
          ]}}
        """);

    Optional<Map<String, String>> names =
        pageImages.imageNames(
            List.of("synthetic a", "Synthetic plain", "Synthetic none", "Synthetic gone"), LOOKUP);

    assertThat(names)
        .contains(
            Map.of("synthetic a", "Synthetic_photo.jpg", "Synthetic plain", "Synthetic plain.jpg"));
  }

  @Test
  void noTitlesSendNothing() {
    List<String> blanks = new ArrayList<>(Arrays.asList(" ", null, ""));

    assertThat(pageImages.imageNames(List.of(), LOOKUP)).contains(Map.of());
    assertThat(pageImages.imageNames(blanks, LOOKUP)).contains(Map.of());
    assertThat(kowiki.getAllServeEvents()).isEmpty();
  }

  @ParameterizedTest(name = "{0} fails with cause [{1}]")
  @CsvSource(
      delimiter = ';',
      value = {
        "{\"error\":{\"info\":\"x\"}};api error",
        "{\"error\":\"x\"};api error",
        "{\"error\":{\"code\":\"badvalue\",\"info\":\"x\"}};api error badvalue",
        "{\"error\":{\"info\":\"x\"},\"query\":{\"pages\":[]}};api error",
        "not json;invalid JSON",
        "[];no query",
        "{\"batchcomplete\":true};no query"
      })
  void errorsAndUnreadableAnswersAreAFailure(String body, String cause, CapturedOutput output) {
    answer(body);

    assertThat(pageImages.imageNames(List.of("Synthetic article"), LOOKUP)).isEmpty();

    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=kowiki title=kowiki pageimages of 1 cause=" + cause);
  }

  @Test
  void ratelimitedBlocksTheHostAndFails() {
    answer("{\"error\":{\"code\":\"ratelimited\",\"info\":\"x\"}}");

    assertThat(pageImages.imageNames(List.of("Synthetic article"), LOOKUP)).isEmpty();
    assertThat(pageImages.imageNames(List.of("Synthetic article"), LOOKUP)).isEmpty();

    assertThat(kowiki.getAllServeEvents()).as("the second call is never sent").hasSize(1);
  }

  @Test
  void unavailableIsOneRequestAndOneLineWithTheLabelOnly(CapturedOutput output) {
    kowiki.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThat(
            pageImages.imageNames(
                List.of("Synthetic 2000-02-29 article", "Synthetic other article"), LOOKUP))
        .isEmpty();

    assertThat(kowiki.getAllServeEvents()).as("no retry on the media path").hasSize(1);
    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=kowiki title=kowiki pageimages of 2 cause=status 503");
    assertThat(failures(output))
        .allSatisfy(line -> assertThat(DATE.matcher(line).find()).as(line).isFalse());
    assertThat(output.getAll()).doesNotContain("File:", "Category:", "Synthetic", "2000-02-29");
  }

  @Test
  void eachCallUsesTheGivenReadTimeoutOnce() {
    kowiki.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(WikimediaWireMockTest.NO_PAGE_IMAGES).withFixedDelay(1_500)));

    long start = System.nanoTime();
    Optional<Map<String, String>> names =
        pageImages.imageNames(List.of("Synthetic article"), Duration.ofMillis(200));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(names).isEmpty();
    assertThat(kowiki.getAllServeEvents()).hasSize(1);
    assertThat(took).isLessThan(Duration.ofMillis(1_000));
  }
}
