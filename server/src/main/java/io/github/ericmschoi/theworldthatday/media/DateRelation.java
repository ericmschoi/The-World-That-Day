package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonValue;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * API only: how the capture time relates to the requested date (plan M3a3 {@code date_relation}).
 * {@code years} is always present: capture year minus requested year for {@code years_apart} and
 * {@code approximate}, otherwise 0.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record DateRelation(Kind kind, int years) {

  public enum Kind {
    SAME_DAY,
    SAME_MONTH,
    SAME_YEAR,
    YEARS_APART,
    APPROXIMATE,
    CURRENT,
    UNKNOWN;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * The relation of a capture time to {@code requested}. {@code capturedAt} must already have the
   * shape of {@code precision} (rule R8, checked by {@link MediaNormalizer}).
   */
  public static DateRelation of(String capturedAt, DatePrecision precision, LocalDate requested) {
    if (precision == null) {
      return new DateRelation(Kind.UNKNOWN, 0);
    }
    return switch (precision) {
      case CURRENT -> new DateRelation(Kind.CURRENT, 0);
      case APPROX ->
          new DateRelation(Kind.APPROXIMATE, Integer.parseInt(capturedAt) - requested.getYear());
      case DAY ->
          LocalDate.parse(capturedAt).equals(requested)
              ? new DateRelation(Kind.SAME_DAY, 0)
              : calendar(YearMonth.from(LocalDate.parse(capturedAt)), requested);
      case MONTH -> calendar(YearMonth.parse(capturedAt), requested);
      case YEAR -> byYear(Integer.parseInt(capturedAt), requested);
    };
  }

  private static DateRelation calendar(YearMonth captured, LocalDate requested) {
    return captured.equals(YearMonth.from(requested))
        ? new DateRelation(Kind.SAME_MONTH, 0)
        : byYear(captured.getYear(), requested);
  }

  private static DateRelation byYear(int capturedYear, LocalDate requested) {
    int years = capturedYear - requested.getYear();
    return years == 0
        ? new DateRelation(Kind.SAME_YEAR, 0)
        : new DateRelation(Kind.YEARS_APART, years);
  }
}
