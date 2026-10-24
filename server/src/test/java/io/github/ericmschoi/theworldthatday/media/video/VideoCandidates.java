package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.VideoSource;
import java.time.LocalDate;
import java.util.List;

/**
 * Synthetic video items built the way an adapter builds them: a record through {@link
 * MediaNormalizer}, here with {@code youtube} allowed (plan M3a4 test policy). Every file name,
 * video ID and address is made up; no recorded response is used.
 */
final class VideoCandidates {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  static final MediaNormalizer NORMALIZER =
      new MediaNormalizer(new MediaProperties(List.of(), List.of("youtube")));

  private VideoCandidates() {}

  static MediaItem commonsVideo(String id, Double durationSeconds, CaptureTime capture) {
    return commonsVideo(id, durationSeconds, capture, LEAP_DAY);
  }

  /** A freely licensed Commons {@code video_file}. */
  static MediaItem commonsVideo(
      String id, Double durationSeconds, CaptureTime capture, LocalDate requested) {
    CaptureTime at = capture == null ? CaptureTime.UNKNOWN : capture;
    return normalize(
        new MediaRecord(
            id,
            Type.VIDEO,
            DisplayMode.VIDEO_FILE,
            "https://upload.wikimedia.org/wikipedia/commons/0/00/Example_test_video.webm",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Example_test_video.webm/330px--Example_test_video.webm.jpg",
            330,
            186,
            List.of(
                new VideoSource(
                    "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/00/Example_test_video.webm/Example_test_video.webm.240p.vp9.webm",
                    "video/webm",
                    240)),
            durationSeconds,
            null,
            null,
            null,
            null,
            "Wikimedia Commons",
            "https://commons.wikimedia.org/wiki/File:Example_test_video.webm",
            "CC BY-SA 4.0",
            LicenseStatus.FREE,
            "https://creativecommons.org/licenses/by-sa/4.0",
            "Example Author · CC BY-SA 4.0 · Wikimedia Commons",
            null,
            at.capturedAt(),
            at.datePrecision(),
            null,
            null,
            "world",
            "2026-10-18T00:00:00Z"),
        requested);
  }

  /** A YouTube {@code video_embed} under the provider's terms, with a made-up video ID. */
  static MediaItem youtubeVideo(
      String videoId, Double durationSeconds, CaptureTime capture, LocalDate requested) {
    CaptureTime at = capture == null ? CaptureTime.UNKNOWN : capture;
    return normalize(
        new MediaRecord(
            "youtube-" + videoId,
            Type.VIDEO,
            DisplayMode.VIDEO_EMBED,
            null,
            null,
            null,
            null,
            null,
            durationSeconds,
            "https://www.youtube-nocookie.com/embed/" + videoId,
            "youtube",
            null,
            null,
            "YouTube",
            "https://www.youtube.com/watch?v=" + videoId,
            "provider-terms",
            LicenseStatus.OTHER,
            "https://www.youtube.com/t/terms",
            "Synthetic channel · YouTube",
            null,
            at.capturedAt(),
            at.datePrecision(),
            null,
            null,
            "country:KR",
            "2026-10-18T00:00:00Z"),
        requested);
  }

  private static MediaItem normalize(MediaRecord record, LocalDate requested) {
    return NORMALIZER
        .normalize(record, requested)
        .orElseThrow(() -> new AssertionError("synthetic record breaks a rule: " + record.id()));
  }
}
