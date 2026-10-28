package io.github.ericmschoi.theworldthatday.place;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * The {@code q} query parameter of {@code GET /api/places/resolve} after normalization (plan M2a):
 * NFC, control characters removed, every run of spaces (full-width and other Unicode separators
 * included) made one ASCII space, leading and trailing spaces removed. The result is 1 to {@value
 * #MAX_LENGTH} code points. The value is a user's input: it is never logged, and the error {@code
 * detail} never contains it.
 */
public record PlaceQuery(String value) {

  public static final int MAX_LENGTH = 50;

  private static final Pattern CONTROL = Pattern.compile("\\p{Cc}+");
  private static final Pattern SPACES = Pattern.compile("\\p{Z}+");

  public static PlaceQuery parse(String raw) {
    if (raw == null) {
      throw invalid("PLACE_QUERY_REQUIRED", "q is required");
    }
    String value = Normalizer.normalize(raw, Normalizer.Form.NFC);
    value = CONTROL.matcher(value).replaceAll("");
    value = SPACES.matcher(value).replaceAll(" ").strip();
    if (value.isEmpty()) {
      throw invalid("PLACE_QUERY_REQUIRED", "q is required");
    }
    if (value.codePointCount(0, value.length()) > MAX_LENGTH) {
      throw invalid("PLACE_QUERY_TOO_LONG", "q must be at most 50 characters");
    }
    return new PlaceQuery(value);
  }

  private static ApiProblem invalid(String code, String detail) {
    return new ApiProblem(HttpStatus.BAD_REQUEST, code, detail);
  }

  /** Never the value itself, so a stray log call or exception message cannot leak it. */
  @Override
  public String toString() {
    return "PlaceQuery[length=" + value.codePointCount(0, value.length()) + "]";
  }
}
