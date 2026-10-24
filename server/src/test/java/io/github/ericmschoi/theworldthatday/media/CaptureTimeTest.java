package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a4b T0 (2): the one rule R8 check that {@link MediaNormalizer} and video candidates share.
 * Dates are public event dates, the leap day 2000-02-29 and boundary values; none is a birthday.
 */
class CaptureTimeTest {

  @ParameterizedTest(name = "[{0}] {1} follows R8: {2}")
  @CsvSource(
      nullValues = "none",
      value = {
        "1969-07-20, DAY, true",
        "2000-02-29, DAY, true",
        "1900-02-28, DAY, true",
        "1900-02-29, DAY, false",
        "1969-02-30, DAY, false",
        "1969-7-20, DAY, false",
        "1969-07, DAY, false",
        "none, DAY, false",
        "1969-07, MONTH, true",
        "1969-13, MONTH, false",
        "1969-07-20, MONTH, false",
        "1969, YEAR, true",
        "1969-07-20, YEAR, false",
        "69, YEAR, false",
        "1969, APPROX, true",
        "c. 1969, APPROX, false",
        "none, APPROX, false",
        "none, CURRENT, true",
        "1969, CURRENT, false",
        "none, none, true",
        "1969, none, false",
        "'', DAY, false",
        "'', none, false"
      })
  void theShapeOfTheTimeFollowsItsPrecision(String at, DatePrecision precision, boolean follows) {
    assertThat(CaptureTime.followsRuleR8(at, precision)).isEqualTo(follows);
    assertThat(new CaptureTime(at, precision).followsRuleR8()).isEqualTo(follows);
  }
}
