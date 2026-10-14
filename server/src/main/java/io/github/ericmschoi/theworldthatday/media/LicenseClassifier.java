package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns a license name into {@code license_status} (plan M3a3a license table). Names are compared
 * without surrounding spaces and case, with hyphens and underscores read as spaces, so {@code CC
 * BY-SA 3.0} and {@code cc by-sa 3.0} match. A version and a jurisdiction after the version ({@code
 * CC BY-SA 2.0 kr}) keep the same kind. Only Public domain, CC0, CC BY, CC BY-SA and KOGL Type 1
 * are free; any other name, including NC and ND licenses, is other; no name, or one made only of
 * spaces (the R1 blank rule, {@link MediaText#isBlank}), is unknown.
 */
public final class LicenseClassifier {

  /** Any Unicode space (category Z, e.g. U+00A0) separates words too. */
  private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Z}_-]+");

  private static final String VERSION = "\\d+(?:\\.\\d+)?";

  /**
   * A jurisdiction after the version ({@code kr}, {@code igo}), never a license element word: a
   * trailing {@code nc}, {@code nd}, {@code sa} or {@code by} is not free.
   */
  private static final String JURISDICTION = "(?!(?:by|sa|nc|nd)\\b)[a-z]{2,3}";

  private static final Pattern FREE =
      Pattern.compile(
          "public domain"
              + "|cc0(?: "
              + VERSION
              + ")?"
              + "|cc by(?: sa)?(?: "
              + VERSION
              + "(?: "
              + JURISDICTION
              + ")?)?"
              + "|kogl type 1");

  private LicenseClassifier() {}

  public static LicenseStatus classify(String license) {
    if (MediaText.isBlank(license)) {
      return LicenseStatus.UNKNOWN;
    }
    String name =
        MediaText.strip(SEPARATORS.matcher(license.toLowerCase(Locale.ROOT)).replaceAll(" "));
    return FREE.matcher(name).matches() ? LicenseStatus.FREE : LicenseStatus.OTHER;
  }
}
