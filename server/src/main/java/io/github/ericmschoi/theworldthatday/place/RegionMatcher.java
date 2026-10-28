package io.github.ericmschoi.theworldthatday.place;

import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Region;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Finds the regions of the dictionary a place query names (plan M2a, rules M1 to M6). The query's
 * words (at most {@value #MAX_WORDS}) are compared whole: the last word must be a name of the
 * region, and the words before it must be names of its ancestors (province, then city) in that
 * order. Several regions can match ("서구"); all are returned in code order, never guessed down to
 * one. A pure function of the dictionary and the query: no calls, no database, no logging.
 */
@Component
public class RegionMatcher {

  static final int MAX_WORDS = 3;

  private final RegionDictionary dictionary;
  private final Map<String, Set<String>> namesByCode = new HashMap<>();

  public RegionMatcher(RegionDictionary dictionary) {
    this.dictionary = dictionary;
    for (Region region : dictionary.regions()) {
      namesByCode.put(region.code(), names(region));
    }
  }

  /** The matching regions in ascending code order; empty when the dictionary has none (M5). */
  public List<Region> match(PlaceQuery query) {
    List<String> words = List.of(query.value().split(" "));
    if (words.size() > MAX_WORDS) {
      return List.of();
    }
    String last = words.getLast();
    List<String> before = words.subList(0, words.size() - 1);
    List<Region> found = new ArrayList<>();
    for (Region region : dictionary.regions()) {
      if (namesByCode.get(region.code()).contains(last) && ancestorsMatch(region, before)) {
        found.add(region);
      }
    }
    return List.copyOf(found);
  }

  /**
   * Whether every word names an ancestor, the words in top-down order (ancestors may be left out).
   */
  private boolean ancestorsMatch(Region region, List<String> words) {
    List<Region> ancestors = dictionary.ancestors(region);
    int next = 0;
    for (String word : words) {
      while (next < ancestors.size()
          && !namesByCode.get(ancestors.get(next).code()).contains(word)) {
        next++;
      }
      if (next == ancestors.size()) {
        return false;
      }
      next++;
    }
    return true;
  }

  /**
   * The names a region is matched by (M2): its name, short name and aliases, and the name without a
   * final 시 or 군 when at least two characters remain. A final 구 is kept ("중구" is never "중").
   */
  static Set<String> names(Region region) {
    Set<String> names = new HashSet<>();
    names.add(normalize(region.name()));
    region.shortName().map(RegionMatcher::normalize).ifPresent(names::add);
    region.aliases().stream().map(RegionMatcher::normalize).forEach(names::add);
    String name = normalize(region.name());
    if ((name.endsWith("시") || name.endsWith("군")) && name.codePointCount(0, name.length()) >= 3) {
      names.add(name.substring(0, name.length() - 1));
    }
    return Set.copyOf(names);
  }

  /** Dictionary text in the query's Unicode form, so equal words compare equal (M4). */
  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFC);
  }
}
