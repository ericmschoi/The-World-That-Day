package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.PATH;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPage;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubRecorded;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Candidate;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Result;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1c T3 on English pages (plan C1 S3a): acceptance checks of T1/T2 that the backend tests
 * left open. The demo day 1976-07-17 through the whole source (which lines were merged, nothing
 * else lost), the second clause of the 2002-06-22 row ("another match of the same day stays") as a
 * neutral synthetic case, rule D3 in the direction where the later line holds the specific main
 * link, and the log rule (T2 (5)) for a whole day at DEBUG, not only for the deduplicator on its
 * own.
 */
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventMergeAcceptanceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate OPENING = LocalDate.of(1976, 7, 17);
  private static final LocalDate QUARTER_FINAL = LocalDate.of(2002, 6, 22);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  private final KoWikiEventDeduplicator deduplicator =
      new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6));

  private static void stubOpeningDay() {
    stubRecorded(wikimedia, "July 17");
    stubRecorded(wikimedia, "1976");
    stubPageprops(wikimedia, read("pageprops-1976-07-17.json"));
  }

  private static void stubQuarterFinalPages() {
    stubPage(wikimedia, "June 22", "synthetic/synthetic-06-22");
    stubPage(wikimedia, "2002", "synthetic/synthetic-2002");
  }

  private static void stubQuarterFinalDay() {
    stubQuarterFinalPages();
    stubPageprops(wikimedia, read("synthetic/synthetic-pageprops-2002-06-22.json"));
  }

  /** Parser output of the two Events sections, in input order. */
  private List<String> parsedTexts(LocalDate date, String datePrefix, String yearPrefix) {
    List<String> texts = new ArrayList<>();
    for (String[] section :
        List.of(
            new String[] {datePrefix + "-events.json", "DATE"},
            new String[] {yearPrefix + "-events.json", "YEAR"})) {
      parts
          .parser
          .parse(wikitext(section[0]), KoWikiPageKind.valueOf(section[1]), date)
          .events()
          .stream()
          .map(ParsedEvent::text)
          .forEach(texts::add);
    }
    return texts;
  }

  /** A line of {@code section} (0 date page Events, 1 year page Events) on the leap-day pages. */
  private static Candidate line(int section, String text, String... links) {
    List<Link> parsed = Arrays.stream(links).map(title -> new Link(title, null)).toList();
    String page = section == 0 ? "February 29" : "2000";
    KoWikiEvent event =
        new KoWikiEvent(
            TwoPageKoWikiEventSource.id(LEAP_DAY, page, text),
            LEAP_DAY,
            page,
            "Events",
            section == 0 ? 100L : 200L,
            text,
            parsed.stream().map(Link::title).toList(),
            null,
            READ);
    return new Candidate(event, section, parsed);
  }

  @Test
  void openingDayRemovesOnlyOpeningLinesAndKeepsEveryOtherLine() {
    stubOpeningDay();

    KoWikiDayResult result = parts.source().find(OPENING);
    List<String> parsed = parsedTexts(OPENING, "07-17", "1976");
    List<String> kept = result.events().stream().map(KoWikiEvent::text).toList();
    List<String> removed = new ArrayList<>(parsed);
    kept.forEach(removed::remove);

    assertThat(removed)
        .as("every removed line is about the opening of the 1976 games")
        .hasSize(result.mergedCount())
        .isNotEmpty()
        .allSatisfy(text -> assertThat(text).contains("Olympics"));
    assertThat(result.events())
        .as("lines without the games link are never merged into the opening")
        .filteredOn(event -> !event.linkTitles().contains("1976 Summer Olympics"))
        .extracting(KoWikiEvent::text)
        .containsExactlyElementsOf(
            parsed.stream().filter(text -> !text.contains("Olympics")).toList());
    assertThat(result.events()).extracting(KoWikiEvent::id).doesNotHaveDuplicates();
  }

  @Test
  void quarterFinalLineKeepsTheOtherMatchOfTheDayItCarries() {
    stubQuarterFinalDay();

    KoWikiDayResult result = parts.source().find(QUARTER_FINAL);

    assertThat(result.events())
        .as("the kept line is unchanged (D5), so the other match it mentions is not lost")
        .filteredOn(event -> event.text().contains("quarter-final A"))
        .singleElement()
        .satisfies(event -> assertThat(event.text()).contains("quarter-final C"));
    assertThat(result.mergedCount()).isEqualTo(1);
    assertThat(result.events().size() + result.mergedCount())
        .isEqualTo(
            parsedTexts(QUARTER_FINAL, "synthetic/synthetic-06-22", "synthetic/synthetic-2002")
                .size());
  }

  @Test
  void anotherMatchOfTheSameDayStaysWhileTheDuplicateMerges() {
    Candidate matchA =
        line(0, "Test Cup quarter-final A is won by Test team A.", "2002 Test Cup", "Test team A");
    Candidate matchB =
        line(0, "Test Cup quarter-final B is won by Test team B.", "2002 Test Cup", "Test team B");
    Candidate againA =
        line(1, "Test team A wins a 2002 Test Cup quarter-final.", "Test team A", "2002 Test Cup");

    Result result = deduplicator.dedup(List.of(matchA, matchB, againA));

    assertThat(result.events()).containsExactly(matchA.event(), matchB.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  /**
   * D3m: when the year page line also has the cup as its main link, it has the same main link as
   * both matches of the date page, so it matches two events and stays apart (D4): shown twice
   * rather than merged into the wrong match.
   */
  @Test
  void yearLineWithTheCupAsMainLinkMatchingTwoMatchesStaysApart() {
    Candidate matchA =
        line(0, "Test Cup quarter-final A is won by Test team A.", "2002 Test Cup", "Test team A");
    Candidate matchB =
        line(0, "Test Cup quarter-final B is won by Test team B.", "2002 Test Cup", "Test team B");
    Candidate againA =
        line(1, "2002 Test Cup quarter-final: Test team A wins.", "2002 Test Cup", "Test team A");

    Result result = deduplicator.dedup(List.of(matchA, matchB, againA));

    assertThat(result.events()).containsExactly(matchA.event(), matchB.event(), againA.event());
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d3MergesWhenOnlyTheLaterLineHasTheSpecificMainLink() {
    Candidate earlier = line(0, "Test Cup is held in South Korea.", "South Korea", "Test Cup");
    Candidate later = line(1, "Test Cup opens.", "Test Cup");

    Result result = deduplicator.dedup(List.of(earlier, later));

    assertThat(result.events()).containsExactly(earlier.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d3BothMainLinksGenericAndOnlyAPageLinkSharedStayApart() {
    Candidate earlier = line(0, "Test event A in South Korea.", "South Korea", "2000");
    Candidate later = line(1, "Test event B in Seoul.", "Seoul", "2000", "South Korea");

    assertThat(deduplicator.dedup(List.of(earlier, later)).mergedCount()).isZero();
  }

  @Test
  void wholeDaysAtDebugLogNoEventTextDateOrFullId(CapturedOutput output) {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    try {
      stubOpeningDay();
      parts.source().find(OPENING);

      wikimedia.resetAll();
      stubQuarterFinalPages();
      wikimedia.stubFor(
          get(urlPathEqualTo(PATH))
              .withQueryParam("action", equalTo("query"))
              .willReturn(aResponse().withStatus(503)));
      parts.source().find(QUARTER_FINAL);
    } finally {
      app.setLevel(before);
    }

    String logs = output.getAll();
    assertThat(logs).as("the dedup ran at DEBUG").contains("rule=D3");
    assertThat(logs)
        .doesNotContain("1976-07-17")
        .doesNotContain("2002-06-22")
        .doesNotContain("enwiki-19")
        .doesNotContain("enwiki-20")
        .doesNotContain("Olympics")
        .doesNotContain("Montreal")
        .doesNotContain("East Timor")
        .doesNotContain("quarter-final")
        .doesNotContain("penalties");
  }
}
