package io.github.ericmschoi.theworldthatday.media;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The media of a world day (plan M3a3b); source adapters sit behind it. Never throws. */
public interface MediaLookup {

  /** The day's photos; none, and no request, on a tragic day (decision #22 (c)). */
  DayMedia dayMedia(LocalDate date, boolean tragicDay);

  /**
   * The day's photos and the event photos and videos of {@code targets} (plans M3a3d, M3a4). {@code
   * tragicDay} turns the day paths off (a tragic day, or a day with an unavailable Wikipedia page);
   * events are still looked up. This default looks up the day's photos only.
   */
  default WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay) {
    DayMedia day = dayMedia(date, tragicDay);
    return new WorldMedia(Map.of(), day.items(), day.failed());
  }

  /** The items, and whether a source failed (the answer is then partial). */
  record DayMedia(List<MediaItem> items, boolean failed) {

    public static final DayMedia NONE = new DayMedia(List.of(), false);
    public static final DayMedia FAILED = new DayMedia(List.of(), true);

    public DayMedia {
      items = List.copyOf(items);
    }
  }

  /**
   * A shown {@code normal} event whose article may have a photo, placed at {@code placeRef}; its
   * text ({@code summary}) and main link's {@code wikidataQid} feed the video search (plan M3a4).
   * Two targets are equal when their event, title and place are: the event ID is a hash of the
   * event text, so the two search inputs follow from it.
   */
  record EventTarget(
      String eventId, String title, String placeRef, String summary, String wikidataQid) {

    /** A target without the video search inputs. */
    public EventTarget(String eventId, String title, String placeRef) {
      this(eventId, title, placeRef, null, null);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof EventTarget that
          && Objects.equals(eventId, that.eventId)
          && Objects.equals(title, that.title)
          && Objects.equals(placeRef, that.placeRef);
    }

    @Override
    public int hashCode() {
      return Objects.hash(eventId, title, placeRef);
    }
  }

  /**
   * Event media by event ID (a photo, then a video, 0 or 1 each), the day's photos, and whether a
   * source failed.
   */
  record WorldMedia(
      Map<String, List<MediaItem>> eventMedia, List<MediaItem> dayMedia, boolean failed) {

    public WorldMedia {
      eventMedia = Map.copyOf(eventMedia);
      dayMedia = List.copyOf(dayMedia);
    }
  }
}
