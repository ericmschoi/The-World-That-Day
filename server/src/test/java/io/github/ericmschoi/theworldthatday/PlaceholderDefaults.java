package io.github.ericmschoi.theworldthatday;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads default values of {@code ${NAME...}} placeholders from file contents. */
final class PlaceholderDefaults {

  /** Separator between the variable name and its default value. */
  enum Syntax {
    /** Docker Compose: {@code ${NAME:-default}}. */
    COMPOSE(":-"),
    /** Spring Boot: {@code ${NAME:default}}. */
    SPRING(":");

    private final String separator;

    Syntax(String separator) {
      this.separator = separator;
    }
  }

  private PlaceholderDefaults() {}

  /**
   * Returns the single default value used for {@code name} in {@code text}. Fails when the name is
   * absent, when any placeholder for it has no default, or when placeholders disagree.
   */
  static String defaultOf(String text, String name, Syntax syntax) {
    // The lookbehind skips Compose's escaped $${NAME} (expanded by the container shell, not by
    // Compose). The lookahead stops POSTGRES_DB from matching POSTGRES_DB_EXTRA.
    Pattern placeholder =
        Pattern.compile("(?<!\\$)\\$\\{" + Pattern.quote(name) + "(?![A-Za-z0-9_])([^}]*)}");
    Matcher matcher = placeholder.matcher(text);
    List<String> rests = new ArrayList<>();
    while (matcher.find()) {
      rests.add(matcher.group(1));
    }
    if (rests.isEmpty()) {
      throw new AssertionError("no ${" + name + "} placeholder found");
    }
    Set<String> defaults = new LinkedHashSet<>();
    for (String rest : rests) {
      if (!rest.startsWith(syntax.separator)) {
        throw new AssertionError(
            "${" + name + rest + "} has no default (expected '" + syntax.separator + "')");
      }
      defaults.add(rest.substring(syntax.separator.length()));
    }
    if (defaults.size() > 1) {
      throw new AssertionError(name + " has different defaults in one file: " + defaults);
    }
    return defaults.iterator().next();
  }

  /** Returns the shared default, or fails when Compose and Spring disagree or one has none. */
  static String sameDefault(String compose, String spring, String name) {
    String composeDefault = defaultOf(compose, name, Syntax.COMPOSE);
    String springDefault = defaultOf(spring, name, Syntax.SPRING);
    if (!composeDefault.equals(springDefault)) {
      throw new AssertionError(
          name
              + " default differs: docker-compose.yml '"
              + composeDefault
              + "', application.yml '"
              + springDefault
              + "'");
    }
    return composeDefault;
  }
}
