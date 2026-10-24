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
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.commons.WikidataVideoLinks.Links;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T2 (2) and (6): the Wikidata link query against WireMock with the recorded answers of
 * 1969-07-20, 1986-09-20 and the demo dates, and synthetic answers with made-up QIDs ({@code
 * Q1}...) for the edges. No network.
 */
@ExtendWith(OutputCaptureExtension.class)
class SparqlWikidataVideoLinksTest {

  @RegisterExtension
  static final WireMockExtension wikidata =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String PATH = "/sparql";
  private static final Duration LOOKUP = Duration.ofSeconds(2);

  private final WikidataVideoLinks links =
      new SparqlWikidataVideoLinks(
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
                  URI.create(wikidata.baseUrl() + "/w/api.php")),
              new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE)),
          new WikidataProperties(
              URI.create(wikidata.baseUrl() + PATH),
              Duration.ofSeconds(5),
              List.of("Q3839081", "Q171558", "Q2223653", "Q350604")),
          JsonMapper.builder().build());

  /** Plan M3a4b T4 (6): every request went to a stub, so nothing could reach the network. */
  @AfterEach
  void noUnmatchedRequest() {
    assertThat(wikidata.findAllUnmatchedRequests()).isEmpty();
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

  /** A SPARQL answer of rows given as variable/value pairs. */
  private static String rows(String[]... rows) {
    StringBuilder body = new StringBuilder("{\"results\":{\"bindings\":[");
    for (int r = 0; r < rows.length; r++) {
      body.append(r == 0 ? "{" : ",{");
      for (int i = 0; i < rows[r].length; i += 2) {
        body.append(i == 0 ? "" : ",")
            .append(
                "\"%s\":{\"type\":\"literal\",\"value\":\"%s\"}"
                    .formatted(rows[r][i], rows[r][i + 1]));
      }
      body.append("}");
    }
    return body.append("]}}").toString();
  }

  private static String[] row(String qid, String... values) {
    String[] row =
        Arrays.copyOf(
            new String[] {"item", "http://www.wikidata.org/entity/" + qid}, 2 + values.length);
    System.arraycopy(values, 0, row, 2, values.length);
    return row;
  }

  // ---- recorded ----

  @Test
  void apolloElevenHasItsP10FileCategoryAndLabel() {
    stub(SparqlWikidataSignalSourceTest.fixture("video-links-1969-07-20.json"));

    Optional<Map<String, Links>> found = links.links(List.of("Q43653"), LOOKUP);

    assertThat(found)
        .contains(
            Map.of(
                "Q43653", new Links(List.of("AP11 FINAL APPROACH.ogv"), "Apollo 11", "Apollo 11")));
    assertThat(sentQuery()).isEqualTo(SparqlWikidataVideoLinks.query(List.of("Q43653")));
    assertThat(wikidata.getAllServeEvents()).hasSize(1);
    assertThat(wikidata.getAllServeEvents().getFirst().getRequest().getHeader("User-Agent"))
        .isEqualTo(WikimediaWireMockTest.TEST_USER_AGENT);
  }

  @Test
  void withoutP373TheP910CategorySitelinkIsUsed() {
    stub(SparqlWikidataSignalSourceTest.fixture("video-links-1986-09-20.json"));

    assertThat(links.links(List.of("Q495611"), LOOKUP))
        .contains(Map.of("Q495611", new Links(List.of(), "1986 Asian Games", "1986 Asian Games")));
  }

  @Test
  void demoDatesHaveCategoriesAndLabelsButNoVideoFile() {
    stub(SparqlWikidataSignalSourceTest.fixture("video-links-1988-09-17.json"));
    Optional<Map<String, Links>> opening =
        links.links(List.of("Q8470", "Q777278", "Q624509"), LOOKUP);
    wikidata.resetAll();
    stub(SparqlWikidataSignalSourceTest.fixture("video-links-2002-06-22.json"));
    Optional<Map<String, Links>> worldCup = links.links(List.of("Q47735"), LOOKUP);

    assertThat(opening)
        .contains(
            Map.of(
                "Q8470",
                new Links(List.of(), "1988 Summer Olympics", "1988 Summer Olympics"),
                "Q777278",
                new Links(List.of(), null, "KBS 1TV"),
                "Q624509",
                new Links(List.of(), null, "KBS 2TV")));
    assertThat(worldCup)
        .contains(
            Map.of("Q47735", new Links(List.of(), "2002 FIFA World Cup", "2002 FIFA World Cup")));
  }

  @Test
  void readmeQueryAndUrlAreWhatTheCodeSends() {
    String readme = SparqlWikidataSignalSourceTest.fixture("README.md");
    Matcher url =
        Pattern.compile(
                "`https://query\\.wikidata\\.org/sparql\\?format=json&query=(SELECT\\+%3fitem\\+%3fvideo[^`]+)`")
            .matcher(readme);

    for (List<String> items :
        List.of(
            List.of("Q43653"),
            List.of("Q8470", "Q777278", "Q624509"),
            List.of("Q47735"),
            List.of("Q495611"))) {
      assertThat(url.find()).as(items.toString()).isTrue();
      assertThat(URLDecoder.decode(url.group(1), StandardCharsets.UTF_8))
          .isEqualTo(SparqlWikidataVideoLinks.query(items));
    }
    assertThat(readme)
        .contains(
            "2026-10-24", "TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)")
        .doesNotContain("@");
  }

  // ---- synthetic ----

  @Test
  void onlyItemIdsAreAskedAndNoneLeftSendsNothing() {
    assertThat(links.links(Arrays.asList(null, "", "Q0", "q1", "Q1 }", "P10"), LOOKUP))
        .contains(Map.of());
    assertThat(wikidata.getAllServeEvents()).isEmpty();

    stub(rows());
    assertThat(links.links(Arrays.asList("Q2", null, "Q1 } wd:Q3", "Q2"), LOOKUP))
        .contains(Map.of("Q2", Links.NONE));
    assertThat(sentQuery()).isEqualTo(SparqlWikidataVideoLinks.query(List.of("Q2")));
  }

  @Test
  void severalValuesAreAllFilesAndTheFirstNameInOrder() {
    stub(
        rows(
            row(
                "Q1",
                "video",
                "http://commons.wikimedia.org/wiki/Special:FilePath/Synthetic%20b%2B1.webm",
                "p373",
                "Synthetic Z",
                "cs",
                "Category:Synthetic sitelink",
                "label",
                "Synthetic label"),
            row(
                "Q1",
                "video",
                "http://commons.wikimedia.org/wiki/Special:FilePath/Synthetic%20a.webm",
                "p373",
                "Synthetic A"),
            row("Q1", "video", "http://commons.wikimedia.org/wiki/Special:FilePath/Bad%zz.webm"),
            row("Q2", "cs", "Synthetic not a category"),
            row("Q99", "p373", "Synthetic not asked")));

    assertThat(links.links(List.of("Q1", "Q2"), LOOKUP))
        .contains(
            Map.of(
                "Q1",
                new Links(
                    List.of("Synthetic a.webm", "Synthetic b+1.webm"),
                    "Synthetic A",
                    "Synthetic label"),
                "Q2",
                Links.NONE));
  }

  @Test
  void failuresAreEmptyNotNoLinksWithOneFixedLine(CapturedOutput output) {
    wikidata.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
    Optional<Map<String, Links>> unavailable = links.links(List.of("Q1", "Q2", "Q3"), LOOKUP);
    int sent = wikidata.getAllServeEvents().size();
    stub("<html>synthetic</html>");
    Optional<Map<String, Links>> unreadable = links.links(List.of("Q1"), LOOKUP);
    stub("{\"head\":{}}");
    Optional<Map<String, Links>> noBindings = links.links(List.of("Q1"), LOOKUP);

    assertThat(unavailable).isEmpty();
    assertThat(sent).isEqualTo(1); // sent once, never retried
    assertThat(unreadable).isEmpty();
    assertThat(noBindings).isEmpty();
    assertThat(
            output
                .getAll()
                .lines()
                .filter(l -> l.contains("Wikimedia call failed"))
                .map(l -> l.substring(l.indexOf("Wikimedia call failed"))))
        .containsExactly(
            "Wikimedia call failed: source=wikidata title=wikidata video links of 3 items"
                + " cause=status 503",
            "Wikimedia call failed: source=wikidata title=wikidata video links of 1 items"
                + " cause=unreadable response",
            "Wikimedia call failed: source=wikidata title=wikidata video links of 1 items"
                + " cause=unreadable response");
    assertThat(output.getAll()).doesNotContain("Q1", "Q2", "Q3", "Category:", "File:");
  }

  @Test
  void theCallUsesTheGivenReadTimeout() {
    wikidata.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(rows()).withFixedDelay(1_500)));

    long start = System.nanoTime();
    Optional<Map<String, Links>> found = links.links(List.of("Q1"), Duration.ofMillis(200));
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(found).isEmpty();
    assertThat(took).isLessThan(Duration.ofMillis(1_400));
  }
}
