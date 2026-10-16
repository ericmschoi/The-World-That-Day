package io.github.ericmschoi.theworldthatday.media;

/**
 * The one blank rule of the media package (plan M3a3a "공백 문자 집합", the R1 blank rule). The schema's
 * {@code nonEmptyString} uses the same set.
 */
public final class MediaText {

  private MediaText() {}

  /**
   * Whether {@code value} is {@code null} or made only of spaces: characters that {@link
   * Character#isWhitespace} accepts (the {@code String.isBlank()} rule) or of Unicode category Z
   * ({@link Character#isSpaceChar}: U+00A0, U+2007, U+202F, U+3000, ...). The empty string is
   * blank.
   */
  public static boolean isBlank(String value) {
    return value == null || value.codePoints().allMatch(MediaText::isSpace);
  }

  /**
   * {@code value} ({@code null} stays {@code null}) without leading and trailing characters of the
   * {@link #isBlank} set; unlike {@link String#strip()} that includes U+00A0, U+2007 and U+202F.
   */
  public static String strip(String value) {
    if (value == null) {
      return null;
    }
    int start = 0;
    int end = value.length();
    while (start < end && isSpace(value.codePointAt(start))) {
      start += Character.charCount(value.codePointAt(start));
    }
    while (end > start && isSpace(value.codePointBefore(end))) {
      end -= Character.charCount(value.codePointBefore(end));
    }
    return value.substring(start, end);
  }

  private static boolean isSpace(int c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }
}
