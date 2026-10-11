package io.github.ericmschoi.theworldthatday.event.kowiki;

/**
 * The "사건" and "문화" sections of one page, read from one revision. A missing section is {@code
 * null}. Other sections (births, deaths, ...) are never requested.
 */
public record KoWikiPage(
    String title, State state, Long revision, String eventsWikitext, String cultureWikitext) {

  public enum State {
    FOUND,
    /** The page does not exist ({@code missingtitle}). Not a failure. */
    ABSENT,
    UNAVAILABLE
  }

  static KoWikiPage absent(String title) {
    return new KoWikiPage(title, State.ABSENT, null, null, null);
  }

  static KoWikiPage unavailable(String title) {
    return new KoWikiPage(title, State.UNAVAILABLE, null, null, null);
  }
}
