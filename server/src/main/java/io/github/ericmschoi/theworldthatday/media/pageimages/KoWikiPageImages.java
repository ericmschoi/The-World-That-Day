package io.github.ericmschoi.theworldthatday.media.pageimages;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lead images of Korean Wikipedia articles through PageImages with {@code pilicense=free} (plan
 * M3a3c), the first filter against non-free local files (M3-P7). One request, sent once with the
 * given read timeout. Never throws.
 */
public interface KoWikiPageImages {

  /** Titles in one request; later ones are not asked. */
  int MAX_TITLES = 50;

  /**
   * Requested title to its lead image file name (without {@code File:}), following normalization
   * and redirects. A title without a free lead image is not in the map. Empty when the request or
   * the answer failed.
   */
  Optional<Map<String, String>> imageNames(List<String> titles, Duration readTimeout);
}
