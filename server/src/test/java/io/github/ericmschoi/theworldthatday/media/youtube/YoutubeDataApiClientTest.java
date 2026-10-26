package io.github.ericmschoi.theworldthatday.media.youtube;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeClient.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a5 T2: the YouTube Data API client against WireMock with synthetic fixtures ({@code
 * fixtures/youtube/}, made-up IDs and texts). No network, no real key.
 */
@ExtendWith(OutputCaptureExtension.class)
class YoutubeDataApiClientTest {

  @RegisterExtension
  static final WireMockExtension youtube =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  static final String CHANNEL = "UCsyntheticChannelId0001";

  /** A synthetic event title: the search query, which must never reach a log line. */
  static final String QUERY = "합성 사건 제목 & 개막식+1";

  private static final String API = "/youtube/v3";
  private static final String SEARCH = API + "/search";
  private static final String VIDEOS = API + "/videos";
  private static final String WARNING = "YouTube call failed: ";
  private static final Duration LOOKUP_DEADLINE = Duration.ofMillis(400);

  private final YoutubeClient client = client(URI.create(youtube.baseUrl() + API), LOOKUP_DEADLINE);

  static YoutubeDataApiClient client(URI apiUrl, Duration lookupDeadline) {
    return client(YoutubePropertiesTest.FAKE_KEY, apiUrl, lookupDeadline);
  }

  private static YoutubeDataApiClient client(String key, URI apiUrl, Duration lookupDeadline) {
    return new YoutubeDataApiClient(
        new YoutubeProperties(
            key, apiUrl, List.of(CHANNEL), 80, 8000, 25, Duration.ofSeconds(2), List.of()),
        new MediaLookupProperties(6, lookupDeadline),
        JsonMapper.builder().build());
  }

  static String fixture(String name) {
    try (InputStream in =
        YoutubeDataApiClientTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/youtube/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * T2 (2): every request carries the key in the header only, no {@code key=} in the query, and no
   * e-mail address in the User-Agent; and every request hit a stub, so none could leave the host.
   */
  @AfterEach
  void everyRequestHasTheKeyHeaderOnly() {
    assertThat(youtube.findAllUnmatchedRequests()).isEmpty();
    for (LoggedRequest request : youtube.findAll(anyRequestedFor(anyUrl()))) {
      assertThat(request.getHeader("x-goog-api-key")).isEqualTo(YoutubePropertiesTest.FAKE_KEY);
      assertThat(request.getUrl()).doesNotContain("key=", YoutubePropertiesTest.FAKE_KEY);
      String userAgent = request.getHeader("User-Agent");
      assertThat(userAgent == null ? "" : userAgent).doesNotContain("@");
    }
  }

  private static LoggedRequest onlyRequest() {
    assertThat(youtube.getAllServeEvents()).hasSize(1);
    return youtube.getAllServeEvents().getFirst().getRequest();
  }

  /** The query parameters of {@code request}, decoded. */
  private static Map<String, String> query(LoggedRequest request) {
    return UriComponentsBuilder.fromUriString(request.getUrl())
        .build()
        .getQueryParams()
        .toSingleValueMap()
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, e -> UriUtils.decode(e.getValue(), StandardCharsets.UTF_8)));
  }

  private static List<String> warnings(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(line -> line.contains(WARNING))
        .map(line -> line.substring(line.indexOf(WARNING)))
        .toList();
  }

  // ---- requests and answers ----

