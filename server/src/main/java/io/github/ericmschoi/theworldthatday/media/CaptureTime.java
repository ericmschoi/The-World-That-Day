package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * A capture time read by {@link CaptureTimeParser}: the {@code captured_at} and {@code
 * date_precision} values of a {@link MediaRecord}. Both are {@code null} when the time is unknown
 * ("촬영 시점 미상"). The shape of {@code capturedAt} always follows {@code datePrecision} (rule R8).
 */
public record CaptureTime(String capturedAt, DatePrecision datePrecision) {

  /** The capture time is unknown: both values are {@code null}. */
  public static final CaptureTime UNKNOWN = new CaptureTime(null, null);

  private static final Pattern YEAR = Pattern.compile("\\d{4}");
  private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
  private static final Pattern DAY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  public boolean isKnown() {
    return datePrecision != null;
  }

  /** Whether this time follows rule R8 ({@link #followsRuleR8(String, DatePrecision)}). */
  public boolean followsRuleR8() {
    return followsRuleR8(capturedAt, datePrecision);
  }

  /**
   * Rule R8: the shape of {@code at} follows {@code precision} — an existing {@code YYYY-MM-DD}
   * day, an existing {@code YYYY-MM} month, a {@code YYYY} year ({@code year} and {@code approx}),
   * and no value for {@code current} or an unknown precision.
   */
  public static boolean followsRuleR8(String at, DatePrecision precision) {
    if (precision == null || precision == DatePrecision.CURRENT) {
      return at == null;
    }
    if (at == null) {
      return false;
    }
    try {
      return switch (precision) {
        case DAY -> DAY.matcher(at).matches() && LocalDate.parse(at) != null;
        case MONTH -> MONTH.matcher(at).matches() && YearMonth.parse(at) != null;
        case YEAR, APPROX -> YEAR.matcher(at).matches();
        case CURRENT -> false;
      };
    } catch (DateTimeException e) {
      return false;
    }
  }
}
