package io.github.ericmschoi.theworldthatday.event.kowiki;

import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtureReadme.Row;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The recorded Wikipedia fixtures are what the plan says they are (plan M3a1 T2 (1), test policy 3
 * and 4, check C1): six demo pages, each row of README.md matches its file, only the "사건"/"문화"
 * sections were recorded, and nothing about births, deaths or anniversaries is stored.
 */
class KoWikiFixtureIntegrityTest {

  private static final Path ROOT = Path.of("src/test/resources/fixtures/wikimedia/kowiki");
  private static final Pattern PEOPLE_HEADING = Pattern.compile("==\\s*(탄생|사망|기념일)\\s*==");
  private static final Pattern LEVEL2_HEADING =
      Pattern.compile("(?m)^==(?!=)\\s*(.*?)\\s*==(?!=)\\s*$");

  private static List<Path> files(Path dir) throws IOException {
    try (Stream<Path> walk = Files.walk(dir)) {
      return walk.filter(Files::isRegularFile).sorted().toList();
    }
  }

  private static List<String> recordedJson() throws IOException {
    try (Stream<Path> list = Files.list(ROOT)) {
      return list.map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  @Test
  void eighteenRecordedFilesCoverTheSixDemoPages() throws IOException {
    List<String> expected =
        KoWikiFixtureReadme.DEMO_PAGES.values().stream()
            .flatMap(
                prefix ->
                    Stream.of("sections", "events", "culture").map(k -> prefix + "-" + k + ".json"))
            .sorted()
            .toList();

    assertThat(recordedJson()).containsExactlyElementsOf(expected);
  }

  @Test
  void noFixtureContainsBirthDeathOrAnniversarySections() throws IOException {
    // Same intent as check C1 in the plan, extended to 기념일 (also out of scope).
    for (Path file : files(ROOT.getParent())) {
      String content = Files.readString(file, StandardCharsets.UTF_8);
      assertThat(PEOPLE_HEADING.matcher(content).find()).as(file.toString()).isFalse();
    }
  }

  @Test
  void readmeListsEveryRecordedFileWithUrlRevidDateAndLicense() throws IOException {
    String readme = KoWikiFixtureReadme.text();
    Map<String, Row> rows = KoWikiFixtureReadme.rows();

    assertThat(rows.keySet()).containsExactlyInAnyOrderElementsOf(recordedJson());
    assertThat(readme).contains("녹화 일자").containsPattern("\\d{4}-\\d{2}-\\d{2}");
    assertThat(readme).contains("CC BY-SA 4.0");
    assertThat(readme)
        .as("User-Agent value is not written down")
        .doesNotContain("TheWorldThatDay/");
  }

  @Test
  void readmeRowsMatchTheRecordedResponses() {
    KoWikiFixtureReadme.DEMO_PAGES.forEach(
        (title, prefix) -> {
          Map<String, Row> rows = KoWikiFixtureReadme.rows();
          long revid = KoWikiFixtureReadme.revid(prefix + "-sections.json");
          String encodedTitle =
              URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");

          Row sections = rows.get(prefix + "-sections.json");
          assertThat(sections.title()).isEqualTo(title);
          assertThat(sections.revid()).isEqualTo(revid);
          assertThat(sections.url())
              .startsWith("https://ko.wikipedia.org/w/api.php?action=parse&")
              .contains("page=" + encodedTitle)
              .contains("prop=sections%7Crevid");

          for (String[] kind :
              List.of(new String[] {"events", "사건"}, new String[] {"culture", "문화"})) {
            String file = prefix + "-" + kind[0] + ".json";
            Row row = rows.get(file);
            assertThat(row.revid()).as(file).isEqualTo(revid);
            assertThat(KoWikiFixtureReadme.revid(file)).as(file).isEqualTo(revid);
            assertThat(row.url())
                .as(file)
                .contains("oldid=" + revid)
                .contains("prop=wikitext")
                .contains("section=" + KoWikiFixtureReadme.sectionIndex(prefix, kind[1]) + "&");
          }
        });
  }

  @Test
  void recordedSectionResponsesHoldOnlyTheirOwnLevel2Section() {
    KoWikiFixtureReadme.DEMO_PAGES.forEach(
        (title, prefix) -> {
          for (String[] kind :
              List.of(new String[] {"events", "사건"}, new String[] {"culture", "문화"})) {
            String text = wikitext(prefix + "-" + kind[0] + ".json");
            List<String> headings =
                LEVEL2_HEADING.matcher(text).results().map(m -> m.group(1)).toList();
            assertThat(headings).as(prefix + " " + kind[0]).containsExactly(kind[1]);
            assertThat(text).as(prefix + " " + kind[0]).startsWith("== " + kind[1] + " ==");
          }
        });
  }

  @Test
  void syntheticFixturesAreNamedSynthetic() throws IOException {
    assertThat(files(ROOT.resolve("synthetic")))
        .isNotEmpty()
        .allSatisfy(path -> assertThat(path.getFileName().toString()).startsWith("synthetic-"));
  }
}
