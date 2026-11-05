package io.github.ericmschoi.theworldthatday.event.kowiki;

import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.DATE;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** English Wikipedia page titles (plan C1 S3a T2 (1)). */
class KoWikiPageKindTest {

  @Test
  void titlesHaveNoLeadingZeros() {
    assertThat(DATE.title(LocalDate.of(1988, 2, 13))).isEqualTo("February 13");
    assertThat(YEAR.title(LocalDate.of(1988, 2, 13))).isEqualTo("1988");
    assertThat(DATE.title(LocalDate.of(1976, 7, 17))).isEqualTo("July 17");
    assertThat(DATE.title(LocalDate.of(2010, 2, 12))).isEqualTo("February 12");
    assertThat(DATE.title(LocalDate.of(1980, 4, 12))).isEqualTo("April 12");
    assertThat(DATE.title(LocalDate.of(1969, 7, 1))).isEqualTo("July 1");
  }

  @Test
  void leapDayHasItsOwnDatePage() {
    assertThat(DATE.title(LocalDate.of(2000, 2, 29))).isEqualTo("February 29");
    assertThat(YEAR.title(LocalDate.of(2000, 2, 29))).isEqualTo("2000");
  }

  @Test
  void monthNamesAreEnglishWhateverTheDefaultLocale() {
    java.util.Locale before = java.util.Locale.getDefault();
    try {
      java.util.Locale.setDefault(java.util.Locale.KOREA);
      assertThat(DATE.title(LocalDate.of(1969, 12, 31))).isEqualTo("December 31");
    } finally {
      java.util.Locale.setDefault(before);
    }
  }
}
