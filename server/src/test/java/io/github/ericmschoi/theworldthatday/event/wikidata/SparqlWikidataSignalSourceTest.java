package io.github.ericmschoi.theworldthatday.event.wikidata;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource.Signals;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a2 T3 (2), (5), (7): the SPARQL adapter against WireMock, no network. Plan M3a3d T0 (2): a
 * task's own {@link InterruptedException} is a failure with one WARN line, an interrupted caller is
 * not.
 */
@ExtendWith(OutputCaptureExtension.class)
public class SparqlWikidataSignalSourceTest {

  @RegisterExtension
  static final WireMockExtension wikidata =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  static final String PATH = "/sparql";
  static final String USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";
  static final List<String> TRAGIC_CLASSES = List.of("Q3839081", "Q171558", "Q2223653", "Q350604");
  static final List<String> OPENING_ITEMS = List.of("Q8470", "Q777278", "Q624509");

  private SparqlWikidataSignalSource source(Duration timeout) {
    WikimediaProperties shared =
        new WikimediaProperties(
            USER_AGENT,
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(wikidata.baseUrl() + "/w/api.php"));
    MutableClock clock =
        new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
    return new SparqlWikidataSignalSource(
        new WikimediaHttpClient(shared, clock),
        new WikidataProperties(URI.create(wikidata.baseUrl() + PATH), timeout, TRAGIC_CLASSES),
        JsonMapper.builder().build());
  }

  private SparqlWikidataSignalSource source() {
    return source(Duration.ofSeconds(2));
  }

