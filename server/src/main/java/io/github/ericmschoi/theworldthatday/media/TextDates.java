package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the dates written inside a text, such as a video title or description (plan M3a4 T5),
 * unlike {@link CaptureTimeParser}, which reads a whole string of one shape. The shapes are ISO
 * {@code 1986-09-20}, Korean {@code 1986년 9월 20일}, {@code 1986년 9월} and {@code 1986년}, and English
 * {@code September 20, 1986}, {@code 20 September 1986} (both with an optional ordinal such as
 * {@code 20th}) and {@code September 1986}, English ignoring case, years 1000 to 2999. The longest
 * shape of one date wins, so {@code 1986년 9월 20일} is one day and no separate year. A bare number, a
 * decade ({@code 1980년대}), a date without a year and a date that does not exist are not dates. It
 * never logs.
 */
public final class TextDates {

  private static final String YEAR = "[12]\\d{3}";
  private static final String MONTH =
      "january|february|march|april|may|june|july|august|september|october|november|december";
  private static final String ORDINAL = "(?:st|nd|rd|th)?";

  /** One alternative per shape; each fills its own groups, the optional parts longest first. */
  private static final Pattern DATE =
      Pattern.compile(
          "(?<!\\d)(?<!\\d-)(?<iy>"
              + YEAR
              + ")-(?<im>\\d{2})-(?<id>\\d{2})(?!\\d)(?!-\\d)"
              + "|(?<!\\d)(?<ky>"
              + YEAR
              + ")년(?!대)(?:\\s?(?<km>\\d{1,2})월(?:\\s?(?<kd>\\d{1,2})일)?)?"
              + "|(?<!\\p{L})(?<am>"
              + MONTH
              + ")\\s(?:(?<ad>\\d{1,2})"
              + ORDINAL
              + ",?\\s)?(?<ay>"
              + YEAR
              + ")(?!\\d)"
              + "|(?<![\\p{L}\\p{N}])(?<bd>\\d{1,2})"
              + ORDINAL
              + "\\s(?<bm>"
              + MONTH
              + ")\\s(?<by>"
              + YEAR
              + ")(?!\\d)",
          Pattern.CASE_INSENSITIVE);

  private TextDates() {}

  /** The dates in {@code text} in the order they appear; empty for {@code null}. */
  public static List<CaptureTime> find(String text) {
    List<CaptureTime> dates = new ArrayList<>();
    Matcher m = DATE.matcher(text == null ? "" : text);
    while (m.find()) {
      try {
        dates.add(read(m));
      } catch (DateTimeException e) {
        // A date that does not exist, such as 2000-02-30, is skipped as a whole.
      }
    }
    return List.copyOf(dates);
  }

  private static CaptureTime read(Matcher m) {
    if (m.group("iy") != null) {
      return day(number(m, "iy"), number(m, "im"), number(m, "id"));
    }
    if (m.group("ky") != null) {
      if (m.group("km") == null) {
        return new CaptureTime(m.group("ky"), DatePrecision.YEAR);
      }
      return m.group("kd") == null
          ? month(number(m, "ky"), Month.of(number(m, "km")))
          : day(number(m, "ky"), number(m, "km"), number(m, "kd"));
    }
    if (m.group("am") != null) {
      Month month = monthName(m.group("am"));
      return m.group("ad") == null
          ? month(number(m, "ay"), month)
          : day(number(m, "ay"), month.getValue(), number(m, "ad"));
    }
    return day(number(m, "by"), monthName(m.group("bm")).getValue(), number(m, "bd"));
  }

  private static CaptureTime day(int year, int month, int day) {
    return new CaptureTime(LocalDate.of(year, month, day).toString(), DatePrecision.DAY);
  }

  private static CaptureTime month(int year, Month month) {
    return new CaptureTime(YearMonth.of(year, month).toString(), DatePrecision.MONTH);
  }

  private static int number(Matcher m, String group) {
    return Integer.parseInt(m.group(group));
  }

  private static Month monthName(String name) {
    return Month.valueOf(name.toUpperCase(Locale.ROOT));
  }
}
