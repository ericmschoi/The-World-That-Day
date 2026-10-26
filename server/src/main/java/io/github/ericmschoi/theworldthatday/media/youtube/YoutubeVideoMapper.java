package io.github.ericmschoi.theworldthatday.media.youtube;

import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.CaptureTimeParser;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.MediaText;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.TextDates;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * One {@code videos.list} item to a YouTube {@code video_embed} candidate for one event, or why
 * there is none (plan M3a5 "규칙", M3a5a T4). An item is dropped, never made {@code link_only}, when
 * its ID is not 11 URL-safe characters, its channel ID is not allowed, it is not embeddable or not
 * public, Korea is blocked or not among the allowed regions, it is age restricted, or its title
 * marks a colour-restored re-upload. Then the record goes through {@link MediaFilter} (blocklist
 * {@code id} and {@code channel}) and {@link MediaNormalizer} (R1 to R9). Length is left to {@link
 * VideoAccuracy}. The capture time is the first of {@code recordingDate}, the title's first date
 * and the description's first date that is not after the event, and none for a retrospective edit
 * or a newsreel title (the newsreel's written dates are issue dates, M3a5b dates it). {@code
 * publishedAt} is never read. Pure apart from the clock: no request, no storage. Not used by the
 * flow yet.
 */
@Component
public class YoutubeVideoMapper {

  public static final String KEY = "youtube";
  static final int DESCRIPTION_LIMIT = 200;

