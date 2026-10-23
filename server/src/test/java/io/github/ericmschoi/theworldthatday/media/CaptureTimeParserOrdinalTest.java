package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a4 T5 (3): the parser reads an English ordinal day. The whole string must still be one
 * shape. Public historical event dates, the leap day and the plan's own example only.
 */
class CaptureTimeParserOrdinalTest {

  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "17th September 1988 | 1988-09-17",
        "September 17th, 1988 | 1988-09-17",
        "September 17th 1988 | 1988-09-17",
        "1st July 1969 | 1969-07-01",
        "20TH JULY 1969 | 1969-07-20",
        "29th February 2000 | 2000-02-29",
        "Taken on 20th September 1986 | 1986-09-20"
      })
  void readsAnOrdinalDay(String raw, String capturedAt) {
    assertThat(CaptureTimeParser.parse(raw))
        .isEqualTo(new CaptureTime(capturedAt, DatePrecision.DAY));
  }

  @ParameterizedTest(name = "\"{0}\" is unknown")
  @ValueSource(
      strings = {
        "29th February 1999", // not a leap year
        "17xx September 1988",
        "17th",
        "17th September 1988 / 18th September 1988"
      })
  void anOrdinalDoesNotLoosenTheShape(String raw) {
    assertThat(CaptureTimeParser.parse(raw)).isEqualTo(CaptureTime.UNKNOWN);
  }
}
