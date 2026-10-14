package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.EXAMPLE_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Schema;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3 T2 (6): the schema {@code data/schema/media.schema.json} and {@link MediaNormalizer}
 * reach the same verdict (decision B (a) keeps both). Every {@code valid/} example passes the
 * normalization unchanged and its re-written JSON, without {@code date_relation}, passes the
 * schema. Every {@code invalid/} example is refused when read (unknown field) or dropped. The
 * synthetic records of {@link MediaNormalizerTest} are checked the other way round.
 *
 * <p>Deliberate differences: the allowed embed providers are a server setting the schema cannot
 * know (the test allows {@code example}, which the embed examples use), and the server repairs a
 * malformed {@code license_url} to {@code null} where the schema refuses it, so the server's output
 * still always passes the schema. The server also refuses every second 60 in {@code retrieved_at}
 * (the schema validator can accept one on a real leap-second date — the server is stricter, so its
 * output always passes the schema).
 */
class MediaSchemaParityTest {

  private static final String EXAMPLES = "schema/examples/media/";
  private static final Schema SCHEMA = DataSchemas.schema("media.schema.json");
  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);

  private static List<String> files(String folder) throws IOException {
    try (Stream<Path> paths = Files.list(DataSchemas.DATA.resolve(EXAMPLES + folder))) {
      return paths
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  static List<String> validExamples() throws IOException {
    return files("valid");
  }

  static List<String> invalidExamples() throws IOException {
    return files("invalid");
  }

  @Test
  void theExampleFoldersHoldThePlannedNumberOfFiles() throws IOException {
    assertThat(validExamples()).hasSizeGreaterThanOrEqualTo(8);
    assertThat(invalidExamples()).hasSizeGreaterThanOrEqualTo(10);
  }

  @ParameterizedTest(name = "valid/{0}")
  @MethodSource("validExamples")
  void validExamplesPassBothUnchanged(String name) {
    JsonNode stored = DataSchemas.read(EXAMPLES + "valid/" + name);
    assertThat(DataSchemas.errors(SCHEMA, stored)).isEmpty();

    MediaRecord input = JSON.treeToValue(stored, MediaRecord.class);
    MediaItem item = EXAMPLE_NORMALIZER.normalize(input, OLYMPICS).orElseThrow();
    assertThat(item.record()).isEqualTo(input);

    ObjectNode written = JSON.valueToTree(item);
    assertThat(written.remove("date_relation")).isNotNull();
    assertThat(DataSchemas.errors(SCHEMA, written)).isEmpty();
    assertThat(written.propertyNames()).containsExactlyElementsOf(stored.propertyNames());
  }

  @ParameterizedTest(name = "invalid/{0}")
  @MethodSource("invalidExamples")
  void invalidExamplesFailBoth(String name) {
    JsonNode stored = DataSchemas.read(EXAMPLES + "invalid/" + name);
    assertThat(DataSchemas.errors(SCHEMA, stored)).isNotEmpty();

    assertThat(readAndNormalize(stored)).isEmpty();
  }

  /** Empty when the record is refused on read or dropped by the rules. */
  private static Optional<MediaItem> readAndNormalize(JsonNode stored) {
    MediaRecord input;
    try {
      input = JSON.treeToValue(stored, MediaRecord.class);
    } catch (RuntimeException refused) {
      return Optional.empty();
    }
    return EXAMPLE_NORMALIZER.normalize(input, OLYMPICS);
  }

  @ParameterizedTest(name = "{0} on {1}: same_day, free")
  @CsvSource({
    "commons-1988-09-17-seoul-olympics-opening-pd.json, 1988-09-17",
    "commons-2002-06-22-gwangju-world-cup-kogl1.json, 2002-06-22"
  })
  void realExamplesAreSameDayAndFree(String name, LocalDate requested) {
    MediaRecord input =
        JSON.treeToValue(DataSchemas.read(EXAMPLES + "valid/" + name), MediaRecord.class);

    MediaItem item = EXAMPLE_NORMALIZER.normalize(input, requested).orElseThrow();

    assertThat(item.dateRelation()).isEqualTo(new DateRelation(Kind.SAME_DAY, 0));
    assertThat(item.record().licenseStatus()).isEqualTo(LicenseStatus.FREE);
  }

  @ParameterizedTest(name = "{0}: the schema accepts what the server returns")
  @MethodSource("io.github.ericmschoi.theworldthatday.media.MediaNormalizerTest#validBases")
  void syntheticOutputPassesTheSchema(String mode, Map<String, Object> base) {
    ObjectNode written =
        JSON.valueToTree(EXAMPLE_NORMALIZER.normalize(record(base), OLYMPICS).orElseThrow());
    written.remove("date_relation");

    assertThat(DataSchemas.errors(SCHEMA, written)).isEmpty();
  }

  static Stream<Arguments> recordsTheSchemaCanJudge() {
    // Provider allow-lists are server settings; the schema only sees a provider string.
    return MediaNormalizerTest.brokenRecords()
        .filter(args -> !args.get()[0].toString().contains("not allowed"));
  }

  @ParameterizedTest(name = "{0}: the schema refuses it too")
  @MethodSource("recordsTheSchemaCanJudge")
  void recordsTheServerDropsFailTheSchema(String rule, Map<String, Object> broken) {
    assertThat(DataSchemas.errors(SCHEMA, JSON.valueToTree(broken))).isNotEmpty();
  }
}
