package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads files at the repository root. Gradle runs tests with server/ as the working directory, so
 * the root is its parent.
 */
final class RepositoryFiles {

  private RepositoryFiles() {}

  static Path path(String relative) {
    Path root = Path.of("").toAbsolutePath().getParent();
    Path file = root.resolve(relative);
    assertThat(file).as("repository file %s", relative).isRegularFile();
    return file;
  }

  static String read(String relative) {
    try {
      return Files.readString(path(relative), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static Map<Object, Object> yaml(String relative) {
    return new Yaml().load(read(relative));
  }

  @SuppressWarnings("unchecked")
  static Map<Object, Object> map(Object value) {
    assertThat(value).isInstanceOf(Map.class);
    return (Map<Object, Object>) value;
  }
}
