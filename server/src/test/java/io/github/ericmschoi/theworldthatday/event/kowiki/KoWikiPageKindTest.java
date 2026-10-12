package io.github.ericmschoi.theworldthatday.event.kowiki;

import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.DATE;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPageKind.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class KoWikiPageKindTest {

  @Test
  void titlesHaveNoLeadingZeros() {
    assertThat(DATE.title(LocalDate.of(1988, 9, 17))).isEqualTo("9월 17일");
    assertThat(YEAR.title(LocalDate.of(1988, 9, 17))).isEqualTo("1988년");
    assertThat(DATE.title(LocalDate.of(2002, 6, 22))).isEqualTo("6월 22일");
    assertThat(DATE.title(LocalDate.of(1995, 3, 15))).isEqualTo("3월 15일");
  }

  @Test
  void leapDayHasItsOwnDatePage() {
    assertThat(DATE.title(LocalDate.of(2000, 2, 29))).isEqualTo("2월 29일");
    assertThat(YEAR.title(LocalDate.of(2000, 2, 29))).isEqualTo("2000년");
  }
}
