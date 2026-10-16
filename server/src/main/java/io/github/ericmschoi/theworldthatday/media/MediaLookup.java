package io.github.ericmschoi.theworldthatday.media;

import java.time.LocalDate;
import java.util.List;

/** The media of a world day (plan M3a3b); source adapters sit behind it. Never throws. */
public interface MediaLookup {

  /** The day's photos; none, and no request, on a tragic day (decision #22 (c)). */
  DayMedia dayMedia(LocalDate date, boolean tragicDay);

  /** The items, and whether a source failed (the answer is then partial). */
  record DayMedia(List<MediaItem> items, boolean failed) {

    public static final DayMedia NONE = new DayMedia(List.of(), false);
    public static final DayMedia FAILED = new DayMedia(List.of(), true);

    public DayMedia {
      items = List.copyOf(items);
    }
  }
}
