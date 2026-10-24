package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies the required-field rules R1 to R9 of plan M3a3 (the same rules as {@code
 * data/schema/media.schema.json}). A record that breaks a rule is dropped, never switched to
 * another display mode. The one repair is a malformed {@code license_url}, which becomes {@code
 * null} (R9). A passing record gets its {@code date_relation} to the requested date. Allowed embed
 * providers come from {@link MediaProperties}; the schema cannot know them.
 */
@Component
public class MediaNormalizer {

  private static final Logger log = LoggerFactory.getLogger(MediaNormalizer.class);

  private static final Pattern URL =
      Pattern.compile("https?://\\S+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern PLACE_REF =
      Pattern.compile("world|unknown|[a-z]+:\\S+", Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * UTC to the second, as the schema's {@code retrieved_at} pattern. Second 60 is always refused:
   * {@link Instant#parse} accepts it, while the schema's {@code date-time} accepts it only on real
   * leap-second dates (plan M3a3a, PR #12 review), so the server stays the stricter side.
   */
  private static final Pattern DATE_TIME =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:[0-5]\\dZ");

  private static final Pattern VIDEO_MIME = Pattern.compile("video/[a-z0-9.+-]+");

  private final MediaProperties properties;

  public MediaNormalizer(MediaProperties properties) {
    this.properties = properties;
  }

  /** The API item for {@code record}, or empty when it breaks a rule. */
  public Optional<MediaItem> normalize(MediaRecord record, LocalDate requested) {
    Objects.requireNonNull(requested, "requested");
    String broken = brokenRule(record);
    if (broken != null) {
      // The ID names the source item only; no date or place is logged.
      log.debug("Media dropped: id={} rule={}", record.id(), broken);
      return Optional.empty();
    }
    MediaRecord kept = isUrl(record.licenseUrl()) ? record : record.withLicenseUrl(null);
    return Optional.of(
        new MediaItem(
            kept, DateRelation.of(record.capturedAt(), record.datePrecision(), requested)));
  }

  /** The first rule {@code r} breaks, or {@code null} when it passes. */
  private String brokenRule(MediaRecord r) {
    if (!required(r) || !located(r)) {
      return "R1";
    }
    if (!urls(r)) {
      return "R9";
    }
    if (!shapes(r)) {
      return "shape";
    }
    if (!modeFields(r)) {
      return "mode:" + r.displayMode().value();
    }
    return datePrecisionMatches(r) ? null : "R8";
  }

  private static boolean required(MediaRecord r) {
    return has(r.id())
        && r.type() != null
        && r.displayMode() != null
        && has(r.sourceName())
        && has(r.sourceUrl())
        && has(r.attribution())
        && r.licenseStatus() != null
        && nullOrHas(r.provider())
        && nullOrHas(r.license())
        && nullOrHas(r.description())
        && has(r.retrievedAt())
        && dateTime(r.retrievedAt());
  }

  /** Coordinates (both or neither, in range) or a place reference. */
  private static boolean located(MediaRecord r) {
    if ((r.lat() == null) != (r.lng() == null)) {
      return false;
    }
    if (r.placeRef() != null && !PLACE_REF.matcher(r.placeRef()).matches()) {
      return false;
    }
    return r.lat() != null ? coordinates(r.lat(), r.lng()) : r.placeRef() != null;
  }

  /** R9: every address that has a value is http(s); {@code license_url} is repaired instead. */
  private static boolean urls(MediaRecord r) {
    Stream<String> sources =
        r.videoSources() == null ? Stream.empty() : r.videoSources().stream().map(VideoSource::url);
    return Stream.concat(Stream.of(r.sourceUrl(), r.url(), r.thumbnailUrl(), r.embedUrl()), sources)
        .allMatch(MediaNormalizer::isUrl);
  }

  /** Sizes, video sources and the SDK render description are well formed when present. */
  private static boolean shapes(MediaRecord r) {
    boolean sizes =
        positive(r.thumbnailWidth())
            && positive(r.thumbnailHeight())
            && (r.durationSeconds() == null || r.durationSeconds() > 0);
    boolean sources =
        r.videoSources() == null
            || (!r.videoSources().isEmpty()
                && r.videoSources().stream()
                    .allMatch(
                        s ->
                            has(s.url())
                                && s.mime() != null
                                && VIDEO_MIME.matcher(s.mime()).matches()
                                && positive(s.height())));
    Render render = r.render();
    boolean rendered =
        render == null
            || (has(render.provider())
                && render.kind() != null
                && render.lat() != null
                && render.lng() != null
                && coordinates(render.lat(), render.lng()));
    return sizes && sources && rendered;
  }

  /** R2 to R7, including that {@code link_only_reason} appears only on {@code link_only}. */
  private boolean modeFields(MediaRecord r) {
    if (r.linkOnlyReason() != null && r.displayMode() != DisplayMode.LINK_ONLY) {
      return false;
    }
    return switch (r.displayMode()) {
      case IMAGE -> has(r.url()) && has(r.thumbnailUrl());
      case IMAGE_EMBED ->
          embed(r, properties.allowedImageEmbedProviders())
              && r.url() == null
              && r.thumbnailUrl() == null;
      case VIDEO_FILE -> has(r.url()) || r.videoSources() != null;
      case VIDEO_EMBED -> embed(r, properties.allowedVideoEmbedProviders()) && r.url() == null;
      case SDK ->
          r.render() != null && has(r.provider()) && r.url() == null && r.thumbnailUrl() == null;
      case LINK_ONLY ->
          r.linkOnlyReason() != null
              && r.url() == null
              && r.thumbnailUrl() == null
              && r.embedUrl() == null;
    };
  }

  private static boolean embed(MediaRecord r, List<String> allowed) {
    return has(r.embedUrl()) && has(r.provider()) && allowed.contains(r.provider());
  }

  /** R8: the shape of {@code captured_at} follows {@code date_precision}. */
  private static boolean datePrecisionMatches(MediaRecord r) {
    return CaptureTime.followsRuleR8(r.capturedAt(), r.datePrecision());
  }

  private static boolean dateTime(String value) {
    try {
      return DATE_TIME.matcher(value).matches() && Instant.parse(value) != null;
    } catch (DateTimeException e) {
      return false;
    }
  }

  private static boolean isUrl(String value) {
    return value == null || URL.matcher(value).matches();
  }

  private static boolean coordinates(double lat, double lng) {
    return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
  }

  private static boolean positive(Integer value) {
    return value == null || value > 0;
  }

  /** Optional text is {@code null} or not blank, never an empty string. */
  private static boolean nullOrHas(String value) {
    return value == null || has(value);
  }

  /** Not blank by the plan M3a3a space set ({@link MediaText#isBlank}). */
  private static boolean has(String value) {
    return !MediaText.isBlank(value);
  }
}
