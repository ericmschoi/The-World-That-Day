package io.github.ericmschoi.theworldthatday.event.kowiki;

/** Reads the "사건" and "문화" sections of a Korean Wikipedia page. Never throws. */
public interface KoWikiClient {

  KoWikiPage fetch(String title);

  /**
   * The page from the cache alone, without any request: {@code CACHED} inside the cache TTL, {@code
   * STALE} after it, {@code UNAVAILABLE} without a copy. A client without a cache has no copy.
   */
  default KoWikiPage cached(String title) {
    return KoWikiPage.unavailable(title);
  }
}
