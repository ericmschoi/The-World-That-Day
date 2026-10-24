package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Objects;

/**
 * API only, on video items (plan M3a4): the search path that found the video and its accuracy
 * score, an integer sum of the rule table. Written as {@code {"basis": "commons_p10", "score":
 * 130}} and never stored.
 */
public record Selection(Basis basis, int score) {

  public Selection {
    Objects.requireNonNull(basis, "basis");
  }

  /** The search path; the JSON values are the six of the web's {@code MediaSelection.basis}. */
  public enum Basis {
    COMMONS_P10,
    COMMONS_CATEGORY,
    COMMONS_DATE_CATEGORY,
    COMMONS_SEARCH,
    YOUTUBE_OFFICIAL,
    DAEHAN_NEWS;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
