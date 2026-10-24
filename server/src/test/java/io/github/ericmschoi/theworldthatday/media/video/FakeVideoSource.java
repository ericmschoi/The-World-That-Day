package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A video source for tests (plan M3a4 test policy): it records what it was asked and gives a set
 * answer, made of synthetic candidates on the leap day 2000-02-29. No HTTP. File names and video
 * IDs are made up ({@code commons-9000xx}, {@code AAAAAAAAAAA}-like).
 */
public final class FakeVideoSource implements EventVideoSource {

  public static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  /** The answer to one search; may sleep or throw. */
  @FunctionalInterface
  public interface Answer {
    SourceResult answer(List<EventContext> events);
  }

  private final String key;
  private final Answer answer;
  public final List<List<EventContext>> asked = new CopyOnWriteArrayList<>();
  public final List<Boolean> dayPathsOff = new CopyOnWriteArrayList<>();

  public FakeVideoSource(String key, Answer answer) {
    this.key = key;
    this.answer = answer;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff) {
    asked.add(events);
    this.dayPathsOff.add(dayPathsOff);
    return answer.answer(events);
  }

  /** The plan's settings with {@code maxEvents} searched events. */
  public static VideoSearchProperties properties(int maxEvents) {
    return new VideoSearchProperties(
        60,
        maxEvents,
        600,
        VideoAccuracyTest.COMMEMORATION,
        VideoAccuracyTest.OTHER_STAGE,
        VideoAccuracyTest.STOPWORDS);
  }

  /** A search over {@code sources} with the plan's settings. */
  public static VideoSearch search(int maxEvents, EventVideoSource... sources) {
    VideoSearchProperties properties = properties(maxEvents);
    return new VideoSearch(List.of(sources), new VideoAccuracy(properties), properties);
  }

  public static SourceResult ok(Map<String, List<Candidate>> byEvent) {
    return new SourceResult(byEvent, Map.of(), Status.OK);
  }

  /** Never answers: sleeps until interrupted, then throws. */
  public static SourceResult silent() {
    try {
      Thread.sleep(60_000);
      throw new AssertionError("not interrupted");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted");
    }
  }

  public static CaptureTime day(String date) {
    return new CaptureTime(date, DatePrecision.DAY);
  }

  /**
   * A freely licensed Commons video {@code commons-<n>} titled {@code title}, 95 s long, with no
   * capture time of its own on the item.
   */
  public static Candidate commons(
      long n, String title, CaptureTime original, EventLink link, Basis basis) {
    MediaItem item = VideoCandidates.commonsVideo("commons-" + n, 95.0, null, LEAP_DAY);
    return new Candidate(item, title, null, original, List.of(), link, false, false, basis);
  }

  /** A YouTube video (license {@code other}) with a made-up 11-letter ID. */
  public static Candidate youtube(
      String videoId, String title, CaptureTime original, EventLink link, boolean official) {
    MediaItem item = VideoCandidates.youtubeVideo(videoId, 95.0, null, LEAP_DAY);
    return new Candidate(
        item, title, null, original, List.of(), link, official, false, Basis.YOUTUBE_OFFICIAL);
  }
}
