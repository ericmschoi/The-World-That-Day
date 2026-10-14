package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/** What a map SDK shows for an {@code sdk} item (M4), e.g. a Kakao road view at a point. */
@JsonNaming(SnakeCaseStrategy.class)
public record Render(String provider, Kind kind, Double lat, Double lng, Double zoom) {

  public enum Kind {
    ROADVIEW,
    SKYVIEW;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
