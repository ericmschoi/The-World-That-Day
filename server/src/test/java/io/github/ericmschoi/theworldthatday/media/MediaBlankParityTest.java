package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.EXAMPLE_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.sdk;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3a T3 (9) and T2b (1), (2): the server ({@link MediaNormalizer}) and the schema ({@code
 * data/schema/media.schema.json}) call the same values blank, character by character and field by
 * field, and both refuse the leap second {@code 2026-10-13T23:59:60Z}. Values are synthetic; the
 * space characters are written as escapes so they stay visible.
 */
class MediaBlankParityTest {

  private static final Schema SCHEMA = DataSchemas.schema("media.schema.json");
  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);
  private static final String EXAMPLES = "schema/examples/media/";

  /** The characters plan M3a3a T3 (9) names, each alone. */
  static final List<String> PLAN_SPACES =
      List.of(" ", "\u0009", "\n", "\u001F", " ", " ", " ", " ", "　");

  /** The rest of the space set: Java whitespace controls and category Z separators. */
  static final List<String> OTHER_SPACES =
      List.of(
          "\u000B", "\u000C", "\r", "\u001C", "\u001D", "\u001E", " ", " ", " ", " ", " ", "  ",
          "　\t ", "");

  /**
   * Outside the space set on both sides (plan M3a3a risk 7 (b)): zero-width and other format
   * characters, NEL and other controls stay "not blank". Inner spaces never make text blank.
   */
  static final List<String> NOT_BLANK =
      List.of(" 작가", "Example Author", "​", "﻿", "\u0085", "\u0000", "­", "⁠", "x", "　가　");

  /** Fields of the image base that are required (R1) and text in the schema. */
  static final List<String> REQUIRED = List.of("attribution", "id", "source_name");

  /** Nullable text fields of the image base: {@code null} or not blank on both sides. */
  static final List<String> OPTIONAL = List.of("license", "description", "provider");

  private static Optional<MediaItem> server(Map<String, Object> fields) {
    return EXAMPLE_NORMALIZER.normalize(record(fields), OLYMPICS);
  }

  private static boolean schemaAccepts(Map<String, Object> fields) {
    return DataSchemas.errors(SCHEMA, JSON.valueToTree(fields)).isEmpty();
  }

  private static String visible(String value) {
    StringBuilder out = new StringBuilder();
    value.codePoints().forEach(c -> out.append(String.format("U+%04X ", c)));
    return out.toString().strip();
  }

  static Stream<Arguments> blankValues() {
    List<String> blanks = new ArrayList<>(PLAN_SPACES);
    blanks.addAll(OTHER_SPACES);
    Stream<Arguments> fields =
        Stream.concat(REQUIRED.stream(), OPTIONAL.stream())
            .flatMap(f -> blanks.stream().map(b -> arguments(f, b, visible(b))));
    return fields;
  }

  @ParameterizedTest(name = "{0} = [{2}]: server drops, schema refuses")
  @MethodSource("blankValues")
  void blankTextIsRefusedByBoth(String field, String blank, String shown) {
    Map<String, Object> fields = with(image(), field, blank);

    assertThat(server(fields)).as("server keeps %s = %s", field, shown).isEmpty();
    assertThat(schemaAccepts(fields)).as("schema accepts %s = %s", field, shown).isFalse();
  }

  @ParameterizedTest(name = "render.provider = [{1}]: server drops, schema refuses")
  @MethodSource("planSpaces")
  void blankRenderProviderIsRefusedByBoth(String blank, String shown) {
    Map<String, Object> fields = sdkWithRenderProvider(blank);

    assertThat(server(fields)).as("server keeps render.provider = %s", shown).isEmpty();
    assertThat(schemaAccepts(fields)).as("schema accepts render.provider = %s", shown).isFalse();
  }

  static Stream<Arguments> planSpaces() {
    return PLAN_SPACES.stream().map(b -> arguments(b, visible(b)));
  }

  private static Map<String, Object> sdkWithRenderProvider(String provider) {
    Map<String, Object> base = sdk();
    @SuppressWarnings("unchecked")
    Map<String, Object> render = new HashMap<>((Map<String, Object>) base.get("render"));
    render.put("provider", provider);
    return with(base, "render", render);
  }

  static Stream<Arguments> notBlankValues() {
    return Stream.concat(REQUIRED.stream(), OPTIONAL.stream())
        .flatMap(f -> NOT_BLANK.stream().map(v -> arguments(f, v, visible(v))));
  }

  /** Kept text: the server keeps it unchanged and its output passes the schema. */
  @ParameterizedTest(name = "{0} = [{2}]: server keeps, schema passes")
  @MethodSource("notBlankValues")
  void textOutsideTheSpaceSetPassesBoth(String field, String value, String shown) {
    Map<String, Object> fields = with(image(), field, value);

    Optional<MediaItem> item = server(fields);

    assertThat(item).as("server drops %s = %s", field, shown).isPresent();
    assertThat(item.get().record()).isEqualTo(record(fields));
    ObjectNode written = JSON.valueToTree(item.get());
    written.remove("date_relation");
    assertThat(DataSchemas.errors(SCHEMA, written)).as("schema on %s = %s", field, shown).isEmpty();
  }

  @Test
  void attributionWithALeadingNoBreakSpaceBeforeHangulPassesBoth() {
    Map<String, Object> fields = with(image(), "attribution", " 작가");

    assertThat(server(fields)).isPresent();
    assertThat(schemaAccepts(fields)).isTrue();
  }

  // ---- every BMP character: server and schema use the same space set ----

  private static final SchemaRegistry REGISTRY =
      SchemaRegistry.withDefaultDialect(
          SpecificationVersion.DRAFT_2020_12,
          builder ->
              builder
                  .schemaRegistryConfig(
                      SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
                  .schemaLoader(loader -> loader.fetchRemoteResources(false)));

  /** {@code $defs.nonEmptyString} of the media schema on its own, validated the same way. */
  private static Schema nonEmptyString() {
    ObjectNode def =
        (ObjectNode)
            DataSchemas.read("schema/media.schema.json").path("$defs").path("nonEmptyString");
    ObjectNode schema = def.deepCopy();
    schema.put("$schema", DataSchemas.META_SCHEMA);
    return REGISTRY.getSchema(schema);
  }

  /** The plan M3a3a space set: {@link Character#isWhitespace} or Unicode category Z. */
  private static boolean inSpaceSet(int c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }

  /**
   * Each single character of the Basic Multilingual Plane (surrogates excepted) and a sample of the
   * other planes, as {@code attribution}: the server drops it exactly when the schema definition
   * refuses it, and exactly when it is in the plan's space set.
   */
  @Test
  void everySingleCharacterGetsTheSameVerdict() {
    Schema def = nonEmptyString();
    Map<String, Object> base = image();
    List<String> mismatches = new ArrayList<>();
    IntStream codePoints =
        IntStream.concat(
            IntStream.rangeClosed(0, 0xFFFF)
                .filter(c -> c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE),
            IntStream.of(0x1F600, 0x20000, 0xE0001, 0xE0020, 0xE007F, 0x10FFFF));
    int checked = 0;
    for (Iterator<Integer> it = codePoints.iterator(); it.hasNext(); ) {
      int c = it.next();
      String value = Character.toString(c);
      boolean serverBlank =
          DEFAULT_NORMALIZER
              .normalize(record(with(base, "attribution", value)), OLYMPICS)
              .isEmpty();
      JsonNode node = JSON.valueToTree(value);
      boolean schemaBlank = !def.validate(node).isEmpty();
      if (serverBlank != schemaBlank || serverBlank != inSpaceSet(c)) {
        mismatches.add(
            String.format(
                "U+%04X server=%s schema=%s set=%s", c, serverBlank, schemaBlank, inSpaceSet(c)));
      }
      checked++;
    }

    assertThat(checked).isEqualTo(0x10000 - 0x800 + 6);
    assertThat(mismatches).isEmpty();
  }

  // ---- leap second (PR #12 review recommendation 1) ----

  @Test
  void leapSecondIsRefusedByBothAndTheLastSecondPassesBoth() {
    Map<String, Object> leap = with(image(), "retrieved_at", "2026-10-13T23:59:60Z");
    Map<String, Object> last = with(image(), "retrieved_at", "2026-10-13T23:59:59Z");

    assertThat(server(leap)).isEmpty();
    assertThat(schemaAccepts(leap)).isFalse();
    assertThat(server(last)).isPresent();
    assertThat(schemaAccepts(last)).isTrue();
  }

  // ---- T2b (1), (2): the schema definition and the new invalid example ----

  @Test
  void nonEmptyStringKeepsTypeAndMinLengthAndOnlyChangesThePattern() {
    JsonNode def =
        DataSchemas.read("schema/media.schema.json").path("$defs").path("nonEmptyString");

    assertThat(def.propertyNames())
        .containsExactlyInAnyOrder("$comment", "type", "minLength", "pattern");
    assertThat(def.path("type").asString()).isEqualTo("string");
    assertThat(def.path("minLength").asInt()).isEqualTo(1);
    assertThat(def.path("pattern").asString()).isEqualTo("[^\\s\\p{Z}\\x1C-\\x1F]");
  }

  @Test
  void noBreakSpaceExampleDiffersFromTheBlankExampleOnlyInIdAndAttribution() {
    JsonNode blank = DataSchemas.read(EXAMPLES + "invalid/r1-blank-attribution.json");
    JsonNode nbsp = DataSchemas.read(EXAMPLES + "invalid/r1-nbsp-only-attribution.json");

    assertThat(nbsp.propertyNames()).containsExactlyElementsOf(blank.propertyNames());
    List<String> differing = new ArrayList<>();
    for (String key : blank.propertyNames()) {
      if (!blank.path(key).equals(nbsp.path(key))) {
        differing.add(key);
      }
    }
    assertThat(differing).containsExactly("id", "attribution");
    assertThat(nbsp.path("id").asString()).isEqualTo("example-invalid-r1-nbsp-only-attribution");
    assertThat(nbsp.path("attribution").asString()).isEqualTo("  ");
  }

  @ParameterizedTest(name = "invalid/{0}: server drops, schema refuses")
  @ValueSource(strings = {"r1-blank-attribution.json", "r1-nbsp-only-attribution.json"})
  void blankAttributionExamplesFailBoth(String name) {
    JsonNode stored = DataSchemas.read(EXAMPLES + "invalid/" + name);

    assertThat(DataSchemas.errors(SCHEMA, stored)).isNotEmpty();
    assertThat(EXAMPLE_NORMALIZER.normalize(JSON.treeToValue(stored, MediaRecord.class), OLYMPICS))
        .isEmpty();
  }
}
