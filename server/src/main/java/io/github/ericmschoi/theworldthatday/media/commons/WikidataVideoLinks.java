package io.github.ericmschoi.theworldthatday.media.commons;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What Wikidata ties to event items for the Commons video search (plan M3a4b, decision 2 (a): one
 * query of the adapter's own per date). Never throws.
 */
public interface WikidataVideoLinks {

  /**
   * The links of every item in {@code qids} that looks like a Wikidata item ID (others are left
   * out; no request when none is left), or empty when the query failed (logged). An item without
   * links has {@link Links#NONE}.
   */
  Optional<Map<String, Links>> links(Collection<String> qids, Duration readTimeout);

  /**
   * The item's video files ({@code P10}, names without {@code File:}), its Commons category name
   * without {@code Category:} ({@code P373}, else the Commons sitelink of its {@code P910} item;
   * {@code null} when neither), and its English label ({@code null} when none).
   */
  record Links(List<String> videoFiles, String category, String englishLabel) {

    public static final Links NONE = new Links(List.of(), null, null);

    public Links {
      videoFiles = List.copyOf(videoFiles);
    }
  }
}
