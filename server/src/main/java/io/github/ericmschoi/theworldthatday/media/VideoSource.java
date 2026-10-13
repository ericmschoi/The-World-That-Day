package io.github.ericmschoi.theworldthatday.media;

import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/** One playable file of a {@code video_file} item, e.g. {@code video/webm} at height 480. */
@JsonNaming(SnakeCaseStrategy.class)
public record VideoSource(String url, String mime, Integer height) {}
