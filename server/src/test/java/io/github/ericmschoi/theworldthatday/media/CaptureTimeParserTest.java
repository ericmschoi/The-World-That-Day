package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The capture-time parser table of plan M3a3a, one test value per row and shape. Dates are the demo
 * and boundary dates only (CLAUDE.md 7.6).
 */
class CaptureTimeParserTest {

  static Stream<Arguments> readable() {
    return Stream.of(
        // numeric day, with and without a time of day, ISO and EXIF separators
        arguments("1988-09-17", "1988-09-17", DatePrecision.DAY),
        arguments("1988-09-17 12:34:56", "1988-09-17", DatePrecision.DAY),
        arguments("1988:09:17 12:34:56", "1988-09-17", DatePrecision.DAY),
        // English day forms
        arguments("17 September 1988", "1988-09-17", DatePrecision.DAY),
        arguments("September 17, 1988", "1988-09-17", DatePrecision.DAY),
        arguments("Taken on 17 September 1988", "1988-09-17", DatePrecision.DAY),
        arguments("17 September 1988 (Exif)", "1988-09-17", DatePrecision.DAY),
        arguments(
            "<time datetime=\"1988-09-17\">17 September 1988</time>",
            "1988-09-17",
            DatePrecision.DAY),
        arguments(
            "<time datetime=\"2002-06-22\">22 June 2002</time>", "2002-06-22", DatePrecision.DAY),
        // month and year
        arguments("September 1988", "1988-09", DatePrecision.MONTH),
        arguments("1988-09", "1988-09", DatePrecision.MONTH),
        arguments("1988", "1988", DatePrecision.YEAR),
        // approximate
        arguments("c. 1988", "1988", DatePrecision.APPROX),
        arguments("circa 1988", "1988", DatePrecision.APPROX),
        arguments("ca. 1988", "1988", DatePrecision.APPROX),
        arguments("around 1988", "1988", DatePrecision.APPROX),
        // boundaries: leap day, surrounding spaces, case
        arguments("2000-02-29", "2000-02-29", DatePrecision.DAY),
        arguments("2000:02:29 00:00:00", "2000-02-29", DatePrecision.DAY),
        arguments("29 February 2000", "2000-02-29", DatePrecision.DAY),
        arguments("  JUNE 22, 2002  ", "2002-06-22", DatePrecision.DAY),
        arguments("2002-06-22 23:59:59", "2002-06-22", DatePrecision.DAY));
  }

  @ParameterizedTest(name = "\"{0}\" -> {1} {2}")
  @MethodSource("readable")
  void readsEveryShapeOfTheTable(String raw, String capturedAt, DatePrecision precision) {
    assertThat(CaptureTimeParser.parse(raw)).isEqualTo(new CaptureTime(capturedAt, precision));
  }

  @ParameterizedTest(name = "\"{0}\" is unknown")
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "   ",
        "1988-09-17 / 1988-09-18", // two different dates
        "1988-1990", // a range
        "1980s",
        "unknown",
        "not a date",
        "1988-02-30", // does not exist
        "1999-02-29", // not a leap year
        "30 February 1988",
        "1988-13",
        "1988:09:17 25:00:00", // a time of day that does not exist
        "0000:00:00 00:00:00", // EXIF placeholder
        "<time datetime=\"1988-09-17\"></time>", // only the attribute has a date
      })
  void unreadableTextIsUnknown(String raw) {
    CaptureTime time = CaptureTimeParser.parse(raw);

    assertThat(time).isEqualTo(CaptureTime.UNKNOWN);
    assertThat(time.capturedAt()).isNull();
    assertThat(time.datePrecision()).isNull();
    assertThat(time.isKnown()).isFalse();
  }

  @Test
  void neverReturnsCurrent() {
    Stream.concat(readable().map(a -> (String) a.get()[0]), Stream.of("current", "now", "today"))
        .map(CaptureTimeParser::parse)
        .forEach(time -> assertThat(time.datePrecision()).isNotEqualTo(DatePrecision.CURRENT));
  }

  @Test
  void takesOneSourceIndependentStringOnly() {
    Method[] parse =
        Arrays.stream(CaptureTimeParser.class.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .toArray(Method[]::new);

    assertThat(parse).hasSize(1);
    assertThat(parse[0].getName()).isEqualTo("parse");
    assertThat(parse[0].getParameterTypes()).containsExactly(String.class);
    assertThat(parse[0].getReturnType()).isEqualTo(CaptureTime.class);
  }

  static Stream<String> everyTestValue() {
    return Stream.concat(
        readable().map(a -> (String) a.get()[0]),
        Stream.of(
            "",
            "   ",
            "1988-09-17 / 1988-09-18",
            "1988-1990",
            "1980s",
            "unknown",
            "1988-02-30",
            "1988-13"));
  }

  /** Every parser result, put into a record, passes rule R8 of the normalizer. */
  @ParameterizedTest(name = "\"{0}\" passes R8")
  @MethodSource("everyTestValue")
  void everyResultPassesRuleR8(String raw) {
    CaptureTime time = CaptureTimeParser.parse(raw);
    Map<String, Object> fields = image();
    fields.put("captured_at", time.capturedAt());
    fields.put(
        "date_precision", time.datePrecision() == null ? null : time.datePrecision().value());

    assertThat(DEFAULT_NORMALIZER.normalize(record(fields), LocalDate.of(1988, 9, 17))).isPresent();
  }

  @Test
  void nullInputPassesRuleR8() {
    CaptureTime time = CaptureTimeParser.parse(null);
    Map<String, Object> fields = image();
    fields.put("captured_at", time.capturedAt());
    fields.put("date_precision", null);

    assertThat(DEFAULT_NORMALIZER.normalize(record(fields), LocalDate.of(2000, 2, 29))).isPresent();
  }
}
