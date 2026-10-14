package io.github.ericmschoi.theworldthatday.media;

/**
 * What the source said about an item, beyond the stored {@link MediaRecord} fields, for {@link
 * MediaFilter} (plan M3a3a "필터 입력"). Any field may be {@code null}.
 *
 * @param source source key in lowercase, e.g. {@code commons}, {@code youtube}
 * @param itemId the item inside the source: a Commons file name without {@code File:}, a video ID
 * @param author plain author text (Commons {@code Artist} without HTML)
 * @param credit plain {@code Credit} text
 * @param channel channel ID
 */
public record MediaOrigin(
    String source, String itemId, String author, String credit, String channel) {}
