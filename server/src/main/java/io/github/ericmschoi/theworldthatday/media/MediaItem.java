package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A media item as the API returns it: every stored field of the {@link MediaRecord}, written flat
 * at the top level (keys with {@code null} values included), plus the API-only {@code
 * date_relation}. Built only by {@link MediaNormalizer}, so it always passes rules R1 to R9.
 * Further API-only fields are added here, never to the record: {@code selection} (plan M3a4) is set
 * on chosen videos only and has no key at all when absent, so photo items keep their keys.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record MediaItem(
    @JsonUnwrapped MediaRecord record,
    DateRelation dateRelation,
    @JsonInclude(JsonInclude.Include.NON_NULL) Selection selection) {

  /** An item without a selection: every photo, and a video before it is chosen. */
  public MediaItem(MediaRecord record, DateRelation dateRelation) {
    this(record, dateRelation, null);
  }

  /** This item as chosen by the video search, with its path and score. */
  public MediaItem withSelection(Selection chosen) {
    return new MediaItem(record, dateRelation, chosen);
  }
}
