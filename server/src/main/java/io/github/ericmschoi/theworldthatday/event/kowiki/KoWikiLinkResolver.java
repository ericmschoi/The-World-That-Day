package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.lang.Character.UnicodeScript;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Resolves Korean Wikipedia link titles to canonical titles and Wikidata items. Never throws. */
public interface KoWikiLinkResolver {

  /** A canonical page title and its Wikidata item, {@code null} for a missing page. */
  record Link(String title, String wikidataQid) {}

  /** Each requested title mapped to its link, or empty when any request fails. */
  Optional<Map<String, Link>> resolve(List<String> titles);

  /**
   * MediaWiki title form of a link target: underscores as spaces, no {@code #section}, single
   * spaces, and an upper-case first letter when it is Latin.
   */
  static String normalize(String title) {
    String text = title.replace('_', ' ').replaceFirst("#.*", "").strip().replaceAll("\\s+", " ");
    if (text.isEmpty() || UnicodeScript.of(text.codePointAt(0)) != UnicodeScript.LATIN) {
      return text;
    }
    int first = text.codePointAt(0);
    return Character.toString(Character.toUpperCase(first))
        + text.substring(Character.charCount(first));
  }
}
