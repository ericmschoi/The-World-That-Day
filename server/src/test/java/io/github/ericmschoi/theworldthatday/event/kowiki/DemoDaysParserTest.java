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
 * The three demo days (product brief 12) against all four recorded sections each (date page and
 * year page, "사건" and "문화"): event counts and skipped-line counts, read off the recorded wikitext
 * by hand. Plan M3a1 T4 (5) table input; the merged {@code KoWikiEventSource} result is T3
 * (separate PR).
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
    // 1988-09-17: every section has the opening; nothing skipped.
    "1988-09-17, 09-17-events.json,  1, 0",
    "1988-09-17, 09-17-culture.json, 1, 0",
    "1988-09-17, 1988-events.json,   1, 0",
    "1988-09-17, 1988-culture.json,  3, 0", // heading line + three sub-lines
    // 2002-06-22: only the culture sections have the day.
    "2002-06-22, 06-22-events.json,  0, 0",
    "2002-06-22, 06-22-culture.json, 1, 0",
    "2002-06-22, 2002-events.json,   0, 0",
    "2002-06-22, 2002-culture.json,  1, 0",
    // 1995-03-15: no event. Skips are lines whose year/date cannot be read, not templates:
    // date page "[[기원전 44년]]"; year page "[[1995년 5월|5월]]"; "* 날짜미상" + its two sub-lines.
    "1995-03-15, 03-15-events.json,  0, 1",
    "1995-03-15, 03-15-culture.json, 0, 0",
    "1995-03-15, 1995-events.json,   0, 1",
    "1995-03-15, 1995-culture.json,  0, 3",
  })
  void demoDayCountsPerSection(String date, String file, int events, int skipped) {
    Result result = parse(date, file);

    assertThat(result.events()).hasSize(events);
    assertThat(result.skippedLines()).isEqualTo(skipped);
  }

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
    "1988-09-17, 09-17-events.json",
    "1988-09-17, 09-17-culture.json",
    "1988-09-17, 1988-events.json",
    "1988-09-17, 1988-culture.json",
    "2002-06-22, 06-22-culture.json",
    "2002-06-22, 2002-culture.json",
  })
  void demoDayTextsAreFreeOfWikiMarkup(String date, String file) {
    assertThat(parse(date, file).events())
        .isNotEmpty()
        .allSatisfy(
            event -> {
              assertThat(event.text())
                  .doesNotContain("[[", "]]", "''", "{{", "}}", "<ref", "<!--", "&nbsp;")
                  .doesNotStartWith(" ")
                  .doesNotEndWith(" ")
                  .doesNotContain("  ");
              assertThat(event.linkTitles())
                  .allSatisfy(link -> assertThat(link).doesNotContain("_", "#", "|"));
            });
  }

  @Test
  void seoulOlympicsOpeningIsInBothPagesWithTheSameLeadingLink() {
    // T3 removes the year-page copy; here both pages must expose the same leading link.
    List<ParsedEvent> datePage = parse("1988-09-17", "09-17-events.json").events();
    List<ParsedEvent> yearPage = parse("1988-09-17", "1988-events.json").events();

    assertThat(datePage.getFirst().linkTitles().getFirst()).isEqualTo("1988년 하계 올림픽");
    assertThat(yearPage.getFirst().linkTitles().getFirst()).isEqualTo("1988년 하계 올림픽");
  }

  @Test
  void yearPageCultureSubLinesUnderTheDayAreAllKept() {
    assertThat(parse("1988-09-17", "1988-culture.json").events())
        .extracting(ParsedEvent::text)
        .containsExactly(
            "대한민국 서울특별시에서 제24회 하계 올림픽이 개최하다.",
            "KBS 1TV의 채널에서 광고 24개를 때려갔다.",
            "KBS 2TV의 채널에서 광고 37개를 때려갔다.");
  }

  @Test
  void quarterFinalLinesKeepTheirOriginalSentence() {
    assertThat(parse("2002-06-22", "06-22-culture.json").events())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.text())
                  .startsWith("2002 FIFA 월드컵에서 대한민국이 스페인에 승부차기 끝에")
                  .endsWith("(터키도 일한 만시즈의 결승골로 세네갈을 1:0으로 꺾고 4강에 진출함.)");
              assertThat(event.linkTitles()).containsExactly("2002 FIFA 월드컵");
            });
    assertThat(parse("2002-06-22", "2002-culture.json").events())
        .singleElement()
        .isEqualTo(
            new ParsedEvent(
                "대한민국이 2002년 FIFA 월드컵 8강전에서 스페인을 승부차기에서 5대 3으로 꺾고 준결승에 진출하였다.",
                List.of("2002년 FIFA 월드컵")));
  }

  @Test
  void nonBreakingSpacesBecomeOneSpace() {
    assertThat(parser.parse(parserCase("nbsp"), DATE, LEAP_DAY).events())
        .extracting(ParsedEvent::text)
        .containsExactly("시험 사건 파가 열렸다.");
  }

  @Test
  void subLinesWithTheSameLinkInOnePageStaySeparateAndDeeperTemplateLinesAreSkipped() {
    Result result = parser.parse(parserCase("same-link-sub-lines"), DATE, LEAP_DAY);

    assertThat(result.events())
        .containsExactly(
            new ParsedEvent("시험 문서 하에서 시험 사건 하 하나.", List.of("시험 문서 하")),
            new ParsedEvent("시험 문서 하에서 시험 사건 하 둘.", List.of("시험 문서 하")));
    assertThat(result.skippedLines()).isEqualTo(1);
  }
}
