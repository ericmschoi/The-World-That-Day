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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a1 T2 (2) recorded fixtures and (3) synthetic boundary cases; plan M3a1b T1 (4) HTML tags
 * and entities.
 */
class WikitextEventParserTest {

  private static final LocalDate SEOUL_OLYMPICS = LocalDate.of(1988, 9, 17);
  private static final LocalDate QUARTER_FINAL = LocalDate.of(2002, 6, 22);
  private static final LocalDate EMPTY_DAY = LocalDate.of(1995, 3, 15);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  private final WikitextEventParser parser = new WikitextEventParser();

  private static List<String> texts(Result result) {
    return result.events().stream().map(ParsedEvent::text).toList();
  }

  @Nested
  class RecordedFixtures {

    @Test
    void datePageEventsGiveTheSeoulOlympicsOpeningOnce() {
      Result result = parser.parse(wikitext("09-17-events.json"), DATE, SEOUL_OLYMPICS);

      assertThat(result.events()).hasSize(1);
      ParsedEvent opening = result.events().getFirst();
      assertThat(opening.text()).isEqualTo("제24회 서울 하계 올림픽 개막.");
      assertThat(opening.linkTitles()).containsExactly("1988년 하계 올림픽");
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void datePageCultureLineKeepsEveryLinkInOrder() {
      Result result = parser.parse(wikitext("09-17-culture.json"), DATE, SEOUL_OLYMPICS);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent(
                  "1988년 하계 올림픽이 대한민국 서울에서 개막하였다.", List.of("1988년 하계 올림픽", "대한민국", "서울")));
    }

    @Test
    void emptyDayHasNoEventsInTheDatePage() {
      Result events = parser.parse(wikitext("03-15-events.json"), DATE, EMPTY_DAY);
      Result culture = parser.parse(wikitext("03-15-culture.json"), DATE, EMPTY_DAY);

      assertThat(events.events()).isEmpty();
      assertThat(culture.events()).isEmpty();
      assertThat(events.skippedLines()).isEqualTo(1); // the BC line: its year cannot be read
    }

    @Test
    void quarterFinalIsInTheCultureSection() {
      Result result = parser.parse(wikitext("06-22-culture.json"), DATE, QUARTER_FINAL);

      assertThat(texts(result)).isNotEmpty().allMatch(text -> text.contains("스페인"));
      assertThat(result.events().getFirst().linkTitles()).containsExactly("2002 FIFA 월드컵");
      assertThat(parser.parse(wikitext("06-22-events.json"), DATE, QUARTER_FINAL).events())
          .isEmpty();
    }

    @Test
    void yearPageEventsHaveTheOpeningWithoutBoldQuotes() {
      Result result = parser.parse(wikitext("1988-events.json"), YEAR, SEOUL_OLYMPICS);

      assertThat(texts(result)).containsExactly("1988 제24회 서울 올림픽이 개막하였다.");
      assertThat(result.events().getFirst().linkTitles()).containsExactly("1988년 하계 올림픽");
    }

    @Test
    void yearPageCultureSubLinesInheritTheDate() {
      Result result = parser.parse(wikitext("1988-culture.json"), YEAR, SEOUL_OLYMPICS);

      assertThat(result.events()).hasSize(3);
      assertThat(result.events().getFirst().text()).isEqualTo("대한민국 서울특별시에서 제24회 하계 올림픽이 개최하다.");
    }

    @Test
    void yearPageQuarterFinal() {
      Result result = parser.parse(wikitext("2002-culture.json"), YEAR, QUARTER_FINAL);

      assertThat(texts(result)).hasSize(1).allMatch(text -> text.contains("스페인"));
    }

    @Test
    void emptyDayHasNoEventsInTheYearPage() {
      assertThat(parser.parse(wikitext("1995-events.json"), YEAR, EMPTY_DAY).events()).isEmpty();
      assertThat(parser.parse(wikitext("1995-culture.json"), YEAR, EMPTY_DAY).events()).isEmpty();
    }
  }

  @Nested
  class SyntheticCases {