  @Test
  void searchSendsThePlanParametersAndReturnsIdsInAnswerOrder() {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-two-videos.json"))));

    Response<List<String>> found = client.search(CHANNEL, QUERY);

    assertThat(found).isEqualTo(new Response.Ok<>(List.of("Abc123xyz_1", "Abc123xyz_0")));
    assertThat(query(onlyRequest()))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "part", "snippet",
                "type", "video",
                "videoEmbeddable", "true",
                "channelId", CHANNEL,
                "q", QUERY,
                "maxResults", "25",
                "regionCode", "KR",
                "relevanceLanguage", "ko"));
  }

  @Test
  void searchWithoutResultsIsAnEmptyList() {
    youtube.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-empty.json"))));

    assertThat(client.search(CHANNEL, QUERY)).isEqualTo(new Response.Ok<>(List.of()));
  }

  @Test
  void blankQueryIsNotSent() {
    assertThat(client.search(CHANNEL, " ")).isEqualTo(new Response.Ok<>(List.of()));
    assertThat(client.search(CHANNEL, null)).isEqualTo(new Response.Ok<>(List.of()));
    assertThat(youtube.getAllServeEvents()).isEmpty();
  }

  @Test
  void videosSendsThePartsAndAPlainCommaListAndReturnsTheItems() {
    youtube.stubFor(
        get(urlPathEqualTo(VIDEOS)).willReturn(okJson(fixture("videos-two-items.json"))));

    Response<List<JsonNode>> found = client.videos(List.of("Abc123xyz_0", "Abc123xyz_1"));

    assertThat(found).isInstanceOf(Response.Ok.class);
    List<JsonNode> items = ((Response.Ok<List<JsonNode>>) found).value();
    assertThat(items)
        .extracting(item -> item.path("id").asString())
        .containsExactly("Abc123xyz_0", "Abc123xyz_1");
    assertThat(items.getFirst().path("contentDetails").path("duration").asString())
        .isEqualTo("PT1M57S");
    LoggedRequest request = onlyRequest();
    assertThat(request.getUrl())
        .contains("part=snippet,status,contentDetails,recordingDetails")
        .contains("id=Abc123xyz_0,Abc123xyz_1");
    assertThat(query(request))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "part", "snippet,status,contentDetails,recordingDetails",
                "id", "Abc123xyz_0,Abc123xyz_1"));
  }

  @Test
  void fiftyIdsAreOneCallAndFiftyOneAreNone() {
    youtube.stubFor(get(urlPathEqualTo(VIDEOS)).willReturn(okJson("{\"items\":[]}")));
    List<String> fifty = IntStream.range(0, 50).mapToObj(i -> "Abc123xy%03d".formatted(i)).toList();
    List<String> fiftyOne = Stream.concat(fifty.stream(), Stream.of("Abc123xyz_X")).toList();

    assertThat(client.videos(fifty)).isEqualTo(new Response.Ok<>(List.of()));
    assertThat(query(onlyRequest()).get("id").split(",")).hasSize(50);

    youtube.resetRequests();
    assertThatIllegalArgumentException().isThrownBy(() -> client.videos(fiftyOne));
    assertThat(client.videos(List.of())).isEqualTo(new Response.Ok<>(List.of()));
    assertThat(youtube.getAllServeEvents()).isEmpty();
  }

  @Test
  void withoutAKeyNothingIsSent() {
    YoutubeClient off = client("", URI.create(youtube.baseUrl() + API), LOOKUP_DEADLINE);

    assertThatIllegalStateException()
        .isThrownBy(() -> off.search(CHANNEL, QUERY))
        .withMessageNotContaining(QUERY);
    assertThatIllegalStateException().isThrownBy(() -> off.videos(List.of("Abc123xyz_0")));
    assertThat(youtube.getAllServeEvents()).isEmpty();
  }

  // ---- outcomes ----

  @Test
  void quotaExceededIsItsOwnOutcomeWithOneWarning(CapturedOutput output) {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH))
            .willReturn(
                aResponse().withStatus(403).withBody(fixture("error-quota-exceeded.json"))));
    youtube.stubFor(
        get(urlPathEqualTo(VIDEOS))
            .willReturn(
                aResponse().withStatus(403).withBody(fixture("error-quota-exceeded.json"))));

    assertThat(client.search(CHANNEL, QUERY)).isEqualTo(new Response.QuotaExceeded<>());
    assertThat(client.videos(List.of("Abc123xyz_0"))).isEqualTo(new Response.QuotaExceeded<>());
    assertThat(youtube.getAllServeEvents()).hasSize(2);
    assertThat(warnings(output))
        .containsExactly(
            "YouTube call failed: call=search cause=quotaExceeded",
            "YouTube call failed: call=videos cause=quotaExceeded");
  }

  static Stream<Arguments> failures() {
    return Stream.of(
        Arguments.of("403 forbidden", 403, fixture("error-forbidden.json"), "status 403"),
        Arguments.of(
            "403 rateLimitExceeded", 403, fixture("error-rate-limit-exceeded.json"), "status 403"),
        // [91] shows no 403 body: one that cannot be read or names no reason is not quota
        Arguments.of("403 unreadable body", 403, "<html>synthetic forbidden</html>", "status 403"),
        Arguments.of(
            "403 without a reason",
            403,
            "{\"error\":{\"code\":403,\"message\":\"Synthetic.\"}}",
            "status 403"),
        Arguments.of("403 empty body", 403, "", "status 403"),
        Arguments.of(
            "400",
            400,
            "{\"error\":{\"code\":400,\"errors\":[{\"reason\":\"badRequest\"}]}}",
            "status 400"),
        Arguments.of("404", 404, "{}", "status 404"),
        Arguments.of("500", 500, "{}", "status 500"),
        Arguments.of("503", 503, "{}", "status 503"),
        Arguments.of("200 broken JSON", 200, "{\"items\":[", "unreadable response"),
        Arguments.of(
            "200 without items",
            200,
            "{\"kind\":\"youtube#searchListResponse\"}",
            "unreadable response"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("failures")
  void otherAnswersAreFailuresWithOneWarning(
      String label, int status, String body, String cause, CapturedOutput output) {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(status).withBody(body)));

    assertThat(client.search(CHANNEL, QUERY)).isEqualTo(new Response.Failed<>());
    assertThat(youtube.getAllServeEvents()).as("sent once, never retried").hasSize(1);
    assertThat(warnings(output)).containsExactly("YouTube call failed: call=search cause=" + cause);
  }

  @Test
  void videosFailureNamesTheVideosCall(CapturedOutput output) {
    youtube.stubFor(get(urlPathEqualTo(VIDEOS)).willReturn(aResponse().withStatus(500)));

    assertThat(client.videos(List.of("Abc123xyz_0"))).isEqualTo(new Response.Failed<>());
    assertThat(warnings(output))
        .containsExactly("YouTube call failed: call=videos cause=status 500");
  }

  @Test
  void connectionFailureIsAFailureWithOneWarning(CapturedOutput output) throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    YoutubeClient refused =
        client(URI.create("http://127.0.0.1:" + closedPort + API), LOOKUP_DEADLINE);

    assertThat(refused.search(CHANNEL, QUERY)).isEqualTo(new Response.Failed<>());
    assertThat(warnings(output))
        .singleElement()
        .satisfies(line -> assertThat(line).startsWith("YouTube call failed: call=search cause="))
        .satisfies(
            line ->
                assertThat(line).doesNotContain("status", "127.0.0.1", String.valueOf(closedPort)));
  }

  @Test
  void answerLaterThanTheLookupDeadlineFailsInTimeAfterOneRequest(CapturedOutput output) {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH))
            .willReturn(okJson(fixture("search-two-videos.json")).withFixedDelay(2_000)));

    long start = System.nanoTime();
    Response<List<String>> found = client.search(CHANNEL, QUERY);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(found).isEqualTo(new Response.Failed<>());
    assertThat(took).isLessThan(LOOKUP_DEADLINE.plusMillis(500));
    assertThat(youtube.getAllServeEvents()).hasSize(1);
    assertThat(warnings(output))
        .containsExactly("YouTube call failed: call=search cause=HttpTimeoutException");
  }

  @Test
  void interruptEndsTheRequestWithinHalfASecond(CapturedOutput output) throws Exception {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH))
            .willReturn(okJson(fixture("search-two-videos.json")).withFixedDelay(5_000)));
    YoutubeClient patient = client(URI.create(youtube.baseUrl() + API), Duration.ofSeconds(10));
    AtomicReference<Response<List<String>>> seen = new AtomicReference<>();
    AtomicBoolean flagSet = new AtomicBoolean();
    Thread caller =
        Thread.ofVirtual()
            .start(
                () -> {
                  seen.set(patient.search(CHANNEL, QUERY));
                  flagSet.set(Thread.currentThread().isInterrupted());
                });
    Thread.sleep(300);

    long start = System.nanoTime();
    caller.interrupt();
    caller.join(Duration.ofSeconds(5));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(caller.isAlive()).isFalse();
    assertThat(took).isLessThan(Duration.ofMillis(500));
    assertThat(seen.get()).isEqualTo(new Response.Failed<>());
    assertThat(flagSet.get()).as("the interrupt flag is kept").isTrue();
    assertThat(warnings(output))
        .containsExactly("YouTube call failed: call=search cause=InterruptedException");
  }

  // ---- logs ----

  @Test
  void logsEvenAtDebugNeverHoldTheKeyQueryIdsChannelOrAddress(CapturedOutput output) {
    youtube.stubFor(
        get(urlPathEqualTo(SEARCH)).willReturn(okJson(fixture("search-two-videos.json"))));
    youtube.stubFor(
        get(urlPathEqualTo(VIDEOS))
            .willReturn(
                aResponse().withStatus(403).withBody(fixture("error-quota-exceeded.json"))));
    ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Level before = root.getLevel();
    root.setLevel(Level.DEBUG);
    try {
      LoggerFactory.getLogger("youtube-client-test").debug("debug-control-line");
      client.search(CHANNEL, QUERY);
      client.videos(List.of("Abc123xyz_0", "Abc123xyz_1"));
      youtube.resetAll();
      youtube.stubFor(get(urlPathEqualTo(SEARCH)).willReturn(aResponse().withStatus(500)));
      client.search(CHANNEL, QUERY);
    } finally {
      root.setLevel(before);
    }

    String all = output.getAll();
    // The control line proves DEBUG output is captured, so the absence below means something.
    assertThat(all).contains("debug-control-line");
    assertThat(warnings(output)).hasSize(2);
    assertThat(all)
        .doesNotContain(
            YoutubePropertiesTest.FAKE_KEY,
            QUERY,
            UriUtils.encode(QUERY, StandardCharsets.UTF_8),
            "Abc123xyz",
            CHANNEL,
            youtube.baseUrl(),
            "localhost:" + youtube.getPort(),
            API);
  }

  @Test
  void readmeNamesTheReferencesAndSaysTheValuesAreSynthetic() {
    assertThat(fixture("README.md"))
        .contains(
            "Synthetic values built from the official field structure; no real responses or real"
                + " video IDs",
            "https://developers.google.com/youtube/v3/docs/search/list",
            "https://developers.google.com/youtube/v3/docs/videos/list",
            "https://developers.google.com/youtube/v3/docs/videos",
            "https://developers.google.com/youtube/v3/docs/search",
            "https://developers.google.com/youtube/v3/docs/errors",
            "no JSON body example");
  }
}
