package io.github.ericmschoi.theworldthatday.event.kowiki;

import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.parserCase;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.DATE;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.Result;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a1 T2 (2) recorded fixtures and (3) synthetic boundary cases, plan M3a1b T1 (4) HTML tags
 * and entities, and plan C1 S3a T3 on English Wikipedia: recorded date and year pages of the demo
 * days, range lines and the English keys.
 */
class WikitextEventParserTest {

  private static final LocalDate CALGARY = LocalDate.of(1988, 2, 13);
  private static final LocalDate MONTREAL = LocalDate.of(1976, 7, 17);
  private static final LocalDate VANCOUVER = LocalDate.of(2010, 2, 12);
  private static final LocalDate LIBERIA = LocalDate.of(1980, 4, 12);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private final WikitextEventParser parser = new WikitextEventParser();

  private static List<String> texts(Result result) {
    return result.events().stream().map(ParsedEvent::text).toList();
  }

  @Nested
  class RecordedFixtures {

    @Test
    void datePageEventsGiveEastTimorAndTheMontrealOpening() {
      Result result = parser.parse(wikitext("07-17-events.json"), DATE, MONTREAL);

      assertThat(result.events()).hasSize(2);
      ParsedEvent opening = result.events().get(1);
      assertThat(opening.text())
          .startsWith(
              "The opening of the Summer Olympics in Montreal is marred by 25 African teams")
          .endsWith("in South African sporting events during apartheid.");
      assertThat(opening.linkTitles())
          .containsExactly(
              "1976 Summer Olympics",
              "Montreal",
              "New Zealand",
              "IOC",
              "South Africa",
              "apartheid");
      assertThat(opening.dateKind()).isEqualTo(DateKind.DAY);
      assertThat(result.skippedLines()).as("[[AD 180|180]]: its year cannot be read").isEqualTo(1);
    }

    /** T3 (4): the text is the original sentence with only the link brackets removed (D5 (a)). */
    @Test
    void datePageLineIsTheOriginalSentenceWithoutLinkMarkupAndKeepsEveryLinkInOrder() {
      String raw =
          "*[[1976]] &ndash; [[East Timor]] is [[Indonesian invasion of East Timor|annexed]] and"
              + " becomes the 27th [[Provinces of Indonesia|province]] of [[Indonesia]].";
      assertThat(wikitext("07-17-events.json").lines()).contains(raw);

      Result result = parser.parse(wikitext("07-17-events.json"), DATE, MONTREAL);

      assertThat(result.events().getFirst())
          .isEqualTo(
              new ParsedEvent(
                  "East Timor is annexed and becomes the 27th province of Indonesia.",
                  List.of(
                      "East Timor",
                      "Indonesian invasion of East Timor",
                      "Provinces of Indonesia",
                      "Indonesia")));
    }

    @Test
    void datePagesOfTheOlympicDaysHaveNoEventOfThatYear() {
      Result calgary = parser.parse(wikitext("02-13-events.json"), DATE, CALGARY);
      Result vancouver = parser.parse(wikitext("02-12-events.json"), DATE, VANCOUVER);

      assertThat(calgary.events()).isEmpty();
      assertThat(vancouver.events()).isEmpty();
      assertThat(calgary.skippedLines()).isZero();
      assertThat(vancouver.skippedLines()).isZero();
    }

    @Test
    void datePageLinesWithReferencesKeepTheSentence() {
      Result result = parser.parse(wikitext("04-12-events.json"), DATE, LIBERIA);

      assertThat(texts(result))
          .containsExactly(
              "The Americo-Liberian government of Liberia is violently deposed.",
              "Transbrasil Flight 303, a Boeing 727, crashes on approach to Hercílio Luz"
                  + " International Airport in Florianópolis, Brazil. Fifty-five out of the 58"
                  + " people on board are killed.",
              "Canadian runner and athlete Terry Fox begins his Marathon of Hope Run in St. John's,"
                  + " NF");
      assertThat(result.events().get(2).linkTitles())
          .containsExactly("Terry Fox", "St. John's, Newfoundland and Labrador");
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void yearPageOlympicsLineIsARangeStartWithItsLastDay() {
      Result result = parser.parse(wikitext("1988-events.json"), YEAR, CALGARY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent(
                  "The 1988 Winter Olympics are held in Calgary, Alberta, Canada.",
                  List.of("1988 Winter Olympics", "Calgary", "Alberta"),
                  DateKind.RANGE_START,
                  LocalDate.of(1988, 2, 28)));
      assertThat(result.skippedLines()).isEqualTo(5);
    }

    @Test
    void vancouverLineIsARangeStartToo() {
      Result result = parser.parse(wikitext("2010-events.json"), YEAR, VANCOUVER);

      assertThat(result.events())
          .singleElement()
          .satisfies(
              event -> {
                assertThat(event.text())
                    .isEqualTo(
                        "The 2010 Winter Olympics are held in Vancouver and Whistler, Canada.");
                assertThat(event.dateKind()).isEqualTo(DateKind.RANGE_START);
                assertThat(event.rangeEnd()).isEqualTo(LocalDate.of(2010, 2, 28));
              });
    }

    @ParameterizedTest(name = "1988-{0}")
    @ValueSource(strings = {"02-14", "02-20", "02-28"})
    void daysInsideTheRangeAreNotItsEvents(String day) {
      Result result =
          parser.parse(wikitext("1988-events.json"), YEAR, LocalDate.parse("1988-" + day));

      assertThat(texts(result)).noneMatch(text -> text.contains("Winter Olympics"));
    }

    @Test
    void yearPageSubLinesInheritTheDate() {
      Result result = parser.parse(wikitext("1976-events.json"), YEAR, MONTREAL);

      assertThat(texts(result))
          .containsExactly(
              "The 1976 Summer Olympics begin in Montreal, Quebec, Canada.",
              "East Timor is declared the 27th province of Indonesia.");
      assertThat(result.events()).extracting(ParsedEvent::dateKind).containsOnly(DateKind.DAY);
      assertThat(result.skippedLines()).isEqualTo(6);
    }

    @Test
    void yearPageSubLinesOfTheCoupDay() {
      Result result = parser.parse(wikitext("1980-events.json"), YEAR, LIBERIA);

      assertThat(result.events())
          .extracting(event -> event.linkTitles().getFirst())
          .containsExactly("1980 Liberian coup d'état", "Terry Fox");
      assertThat(result.skippedLines()).isZero();
    }
  }

