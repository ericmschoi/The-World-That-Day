package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.CaptureTimeParser;
import io.github.ericmschoi.theworldthatday.media.LicenseClassifier;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.MediaText;
import io.github.ericmschoi.theworldthatday.media.VideoSource;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

/**
 * One Commons file page ({@code query.pages[]} with {@code imageinfo}) to a photo {@link MediaItem}
 * (plan M3a3b "Commons 사진 규칙"), or with {@code videoinfo} to a video item (plan M3a4b). The caller
 * picks the allowed MIME types and the place used when the file has no GPS position. Order: record,
 * then {@link MediaFilter}, then {@link MediaNormalizer}. Addresses are copied exactly as the API
 * gives them. It never logs.
 */
@Component
public class CommonsFileMapper {

  public static final String SOURCE_NAME = "Wikimedia Commons";
  static final String NO_AUTHOR = "작가 정보 없음";
  static final String NO_LICENSE = "라이선스 확인 안 됨";
  static final int DESCRIPTION_LIMIT = 200;

  private static final Pattern TAG = Pattern.compile("<[^>]*>");

  /** Runs of the {@link MediaText} space set (the schema's {@code [\s\p{Z}\x1C-\x1F]}). */
  private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}\\x1C-\\x1F]+");

  /** Why a page gives no item. */
  public enum Drop {
    MISSING,
    NON_FREE,
    MIME,
    FILTERED,
    RULE
  }

  /** The item, or why there is none. */
  public record Result(MediaItem item, Drop drop) {
    public Optional<MediaItem> kept() {
      return Optional.ofNullable(item);
    }
  }

  private final MediaFilter filter;
  private final MediaNormalizer normalizer;
  private final Clock clock;

  public CommonsFileMapper(MediaFilter filter, MediaNormalizer normalizer, Clock clock) {
    this.filter = filter;
    this.normalizer = normalizer;
    this.clock = clock;
  }

  public Result map(JsonNode page, Predicate<String> mime, String placeRef, LocalDate date) {
    VideoResult mapped = file(page, false, info -> mime.test(text(info, "mime")), placeRef, date);
    return new Result(mapped.item(), mapped.drop());
  }

  /**
   * A {@code videoinfo} page to a {@code video_file} item (plan M3a4b "미디어 레코드"), only for {@code
   * mediatype} {@code VIDEO} (other files are dropped as {@code MIME}). Its {@code captured_at} is
   * the {@code DateTimeOriginal} value unless that is later than {@code date} ({@link
   * VideoAccuracy#notAfter}); the upload time is never read.
   */
  public VideoResult mapVideo(JsonNode page, String placeRef, LocalDate date) {
    return file(page, true, info -> "VIDEO".equals(text(info, "mediatype")), placeRef, date);
  }

  /**
   * A video item, or why there is none; with what the accuracy rules read when there is one: the
   * file name without {@code File:}, the whole plain description and the capture time as parsed,
   * and the {@link MediaFilter} input it passed (plan M3a4c, for the result cache).
   */
  public record VideoResult(
      MediaItem item,
      Drop drop,
      String title,
      String description,
      CaptureTime originalCapture,
      MediaOrigin origin) {

    /** A result without an origin. */
    public VideoResult(
        MediaItem item, Drop drop, String title, String description, CaptureTime originalCapture) {
      this(item, drop, title, description, originalCapture, null);
    }

    static VideoResult dropped(Drop drop) {
      return new VideoResult(null, drop, null, null, null);
    }
  }

  private VideoResult file(
      JsonNode page, boolean video, Predicate<JsonNode> accepted, String placeRef, LocalDate date) {
    JsonNode info = page.path(video ? "videoinfo" : "imageinfo").path(0);
    long pageId = page.path("pageid").asLong(0);
    if (page.has("missing") || pageId <= 0 || !info.isObject()) {
      return VideoResult.dropped(Drop.MISSING);
    }
    JsonNode meta = info.path("extmetadata");
    String nonFree = MediaText.strip(value(meta, "NonFree"));
    if (nonFree != null && !nonFree.isEmpty() && !"false".equalsIgnoreCase(nonFree)) {
      return VideoResult.dropped(Drop.NON_FREE);
    }
    if (!accepted.test(info)) {
      return VideoResult.dropped(Drop.MIME);
    }
    String artist = plain(value(meta, "Artist"));
    String credit = plain(value(meta, "Credit"));
    String license = plain(value(meta, "LicenseShortName"));
    String description = plain(value(meta, "ImageDescription"));
    CaptureTime original = CaptureTimeParser.parse(value(meta, "DateTimeOriginal"));
    CaptureTime captured = video ? VideoAccuracy.notAfter(original, date) : original;
    Double lat = coordinate(value(meta, "GPSLatitude"), 90);
    Double lng = coordinate(value(meta, "GPSLongitude"), 180);
    boolean located = lat != null && lng != null;
    String author = artist != null ? artist : credit != null ? credit : NO_AUTHOR;
    MediaRecord record =
        new MediaRecord(
            "commons-" + pageId,
            video ? Type.VIDEO : Type.PHOTO,
            video ? DisplayMode.VIDEO_FILE : DisplayMode.IMAGE,
            text(info, "url"),
            text(info, "thumburl"),
            integer(info, "thumbwidth"),
            integer(info, "thumbheight"),
            video ? videoSources(info) : null,
            video && info.path("duration").isNumber() ? info.path("duration").doubleValue() : null,
            null,
            null,
            null,
            null,
            SOURCE_NAME,
            text(info, "descriptionurl"),
            license,
            LicenseClassifier.classify(license),
            value(meta, "LicenseUrl"),
            String.join(" · ", author, license != null ? license : NO_LICENSE, SOURCE_NAME),
            description(description),
            captured.capturedAt(),
            captured.datePrecision(),
            located ? lat : null,
            located ? lng : null,
            located ? null : placeRef,
            clock.instant().truncatedTo(ChronoUnit.SECONDS).toString());
    String title = page.path("title").asString("");
    String fileName = title.startsWith("File:") ? title.substring(5) : title;
    MediaOrigin origin = new MediaOrigin("commons", fileName, artist, credit, null);
    if (filter.check(record, origin).isPresent()) {
      return VideoResult.dropped(Drop.FILTERED);
    }
    return normalizer
        .normalize(record, date)
        .map(item -> new VideoResult(item, null, fileName, description, original, origin))
        .orElseGet(() -> VideoResult.dropped(Drop.RULE));
  }

  /**
   * The derivatives (the original's own entry, which has no {@code transcodekey}, left out), lowest
   * first and at one height {@code video/webm} first and {@code video/quicktime} last, with the
   * codec parameters cut from the MIME type; then the original when its MIME type is {@code
   * video/…}. An {@code application/ogg} original stays in {@code url} only. {@code null} when
   * none.
   */
  private static List<VideoSource> videoSources(JsonNode info) {
    List<VideoSource> sources = new ArrayList<>();
    for (JsonNode derivative : info.path("derivatives")) {
      String src = text(derivative, "src");
      String mime = bareMime(text(derivative, "type"));
      if (derivative.has("transcodekey")
          && src != null
          && mime != null
          && mime.startsWith("video/")) {
        sources.add(new VideoSource(src, mime, integer(derivative, "height")));
      }
    }
    sources.sort(
        Comparator.comparing(
                VideoSource::height, Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
            .thenComparingInt(CommonsFileMapper::mimeRank));
    String originalMime = bareMime(text(info, "mime"));
    if (originalMime != null && originalMime.startsWith("video/")) {
      sources.add(new VideoSource(text(info, "url"), originalMime, integer(info, "height")));
    }
    return sources.isEmpty() ? null : sources;
  }

  private static int mimeRank(VideoSource source) {
    return switch (source.mime()) {
      case "video/webm" -> 0;
      case "video/quicktime" -> 2;
      default -> 1;
    };
  }

  /** The MIME type before any {@code ;} parameter, lowercase; {@code null} when there is none. */
  private static String bareMime(String type) {
    if (type == null) {
      return null;
    }
    String bare = type.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    return bare.isEmpty() ? null : bare;
  }

  /**
   * Plain text of an {@code extmetadata} HTML value: tags removed, entities decoded, runs of spaces
   * as one space, no spaces at either end; {@code null} when nothing is left.
   */
  static String plain(String html) {
    if (html == null) {
      return null;
    }
    String text = HtmlUtils.htmlUnescape(TAG.matcher(html).replaceAll(" "));
    text = MediaText.strip(SPACES.matcher(text).replaceAll(" "));
    return MediaText.isBlank(text) ? null : text;
  }

  private static String description(String text) {
    if (text == null || text.codePointCount(0, text.length()) <= DESCRIPTION_LIMIT) {
      return text;
    }
    return text.substring(0, text.offsetByCodePoints(0, DESCRIPTION_LIMIT));
  }

  /** A GPS value in degrees, or {@code null} when absent, unreadable or out of range. */
  private static Double coordinate(String raw, double limit) {
    try {
      double value = raw == null ? Double.NaN : Double.parseDouble(MediaText.strip(raw));
      return Math.abs(value) <= limit ? value : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String value(JsonNode meta, String key) {
    JsonNode value = meta.path(key).path("value");
    return value.isValueNode() && !value.isNull() ? value.asString() : null;
  }

  private static String text(JsonNode node, String key) {
    JsonNode value = node.path(key);
    return value.isString() ? value.stringValue() : null;
  }

  private static Integer integer(JsonNode node, String key) {
    JsonNode value = node.path(key);
    return value.isIntegralNumber() && value.canConvertToInt() ? value.intValue() : null;
  }
}
