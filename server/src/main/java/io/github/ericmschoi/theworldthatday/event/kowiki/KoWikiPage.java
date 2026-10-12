package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.Instant;

/**
 * The "사건" and "문화" sections of one page, read from one revision. A missing section is {@code
 * null}. Other sections (births, deaths, ...) are never requested. {@code fetchedAt} is when the
 * sections were read from Wikipedia (for a cached page, the cache row's {@code fetched_at}); it is
 * {@code null} when there are no sections.
 */
public record KoWikiPage(
    String title,
    State state,
    Long revision,
    String eventsWikitext,
    String cultureWikitext,
    Instant fetchedAt) {

  /** Document state (plan M3a1 {@code DocState}; {@code CACHED} and {@code STALE} from M3a1b). */
  public enum State {
    /** Read from Wikipedia for this request. */
    FRESH,
    /** From the wikitext cache, younger than {@code wikimedia.cache-ttl}; nothing was requested. */
    CACHED,
    /** From an expired cache row because reading Wikipedia again failed. */
    STALE,
    /** The page does not exist ({@code missingtitle}). Not a failure. */
    ABSENT,
    UNAVAILABLE
  }

  static KoWikiPage absent(String title) {
    return new KoWikiPage(title, State.ABSENT, null, null, null, null);
  }

  static KoWikiPage unavailable(String title) {
    return new KoWikiPage(title, State.UNAVAILABLE, null, null, null, null);
  }
}
