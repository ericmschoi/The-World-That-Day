package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Region;
import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M2a T2 (1)(2): the region dictionary under {@code app.data-dir}. The valid dictionary is the
 * synthetic {@code src/test/resources/places/regions.json}; every rule has a broken copy of it.
 */
@ExtendWith(OutputCaptureExtension.class)
class RegionDictionaryTest {

  static final JsonMapper JSON = JsonMapper.builder().build();

  @TempDir Path dataDir;

  private final ch.qos.logback.classic.Logger dictionaryLogger =
      (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RegionDictionary.class);
  private Level levelBefore;

  /** The test yml lowers this logger to ERROR for other tests; this class needs its WARN. */
  @BeforeEach
  void showTheWarning() {
    levelBefore = dictionaryLogger.getLevel();
    dictionaryLogger.setLevel(Level.INFO);
  }

  @AfterEach
  void restoreTheLevel() {
    dictionaryLogger.setLevel(levelBefore);
  }

  /** The synthetic test dictionary as a tree. */
  static ObjectNode synthetic() {
    try (InputStream in = RegionDictionaryTest.class.getResourceAsStream("/places/regions.json")) {
      return (ObjectNode) JSON.readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Writes {@code content} as {@code places/regions.json} under {@code dataDir}. */
  static Path write(Path dataDir, String content) {
    try {
      Path file = dataDir.resolve(RegionDictionary.FILE);
      Files.createDirectories(file.getParent());
      Files.writeString(file, content);
      return dataDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static RegionDictionary load(Path dataDir) {
    return new RegionDictionary(new AppProperties(dataDir.toString()), JSON);
  }

  private static int count(String text, String part) {
    return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
  }

  @Test
  void syntheticDictionaryLoadsInCodeOrderWithItsSource() {
    RegionDictionary dictionary = load(write(dataDir, synthetic().toString()));

    List<Region> regions = dictionary.regions();
    assertThat(regions).hasSize(22);
    assertThat(regions).extracting(Region::code).isSorted();
    assertThat(regions.getFirst())
        .isEqualTo(
            new Region(
                "1100000000",
                RegionDictionary.Level.SIDO,
                "서울특별시",
                Optional.of("서울"),
                Optional.empty(),
                List.of("서울시")));
    assertThat(dictionary.byCode("4111100000"))
        .hasValueSatisfying(
            gu -> {
              assertThat(gu.level()).isEqualTo(RegionDictionary.Level.GU);
              assertThat(gu.parentCode()).contains("4111000000");
              assertThat(gu.shortName()).isEmpty();
              assertThat(gu.aliases()).isEmpty();
            });
    assertThat(dictionary.byCode("9999999999")).isEmpty();
    assertThat(regions)
        .filteredOn(region -> region.name().equals("서구"))
        .extracting(Region::code)
        .containsExactly("2614000000", "2723000000", "2826000000", "2914000000", "3017000000");
    assertThat(dictionary.source())
        .contains(
            new Source("행정안전부 법정동코드 (공공데이터포털)", "https://www.data.go.kr/data/15077871/openapi.do"));
  }

  @Test
  void loadedDictionaryLogsOnlyTheCount(CapturedOutput output) {
    load(write(dataDir, synthetic().toString()));

    assertThat(output.getAll()).contains("Region dictionary loaded: regions=22");
    assertThat(output.getAll())
        .doesNotContain("서울")
        .doesNotContain("서구")
        .doesNotContain("1100000000")
        .doesNotContain("data.go.kr");
  }

  @Test
  void missingFileLogsOneWarnLineAndTheDictionaryIsEmpty(CapturedOutput output) {
    RegionDictionary dictionary = load(dataDir.resolve("nowhere"));

    assertThat(dictionary.regions()).isEmpty();
    assertThat(dictionary.byCode("1100000000")).isEmpty();
    assertThat(dictionary.source()).isEmpty();
    assertThat(output.getAll().lines().filter(line -> line.contains(" WARN ")))
        .singleElement()
        .satisfies(
            line ->
                assertThat(line)
                    .contains("Region dictionary file not found")
                    .contains(
                        dataDir.resolve("nowhere").resolve(RegionDictionary.FILE).toString()));
  }

  @Test
  void emptyRegionListIsValid() {
    ObjectNode root = synthetic();
    root.putArray("regions");

    assertThat(load(write(dataDir, root.toString())).regions()).isEmpty();
  }

  private static ObjectNode region(ObjectNode root, String code) {
    for (JsonNode node : root.path("regions")) {
      if (node.path("code").asString().equals(code)) {
        return (ObjectNode) node;
      }
    }
    throw new IllegalArgumentException("no region " + code);
  }

  private static Consumer<ObjectNode> seoul(Consumer<ObjectNode> edit) {
    return root -> edit.accept(region(root, "1100000000"));
  }

  private static Consumer<ObjectNode> jongno(Consumer<ObjectNode> edit) {
    return root -> edit.accept(region(root, "1111000000"));
  }

  private static Consumer<ObjectNode> jangan(Consumer<ObjectNode> edit) {
    return root -> edit.accept(region(root, "4111100000"));
  }

  /**
   * One broken copy per rule: what it breaks, the edit, and the rule text the message must name.
   */
  static List<Arguments> brokenCopies() {
    return List.of(
        Arguments.of("version not 1", root(r -> r.put("version", 2)), "version must be 1"),
        Arguments.of("version missing", root(r -> r.remove("version")), "version must be 1"),
        Arguments.of(
            "unknown root property",
            root(r -> r.put("note", "x")),
            "the root has an unknown property note"),
        Arguments.of("source missing", root(r -> r.remove("source")), "source must be an object"),
        Arguments.of(
            "source name blank",
            root(r -> ((ObjectNode) r.get("source")).put("name", " ")),
            "source.name must be a non-empty string"),
        Arguments.of(
            "source url not https",
            root(r -> ((ObjectNode) r.get("source")).put("url", "http://www.data.go.kr/")),
            "source.url must be an https URL"),
        Arguments.of(
            "unknown source property",
            root(r -> ((ObjectNode) r.get("source")).put("license", "x")),
            "source has an unknown property license"),
        Arguments.of(
            "regions not an array", root(r -> r.put("regions", "x")), "regions must be an array"),
        Arguments.of(
            "region not an object",
            root(r -> ((ArrayNode) r.get("regions")).add(1)),
            "regions[22] must be an object"),
        Arguments.of(
            "unknown region property",
            seoul(s -> s.put("note", "x")),
            "regions[1] has an unknown property note"),
        Arguments.of(
            "code with 9 digits",
            seoul(s -> s.put("code", "110000000")),
            "regions[1].code must be 10 digits"),
        Arguments.of(
            "code with a letter",
            seoul(s -> s.put("code", "11000000A0")),
            "regions[1].code must be 10 digits"),
        Arguments.of(
            "code as a number",
            seoul(s -> s.put("code", 1100000000L)),
            "regions[1].code must be 10 digits"),
        Arguments.of(
            "duplicate code",
            jongno(j -> j.put("code", "1100000000")),
            "regions[2].code repeats the code of regions[1]"),
        Arguments.of(
            "unknown level",
            jongno(j -> j.put("level", "dong")),
            "regions[2].level must be sido, sigungu or gu"),
        Arguments.of(
            "blank name",
            jongno(j -> j.put("name", "  ")),
            "regions[2].name must be a non-empty string"),
        Arguments.of(
            "missing name", jongno(j -> j.remove("name")), "regions[2].name must be a non-empty"),
        Arguments.of(
            "sido without short_name",
            seoul(s -> s.remove("short_name")),
            "regions[1].short_name must be a non-empty string"),
        Arguments.of(
            "sido with a blank short_name",
            seoul(s -> s.put("short_name", "")),
            "regions[1].short_name must be a non-empty string"),
        Arguments.of(
            "short_name on a sigungu",
            jongno(j -> j.put("short_name", "종로")),
            "regions[2].short_name is only for sido"),
        Arguments.of("parent missing", jongno(j -> j.remove("parent")), "regions[2] needs parent"),
        Arguments.of(
            "sido with a parent",
            seoul(s -> s.put("parent", "2600000000")),
            "regions[1].parent must be null for sido"),
        Arguments.of(
            "sigungu without a parent",
            jongno(j -> j.putNull("parent")),
            "regions[2].parent must be the code of a sido"),
        Arguments.of(
            "parent not in the file",
            jongno(j -> j.put("parent", "1200000000")),
            "regions[2].parent is not a code in regions"),
        Arguments.of(
            "sigungu under a sigungu",
            jongno(j -> j.put("parent", "1114000000")),
            "regions[2].parent must be a sido, not a sigungu"),
        Arguments.of(
            "gu under a sido",
            jangan(g -> g.put("parent", "4100000000")),
            "regions[0].parent must be a sigungu, not a sido"),
        Arguments.of(
            "aliases missing",
            jongno(j -> j.remove("aliases")),
            "regions[2].aliases must be an array"),
        Arguments.of(
            "blank alias",
            seoul(s -> s.putArray("aliases").add("서울시").add(" ")),
            "regions[1].aliases[1] must be a non-empty string"),
        Arguments.of(
            "alias not a string",
            seoul(s -> s.putArray("aliases").add(1)),
            "regions[1].aliases[0] must be a non-empty string"),
        Arguments.of(
            "duplicate alias in one region",
            seoul(s -> s.putArray("aliases").add("서울시").add("서울시")),
            "regions[1].aliases[1] repeats an alias of the same region"));
  }

  private static Consumer<ObjectNode> root(Consumer<ObjectNode> edit) {
    return edit;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("brokenCopies")
  void brokenCopyStopsLoadingWithThePathAndTheRule(
      String name, Consumer<ObjectNode> edit, String rule) {
    ObjectNode broken = synthetic();
    edit.accept(broken);
    write(dataDir, broken.toString());

    assertThatThrownBy(() -> load(dataDir))
        .as(name)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith(
            "Region dictionary file is invalid: " + dataDir.resolve(RegionDictionary.FILE) + ": ")
        .hasMessageContaining(rule);
  }

  @Test
  void sameAliasInTwoRegionsIsAllowed() {
    ObjectNode root = synthetic();
    region(root, "2600000000").putArray("aliases").add("서울시");

    assertThat(load(write(dataDir, root.toString())).regions()).hasSize(22);
  }

  @Test
  void messagesNameEntriesByIndexNeverByValue() {
    ObjectNode broken = synthetic();
    region(broken, "1111000000").put("parent", "1114000000");
    write(dataDir, broken.toString());

    assertThatThrownBy(() -> load(dataDir))
        .hasMessageNotContaining("1114000000")
        .hasMessageNotContaining("1111000000")
        .hasMessageNotContaining("종로구");
  }

  @ParameterizedTest
  @ValueSource(strings = {"[]", "not json", "{\"version\": 1"})
  void fileThatIsNotADictionaryObjectIsRejected(String content) {
    write(dataDir, content);

    assertThatThrownBy(() -> load(dataDir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(dataDir.resolve(RegionDictionary.FILE).toString());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AppProperties.class)
  @Import(RegionDictionary.class)
  static class DictionaryOnly {}

  @Test
  void brokenFileStopsTheContextAndAMissingOneDoesNot(CapturedOutput output) {
    ApplicationContextRunner runner =
        new ApplicationContextRunner()
            .withBean(JsonMapper.class, () -> JSON)
            .withUserConfiguration(DictionaryOnly.class);
    ObjectNode broken = synthetic();
    region(broken, "1100000000").remove("short_name");
    Path brokenDir = write(dataDir.resolve("broken"), broken.toString());

    runner
        .withPropertyValues("app.data-dir=" + brokenDir)
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining(
                      "Region dictionary file is invalid: "
                          + brokenDir.resolve(RegionDictionary.FILE)
                          + ": regions[1].short_name must be a non-empty string");
            });
    runner
        .withPropertyValues("app.data-dir=" + dataDir.resolve("nowhere"))
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasSingleBean(RegionDictionary.class);
              assertThat(context.getBean(RegionDictionary.class).regions()).isEmpty();
            });
    assertThat(count(output.getAll(), "Region dictionary file not found")).isEqualTo(1);
  }
}