  public static String fixture(String path) {
    try (InputStream in =
        SparqlWikidataSignalSourceTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/wikimedia/wikidata/" + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void stub(String body) {
    wikidata.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(body)));
  }

  private static String sentQuery() {
    return wikidata
        .getAllServeEvents()
        .getFirst()
        .getRequest()
        .queryParameter("query")
        .firstValue();
  }

  @Test
  void openingDayAsksOnceForEveryItemWithTheUserAgent() {
    stub(fixture("sparql-1988-09-17.json"));

    Optional<Map<String, Signals>> signals = source().signals(OPENING_ITEMS);

    wikidata.verify(
        1,
        getRequestedFor(urlPathEqualTo(PATH))
            .withHeader("User-Agent", equalTo(USER_AGENT))
            .withQueryParam("format", equalTo("json")));
    assertThat(sentQuery()).contains("VALUES ?item { wd:Q8470 wd:Q777278 wd:Q624509 }");
    assertThat(sentQuery())
        .isEqualTo(SparqlWikidataSignalSource.query(OPENING_ITEMS, TRAGIC_CLASSES));
    Signals korean = new Signals(false, false, Set.of("Q884"));
    assertThat(signals).contains(Map.of("Q8470", korean, "Q777278", korean, "Q624509", korean));
  }

  @Test
  void quarterFinalItemHasTwoCountriesAndNoTragicSignal() {
    stub(fixture("sparql-2002-06-22.json"));

    assertThat(source().signals(List.of("Q47735")))
        .contains(Map.of("Q47735", new Signals(false, false, Set.of("Q884", "Q17"))));
  }

  @Test
  void syntheticSignalsMapToTheirItems() {
    stub(fixture("synthetic/synthetic-signals.json"));

    Map<String, Signals> signals =
        source().signals(List.of("Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", "Q8")).orElseThrow();

    assertThat(signals.get("Q1")).isEqualTo(new Signals(true, false, Set.of()));
    assertThat(signals.get("Q2")).isEqualTo(new Signals(false, true, Set.of()));
    assertThat(signals.get("Q3")).as("a death toll of 0 is no signal").isEqualTo(Signals.NONE);
    assertThat(signals.get("Q4")).isEqualTo(new Signals(false, false, Set.of("Q884", "Q17")));
    assertThat(signals.get("Q5")).isEqualTo(new Signals(false, false, Set.of("Q17")));
    assertThat(signals.get("Q6")).isEqualTo(Signals.NONE);
    assertThat(signals.get("Q7")).as("unreadable death toll").isEqualTo(Signals.NONE);
    assertThat(signals.get("Q8")).as("asked but no row").isEqualTo(Signals.NONE);
    assertThat(signals).as("items that were not asked are ignored").doesNotContainKey("Q99");
  }

  @Test
  void duplicateNullAndMalformedIdsAreNeverSent() {
    stub(fixture("synthetic/synthetic-signals.json"));

    source().signals(Arrays.asList("Q1", null, "Q1", "Q0", "q2", "Q2 } . ?x ?y ?z", "Q2"));

    assertThat(sentQuery()).contains("VALUES ?item { wd:Q1 wd:Q2 }");
  }

  @Test
  void noIdsMeansNoRequest() {
    assertThat(source().signals(List.of())).contains(Map.of());
    assertThat(source().signals(Arrays.asList(null, "not an id"))).contains(Map.of());
    assertThat(wikidata.getAllServeEvents()).isEmpty();
  }

  @Test
  void serverErrorIsNoAnswer() {
    wikidata.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThat(source().signals(List.of("Q1"))).isEmpty();
  }

  @Test
  void unreadableBodyIsNoAnswerWithOneWarn(CapturedOutput output) {
    stub("{\"head\":{}}");
    assertThat(source().signals(List.of("Q1"))).isEmpty();

    wikidata.resetAll();
    wikidata.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(aResponse().withStatus(200).withBody("<html>not json</html>")));
    assertThat(source().signals(List.of("Q1"))).isEmpty();

    assertThat(output.getAll())
        .contains("source=wikidata title=sparql of 1 items cause=unreadable response");
  }

  @Test
  void slowServiceEndsWithinTheReadTimeoutWithOneWarn(CapturedOutput output) {
    wikidata.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(fixture("synthetic/synthetic-signals.json")).withFixedDelay(3000)));

    long start = System.nanoTime();
    Optional<Map<String, Signals>> signals = source(Duration.ofMillis(300)).signals(List.of("Q1"));

    assertThat(signals).isEmpty();
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2500));
    assertThat(output.getAll().lines())
        .filteredOn(line -> line.contains(" WARN ") && line.contains("source=wikidata"))
        .as("one failure, one WARN line")
        .hasSize(1);
  }

  private static List<String> warnings(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(line -> line.contains(" WARN ") && line.contains("source=wikidata"))
        .map(line -> line.substring(line.indexOf("Wikimedia call failed")))
        .toList();
  }

  @Test
  void callThrowingItsOwnInterruptedExceptionIsNoAnswerWithOneWarn(CapturedOutput output) {
    Optional<WikimediaResponse> response =
        source()
            .within(
                "sparql of 1 items",
                () -> {
                  throw new InterruptedException("synthetic");
                });
    boolean flagSet = Thread.interrupted(); // also clears it for the next test

    assertThat(response).isEmpty();
    assertThat(flagSet).isFalse();
    assertThat(warnings(output))
        .containsExactly(
            "Wikimedia call failed: source=wikidata title=sparql of 1 items"
                + " cause=InterruptedException");
  }

  @Test
  void interruptedCallerIsNoAnswerWithoutAWarn(CapturedOutput output) throws Exception {
    AtomicReference<Optional<WikimediaResponse>> seen = new AtomicReference<>();
    AtomicBoolean flagSet = new AtomicBoolean();
    SparqlWikidataSignalSource source = source();
    Thread caller =
        Thread.ofVirtual()
            .start(
                () -> {
                  seen.set(
                      source.within(
                          "sparql of 1 items",
                          () -> {
                            Thread.sleep(60_000);
                            return null;
                          }));
                  flagSet.set(Thread.currentThread().isInterrupted());
                });
    Thread.sleep(100);
    caller.interrupt();
    caller.join(Duration.ofSeconds(2));

    assertThat(seen.get()).isEmpty();
    assertThat(flagSet.get()).isTrue();
    assertThat(warnings(output)).isEmpty();
  }

  @Test
  void readmeQueryAndUrlAreWhatTheCodeSends() {
    String readme = fixture("README.md");
    String query = SparqlWikidataSignalSource.query(OPENING_ITEMS, TRAGIC_CLASSES);

    assertThat(readme).contains("```sparql\n" + query + "\n```");
    Matcher url =
        Pattern.compile("`https://query\\.wikidata\\.org/sparql\\?format=json&query=([^`]+)`")
            .matcher(readme);
    assertThat(url.find()).isTrue();
    assertThat(URLDecoder.decode(url.group(1), StandardCharsets.UTF_8)).isEqualTo(query);
    assertThat(readme)
        .contains("CC0", "2026-10-13", "https://github.com/ericmschoi/The-World-That-Day");
  }
}
