package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Candidate;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Result;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1c T2 (2)~(5) and plan C1 S3a T4 (D3m): one synthetic case per rule D1~D6, link
 * normalization and the log rule. Sentences are neutral test lines ("Test Cup group A match A"),
 * never real events. Sections: 0 date page Events, 1 year page Events.
 */
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventDeduplicatorTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final int DATE = 0;
  private static final int YEAR = 1;

  private final KoWikiEventDeduplicator deduplicator =
      new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6));

  /** A line of {@code section}; a link is {@code "title"} or {@code "title=Q123"}. */
  private static Candidate line(int section, String text, String... links) {
    List<Link> parsed =
        Arrays.stream(links)
            .map(l -> l.contains("=") ? l.split("=") : new String[] {l, null})
            .map(parts -> new Link(parts[0], parts[1]))
            .toList();
    String page = section == DATE ? "February 29" : "2000";
    KoWikiEvent event =
        new KoWikiEvent(
            TwoPageKoWikiEventSource.id(LEAP_DAY, page, text),
            LEAP_DAY,
            page,
            "Events",
            section == DATE ? 100L : 200L,
            text,
            parsed.stream().map(Link::title).toList(),
            parsed.isEmpty() ? null : parsed.getFirst().wikidataQid(),
            READ);
    return new Candidate(event, section, parsed);
  }

  private Result dedup(Candidate... lines) {
    return deduplicator.dedup(List.of(lines));
  }

  private static List<KoWikiEvent> events(Candidate... lines) {
    return Arrays.stream(lines).map(Candidate::event).toList();
  }

  @Test
  void d1LinesOfOneSectionNeverMergeEvenWithTheSameMainLink() {
    Candidate a = line(DATE, "Test Cup match 1 is played.", "Test Cup");
    Candidate b = line(DATE, "Test Cup match 2 is played.", "Test Cup");

    Result result = dedup(a, b);

    assertThat(result.events()).isEqualTo(events(a, b));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d1ExceptionIdenticalLineTwiceOnOnePageIsOneEventSoIdsStayUnique() {
    Candidate a = line(DATE, "Test event A.", "Test page A");
    Candidate twice = line(DATE, "Test event A.", "Test page A");
    Candidate otherPage = line(YEAR, "Test event A.");

    Result result = dedup(a, twice, otherPage);

    assertThat(result.events()).containsExactly(a.event());
    assertThat(result.mergedCount()).isEqualTo(2);
    assertThat(result.events()).extracting(KoWikiEvent::id).doesNotHaveDuplicates();
  }

  @Test
  void d2SameTextApartFromSpacesAndPunctuationMerges() {
    Candidate date = line(DATE, "Test event A opens.");
    Candidate year = line(YEAR, "Test event A  opens");

    Result result = dedup(date, year);

    assertThat(result.events()).containsExactly(date.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d2DifferentWordsStayApart() {
    Result result = dedup(line(DATE, "Test event A opens."), line(YEAR, "Test event B opens."));

    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d3LineWithAGenericMainLinkMergesThroughTheSharedSpecificLink() {
    Candidate date = line(DATE, "Test Cup opens.", "Test Cup");
    Candidate year = line(YEAR, "Test Cup is held in South Korea.", "South Korea", "Test Cup");

    Result result = dedup(date, year);

    assertThat(result.events()).containsExactly(date.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d3OnlyGenericLinksSharedStayApart() {
    Candidate a = line(DATE, "Test event A in South Korea.", "South Korea", "Test page A");
    Candidate b = line(YEAR, "Test event B in South Korea.", "South Korea", "Test page B");
    Candidate c = line(YEAR, "Test event C in South Korea.", "South Korea");

    Result result = dedup(a, b, c);

    assertThat(result.events()).isEqualTo(events(a, b, c));
    assertThat(result.mergedCount()).isZero();
  }

  /** The overlap rule on its own: the cup is the date line's main link only. */
  @Test
  void d3CounterexampleSharingOnlyTheTournamentStaysApart() {
    Candidate a = line(DATE, "Test Cup group A match A", "Test Cup", "Test team A");
    Candidate b = line(YEAR, "Test team B wins group A match B", "Test team B", "Test Cup");
    Candidate c =
        line(YEAR, "Test team C meets Test team D", "Test team C", "Test Cup", "Test team D");

    Result result = dedup(a, b, c);

    assertThat(result.events()).isEqualTo(events(a, b, c));
  }

  /**
   * D3m (plan C1 S3a, PM decision): the counterexample with the cup as the main link of both lines
   * now merges across the two pages, whatever the overlap; on one page it stays two (D1).
   */
  @Test
  void d3mSameSpecificMainLinkMergesAcrossPagesOnly() {
    Candidate date = line(DATE, "Test Cup group A match A", "Test Cup", "Test team A");
    Candidate year = line(YEAR, "Test Cup group A match B", "Test Cup", "Test team B");
    Candidate sameDate = line(DATE, "Test Cup group A match B", "Test Cup", "Test team B");

    Result across = dedup(date, year);
    Result onePage = dedup(date, sameDate);

    assertThat(across.events()).containsExactly(date.event());
    assertThat(across.mergedCount()).isEqualTo(1);
    assertThat(onePage.events()).isEqualTo(events(date, sameDate));
    assertThat(onePage.mergedCount()).isZero();
  }

  @Test
  void d3mSameItemUnderAnotherTitleIsTheSameMainLink() {
    Candidate date = line(DATE, "Test event A.", "Test page A=Q9", "Test page C");
    Candidate year =
        line(YEAR, "Test event A, written another way.", "Test page B=Q9", "Test page D");

    assertThat(dedup(date, year).events()).containsExactly(date.event());
  }

  @Test
  void d3mGenericOrPageMainLinksNeverMergeAlone() {
    assertThat(
            dedup(
                    line(DATE, "Test event A in Montreal.", "Montreal", "Test page A"),
                    line(YEAR, "Test event B in Montreal.", "Montreal", "Test page B"))
                .mergedCount())
        .as("a city main link")
        .isZero();
    assertThat(
            dedup(
                    line(DATE, "Test event A.", "1999", "Test page A"),
                    line(YEAR, "Test event B.", "1999", "Test page B"))
                .mergedCount())
        .as("a year page main link")
        .isZero();
    assertThat(
            dedup(
                    line(DATE, "Test event A.", "March 1", "Test page A"),
                    line(YEAR, "Test event B.", "March 1", "Test page B"))
                .mergedCount())
        .as("a date page main link")
        .isZero();
  }

  @Test
  void d3SharedMainLinkWithEnoughOverlapMerges() {
    Candidate a = line(DATE, "Test Cup group A match A", "Test Cup", "Test team A", "Test team B");
    Candidate b = line(YEAR, "Test team A wins at the Test Cup", "Test Cup", "Test team A");

    assertThat(dedup(a, b).events()).containsExactly(a.event());
  }

  @Test
  void d3PageLinksAreNeitherMainLinksNorCounted() {
    Candidate a = line(DATE, "Test event A.", "1999", "Test page A");
    Candidate b = line(YEAR, "Test event B.", "1999", "Test page B");
    Candidate c = line(YEAR, "Test event C.", "February 28");
    Candidate d = line(DATE, "Test event D.", "February 28");

    assertThat(dedup(a, b, c, d).mergedCount()).isZero();
  }

  /**
   * T4 (2) of plan M3a1c, under D3m: the one-link line of the T3 repro now absorbs the year page
   * line with the same main link (approved expectation change, plan C1 S3a); two such lines of one
   * page stay apart. Without a shared main link the overlap boundary is unchanged.
   */
  @Test
  void d3OneLinkLineMergesAcrossPagesByItsMainLinkButNotOnOnePage() {
    Candidate date = line(DATE, "Test Cup match 1.", "2002 Test Cup");
    Candidate year = line(YEAR, "Test Cup match 2.", "2002 Test Cup", "Test team C", "Test team D");
    Candidate sameDate =
        line(DATE, "Test Cup match 2.", "2002 Test Cup", "Test team C", "Test team D");

    Result across = dedup(date, year);

    assertThat(across.events()).containsExactly(date.event());
    assertThat(across.mergedCount()).isEqualTo(1);
    assertThat(dedup(date, sameDate).events()).isEqualTo(events(date, sameDate));
    Result boundary =
        dedup(
            line(DATE, "Test Cup match 3.", "Test Cup"),
            line(YEAR, "Test Cup match 4.", "Test venue", "Test Cup"));
    assertThat(boundary.events()).as("1/2 is below 0.6").hasSize(2);
  }

  /** T4 (5): the same link written more than once in a line counts once. */
  @Test
  void d3SameLinkTwiceInALineCountsOnce() {
    Candidate date =
        line(DATE, "Test Cup opens.", "Test Cup", "Test Cup", "Test Cup", "Test team A");
    Candidate year = line(YEAR, "Test team A at the Test Cup opening.", "Test team A", "Test Cup");

    assertThat(dedup(date, year).events()).as("2 of 2, not 2 of 4").containsExactly(date.event());
  }

  /** T4 (6): same tournament, different country pairs linked only by country pages. */
  @Test
  void d3DifferentCountryPairsStayApart() {
    Candidate date = line(DATE, "Test Cup group A match.", "2002 Test Cup", "South Korea", "Spain");
    Candidate year =
        line(YEAR, "Test Cup group B match.", "2002 Test Cup", "Germany", "United States");

    Result result = dedup(date, year);

    assertThat(result.events())
        .as("the D3 (iii) place conflict also holds for D3m")
        .isEqualTo(events(date, year));
    assertThat(result.mergedCount()).isZero();
  }

  /** T4 (7): the generic-link conflict only blocks when both lines list places and none match. */
  @Test
  void d3ControlsWithoutAGenericConflictStillMerge() {
    assertThat(
            dedup(
                    line(DATE, "Test Cup A.", "2002 Test Cup", "South Korea"),
                    line(YEAR, "Test Cup B.", "2002 Test Cup", "South Korea", "United States"))
                .mergedCount())
        .as("(a) a shared country")
        .isEqualTo(1);
    assertThat(
            dedup(
                    line(DATE, "Test Cup A.", "2002 Test Cup"),
                    line(YEAR, "Test Cup B.", "2002 Test Cup", "United States"))
                .mergedCount())
        .as("(b) one line has no country or city")
        .isEqualTo(1);
    assertThat(
            dedup(
                    line(DATE, "Test Cup A.", "2002 Test Cup", "1999"),
                    line(YEAR, "Test Cup B.", "2002 Test Cup", "United States"))
                .mergedCount())
        .as("(c) a year page is not a listed generic link")
        .isEqualTo(1);
  }

  @Test
  void d4LineMatchingTwoEarlierEventsIsNotMerged(CapturedOutput output) {
    // each earlier line has the same main link as the later one (D3m) and shares 2 of 3 links
    Candidate first = line(DATE, "Test event A.", "Test page A", "Test page B", "Test page C");
    Candidate second = line(DATE, "Test event B.", "Test page A", "Test page B", "Test page D");
    Candidate both = line(YEAR, "Test events A and B.", "Test page A", "Test page B");
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(KoWikiEventDeduplicator.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    Result result;
    try {
      result = dedup(first, second, both);
    } finally {
      logger.setLevel(before);
    }

    assertThat(result.events()).isEqualTo(events(first, second, both));
    assertThat(result.mergedCount()).isZero();
    String hashBoth = both.event().id().substring(both.event().id().lastIndexOf('-') + 1);
    assertThat(output.getAll())
        .as("M3a1c review 4: the D4 branch itself kept the later line apart")
        .contains("Wiki event not merged: rule=D4 event=" + hashBoth)
        .doesNotContain("rule=D3");
  }

  @Test
  void d5TheFirstLineIsKeptUnchanged() {
    Candidate date = line(DATE, "Test Cup opened.", "Test Cup");
    Candidate year = line(YEAR, "Test Cup opens.", "Test Cup=Q7");

    Result result = dedup(date, year);

    assertThat(result.events()).singleElement().isSameAs(date.event());
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d1KeepsASecondYearLineOutOfAGroupThatHasOne() {
    Candidate date = line(DATE, "Test Cup opened.", "Test Cup");
    Candidate year = line(YEAR, "Test Cup opens.", "Test Cup=Q7");
    Candidate yearAgain = line(YEAR, "Test Cup opening ceremony.", "South Korea", "Test Cup");

    Result result = dedup(date, year, yearAgain);

    assertThat(result.events()).containsExactly(date.event(), yearAgain.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void sameTitleMergesWithoutItems() {
    Candidate date = line(DATE, "Test team A wins at the Test Cup.", "2002 Test Cup");
    Candidate year = line(YEAR, "Test team A wins a Test Cup quarter-final.", "2002 Test Cup");

    assertThat(dedup(date, year).events()).containsExactly(date.event());
  }

  @Test
  void differentTitlesWithTheSameItemMerge() {
    Candidate date = line(DATE, "Test event A.", "Test page A=Q9");
    Candidate year = line(YEAR, "Test event A, written another way.", "Test page B=Q9");

    assertThat(dedup(date, year).events()).containsExactly(date.event());
    assertThat(KoWikiEventDeduplicator.sameLink(new Link("A", "Q1"), new Link("B", "Q2")))
        .isFalse();
    assertThat(KoWikiEventDeduplicator.sameLink(new Link("A", null), new Link("A", "Q2")))
        .as("same title")
        .isTrue();
    assertThat(
            KoWikiEventDeduplicator.sameLink(
                new Link("2002 Test Cup", null), new Link("2002 test Cup", null)))
        .as("titles are compared as resolved, no spelling rule")
        .isFalse();
  }

  @Test
  void mergedCountIsTheNumberOfRemovedLines() {
    Candidate[] lines = {
      line(DATE, "Test Cup opens.", "Test Cup"),
      line(DATE, "Test event B.", "Test page B"),
      line(DATE, "Test event D.", "Test page D"),
      line(YEAR, "Test event B", "Test page C"),
      line(YEAR, "Test Cup opening ceremony.", "South Korea", "Test Cup"),
      line(YEAR, "Test event D.")
    };

    Result result = dedup(lines);

    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .containsExactly("Test Cup opens.", "Test event B.", "Test event D.");
    assertThat(result.mergedCount()).isEqualTo(3).isEqualTo(lines.length - result.events().size());
  }

  @Test
  void debugLogsNameTheRuleAndIdHashButNoTextOrDate(CapturedOutput output) {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(KoWikiEventDeduplicator.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      Candidate first = line(DATE, "Test event A.", "Test page A", "Test page B", "Test page C");
      Candidate second = line(DATE, "Test event B.", "Test page A", "Test page B", "Test page D");
      Candidate third = line(DATE, "Test event E.", "Test page E");
      Candidate fourth = line(DATE, "Test Cup opens.", "Test Cup");
      Candidate d4 = line(YEAR, "Test events A and B.", "Test page A", "Test page B");
      Candidate d2 = line(YEAR, "Test event E");
      Candidate d3 = line(YEAR, "Test Cup is held in South Korea.", "South Korea", "Test Cup");
      Candidate d3m = line(DATE, "Test event F.", "Test page F");
      Candidate d3mYear = line(YEAR, "A line about Test page F.", "Test page F");
      dedup(first, second, third, fourth, d3m, d4, d2, d3, d3mYear);
    } finally {
      logger.setLevel(before);
    }

    String hashThird = line(DATE, "Test event E.").event().id().substring(18);
    assertThat(output.getAll())
        .contains("rule=D2 kept=" + hashThird)
        .contains("rule=D3 kept=")
        .contains("rule=D3m kept=")
        .contains("rule=D4")
        .doesNotContain("Test event")
        .doesNotContain("Test page")
        .doesNotContain("Test Cup")
        .doesNotContain("South Korea")
        .doesNotContain("2000-02-29")
        .doesNotContain("enwiki-");
  }

  @Test
  void genericLinksAreLoadedWithoutCommentsOrBlankLines() {
    assertThat(deduplicator.genericLinks())
        .contains("South Korea", "United States", "Montreal", "Calgary", "Seoul", "Timor-Leste")
        .doesNotContain(
            "", "1988 Winter Olympics", "1976 Summer Olympics", "2002 FIFA World Cup", "대한민국")
        .noneMatch(title -> title.startsWith("#") || !title.equals(title.strip()));
  }

  @ParameterizedTest(name = "min-link-overlap {0} is rejected")
  @ValueSource(doubles = {0, -0.1, 1.01, Double.NaN})
  void overlapOutsideZeroToOneIsRejected(double overlap) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new KoWikiDedupProperties(overlap))
        .withMessageContaining("wikimedia.dedup.min-link-overlap");
  }

  /**
   * The counterexample fixture through the whole source, with the link lookup failing: the cup is
   * the main link of the date line only, so D3m does not apply and 1 of 2 links is below 0.6.
   */
  @Test
  void counterexampleFixtureStaysTwoEventsThroughTheSource() {
    WikimediaProperties properties =
        new WikimediaProperties(
            "",
            Duration.ofSeconds(2),
            Duration.ofSeconds(4),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create("http://localhost:9/w/api.php"));
    KoWikiClient pages =
        title ->
            title.equals("February 29")
                ? new KoWikiPage(
                    title,
                    State.FRESH,
                    100L,
                    KoWikiFixtures.read("synthetic/dedup/synthetic-counterexample-date.wikitext"),
                    null,
                    READ)
                : new KoWikiPage(
                    title,
                    State.FRESH,
                    200L,
                    KoWikiFixtures.read("synthetic/dedup/synthetic-counterexample-year.wikitext"),
                    null,
                    READ);
    KoWikiEventSource source =
        new TwoPageKoWikiEventSource(
            pages,
            titles -> Optional.empty(),
            new WikitextEventParser(),
            new WikimediaHttpClient(properties, new MutableClock(READ, ClockConfig.SERVICE_ZONE)),
            properties,
            deduplicator);

    KoWikiDayResult result = source.find(LEAP_DAY);

    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .containsExactly(
            "Test Cup group A match A is won by Test team A.",
            "Test team B wins group A match B of the Test Cup.");
    assertThat(result.mergedCount()).isZero();
    assertThat(result.linkResolutionFailed()).isTrue();
  }
}
