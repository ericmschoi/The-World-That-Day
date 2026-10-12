package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Reads the recorded-fixture table of {@code fixtures/wikimedia/kowiki/README.md}. */
final class KoWikiFixtureReadme {

  /** One table row: file, page title, revid and original URL. */
  record Row(String file, String title, long revid, String url) {}

  /** The six demo pages and the fixture prefix of each (plan M3a1 test policy 3). */
  static final Map<String, String> DEMO_PAGES =
      Map.of(
          "9월 17일", "09-17",
          "6월 22일", "06-22",
          "3월 15일", "03-15",
          "1988년", "1988",
          "2002년", "2002",
          "1995년", "1995");

  private static final Pattern ROW =
      Pattern.compile("^\\| `([^`]+\\.json)` \\| ([^|]+?) \\| (\\d+) \\| `([^`]+)` \\|$");

  private KoWikiFixtureReadme() {}

  static String text() {
    return KoWikiFixtures.read("README.md");
  }

  static Map<String, Row> rows() {
    Map<String, Row> rows = new LinkedHashMap<>();
    for (String line : text().split("\n")) {
      Matcher row = ROW.matcher(line.strip());
      if (row.matches()) {
        rows.put(
            row.group(1),
            new Row(row.group(1), row.group(2), Long.parseLong(row.group(3)), row.group(4)));
      }
    }
    return rows;
  }

  /** Index of the level-2 section named {@code line} in a recorded section list, or null. */
  static String sectionIndex(String prefix, String line) {
    JsonNode sections =
        KoWikiFixtures.JSON
            .readTree(KoWikiFixtures.read(prefix + "-sections.json"))
            .path("parse")
            .path("sections");
    for (JsonNode section : sections) {
      if ("2".equals(section.path("level").asString(null))
          && line.equals(section.path("line").asString(null))) {
        return section.path("index").asString(null);
      }
    }
    return null;
  }

  static long revid(String file) {
    return KoWikiFixtures.JSON
        .readTree(KoWikiFixtures.read(file))
        .path("parse")
        .path("revid")
        .asLong(0);
  }
}
