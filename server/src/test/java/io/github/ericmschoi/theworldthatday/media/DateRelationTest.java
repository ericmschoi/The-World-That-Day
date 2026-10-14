package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The {@code date_relation} table of plan M3a3; {@code years} is always set. */
class DateRelationTest {

  @ParameterizedTest(name = "{1} {0} vs {2} -> {3} {4}")
  @CsvSource(
      nullValues = "null",
      value = {
        // every kind against the 1988-09-17 demo date
        "1988-09-17, DAY,     1988-09-17, SAME_DAY,     0",
        "1988-09-01, DAY,     1988-09-17, SAME_MONTH,   0",
        "1988-09,    MONTH,   1988-09-17, SAME_MONTH,   0",
        "1988-10-03, DAY,     1988-09-17, SAME_YEAR,    0",
        "1988-03,    MONTH,   1988-09-17, SAME_YEAR,    0",
        "1988,       YEAR,    1988-09-17, SAME_YEAR,    0",
        "1990-09-17, DAY,     1988-09-17, YEARS_APART,  2",
        "1987-09,    MONTH,   1988-09-17, YEARS_APART,  -1",
        "1983,       YEAR,    1988-09-17, YEARS_APART,  -5",
        "1988,       APPROX,  1988-09-17, APPROXIMATE,  0",
        "1985,       APPROX,  1988-09-17, APPROXIMATE,  -3",
        "null,       CURRENT, 1988-09-17, CURRENT,      0",
        "null,       null,    1988-09-17, UNKNOWN,      0",
        // leap day boundaries
        "2000-02-01, DAY,     2000-02-29, SAME_MONTH,   0",
        "1999-02-28, DAY,     2000-02-29, YEARS_APART,  -1",
        "2000-02-29, DAY,     2000-02-29, SAME_DAY,     0",
        "2000-03-01, DAY,     2000-02-29, SAME_YEAR,    0",
        "2004-02-29, DAY,     2000-02-29, YEARS_APART,  4",
        // same day and month, another year
        "2001-06-22, DAY,     2002-06-22, YEARS_APART,  -1",
        "2002-06,    MONTH,   2002-06-22, SAME_MONTH,   0",
      })
  void relationFollowsThePlanTable(
      String capturedAt, DatePrecision precision, LocalDate requested, Kind kind, int years) {
    assertThat(DateRelation.of(capturedAt, precision, requested))
        .isEqualTo(new DateRelation(kind, years));
  }
}
