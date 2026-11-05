package io.github.ericmschoi.theworldthatday.event.kowiki;

import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.parserCase;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.DATE;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.Result;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The four Canada demo days and the video sample 1969-07-20 (plan C1 S3a decision 1 (b)) against
 * the recorded English Events sections of their date page and year page: event counts and
 * skipped-line counts (plan T3 (1), (5)), read off the recorded wikitext by hand. The merged {@code
 * KoWikiEventSource} result is EnWikiDemoDaysEventSourceTest.
 */
class DemoDaysParserTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private final WikitextEventParser parser = new WikitextEventParser();

  private Result parse(String date, String file) {
    LocalDate day = LocalDate.parse(date);
    KoWikiPageKind kind = file.startsWith(Integer.toString(day.getYear())) ? YEAR : DATE;
    return parser.parse(wikitext(file), kind, day);
  }

  @ParameterizedTest(name = "{0} {1}: {2} events, {3} skipped")
  @CsvSource({
    // 1988-02-13: only the year page, as a range line (February 13–28).
    "1988-02-13, 02-13-events.json, 0, 0",
    // year page skips: two month-only lines, three lines without a date
    "1988-02-13, 1988-events.json,  1, 5",
    // 1976-07-17: both pages; the date page skips [[AD 180|180]].
    "1976-07-17, 07-17-events.json, 2, 1",
    // year page skips: one month-only line, five lines without a date
    "1976-07-17, 1976-events.json,  2, 6",
    // 2010-02-12: only the year page, as a range line (February 12–28).
    "2010-02-12, 02-12-events.json, 0, 0",
    // year page skips: one line without a date, one month-only line
    "2010-02-12, 2010-events.json,  1, 2",
    // 1980-04-12: three date page lines (references removed), two year page sub-lines.
    "1980-04-12, 04-12-events.json, 3, 0",
    "1980-04-12, 1980-events.json,  2, 0",
    // 1969-07-20: the date page skips [[AD 70|70]] and [[911 (year)|911]].
    "1969-07-20, 07-20-events.json, 2, 2",
    // year page skips: a date key that links another page, a season line, two lines without a date
    "1969-07-20, 1969-events.json,  2, 4",
  })
  void demoDayCountsPerSection(String date, String file, int events, int skipped) {
    Result result = parse(date, file);

    assertThat(result.events()).hasSize(events);
    assertThat(result.skippedLines()).isEqualTo(skipped);
  }

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
    "1988-02-13, 1988-events.json",
    "1976-07-17, 07-17-events.json",
    "1976-07-17, 1976-events.json",
    "2010-02-12, 2010-events.json",
    "1980-04-12, 04-12-events.json",
    "1980-04-12, 1980-events.json",
    "1969-07-20, 07-20-events.json",
    "1969-07-20, 1969-events.json",
  })
  void demoDayTextsAreFreeOfWikiMarkup(String date, String file) {
    assertThat(parse(date, file).events())
        .isNotEmpty()
        .allSatisfy(
            event -> {
              assertThat(event.text())
                  .doesNotContain("[[", "]]", "''", "{{", "}}", "<ref", "<!--", "&nbsp;", "&ndash;")
                  .doesNotStartWith(" ")
                  .doesNotEndWith(" ")
                  .doesNotContain("  ");
              assertThat(event.linkTitles())
                  .allSatisfy(link -> assertThat(link).doesNotContain("_", "#", "|"));
            });
  }

  @Test
  void montrealOpeningIsInBothPagesWithTheSameLeadingLink() {
    // the source merges the year-page copy (D3m); here both pages expose the same leading link
    List<ParsedEvent> datePage = parse("1976-07-17", "07-17-events.json").events();
    List<ParsedEvent> yearPage = parse("1976-07-17", "1976-events.json").events();

    assertThat(datePage.get(1).linkTitles().getFirst()).isEqualTo("1976 Summer Olympics");
    assertThat(yearPage.getFirst().linkTitles().getFirst()).isEqualTo("1976 Summer Olympics");
  }

  @Test
  void yearPageSubLinesUnderTheDayAreAllKept() {
    assertThat(parse("1980-04-12", "1980-events.json").events())
        .extracting(ParsedEvent::text)
        .containsExactly(
            "1980 Liberian coup d'état: Samuel K. Doe overthrows the government of Liberia in a"
                + " violent coup d'état, executing President William Tolbert and ending over 130"
                + " years of democratic presidential succession in that country.",
            "Terry Fox begins his Marathon of Hope, a plan to run across Canada to raise money for"
                + " cancer research, setting off from St. John's, Newfoundland and running"
                + " westward.");
  }

  @Test
  void rangeAndCoupLinesKeepTheirOriginalSentence() {
    assertThat(parse("1988-02-13", "1988-events.json").events())
        .singleElement()
        .isEqualTo(
            new ParsedEvent(
                "The 1988 Winter Olympics are held in Calgary, Alberta, Canada.",
                List.of("1988 Winter Olympics", "Calgary", "Alberta"),
                DateKind.RANGE_START,
                LocalDate.of(1988, 2, 28)));
    assertThat(parse("1980-04-12", "04-12-events.json").events().getFirst())
        .isEqualTo(
            new ParsedEvent(
                "The Americo-Liberian government of Liberia is violently deposed.",
                List.of("1980 Liberian coup d'état")));
  }

  @Test
  void nonBreakingSpacesBecomeOneSpace() {
    assertThat(parser.parse(parserCase("nbsp"), DATE, LEAP_DAY).events())
        .extracting(ParsedEvent::text)
        .containsExactly("Test event R is held.");
  }

  @Test
  void subLinesWithTheSameLinkInOnePageStaySeparateAndDeeperTemplateLinesAreSkipped() {
    Result result = parser.parse(parserCase("same-link-sub-lines"), DATE, LEAP_DAY);

    assertThat(result.events())
        .containsExactly(
            new ParsedEvent("At Test page S, test event S one.", List.of("Test page S")),
            new ParsedEvent("At Test page S, test event S two.", List.of("Test page S")));
    assertThat(result.skippedLines()).isEqualTo(1);
  }
}
