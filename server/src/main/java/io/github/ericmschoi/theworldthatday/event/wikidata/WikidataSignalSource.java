package io.github.ericmschoi.theworldthatday.event.wikidata;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Structured tone and scope signals of Wikidata items (plan M3a2 "Wikidata 조회"). Never throws. */
public interface WikidataSignalSource {

  /**
   * Signals for each asked item (an item without any signal is still in the map), or empty when
   * Wikidata could not be asked or answered unusably. No IDs: an empty map and no request.
   */
  Optional<Map<String, Signals>> signals(Collection<String> qids);

  /**
   * {@code tragicClass}: an instance of a subclass of a configured tragic class; {@code deaths}: a
   * death toll above 0 (P1120); {@code countries}: P17 values.
   */
  record Signals(boolean tragicClass, boolean deaths, Set<String> countries) {

    public static final Signals NONE = new Signals(false, false, Set.of());

    public Signals {
      countries = Set.copyOf(countries);
    }
  }
}
