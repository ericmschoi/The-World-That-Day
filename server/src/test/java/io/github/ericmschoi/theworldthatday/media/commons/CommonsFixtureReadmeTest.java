package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3c T1 (6) on the Commons side: every recorded Commons fixture, including the file batch
 * {@code files-*.json}, has a README row with its recording date and original URL, the recording
 * User-Agent is the repository address only, the file batch URL sends no language, and each
 * recorded file batch answer holds its requested file page.
 */
class CommonsFixtureReadmeTest {

  private static final Path ROOT = Path.of("src/test/resources/fixtures/wikimedia/commons");
  private static final Pattern ROW =
      Pattern.compile(
          "^\\| `([^`]+\\.json)` \\| (\\d{4}-\\d{2}-\\d{2}) [^|]*\\|.*\\| `([^`]+)` \\|$");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static String readme() throws IOException {
    return Files.readString(ROOT.resolve("README.md"), StandardCharsets.UTF_8);
  }

  private static List<String> jsonFiles() throws IOException {
    try (Stream<Path> list = Files.list(ROOT)) {
      return list.map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  /** README rows by file name: recording date and original URL. */
  private static Map<String, String[]> rows() throws IOException {
    Map<String, String[]> rows = new LinkedHashMap<>();
    for (String line : readme().split("\n")) {
      Matcher row = ROW.matcher(line.strip());
      if (row.matches()) {
        rows.put(row.group(1), new String[] {row.group(2), row.group(3)});
      }
    }
    return rows;
  }

  @Test
  void everyRecordedCommonsFixtureHasAReadmeRow() throws IOException {
    assertThat(rows().keySet()).containsExactlyInAnyOrderElementsOf(jsonFiles());
    assertThat(jsonFiles())
        .contains("files-1988-09-17.json")
        .doesNotContain("files-2002-06-22.json");
  }

  @Test
  void readmeNamesTheRepositoryUserAgentAndNoEmail() throws IOException {
    assertThat(readme())
        .contains("The-World-That-Day (https://github.com/ericmschoi/The-World-That-Day)")
        .contains("CC BY-SA 4.0")
        .contains("No image file was downloaded")
        .doesNotContain("@");
  }

  @Test
  void fileBatchRowsAskCommonsForFileInfoWithoutALanguage() throws IOException {
    Map<String, String[]> files = new LinkedHashMap<>(rows());
    files.keySet().removeIf(name -> !name.startsWith("files-"));

    assertThat(files).isNotEmpty();
    files.forEach(
        (file, row) -> {
          assertThat(row[0]).as(file).isEqualTo("2026-10-16");
          assertThat(row[1])
              .as(file)
              .startsWith("https://commons.wikimedia.org/w/api.php?action=query&titles=File%3a")
              .contains("prop=imageinfo")
              .contains("iiprop=url%7cmime%7csize%7cextmetadata")
              .contains("iiurlwidth=640")
              .contains("format=json&formatversion=2")
              .doesNotContain("iiextmetadatalanguage");
        });
  }

  @Test
  void recordedFileBatchHoldsTheRequestedFilePage() throws IOException {
    JsonNode tree =
        JSON.readTree(
            Files.readString(ROOT.resolve("files-1988-09-17.json"), StandardCharsets.UTF_8));

    JsonNode pages = tree.path("query").path("pages");
    assertThat(pages.isArray()).isTrue();
    assertThat(pages.size()).isEqualTo(1);
    assertThat(pages.get(0).path("title").asString()).isEqualTo("File:KBS 2 logo.svg");
    assertThat(pages.get(0).path("imageinfo").get(0).path("mime").asString())
        .isEqualTo("image/svg+xml");
    assertThat(tree.path("query").path("normalized").get(0).path("from").asString())
        .isEqualTo("File:KBS_2_logo.svg");
  }
}
