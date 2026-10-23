package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one capture-time string from any source (for example Commons {@code DateTimeOriginal}) into
 * {@code captured_at} and {@code date_precision}, following the parser table of plan M3a3a. HTML
 * tags are stripped first. The whole text must have one of the table's shapes; anything else (two
 * dates, a range, a decade, a date that does not exist, an empty value) is unknown, which is the
 * safe side. The parser never returns {@code current}: only a source can say that. It never logs.
 */
public final class CaptureTimeParser {

  private static final int CI = Pattern.CASE_INSENSITIVE;

  private static final Pattern TAG = Pattern.compile("<[^>]*>");
  private static final Pattern SPACE = Pattern.compile("(?:\\s|\\u00A0|&nbsp;|&#160;)+");
  private static final Pattern TAKEN_ON = Pattern.compile("^taken on ", CI);
  private static final Pattern EXIF = Pattern.compile(" ?\\(exif\\)$", CI);

  /** Years 1000 to 2999, so EXIF zero placeholders such as {@code 0000} stay unknown. */
  private static final String YEAR = "([12]\\d{3})";

  private static final String MONTH_NAME =
      "(january|february|march|april|may|june|july|august|september|october|november|december)";

  /** An English ordinal after a day number, as in {@code 17th September 1988} (plan M3a4 T5). */
  private static final String ORDINAL = "(?:st|nd|rd|th)?";

  /** {@code 1988-09-17}, {@code 1988:09:17}, each with an optional time of day. */
  private static final Pattern NUMERIC_DAY =
      Pattern.compile(YEAR + "([-:])(\\d{2})\\2(\\d{2})(?:[ T](\\d{2}):(\\d{2})(?::(\\d{2}))?)?");

  private static final Pattern DAY_MONTH_YEAR =
      Pattern.compile("(\\d{1,2})" + ORDINAL + " " + MONTH_NAME + " " + YEAR, CI);
  private static final Pattern MONTH_DAY_YEAR =
      Pattern.compile(MONTH_NAME + " (\\d{1,2})" + ORDINAL + ",? " + YEAR, CI);
  private static final Pattern MONTH_YEAR = Pattern.compile(MONTH_NAME + " " + YEAR, CI);
  private static final Pattern NUMERIC_MONTH = Pattern.compile(YEAR + "-(\\d{2})");
  private static final Pattern YEAR_ONLY = Pattern.compile(YEAR);
  private static final Pattern APPROX_YEAR =
      Pattern.compile("(?:c\\. ?|ca\\. ?|circa |around )" + YEAR, CI);

  private record Shape(Pattern pattern, Function<Matcher, CaptureTime> read) {}

  private static final List<Shape> SHAPES =
      List.of(
          new Shape(NUMERIC_DAY, CaptureTimeParser::numericDay),
          new Shape(
              DAY_MONTH_YEAR, m -> day(LocalDate.of(number(m, 3), monthName(m, 2), number(m, 1)))),
          new Shape(
              MONTH_DAY_YEAR, m -> day(LocalDate.of(number(m, 3), monthName(m, 1), number(m, 2)))),
          new Shape(MONTH_YEAR, m -> month(YearMonth.of(number(m, 2), monthName(m, 1)))),
          new Shape(NUMERIC_MONTH, m -> month(YearMonth.of(number(m, 1), number(m, 2)))),
          new Shape(YEAR_ONLY, m -> new CaptureTime(m.group(1), DatePrecision.YEAR)),
          new Shape(APPROX_YEAR, m -> new CaptureTime(m.group(1), DatePrecision.APPROX)));

  private CaptureTimeParser() {}

  /** The capture time in {@code raw}, or {@link CaptureTime#UNKNOWN} when it cannot be read. */
  public static CaptureTime parse(String raw) {
    if (raw == null) {
      return CaptureTime.UNKNOWN;
    }
    String text = clean(raw);
    for (Shape shape : SHAPES) {
      Matcher m = shape.pattern().matcher(text);
      if (m.matches()) {
        try {
          return shape.read().apply(m);
        } catch (DateTimeException e) {
          return CaptureTime.UNKNOWN;
        }
      }
    }
    return CaptureTime.UNKNOWN;
  }

  /** Plain text without tags, single spaces, and without the "Taken on" and "(Exif)" wording. */
  private static String clean(String raw) {
    String text = TAG.matcher(raw).replaceAll(" ");
    text = SPACE.matcher(text).replaceAll(" ").strip();
    text = TAKEN_ON.matcher(text).replaceFirst("");
    return EXIF.matcher(text).replaceFirst("");
  }

  private static CaptureTime numericDay(Matcher m) {
    LocalDate date = LocalDate.of(number(m, 1), number(m, 3), number(m, 4));
    if (m.group(5) != null) {
      // Only checks that the time of day exists; the precision stays a day.
      LocalTime.of(number(m, 5), number(m, 6), m.group(7) == null ? 0 : number(m, 7));
    }
    return day(date);
  }

  private static CaptureTime day(LocalDate date) {
    return new CaptureTime(date.toString(), DatePrecision.DAY);
  }

  private static CaptureTime month(YearMonth month) {
    return new CaptureTime(month.toString(), DatePrecision.MONTH);
  }

  private static int number(Matcher m, int group) {
    return Integer.parseInt(m.group(group));
  }

  private static Month monthName(Matcher m, int group) {
    return Month.valueOf(m.group(group).toUpperCase(Locale.ROOT));
  }
}
