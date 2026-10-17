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
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Plan M3a3c T1 (2), (3), (4) and (6) on the Commons file batch: the query without a language,
 * strict file name encoding, the 50-file cap, normalization, missing files, failures, one request
 * without a retry, label-only log lines, and the recorded lead image of the opening day. Synthetic
 * file names are made up.
 */
@ExtendWith(OutputCaptureExtension.class)
class ActionApiCommonsClientFilesTest {

  @RegisterExtension
  static final WireMockExtension commons =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = WikimediaWireMockTest.COMMONS_PATH;
  private static final Duration LOOKUP = Duration.ofSeconds(2);
  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);
  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

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
  private final CommonsFileMapper mapper =
      new CommonsFileMapper(
          new MediaFilter(new MediaProperties(List.of(), List.of())),
          new MediaNormalizer(new MediaProperties(List.of(), List.of())),
          clock);

  private static String fixture(String name) {
    try (InputStream in =
        ActionApiCommonsClientFilesTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/wikimedia/commons/" + name)) {
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

  private static Map<String, String> params(ServeEvent event) {
    Map<String, String> params = new LinkedHashMap<>();
    event
        .getRequest()
        .getQueryParams()
        .forEach((name, value) -> params.put(name, value.firstValue()));
    return params;
  }

  private static List<String> requestedTitles() {
    String titles = params(commons.getAllServeEvents().getFirst()).get("titles");
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

  @Test
  void recordedLeadImageOfTheOpeningDayIsAnSvgLogoThatTheMapperDrops() {
    answer(fixture("files-1988-09-17.json"));

    Files files = client.files(List.of("KBS_2_logo.svg"), LOOKUP);

    assertThat(files.failed()).isFalse();
    assertThat(files.byName()).containsOnlyKeys("KBS_2_logo.svg");
    assertThat(files.byName().get("KBS_2_logo.svg").path("pageid").asLong()).isEqualTo(58546568L);
    assertThat(
            mapper.map(
                files.byName().get("KBS_2_logo.svg"),
                "image/jpeg"::equals,
                CommonsMediaLookup.PLACE,
                OLYMPICS))
        .extracting(CommonsFileMapper.Result::drop)
        .isEqualTo(Drop.MIME);
    ServeEvent request = commons.getAllServeEvents().getFirst();
    assertThat(commons.getAllServeEvents()).hasSize(1);
    assertThat(request.getRequest().getUrl()).startsWith(PATH + "?");
    assertThat(request.getRequest().getHeader("User-Agent"))
        .isEqualTo(WikimediaWireMockTest.TEST_USER_AGENT);
    assertThat(params(request))
        .doesNotContainKey("iiextmetadatalanguage")
        .containsExactlyInAnyOrderEntriesOf(
            Map.ofEntries(
                Map.entry("action", "query"),
                Map.entry("titles", "File:KBS_2_logo.svg"),
                Map.entry("prop", "imageinfo"),
                Map.entry("iiprop", "url|mime|size|extmetadata"),
                Map.entry("iiurlwidth", "640"),
                Map.entry("format", "json"),
                Map.entry("formatversion", "2")));
  }

  @Test
  void ampersandAndPlusStayPartOfTheFileNames() {
    answer("{\"batchcomplete\":true,\"query\":{\"pages\":[]}}");

    client.files(List.of("Synthetic A&B.jpg", "Synthetic C+D.jpg"), LOOKUP);

    assertThat(requestedTitles())
        .containsExactly("File:Synthetic A&B.jpg", "File:Synthetic C+D.jpg");
    assertThat(params(commons.getAllServeEvents().getFirst())).doesNotContainKeys("B.jpg");
  }

  @Test
  void fiftyOneNamesAreOneRequestWithTheFirstFifty() {
    answer("{\"batchcomplete\":true,\"query\":{\"pages\":[]}}");
    List<String> names =
        IntStream.rangeClosed(1, 51).mapToObj(i -> "Synthetic " + i + ".jpg").toList();

    assertThat(client.files(names, LOOKUP).failed()).isFalse();

    assertThat(commons.getAllServeEvents()).hasSize(1);
    assertThat(requestedTitles())
        .isEqualTo(names.subList(0, 50).stream().map(name -> "File:" + name).toList());
  }

  @Test
  void normalizedAndMissingPagesMapBackToTheRequestedName() {
    answer(
        """
        {"batchcomplete": true, "query": {
          "normalized": [{"fromencoded": false, "from": "File:synthetic_a.jpg", "to": "File:Synthetic a.jpg"}],
          "pages": [
            {"pageid": 7, "ns": 6, "title": "File:Synthetic a.jpg", "imageinfo": [{"mime": "image/jpeg"}]},
            {"ns": 6, "title": "File:Synthetic local.jpg", "missing": true, "known": true}
          ]}}
        """);

    Files files = client.files(List.of("synthetic_a.jpg", "Synthetic local.jpg"), LOOKUP);

    assertThat(files.failed()).isFalse();
    assertThat(files.byName()).containsOnlyKeys("synthetic_a.jpg", "Synthetic local.jpg");
    assertThat(files.byName().get("synthetic_a.jpg").path("pageid").asInt()).isEqualTo(7);
    assertThat(
            mapper.map(
                files.byName().get("Synthetic local.jpg"),
                m -> true,
                CommonsMediaLookup.PLACE,
                OLYMPICS))
        .extracting(CommonsFileMapper.Result::drop)
        .isEqualTo(Drop.MISSING);
  }

  @Test
  void noNamesSendNothing() {
    assertThat(client.files(List.of(), LOOKUP)).isEqualTo(new Files(Map.of(), false));
    assertThat(commons.getAllServeEvents()).isEmpty();
  }

  @ParameterizedTest(name = "{0} fails with cause [{1}]")
  @CsvSource(
      delimiter = ';',
      value = {
        "{\"error\":{\"info\":\"x\"}};api error",
        "{\"error\":\"x\"};api error",
        "{\"error\":{\"code\":\"badvalue\",\"info\":\"x\"}};api error badvalue",
        "not json;invalid JSON",
        "{\"query\":{\"pages\":{\"7\":{}}}};unexpected body"
      })
  void errorsAndUnreadableAnswersAreAFailure(String body, String cause, CapturedOutput output) {
    answer(body);

    assertThat(client.files(List.of("Synthetic.jpg"), LOOKUP).failed()).isTrue();

    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons files of 1 cause=" + cause);
  }

  @Test
  void ratelimitedBlocksTheHostAndFails() {
    answer("{\"error\":{\"code\":\"ratelimited\",\"info\":\"x\"}}");

    assertThat(client.files(List.of("Synthetic.jpg"), LOOKUP).failed()).isTrue();
    assertThat(client.files(List.of("Synthetic.jpg"), LOOKUP).failed()).isTrue();

    assertThat(commons.getAllServeEvents()).hasSize(1);
  }

  @Test
  void unavailableIsOneRequestAndOneLineWithTheLabelOnly(CapturedOutput output) {
    commons.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    Files files = client.files(List.of("Synthetic 2000-02-29 a.jpg", "Synthetic b.jpg"), LOOKUP);

    assertThat(files).isEqualTo(new Files(Map.of(), true));
    assertThat(commons.getAllServeEvents()).as("no retry on the media path").hasSize(1);
    assertThat(failures(output))
        .containsExactly(
            "Wikimedia call failed: source=commons title=commons files of 2 cause=status 503")
        .allSatisfy(line -> assertThat(DATE.matcher(line).find()).isFalse());
    assertThat(output.getAll()).doesNotContain("File:", "Category:", "Synthetic", "2000-02-29");
  }
}
