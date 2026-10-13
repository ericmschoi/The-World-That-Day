package io.github.ericmschoi.theworldthatday.media;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Synthetic media records in the stored format, one valid base per display mode (plan M3a3 test
 * policy: synthetic records, demo and boundary dates only). Tests change one key of a base map and
 * read it with {@link #record}, which also checks that the snake_case names bind.
 */
final class MediaRecords {

  /**
   * Reads strictly: an unknown field fails (Jackson 3 ignores unknown fields by default), as the
   * schema's {@code additionalProperties: false} does.
   */
  static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  /** The stored fields of the field table, in table order. */
  static final List<String> STORED_KEYS =
      List.of(
          "id",
          "type",
          "display_mode",
          "url",
          "thumbnail_url",
          "thumbnail_width",
          "thumbnail_height",
          "video_sources",
          "duration_seconds",
          "embed_url",
          "provider",
          "render",
          "link_only_reason",
          "source_name",
          "source_url",
          "license",
          "license_status",
          "license_url",
          "attribution",
          "description",
          "captured_at",
          "date_precision",
          "lat",
          "lng",
          "place_ref",
          "retrieved_at");

  static final MediaNormalizer DEFAULT_NORMALIZER =
      new MediaNormalizer(new MediaProperties(List.of(), List.of()));

  /** Both allowed-provider lists hold the test provider {@code example}. */
  static final MediaNormalizer EXAMPLE_NORMALIZER =
      new MediaNormalizer(new MediaProperties(List.of("example"), List.of("example")));

  private MediaRecords() {}

  static MediaRecord record(Map<String, Object> fields) {
    return JSON.convertValue(fields, MediaRecord.class);
  }

  /** A Commons-like photo shown with {@code <img>}, taken on 1988-09-17. */
  static Map<String, Object> image() {
    Map<String, Object> m = new LinkedHashMap<>();
    STORED_KEYS.forEach(key -> m.put(key, null));
    m.put("id", "example-image");
    m.put("type", "photo");
    m.put("display_mode", "image");
    m.put("url", "https://upload.wikimedia.org/wikipedia/commons/a/ab/Example.jpg");
    m.put("thumbnail_url", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Example.jpg");
    m.put("thumbnail_width", 640);
    m.put("thumbnail_height", 427);
    m.put("source_name", "Wikimedia Commons");
    m.put("source_url", "https://commons.wikimedia.org/wiki/File:Example.jpg");
    m.put("license", "Public domain");
    m.put("license_status", "free");
    m.put("attribution", "Example author · Public domain · Wikimedia Commons");
    m.put("description", "1988년 서울 올림픽 개막식 (합성 예시)");
    m.put("captured_at", "1988-09-17");
    m.put("date_precision", "day");
    m.put("place_ref", "country:KR");
    m.put("retrieved_at", "2026-10-13T03:00:00Z");
    return m;
  }

  static Map<String, Object> imageEmbed() {
    Map<String, Object> m = image();
    m.put("id", "example-image-embed");
    m.put("display_mode", "image_embed");
    m.put("url", null);
    m.put("thumbnail_url", null);
    m.put("embed_url", "https://embed.example.test/image/1");
    m.put("provider", "example");
    m.put("license", "provider-terms");
    m.put("license_status", "other");
    return m;
  }

  static Map<String, Object> videoFile() {
    Map<String, Object> m = image();
    m.put("id", "example-video-file");
    m.put("type", "video");
    m.put("display_mode", "video_file");
    m.put("url", null);
    m.put(
        "video_sources",
        List.of(
            Map.of(
                "url",
                "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/ab/Example.webm",
                "mime",
                "video/webm",
                "height",
                480)));
    m.put("duration_seconds", 61.5);
    return m;
  }

  static Map<String, Object> videoEmbed() {
    Map<String, Object> m = image();
    m.put("id", "example-video-embed");
    m.put("type", "video");
    m.put("display_mode", "video_embed");
    m.put("url", null);
    m.put("embed_url", "https://www.youtube-nocookie.com/embed/example");
    m.put("provider", "example");
    m.put("license", "provider-terms");
    m.put("license_status", "other");
    m.put("license_url", "https://www.youtube.com/t/terms");
    return m;
  }

  static Map<String, Object> sdk() {
    Map<String, Object> m = image();
    m.put("id", "example-sdk");
    m.put("type", "streetview");
    m.put("display_mode", "sdk");
    m.put("url", null);
    m.put("thumbnail_url", null);
    m.put("thumbnail_width", null);
    m.put("thumbnail_height", null);
    m.put("provider", "kakao");
    m.put(
        "render",
        Map.of("provider", "kakao", "kind", "roadview", "lat", 37.5665, "lng", 126.978, "zoom", 3));
    m.put("captured_at", null);
    m.put("date_precision", "current");
    m.put("lat", 37.5665);
    m.put("lng", 126.978);
    m.put("place_ref", null);
    return m;
  }

  static Map<String, Object> linkOnly() {
    Map<String, Object> m = image();
    m.put("id", "example-link-only");
    m.put("display_mode", "link_only");
    m.put("url", null);
    m.put("thumbnail_url", null);
    m.put("link_only_reason", "hotlink_blocked");
    return m;
  }

  /** {@code base} with one key replaced. */
  static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
    Map<String, Object> copy = new LinkedHashMap<>(base);
    copy.put(key, value);
    return copy;
  }
}
