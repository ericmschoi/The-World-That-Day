package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a4 T5 (1), (2), (5): dates written inside a text. Titles are the strings written in {@code
 * docs/research/video-sources.md}; dates are public historical event dates, the leap day 2000-02-29
 * and the plan's own parser examples, none of them anyone's birthday.
 */
@ExtendWith(OutputCaptureExtension.class)
class TextDatesTest {

  private static CaptureTime day(String at) {
    return new CaptureTime(at, DatePrecision.DAY);
  }

  static Stream<Arguments> oneDate() {
    return Stream.of(
        // ISO
        arguments("1969-07-20", day("1969-07-20")),
        arguments("제작일: 1988-09-17", day("1988-09-17")),
        // Korean, zero or one space between the parts
        arguments("1986년 9월 20일 오늘 #제10회 #아시안게임 #개막", day("1986-09-20")),
        arguments("1986년9월20일", day("1986-09-20")),
        arguments("1986년 9월20일", day("1986-09-20")),
        arguments("1986년 9월", new CaptureTime("1986-09", DatePrecision.MONTH)),
        arguments("1986년 서울아시안게임 개막식", new CaptureTime("1986", DatePrecision.YEAR)),
        // English, month first, with and without an ordinal, any case
        arguments("September 17, 1988", day("1988-09-17")),
        arguments("September 17th, 1988", day("1988-09-17")),
        arguments("SEPTEMBER 17, 1988", day("1988-09-17")),
        // English, day first
        arguments("17 September 1988", day("1988-09-17")),
        arguments("17th September 1988", day("1988-09-17")),
        arguments("filmed 1st July 1969.", day("1969-07-01")),
        arguments("September 1988", new CaptureTime("1988-09", DatePrecision.MONTH)),
        arguments("july 1969", new CaptureTime("1969-07", DatePrecision.MONTH)),
        // the leap day in every day shape
        arguments("2000-02-29", day("2000-02-29")),
        arguments("2000년 2월 29일", day("2000-02-29")),
        arguments("29 February 2000", day("2000-02-29")),
        arguments("February 29th, 2000", day("2000-02-29")),
        // the year range
        arguments("1000년", new CaptureTime("1000", DatePrecision.YEAR)),
        arguments("2999년", new CaptureTime("2999", DatePrecision.YEAR)));
  }

  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @MethodSource("oneDate")
  void readsEachShapeAsOneDateTheLongestShapeWinning(String text, CaptureTime expected) {
    assertThat(TextDates.find(text)).containsExactly(expected);
  }

  @ParameterizedTest(name = "\"{0}\" has no date")
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "1969", // a bare year
        "AP11 FINAL APPROACH.ogv",
        "대한뉴스 제 1715호-서울 올림픽 개막",
        "서울 지하철 7호선",
        "9월 20일", // no year
        "September 20", // no year
        "2000-02-30", // does not exist
        "1999년 2월 29일", // not a leap year: skipped whole, no month or year left
        "29 February 1999",
        "1988년 13월",
        "x19690720",
        "11969-07-20", // ISO glued to a digit
        "1969-07-201",
        "1980년대", // a decade
        "0999년",
        "3000년",
        "Septembers 1988"
      })
  void findsNothingThatIsNotADate(String text) {
    assertThat(TextDates.find(text)).isEmpty();
  }

  @Test
  void givesTheDatesInTheOrderTheyAppear() {
    assertThat(TextDates.find("1988-09-17 방송, 1986년 9월 20일 개막, 1969년 영상과 July 1969 자료"))
        .containsExactly(
            day("1988-09-17"),
            day("1986-09-20"),
            new CaptureTime("1969", DatePrecision.YEAR),
            new CaptureTime("1969-07", DatePrecision.MONTH));
  }

  @Test
  void skipsADateThatDoesNotExistAndKeepsTheRest() {
    assertThat(TextDates.find("2000-02-30 / 2000-02-29 / 1999년 2월 29일 / 20 September 1986"))
        .containsExactly(day("2000-02-29"), day("1986-09-20"));
  }

  @Test
  void writesNoLog(CapturedOutput output) {
    TextDates.find("1969-07-20 아폴로 11호");

    assertThat(output.getAll()).doesNotContain("1969", "아폴로");
    assertThat(Arrays.stream(TextDates.class.getDeclaredFields()).map(Field::getType))
        .noneMatch(type -> type.getName().startsWith("org.slf4j"));
  }
}
