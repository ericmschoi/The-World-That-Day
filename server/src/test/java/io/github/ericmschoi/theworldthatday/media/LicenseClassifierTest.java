package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** The license status table of plan M3a3a: every name example, plus spacing and case. */
class LicenseClassifierTest {

  @ParameterizedTest(name = "\"{0}\" is free")
  @ValueSource(
      strings = {
        "Public domain",
        "CC0",
        "CC0 1.0",
        "CC BY 4.0",
        "CC BY-SA 3.0",
        "CC BY-SA 2.0 kr",
        "KOGL Type 1",
        // spacing, case and separators
        " cc by-sa 4.0 ",
        "PUBLIC DOMAIN",
        "CC-BY-SA-3.0",
        "cc by sa 3.0",
        "CC BY",
        "CC BY-SA",
        "CC BY 3.0 IGO",
        "kogl type 1",
      })
  void freeLicenses(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.FREE);
  }

  @ParameterizedTest(name = "\"{0}\" is other")
  @ValueSource(
      strings = {
        "CC BY-NC 4.0",
        "CC BY-ND 2.0",
        "CC BY-NC-SA 3.0",
        "KOGL Type 3",
        "provider-terms",
        "Example Made-up License 2.0", // a made-up name
        // near misses that must not become free
        " cc by-nc 4.0 ",
        "CC BY-NC",
        "CC BY-ND",
        "CC BY-SA-NC 3.0",
        "KOGL Type 10",
        "Public domain in some countries",
        "CC BY 4.0 kr nc",
      })
  void otherLicenses(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.OTHER);
  }

  @ParameterizedTest(name = "\"{0}\" is unknown")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void missingLicenseIsUnknown(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.UNKNOWN);
  }

  /** The R1 blank rule: a name made only of Unicode spaces (e.g. a stripped {@code &nbsp;}). */
  @ParameterizedTest(name = "\"{0}\" of Unicode spaces is unknown")
  @ValueSource(strings = {"\u00A0", "\u2007", "\u202F", "\u3000", " \u00A0\t", "\u2028"})
  void unicodeSpacesOnlyIsUnknown(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.UNKNOWN);
  }

  @ParameterizedTest(name = "\"{0}\" with Unicode spaces around text is not unknown")
  @ValueSource(strings = {"\u00A0Example License", "provider-terms\u3000"})
  void textWithUnicodeSpacesIsNotUnknown(String name) {
    assertThat(LicenseClassifier.classify(name)).isEqualTo(LicenseStatus.OTHER);
  }
}
