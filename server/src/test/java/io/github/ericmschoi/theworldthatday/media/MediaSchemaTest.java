package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.STORED_KEYS;
import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.path.NodePath;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a3 T4 (1), (2): the schema {@code data/schema/media.schema.json} and its examples as T1
 * (1) to (4) describe them. The schema is a valid 2020-12 schema with the planned shape, the {@code
 * valid/} folder holds the planned kinds of example, and every {@code invalid/} file fails with
 * errors only at the fields its README row names. {@link MediaSchemaParityTest} covers the
 * agreement between the schema and the server normalization.
 */
class MediaSchemaTest {

  private static final String SCHEMA_FILE = "schema/media.schema.json";
  private static final String EXAMPLES = "schema/examples/media/";
  private static final Schema SCHEMA = DataSchemas.schema("media.schema.json");
  private static final Pattern CODE = Pattern.compile("`([^`]*)`");

  /**
   * CLAUDE.md 7.6 and decision M3-P13: the demo dates, the leap-day boundary and the public event
   * sample dates of docs/plans/m3-video-sources-research.md. The {@code video_embed} example copies
   * the M3a5 contract example, which uses the 1986-09-20 Asian Games opening.
   */
  private static final List<String> ALLOWED_DATES =
      List.of(
          "1988-09-17",
          "2002-06-22",
          "1995-03-15",
          "2000-02-29",
          "1986-09-20",
          "1950-06-25",
          "1969-07-20",
          "2014-04-16");

  private static JsonNode schemaDocument() {
    return DataSchemas.read(SCHEMA_FILE);
  }

