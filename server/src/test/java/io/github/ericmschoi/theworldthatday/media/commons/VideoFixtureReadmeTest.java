package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T4 (7): the READMEs of the recorded Commons video and Wikidata video-link fixtures
 * hold no email address, name the repository address as the User-Agent, state the license, and give
 * every recorded file a row with its recording date and original URL; each recorded file is valid
 * JSON. (The recorded bytes themselves are never edited, so a contact address in a recorded file
 * description is not the project's and is not checked here.)
 */
class VideoFixtureReadmeTest {

  private static final Path COMMONS =
      Path.of("src/test/resources/fixtures/wikimedia/commons/video");
  private static final Path WIKIDATA = Path.of("src/test/resources/fixtures/wikimedia/wikidata");
  private static final String USER_AGENT =
      "TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)";
  private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
  private static final Pattern DATE_ROW =
      Pattern.compile("^\\| `([^`]+\\.json)` \\|.*(20\\d\\d-\\d\\d-\\d\\d).*\\|.*https://.*\\|$");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static String readme(Path folder) throws IOException {
    return Files.readString(folder.resolve("README.md"), StandardCharsets.UTF_8);
  }

  private static List<String> jsonFiles(Path folder, String prefix) throws IOException {
    try (Stream<Path> list = Files.list(folder)) {
      return list.map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith(prefix) && name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  @Test
  void noReadmeHoldsAnEmailAddress() throws IOException {
    assertThat(EMAIL.matcher(readme(COMMONS)).find()).isFalse();
    assertThat(EMAIL.matcher(readme(WIKIDATA)).find()).isFalse();
  }

  @Test
  void everyUserAgentLineIsTheRepositoryAddress() throws IOException {
    for (String readme : List.of(readme(COMMONS), readme(WIKIDATA))) {
      List<String> lines = readme.lines().filter(l -> l.contains("User-Agent")).toList();
      assertThat(lines).isNotEmpty();
      assertThat(lines).allSatisfy(line -> assertThat(line).contains(USER_AGENT));
      Matcher agent = Pattern.compile("TheWorldThatDay/[^ ]+ \\(([^)]*)\\)").matcher(readme);
      while (agent.find()) {
        assertThat(agent.group(1)).isEqualTo("https://github.com/ericmschoi/The-World-That-Day");
      }
    }
  }

  @Test
  void licensesAreStated() throws IOException {
    assertThat(readme(COMMONS)).contains("CC BY-SA 4.0").contains("No video file");
    assertThat(readme(WIKIDATA)).contains("CC0");
  }

  @Test
  void everyRecordedVideoFixtureIsValidJsonWithAReadmeRowOfDateAndUrl() throws IOException {
    List<String> commons = jsonFiles(COMMONS, "");
    List<String> links = jsonFiles(WIKIDATA, "video-links-");
    assertThat(commons).hasSize(8);
    assertThat(links).hasSize(4);
    for (String name : commons) {
      assertThat(JSON.readTree(Files.readString(COMMONS.resolve(name))).isObject())
          .as(name)
          .isTrue();
      assertThat(rowFiles(readme(COMMONS))).as("README row of " + name).contains(name);
    }
    for (String name : links) {
      assertThat(JSON.readTree(Files.readString(WIKIDATA.resolve(name))).isObject())
          .as(name)
          .isTrue();
      assertThat(readme(WIKIDATA)).as("README row of " + name).contains("`" + name + "`");
    }
  }

  private static List<String> rowFiles(String readme) {
    return readme
        .lines()
        .map(String::strip)
        .map(DATE_ROW::matcher)
        .filter(Matcher::matches)
        .map(m -> m.group(1))
        .toList();
  }
}
