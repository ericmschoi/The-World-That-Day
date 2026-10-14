package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a3b T0 (3): Unicode spaces (category Z) separate license name words, and the name is
 * stripped after separators become spaces. Spaces are written as escapes.
 */
class LicenseClassifierSpacesTest {

  @ParameterizedTest(name = "[{0}] is free")
  @ValueSource(
      strings = {
        "CC BY-SA 3.0",
        "CC BY 4.0",
        "Public domain ",
        "　CC0 1.0",
        "KOGL Type 1",
        "CC - BY SA 4.0"
      })
  void unicodeSpacesSeparateWords(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.FREE);
  }

  @ParameterizedTest(name = "[{0}] is other")
  @ValueSource(strings = {"CC BY-NC 4.0", "CC BY 4.0 NC", "KOGL　Type　3"})
  void nonFreeNamesStayOther(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.OTHER);
  }

  @ParameterizedTest(name = "[{0}] is unknown")
  @ValueSource(strings = {" 　", "　", "  "})
  void onlyUnicodeSpacesIsUnknown(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.UNKNOWN);
  }
}