  @Nested
  class SyntheticCases {

    @Test
    void aliasLinkShowsTheAliasAndKeepsTheTargetTitle() {
      Result result = parser.parse(parserCase("alias-link"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent(
                  "Test event A is held at Test page B#Section.",
                  List.of("Test page A", "Test page B")));
    }

    @Test
    void boldAndItalicQuotesAreRemoved() {
      Result result = parser.parse(parserCase("bold"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event B is held.");
    }

    @Test
    void referencesAndCommentsAreRemovedBeforeTheTemplateCheck() {
      Result result = parser.parse(parserCase("ref-comment"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event C is held.");
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void fileAndCategoryLinksAreRemovedWithTheirCaptions() {
      Result result = parser.parse(parserCase("file-link"), DATE, LEAP_DAY);

      assertThat(result.events()).containsExactly(new ParsedEvent("Test event D.", List.of()));
    }

    @Test
    void subLinesInheritTheYearOfTheDatePageHeading() {
      Result result = parser.parse(parserCase("sub-lines-date"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent("At Test page E, test event E one.", List.of("Test page E")),
              new ParsedEvent("Test event E two.", List.of()));
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void lineWithBodyAndItsSubLinesAreSeparateEventsOnTheYearPage() {
      Result result = parser.parse(parserCase("sub-lines-year"), YEAR, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event F one.", "Test event F two.");
    }

    @Test
    void yearPageAcceptsAnUnlinkedDate() {
      Result result = parser.parse(parserCase("unlinked-date"), YEAR, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event G.");
    }

    @Test
    void datePageAcceptsAnUnlinkedSecondLineOfTheYear() {
      Result result =
          parser.parse(wikitext("synthetic/synthetic-02-29-events.json"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event A opens.", "Test event B.");
    }

    @Test
    void everySeparatorSeparates() {
      Result result = parser.parse(parserCase("en-dash"), DATE, LEAP_DAY);

      assertThat(texts(result))
          .as("–, -, &mdash; and —")
          .containsExactly(
              "Test event H one.",
              "Test event H two.",
              "Test event H three.",
              "Test event H four.");
    }

    @Test
    void templateLinesOfTheDateAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("template"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event I two.");
      assertThat(result.skippedLines()).isEqualTo(2); // the line and the sub-line of 2000
    }

    @Test
    void externalLinkLinesAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("external-link"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event J two.");
      assertThat(result.skippedLines()).isEqualTo(1);
    }

    @Test
    void unreadableLeadingYearsAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("unknown-key"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event K.");
      assertThat(result.skippedLines()).as("44 BC, AD 70 and Date unknown").isEqualTo(3);
    }

    @Test
    void missingSectionGivesNothing() {
      assertThat(parser.parse(null, DATE, LEAP_DAY)).isEqualTo(new Result(List.of(), 0));
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

  /** Plan C1 S3a T3 (3): range lines of a year page are events of their first day only. */
  @Nested
  class Ranges {

    @Test
    void rangesStartingOnTheDateAreRangeStartsWithTheirLastDay() {
      Result result = parser.parse(parserCase("ranges"), YEAR, LEAP_DAY);

      assertThat(result.events())
          .extracting(ParsedEvent::text, ParsedEvent::dateKind, ParsedEvent::rangeEnd)
          .containsExactly(
              tuple("Test range A across a month.", DateKind.RANGE_START, LocalDate.of(2000, 3, 3)),
              tuple("Test range B without links.", DateKind.RANGE_START, LocalDate.of(2000, 3, 2)),
              tuple("Test range C with spaces.", DateKind.RANGE_START, LocalDate.of(2000, 3, 5)),
              tuple("Test range D sub-line one.", DateKind.RANGE_START, LocalDate.of(2000, 3, 4)),
              tuple("Test range D sub-line two.", DateKind.RANGE_START, LocalDate.of(2000, 3, 4)),
              tuple("Test event of the day.", DateKind.DAY, null));
      assertThat(result.skippedLines())
          .as("the range ending on February 31 and the month-only line; not the range of the 28th")
          .isEqualTo(2);
    }

    @ParameterizedTest(name = "2000-{0}")
    @ValueSource(strings = {"03-01", "03-02", "03-03"})
    void daysInsideOrAtTheEndOfARangeGetNothing(String day) {
      Result result = parser.parse(parserCase("ranges"), YEAR, LocalDate.parse("2000-" + day));

      assertThat(result.events()).isEmpty();
    }

    @Test
    void rangeEndingOnTheStartDayIsNotARange() {
      Result result =
          parser.parse(
              "==Events==\n* [[February 29]]–[[February 29|29]] – Test range of one day.",
              YEAR,
              LEAP_DAY);

      assertThat(result.events()).isEmpty();
      assertThat(result.skippedLines()).as("its last day cannot be read as a range").isEqualTo(1);
    }

    @Test
    void lastDayBeforeTheFirstIsInTheNextYear() {
      Result result = parser.parse(parserCase("range-year-end"), YEAR, LocalDate.of(2000, 12, 30));

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent(
                  "Test range E over the new year.",
                  List.of(),
                  DateKind.RANGE_START,
                  LocalDate.of(2001, 1, 2)));
    }

    @Test
    void monthOnlyLinesAreNeverEventsOfADay() {
      for (LocalDate day : List.of(LocalDate.of(2000, 2, 1), LEAP_DAY)) {
        assertThat(parser.parse(parserCase("ranges"), YEAR, day).events())
            .extracting(ParsedEvent::text)
            .doesNotContain("Test event of a month only.");
      }
    }

    @Test
    void datePageLinesAreNeverRanges() {
      Result result =
          parser.parse("==Events==\n*[[2000]] &ndash; Test event 1–2 of a score.", DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(new ParsedEvent("Test event 1–2 of a score.", List.of()));
    }
  }

  @Nested
  class HtmlTagsAndEntities {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"<br>", "<small>", "<sup>", "<span style="})
    void lineWithTheTagIsSkippedAndCounted(String tag) {
      String line =
          parserCase("html-tags").lines().filter(l -> l.contains(tag)).findFirst().orElseThrow();

      Result result = parser.parse("==Events==\n" + line, DATE, LEAP_DAY);

      assertThat(result.events()).isEmpty();
      assertThat(result.skippedLines()).isEqualTo(1);
    }

    @Test
    void tagLinesAndTagSubLinesOfTheDateAreSkippedButPlainAngleBracketsStay() {
      Result result = parser.parse(parserCase("html-tags"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test event Q: 1 < 2 > 0.");
      assertThat(result.skippedLines()).isEqualTo(5); // four lines and one sub-line; not 2001
    }

    @Test
    void listedAndNumericEntitiesBecomeTheirCharacters() {
      Result result = parser.parse(parserCase("entities"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent("Test event A & B.", List.of()),
              new ParsedEvent("Test event C 1–2.", List.of()),
              new ParsedEvent("Test event D 3–4.", List.of()),
              new ParsedEvent("Test event E 5–6.", List.of()),
              new ParsedEvent("Test event G <small> markup and &lt; markup.", List.of()),
              new ParsedEvent("Test event H… is held.", List.of("Test page H&I")));
    }

    @Test
    void unknownNamedEntityAndNonCharacterLinesAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("entities"), DATE, LEAP_DAY);

      assertThat(texts(result)).noneMatch(text -> text.contains("Test event F"));
      assertThat(texts(result)).noneMatch(text -> text.contains("Test event J"));
      assertThat(result.skippedLines()).isEqualTo(2); // &foo; and &#xD800;, not the 2001 line
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "&quot;Test&quot; &apos;event&apos; &mdash; &middot; &gt;",
          "&#34;Test&#34; &#39;event&#39; &#x2014; &#xB7; &#X3E;"
        })
    void everyListedNameMatchesItsCodePoint(String body) {
      Result result = parser.parse("==Events==\n*[[2000]] &ndash; " + body, DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("\"Test\" 'event' — · >");
    }

    @Test
    void ampersandWithoutSemicolonIsLeftAlone() {
      Result result =
          parser.parse("==Events==\n*[[2000]] &ndash; Test A&B event &amp end.", DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("Test A&B event &amp end.");
    }
  }
}
