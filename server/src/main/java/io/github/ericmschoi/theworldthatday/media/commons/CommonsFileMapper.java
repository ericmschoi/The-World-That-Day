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
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

/**
 * One Commons file page ({@code query.pages[]} with {@code imageinfo}) to a photo {@link MediaItem}
 * (plan M3a3b "Commons 사진 규칙"). The caller picks the allowed MIME types and the place used when the
 * file has no GPS position. Order: record, then {@link MediaFilter}, then {@link MediaNormalizer}.
 * Addresses are copied exactly as the API gives them. It never logs.
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
    JsonNode info = page.path("imageinfo").path(0);
    long pageId = page.path("pageid").asLong(0);
    if (page.has("missing") || pageId <= 0 || !info.isObject()) {
      return new Result(null, Drop.MISSING);
    }
    JsonNode meta = info.path("extmetadata");
    String nonFree = MediaText.strip(value(meta, "NonFree"));
    if (nonFree != null && !nonFree.isEmpty() && !"false".equalsIgnoreCase(nonFree)) {
      return new Result(null, Drop.NON_FREE);
    }
    if (!mime.test(text(info, "mime"))) {
      return new Result(null, Drop.MIME);
    }
    String artist = plain(value(meta, "Artist"));
    String credit = plain(value(meta, "Credit"));
    String license = plain(value(meta, "LicenseShortName"));
    CaptureTime captured = CaptureTimeParser.parse(value(meta, "DateTimeOriginal"));
    Double lat = coordinate(value(meta, "GPSLatitude"), 90);
    Double lng = coordinate(value(meta, "GPSLongitude"), 180);
    boolean located = lat != null && lng != null;
    String author = artist != null ? artist : credit != null ? credit : NO_AUTHOR;
    MediaRecord record =
        new MediaRecord(
            "commons-" + pageId,
            Type.PHOTO,
            DisplayMode.IMAGE,
            text(info, "url"),
            text(info, "thumburl"),
            integer(info, "thumbwidth"),
            integer(info, "thumbheight"),
            null,
            null,
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
            description(plain(value(meta, "ImageDescription"))),
            captured.capturedAt(),
            captured.datePrecision(),
            located ? lat : null,
            located ? lng : null,
            located ? null : placeRef,
            clock.instant().truncatedTo(ChronoUnit.SECONDS).toString());
    String title = page.path("title").asString("");
    String fileName = title.startsWith("File:") ? title.substring(5) : title;
    if (filter
        .check(record, new MediaOrigin("commons", fileName, artist, credit, null))
        .isPresent()) {
      return new Result(null, Drop.FILTERED);
    }
    return normalizer
        .normalize(record, date)
        .map(item -> new Result(item, null))
        .orElseGet(() -> new Result(null, Drop.RULE));
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