  private static final Logger log = LoggerFactory.getLogger(YoutubeVideoMapper.class);
  private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
  private static final Pattern COLOR_RESTORED =
      Pattern.compile("컬러\\s*복원|\\[대한뉴스\\s*(?:제\\s*)?\\d+\\s*호\\]");
  private static final Pattern NEWSREEL = Pattern.compile("^\\s*대한뉴스\\s*제\\s*\\d+\\s*호");
  private static final Pattern RECORDING_DAY = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})(?:T.*)?");
  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

  /** Why an item gives no candidate. */
  public enum Drop {
    ID,
    CHANNEL,
    NOT_EMBEDDABLE,
    NOT_PUBLIC,
    REGION,
    AGE_RESTRICTED,
    COLOR_RESTORED,
    FILTERED,
    RULE
  }

  /** The candidate, or why there is none. */
  public record Result(Candidate candidate, Drop drop) {}

  private final Set<String> channels;
  private final List<Pattern> retrospective;
  private final String origin;
  private final MediaFilter filter;
  private final MediaNormalizer normalizer;
  private final Clock clock;

  public YoutubeVideoMapper(
      YoutubeProperties properties,
      AppProperties app,
      MediaFilter filter,
      MediaNormalizer normalizer,
      Clock clock) {
    this.channels = Set.copyOf(properties.allowedChannels());
    this.retrospective =
        properties.retrospectiveTitlePatterns().stream().map(Pattern::compile).toList();
    this.origin = URLEncoder.encode(app.webOrigin(), StandardCharsets.UTF_8);
    this.filter = filter;
    this.normalizer = normalizer;
    this.clock = clock;
  }

  /** The candidates of {@code videos} in their order; the drops are logged as counts only. */
  public List<Candidate> candidates(List<JsonNode> videos, EventContext event) {
    List<Candidate> kept = new ArrayList<>();
    Map<Drop, Integer> dropped = new EnumMap<>(Drop.class);
    for (JsonNode video : videos) {
      Result result = map(video, event);
      if (result.candidate() != null) {
        kept.add(result.candidate());
      } else {
        dropped.merge(result.drop(), 1, Integer::sum);
      }
    }
    if (!dropped.isEmpty()) {
      log.debug("YouTube videos dropped: {}", dropped); // reason counts only
    }
    return kept;
  }

  public Result map(JsonNode video, EventContext event) {
    String id = text(video, "id");
    JsonNode snippet = video.path("snippet");
    JsonNode details = video.path("contentDetails");
    String channel = text(snippet, "channelId");
    String title = text(snippet, "title");
    String plainTitle = title == null ? "" : Normalizer.normalize(title, Normalizer.Form.NFC);
    Drop drop = check(id, channel, video.path("status"), details, plainTitle);
    if (drop != null) {
      return new Result(null, drop);
    }
    String description = text(snippet, "description");
    LocalDate date = event.date();
    CaptureTime recorded = recording(text(video.path("recordingDetails"), "recordingDate"));
    boolean newsreel = NEWSREEL.matcher(plainTitle).find();
    boolean edit = retrospective.stream().anyMatch(p -> p.matcher(plainTitle).find());
    CaptureTime captured =
        newsreel || edit ? CaptureTime.UNKNOWN : captured(date, recorded, title, description);
    String name = text(snippet, "channelTitle");
    name = MediaText.isBlank(name) ? null : name; // MediaNormalizer drops it then (R1)
    JsonNode thumbnail = snippet.path("thumbnails").path("medium");
    MediaRecord record =
        new MediaRecord(
            KEY + "-" + id,
            Type.VIDEO,
            DisplayMode.VIDEO_EMBED,
            null,
            text(thumbnail, "url"),
            integer(thumbnail, "width"),
            integer(thumbnail, "height"),
            null,
            seconds(text(details, "duration")),
            "https://www.youtube-nocookie.com/embed/"
                + id
                + "?playsinline=1&rel=0&enablejsapi=1&origin="
                + origin,
            KEY,
            null,
            null,
            name,
            "https://www.youtube.com/watch?v=" + id,
            "provider-terms",
            LicenseStatus.OTHER,
            "https://www.youtube.com/t/terms",
            name == null ? null : name + " · YouTube",
            firstLine(description),
            captured.capturedAt(),
            captured.datePrecision(),
            null,
            null,
            event.placeRef(),
            clock.instant().truncatedTo(ChronoUnit.SECONDS).toString());
    MediaOrigin from = new MediaOrigin(KEY, id, null, null, channel);
    if (filter.check(record, from).isPresent()) {
      return new Result(null, Drop.FILTERED);
    }
    return normalizer
        .normalize(record, date)
        .map(
            item ->
                new Result(
                    new Candidate(
                        item,
                        title,
                        description,
                        recorded,
                        List.of(),
                        EventLink.NONE,
                        true,
                        newsreel,
                        Basis.YOUTUBE_OFFICIAL,
                        from),
                    null))
        .orElseGet(() -> new Result(null, Drop.RULE));
  }

  /** The first rule of the "버림" row the item breaks, or {@code null}. */
  private Drop check(String id, String channel, JsonNode status, JsonNode details, String title) {
    JsonNode region = details.path("regionRestriction");
    if (id == null || !VIDEO_ID.matcher(id).matches()) {
      return Drop.ID;
    } else if (channel == null || !channels.contains(channel)) {
      return Drop.CHANNEL;
    } else if (!(status.path("embeddable").isBoolean()
        && status.path("embeddable").booleanValue())) {
      return Drop.NOT_EMBEDDABLE;
    } else if (!"public".equals(text(status, "privacyStatus"))) {
      return Drop.NOT_PUBLIC;
    } else if (korea(region.path("blocked"))
        || (region.has("allowed") && !korea(region.path("allowed")))) {
      return Drop.REGION;
    } else if ("ytAgeRestricted".equals(text(details.path("contentRating"), "ytRating"))) {
      return Drop.AGE_RESTRICTED;
    }
    return COLOR_RESTORED.matcher(title).find() ? Drop.COLOR_RESTORED : null;
  }

  private static boolean korea(JsonNode regions) {
    for (JsonNode region : regions) {
      if ("KR".equals(region.asString(""))) {
        return true;
      }
    }
    return false;
  }

  /** The first of the three sources whose date is not after the event; unknown when none. */
  private static CaptureTime captured(
      LocalDate date, CaptureTime recorded, String title, String description) {
    for (CaptureTime time : List.of(recorded, firstDate(title), firstDate(description))) {
      CaptureTime kept = VideoAccuracy.notAfter(time, date);
      if (kept.isKnown()) {
        return kept;
      }
    }
    return CaptureTime.UNKNOWN;
  }

  private static CaptureTime firstDate(String text) {
    return TextDates.find(text).stream().findFirst().orElse(CaptureTime.UNKNOWN);
  }

  /** The day of an RFC 3339 {@code recordingDate}, such as {@code 1986-09-20T00:00:00Z}. */
  private static CaptureTime recording(String value) {
    Matcher m = RECORDING_DAY.matcher(value == null ? "" : value.strip());
    return m.matches() ? CaptureTimeParser.parse(m.group(1)) : CaptureTime.UNKNOWN;
  }

  /** ISO 8601 {@code PT1M57S} as 117.0; {@code null} when unreadable or not positive. */
  static Double seconds(String iso) {
    try {
      Duration length = iso == null ? Duration.ZERO : Duration.parse(iso);
      return length.isNegative() || length.isZero() ? null : length.toMillis() / 1000.0;
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /** The first line, at most {@link #DESCRIPTION_LIMIT} code points; {@code null} when blank. */
  static String firstLine(String description) {
    String line = MediaText.strip(description == null ? null : LINE_BREAK.split(description, 2)[0]);
    if (MediaText.isBlank(line)) {
      return null;
    }
    return line.codePointCount(0, line.length()) <= DESCRIPTION_LIMIT
        ? line
        : line.substring(0, line.offsetByCodePoints(0, DESCRIPTION_LIMIT));
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
