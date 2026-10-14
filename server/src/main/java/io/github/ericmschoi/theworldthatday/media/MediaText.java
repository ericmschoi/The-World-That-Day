package io.github.ericmschoi.theworldthatday.media;

/**
 * The one blank rule of the media package (plan M3a3a "공백 문자 집합", the R1 blank rule). The schema's
 * {@code nonEmptyString} uses the same set.
 */
final class MediaText {

  private MediaText() {}

  /**
   * Whether {@code value} is {@code null} or made only of spaces: characters that {@link
   * Character#isWhitespace} accepts (the {@code String.isBlank()} rule) or of Unicode category Z
   * ({@link Character#isSpaceChar}: U+00A0, U+2007, U+202F, U+3000, ...). The empty string is
   * blank.
   */
  static boolean isBlank(String value) {
    return value == null
        || value.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
  }
}
