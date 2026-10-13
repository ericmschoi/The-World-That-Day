package io.github.ericmschoi.theworldthatday.media;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Locale;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One media item in the stored format of {@code data/schema/media.schema.json} (plan M3a3 field
 * table): every field except the API-only ones. Source adapters build these; {@link
 * MediaNormalizer} checks rules R1 to R9 and turns a passing record into a {@link MediaItem}.
 * Nullable numbers are boxed so a missing value stays {@code null}.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record MediaRecord(
    String id,
    Type type,
    DisplayMode displayMode,
    String url,
    String thumbnailUrl,
    Integer thumbnailWidth,
    Integer thumbnailHeight,
    List<VideoSource> videoSources,
    Double durationSeconds,
    String embedUrl,
    String provider,
    Render render,
    LinkOnlyReason linkOnlyReason,
    String sourceName,
    String sourceUrl,
    String license,
    LicenseStatus licenseStatus,
    String licenseUrl,
    String attribution,
    String description,
    String capturedAt,
    DatePrecision datePrecision,
    Double lat,
    Double lng,
    String placeRef,
    String retrievedAt) {

  public MediaRecord {
    videoSources = videoSources == null ? null : List.copyOf(videoSources);
  }

  /** The same record with {@code licenseUrl} replaced (R9 drops only a malformed license link). */
  MediaRecord withLicenseUrl(String value) {
    return new MediaRecord(
        id,
        type,
        displayMode,
        url,
        thumbnailUrl,
        thumbnailWidth,
        thumbnailHeight,
        videoSources,
        durationSeconds,
        embedUrl,
        provider,
        render,
        linkOnlyReason,
        sourceName,
        sourceUrl,
        license,
        licenseStatus,
        value,
        attribution,
        description,
        capturedAt,
        datePrecision,
        lat,
        lng,
        placeRef,
        retrievedAt);
  }

  /** What the media is. */
  public enum Type {
    PHOTO,
    VIDEO,
    AERIAL,
    SATELLITE,
    STREETVIEW;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** How the media is shown (never whether it is shown). */
  public enum DisplayMode {
    IMAGE,
    IMAGE_EMBED,
    VIDEO_FILE,
    VIDEO_EMBED,
    SDK,
    LINK_ONLY;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Only for {@code link_only}: why the source cannot be shown inline. */
  public enum LinkOnlyReason {
    HOTLINK_BLOCKED,
    EMBED_DISABLED;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** {@code unknown} is shown as "라이선스 확인 안 됨". */
  public enum LicenseStatus {
    FREE,
    OTHER,
    UNKNOWN;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Precision of {@code capturedAt}; a {@code null} precision means the capture time is unknown.
   */
  public enum DatePrecision {
    DAY,
    MONTH,
    YEAR,
    APPROX,
    CURRENT;

    @JsonValue
    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
