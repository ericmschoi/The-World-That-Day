package io.github.ericmschoi.theworldthatday.place;

import static io.github.ericmschoi.theworldthatday.place.NameFold.fold;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plan C2 {@code fold}, T1 (1). Inputs are the demo and same-name places and their spellings. */
class NameFoldTest {

  @Test
  void accentsAreDropped() {
    assertThat(fold("Montreal")).isEqualTo(fold("Montréal")).isEqualTo("montreal");
  }

  @Test
  void enDashAndHyphenAreTheSame() {
    assertThat(fold("Mercier–Hochelaga-Maisonneuve"))
        .isEqualTo(fold("Mercier-Hochelaga-Maisonneuve"))
        .isEqualTo("mercier hochelaga maisonneuve");
  }

  @Test
  void stJohnsStaysApartFromSaintJohn() {
    assertThat(fold("St. John's")).isNotEqualTo(fold("Saint John")).isEqualTo("saint john's");
  }

  @Test
  void stJohnIsSaintJohn() {
    assertThat(fold("St. John")).isEqualTo(fold("Saint John")).isEqualTo("saint john");
  }

  @Test
  void fortStJohnIsNotSaintJohn() {
    assertThat(fold("Fort St. John")).isEqualTo("fort saint john").isNotEqualTo(fold("Saint John"));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "St John's|saint john's",
        "st. john’s|saint john's",
        "ST. JOHN‘S|saint john's",
        "  Saint   John  |saint john",
        "Mercier--Hochelaga-Maisonneuve|mercier hochelaga maisonneuve",
        "Mercier—Hochelaga‑Maisonneuve|mercier hochelaga maisonneuve",
        "hochelaga maisonneuve|hochelaga maisonneuve",
        "MONTRÉAL|montreal",
        // The next input is "e" followed by a combining acute accent (U+0301, NFD form).
        "Montréal|montreal",
        "Ste-Victoria|sainte victoria",
        "Ste. Victoria|sainte victoria",
        "Sainte Victoria|sainte victoria",
        "St.-Victoria|saint victoria",
        "Victoria B.C.|victoria bc",
      })
  void spellingsFoldToOneKey(String input, String expected) {
    assertThat(fold(input)).isEqualTo(expected);
  }

  @Test
  void onlyWholeWordsBecomeSaint() {
    assertThat(fold("Stanley Victoria")).isEqualTo("stanley victoria");
    assertThat(fold("Saintjohn")).isEqualTo("saintjohn");
  }

  @Test
  void emptyAndPunctuationOnlyFoldToEmpty() {
    assertThat(fold("")).isEmpty();
    assertThat(fold(" - . – ")).isEmpty();
  }
}
