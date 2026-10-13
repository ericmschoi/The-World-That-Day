package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A media item as the API returns it: every stored field of the {@link MediaRecord}, written flat
 * at the top level (keys with {@code null} values included), plus the API-only {@code
 * date_relation}. Built only by {@link MediaNormalizer}, so it always passes rules R1 to R9.
 * Further API-only fields (M3a4 {@code selection}) are added here, never to the record.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record MediaItem(@JsonUnwrapped MediaRecord record, DateRelation dateRelation) {}