  private static List<String> files(String folder) throws IOException {
    try (Stream<Path> paths = Files.list(DataSchemas.DATA.resolve(EXAMPLES + folder))) {
      return paths
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  private static List<JsonNode> validExamples() throws IOException {
    List<JsonNode> examples = new ArrayList<>();
    for (String name : files("valid")) {
      examples.add(DataSchemas.read(EXAMPLES + "valid/" + name));
    }
    return examples;
  }

  // ---- T1 (1): the schema itself ----

  @Test
  void schemaIsAValidDraft202012Schema() {
    JsonNode schema = schemaDocument();

    assertThat(schema.path("$schema").asString()).isEqualTo(DataSchemas.META_SCHEMA);
    assertThat(DataSchemas.metaSchemaErrors(schema)).isEmpty();
  }

  @Test
  void schemaRequiresExactlyTheStoredFieldsAndNothingElse() {
    JsonNode schema = schemaDocument();

    assertThat(schema.path("additionalProperties").isBoolean()).isTrue();
    assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
    assertThat(schema.path("properties").propertyNames())
        .containsExactlyInAnyOrderElementsOf(STORED_KEYS);
    List<String> required = new ArrayList<>();
    schema.path("required").forEach(key -> required.add(key.asString()));
    assertThat(required).containsExactlyInAnyOrderElementsOf(STORED_KEYS);
    assertThat(schema.path("properties").has("date_relation"))
        .as("API-only fields are not stored")
        .isFalse();
  }

  @Test
  void addressesUseOnlyTheHttpPatternAndNeverFormatUri() throws IOException {
    String text =
        Files.readString(DataSchemas.DATA.resolve(SCHEMA_FILE), StandardCharsets.UTF_8)
            .replaceAll("\\s", "");
    JsonNode defs = schemaDocument().path("$defs");

    assertThat(text).doesNotContain("\"format\":\"uri\"");
    assertThat(defs.path("httpUrl").path("pattern").asString()).isEqualTo("^https?://\\S+$");
    for (String address : List.of("url", "thumbnail_url", "embed_url", "license_url")) {
      assertThat(schemaDocument().path("properties").path(address).path("$ref").asString())
          .as(address)
          .isEqualTo("#/$defs/nullableHttpUrl");
    }
    assertThat(schemaDocument().path("properties").path("source_url").path("$ref").asString())
        .isEqualTo("#/$defs/httpUrl");
    assertThat(defs.path("videoSource").path("properties").path("url").path("$ref").asString())
        .isEqualTo("#/$defs/httpUrl");
  }

  @Test
  void retrievedAtPlaceRefAndProviderFollowThePlan() {
    JsonNode properties = schemaDocument().path("properties");

    assertThat(properties.path("retrieved_at").path("format").asString()).isEqualTo("date-time");
    assertThat(properties.path("place_ref").toString())
        .contains("\"pattern\":\"^(world|unknown|[a-z]+:\\\\S+)$\"");
    assertThat(properties.path("provider").toString())
        .as("allowed providers are server settings; the schema checks only a string")
        .doesNotContain("enum")
        .doesNotContain("const");
  }

  // ---- T1 (2): what valid/ holds ----

  @Test
  void validFolderHoldsThePlannedExamples() throws IOException {
    List<JsonNode> examples = validExamples();

    assertThat(examples).hasSizeGreaterThanOrEqualTo(8);
    assertThat(examples)
        .as("real Commons photo of 1988-09-17, public domain")
        .anySatisfy(
            e -> {
              assertThat(e.path("id").asString()).startsWith("commons-");
              assertThat(e.path("source_url").asString())
                  .startsWith("https://commons.wikimedia.org/wiki/File:");
              assertThat(e.path("captured_at").asString()).isEqualTo("1988-09-17");
              assertThat(e.path("license").asString()).isEqualTo("Public domain");
            });
    assertThat(examples)
        .as("real Commons photo of 2002-06-22, KOGL Type 1")
        .anySatisfy(
            e -> {
              assertThat(e.path("id").asString()).startsWith("commons-");
              assertThat(e.path("source_url").asString())
                  .startsWith("https://commons.wikimedia.org/wiki/File:");
              assertThat(e.path("captured_at").asString()).isEqualTo("2002-06-22");
              assertThat(e.path("license").asString()).isEqualTo("KOGL Type 1");
            });
    assertThat(examples)
        .as("photo with license null and status unknown, still shown as an image")
        .anySatisfy(
            e -> {
              assertThat(e.path("license").isNull()).isTrue();
              assertThat(e.path("license_status").asString()).isEqualTo("unknown");
              assertThat(e.path("display_mode").asString()).isEqualTo("image");
            });
    assertThat(examples)
        .as("photo whose capture time is unknown")
        .anySatisfy(
            e -> {
              assertThat(e.path("type").asString()).isEqualTo("photo");
              assertThat(e.path("captured_at").isNull()).isTrue();
              assertThat(e.path("date_precision").isNull()).isTrue();
            });
    for (String mode : List.of("image_embed", "video_file", "video_embed", "sdk")) {
      assertThat(examples)
          .as("synthetic %s example", mode)
          .anySatisfy(
              e -> {
                assertThat(e.path("display_mode").asString()).isEqualTo(mode);
                assertThat(e.path("id").asString()).startsWith("example-");
              });
    }
  }

  @Test
  void syntheticExamplesUseExampleIdsAndOnlyAllowedDates() throws IOException {
    for (JsonNode e : validExamples()) {
      String id = e.path("id").asString();
      if (id.startsWith("commons-")) {
        continue;
      }
      assertThat(id).as("synthetic id").startsWith("example-");
      if (!e.path("captured_at").isNull()) {
        String at = e.path("captured_at").asString();
        assertThat(ALLOWED_DATES)
            .as(
                "%s captured_at %s is a demo, boundary or public event date (CLAUDE.md 7.6)",
                id, at)
            .anySatisfy(date -> assertThat(date).startsWith(at));
      }
    }
  }

  // ---- T1 (3), (4) and T4 (2): invalid/ against the README ----

  /** README rows of one section: file name to the remaining cells. */
  private static Map<String, List<String>> readmeRows(String section) throws IOException {
    List<String> lines =
        Files.readAllLines(
            DataSchemas.DATA.resolve(EXAMPLES + "README.md"), StandardCharsets.UTF_8);
    int start = lines.indexOf("## " + section);
    assertThat(start).as("README section %s", section).isNotNegative();
    Map<String, List<String>> rows = new LinkedHashMap<>();
    for (String line : lines.subList(start + 1, lines.size())) {
      if (line.startsWith("## ")) {
        break;
      }
      if (!line.startsWith("| `")) {
        continue;
      }
      List<String> cells =
          Stream.of(line.substring(1, line.length() - 1).split(" \\| "))
              .map(String::strip)
              .toList();
      Matcher name = CODE.matcher(cells.getFirst());
      assertThat(name.find()).isTrue();
      if (name.group(1).endsWith(".json")) {
        rows.put(name.group(1), cells.subList(1, cells.size()));
      }
    }
    return rows;
  }

  @Test
  void readmeDescribesEveryExampleFile() throws IOException {
    Map<String, List<String>> valid = readmeRows("valid/");
    Map<String, List<String>> invalid = readmeRows("invalid/");

    assertThat(valid.keySet()).containsExactlyInAnyOrderElementsOf(files("valid"));
    assertThat(invalid.keySet()).containsExactlyInAnyOrderElementsOf(files("invalid"));
    valid.forEach(
        (name, cells) -> {
          String source = cells.getLast();
          if (name.startsWith("commons-")) {
            assertThat(source)
                .as("%s names its real source and check date", name)
                .startsWith("https://commons.wikimedia.org/wiki/File:")
                .containsPattern("checked \\d{4}-\\d{2}-\\d{2}");
          } else {
            assertThat(source).as(name).isEqualTo("synthetic");
          }
        });
    invalid.forEach(
        (name, cells) ->
            assertThat(cells.getFirst())
                .as("%s names the rule it breaks", name)
                .containsPattern("^(R[1-9] |`additionalProperties: false`)"));
  }

  @Test
  void invalidFolderCoversEveryRuleAndThePlannedCases() throws IOException {
    Map<String, List<String>> invalid = readmeRows("invalid/");
    Set<String> rules = new TreeSet<>();
    invalid.values().forEach(cells -> rules.add(cells.getFirst().split(" ")[0]));

    assertThat(invalid).hasSizeGreaterThanOrEqualTo(10);
    assertThat(rules).contains("R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9");
    assertThat(DataSchemas.read(EXAMPLES + "invalid/r1-empty-source-url.json").path("source_url"))
        .hasToString("\"\"");
    assertThat(
            DataSchemas.read(EXAMPLES + "invalid/r9-javascript-thumbnail-url.json")
                .path("thumbnail_url")
                .asString())
        .startsWith("javascript:");
  }

  static Stream<String> invalidFiles() throws IOException {
    return files("invalid").stream();
  }

  /** The JSON pointer of an error's instance location; {@code ""} is the document root. */
  private static String pointer(NodePath path) {
    StringBuilder pointer = new StringBuilder();
    for (int i = 0; i < path.getNameCount(); i++) {
      pointer.append('/').append(path.getElement(i));
    }
    return pointer.toString();
  }

  /** The "Error points at" cell: backticked pointers, or {@code ""} for "root (...)". */
  private static Set<String> expectedPointers(String cell) {
    if (cell.startsWith("root")) {
      return Set.of("");
    }
    Set<String> pointers = new TreeSet<>();
    Matcher code = CODE.matcher(cell);
    while (code.find()) {
      pointers.add(code.group(1));
    }
    return pointers;
  }

  @ParameterizedTest(name = "invalid/{0}")
  @MethodSource("invalidFiles")
  void invalidExampleErrorsPointAtTheFieldsTheReadmeNames(String name) throws IOException {
    List<String> cells = readmeRows("invalid/").get(name);
    assertThat(cells).as("README row for %s", name).isNotNull();
    Set<String> expected = expectedPointers(cells.getLast());
    assertThat(expected).as("README pointers for %s", name).isNotEmpty();

    List<Error> errors = DataSchemas.errors(SCHEMA, DataSchemas.read(EXAMPLES + "invalid/" + name));

    Set<String> actual =
        errors.stream()
            .map(error -> pointer(error.getInstanceLocation()))
            .collect(Collectors.toCollection(TreeSet::new));
    assertThat(errors).as(name).isNotEmpty();
    assertThat(actual).as("%s errors: %s", name, errors).containsAll(expected);
    // A oneOf wrapper (e.g. nullable video_sources) also reports at the parent of the named field;
    // nothing may point anywhere else, the document root included.
    assertThat(actual)
        .as("%s errors: %s", name, errors)
        .allSatisfy(
            at ->
                assertThat(expected)
                    .anySatisfy(
                        field ->
                            assertThat(
                                    field.equals(at)
                                        || (!at.isEmpty() && field.startsWith(at + "/")))
                                .isTrue()));
  }

  @Test
  void apiOnlyFieldIsRefusedByName() {
    List<Error> errors =
        DataSchemas.errors(
            SCHEMA, DataSchemas.read(EXAMPLES + "invalid/api-only-date-relation.json"));

    assertThat(errors)
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.getKeyword()).isEqualTo("additionalProperties");
              assertThat(error.getProperty()).isEqualTo("date_relation");
            });
  }
}
