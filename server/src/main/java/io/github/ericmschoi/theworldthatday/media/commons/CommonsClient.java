package io.github.ericmschoi.theworldthatday.media.commons;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Wikimedia Commons file lookups for photos (plan M3a3b). Each request is sent once with the given
 * read timeout (no retry, plan M3a3c decision 1 (a)). Never throws.
 */
public interface CommonsClient {

  /**
   * The file pages of {@code Category:South Korea photographs taken on <date>} (at most 20, one
   * request, no continuation), each with its {@code imageinfo}. A category that does not exist
   * gives no pages and is not a failure.
   */
  Pages dayCategory(LocalDate date, Duration readTimeout);

  /** The {@code query.pages} of one response, or a failure (request, body or API error). */
  record Pages(List<JsonNode> pages, boolean failed) {

    public Pages {
      pages = List.copyOf(pages);
    }

    static Pages failure() {
      return new Pages(List.of(), true);
    }
  }
}
