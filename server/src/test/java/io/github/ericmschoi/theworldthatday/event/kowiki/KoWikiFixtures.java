package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/** Reads fixtures under {@code fixtures/wikimedia/kowiki/} (README.md lists their origin). */
final class KoWikiFixtures {

  static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String ROOT = "fixtures/wikimedia/kowiki/";

  private KoWikiFixtures() {}

  /** Raw file content, e.g. {@code 09-17-events.json}. */
  static String read(String path) {
    try (InputStream in = KoWikiFixtures.class.getClassLoader().getResourceAsStream(ROOT + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The {@code parse.wikitext} field of a recorded or synthetic section response. */
  static String wikitext(String path) {
    return JSON.readTree(read(path)).path("parse").path("wikitext").stringValue();
  }

  /** {@code synthetic/parser/synthetic-<name>.wikitext}. */
  static String parserCase(String name) {
    return read("synthetic/parser/synthetic-" + name + ".wikitext");
  }
}