    @Test
    void aliasLinkShowsTheAliasAndKeepsTheTargetTitle() {
      Result result = parser.parse(parserCase("alias-link"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent("시험 사건 가가 시험 문서 나#절에서 열렸다.", List.of("시험 문서 가", "시험 문서 나")));
    }

    @Test
    void boldAndItalicQuotesAreRemoved() {
      Result result = parser.parse(parserCase("bold"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 나가 열렸다.");
    }

    @Test
    void referencesAndCommentsAreRemovedBeforeTheTemplateCheck() {
      Result result = parser.parse(parserCase("ref-comment"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 다 개최.");
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void fileAndCategoryLinksAreRemovedWithTheirCaptions() {
      Result result = parser.parse(parserCase("file-link"), DATE, LEAP_DAY);

      assertThat(result.events()).containsExactly(new ParsedEvent("시험 사건 라.", List.of()));
    }

    @Test
    void subLinesInheritTheYearOfTheDatePageHeading() {
      Result result = parser.parse(parserCase("sub-lines-date"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent("시험 문서 마에서 시험 사건 마 하나.", List.of("시험 문서 마")),
              new ParsedEvent("시험 사건 마 둘.", List.of()));
      assertThat(result.skippedLines()).isZero();
    }

    @Test
    void lineWithBodyAndItsSubLinesAreSeparateEventsOnTheYearPage() {
      Result result = parser.parse(parserCase("sub-lines-year"), YEAR, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 바 하나.", "시험 사건 바 둘.");
    }

    @Test
    void yearPageAcceptsAnUnlinkedDate() {
      Result result = parser.parse(parserCase("unlinked-date"), YEAR, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 사.");
    }

    @Test
    void enDashSeparatesLikeAHyphen() {
      Result result = parser.parse(parserCase("en-dash"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 아.");
    }

    @Test
    void templateLinesOfTheDateAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("template"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 자 둘.");
      assertThat(result.skippedLines()).isEqualTo(2); // the line and the sub-line of 2000
    }

    @Test
    void externalLinkLinesAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("external-link"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 차 둘.");
      assertThat(result.skippedLines()).isEqualTo(1);
    }

    @Test
    void dateRangeLinesAreSkipped() {
      Result result = parser.parse(parserCase("date-range"), YEAR, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 카 둘.");
      assertThat(result.skippedLines()).isEqualTo(1); // only the range that starts on the date
    }

    @Test
    void unreadableLeadingYearsAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("unknown-key"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 타.");
      assertThat(result.skippedLines()).isEqualTo(2);
    }

    @Test
    void missingSectionGivesNothing() {
      assertThat(parser.parse(null, DATE, LEAP_DAY)).isEqualTo(new Result(List.of(), 0));
    }
  }

  @Nested
  class HtmlTagsAndEntities {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"<br>", "<small>", "<sup>", "<span style="})
    void lineWithTheTagIsSkippedAndCounted(String tag) {
      String line =
          parserCase("html-tags").lines().filter(l -> l.contains(tag)).findFirst().orElseThrow();

      Result result = parser.parse("== 사건 ==\n" + line, DATE, LEAP_DAY);

      assertThat(result.events()).isEmpty();
      assertThat(result.skippedLines()).isEqualTo(1);
    }

    @Test
    void tagLinesAndTagSubLinesOfTheDateAreSkippedButPlainAngleBracketsStay() {
      Result result = parser.parse(parserCase("html-tags"), DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 사건 버: 1 < 2 > 0.");
      assertThat(result.skippedLines()).isEqualTo(5); // four lines and one sub-line; not 2001
    }

    @Test
    void listedAndNumericEntitiesBecomeTheirCharacters() {
      Result result = parser.parse(parserCase("entities"), DATE, LEAP_DAY);

      assertThat(result.events())
          .containsExactly(
              new ParsedEvent("시험 사건 가 & 나.", List.of()),
              new ParsedEvent("시험 사건 다 1–2.", List.of()),
              new ParsedEvent("시험 사건 라 3–4.", List.of()),
              new ParsedEvent("시험 사건 마 5–6.", List.of()),
              new ParsedEvent("시험 사건 사 <small> 표기와 &lt; 표기.", List.of()),
              new ParsedEvent("시험 사건 아… 열렸다.", List.of("시험 문서 아&자")));
    }

    @Test
    void unknownNamedEntityAndNonCharacterLinesAreSkippedAndCounted() {
      Result result = parser.parse(parserCase("entities"), DATE, LEAP_DAY);

      assertThat(texts(result)).noneMatch(text -> text.contains("시험 사건 바"));
      assertThat(texts(result)).noneMatch(text -> text.contains("시험 사건 차"));
      assertThat(result.skippedLines()).isEqualTo(2); // &foo; and &#xD800;, not the 2001 line
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "&quot;시험&quot; &apos;사건&apos; &mdash; &middot; &gt;",
          "&#34;시험&#34; &#39;사건&#39; &#x2014; &#xB7; &#X3E;"
        })
    void everyListedNameMatchesItsCodePoint(String body) {
      Result result = parser.parse("== 사건 ==\n* [[2000년]] - " + body, DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("\"시험\" '사건' — · >");
    }

    @Test
    void ampersandWithoutSemicolonIsLeftAlone() {
      Result result = parser.parse("== 사건 ==\n* [[2000년]] - 시험 A&B 사건 &amp 끝.", DATE, LEAP_DAY);

      assertThat(texts(result)).containsExactly("시험 A&B 사건 &amp 끝.");
    }
  }
}
