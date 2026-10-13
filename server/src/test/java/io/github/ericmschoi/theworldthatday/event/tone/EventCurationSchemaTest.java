package io.github.ericmschoi.theworldthatday.event.tone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a2 T4 (2): {@code data/curation/event-tone.json} conforms to {@code
 * data/schema/event-curation.schema.json} (JSON Schema 2020-12), and deliberately broken copies of
 * it do not. Each broken copy is also rejected by the server's own loader ({@link
 * EventCuration#parse}), so the schema the curator checks and the rules the server enforces agree.
 */
class EventCurationSchemaTest {

  private static final String SCHEMA = "event-curation.schema.json";
  private static final String FILE = "curation/event-tone.json";

  private final Schema schema = DataSchemas.schema(SCHEMA);

  /** A copy of the repository file with {@code edit} applied to its first entry. */
  private static JsonNode brokenCopy(Consumer<ObjectNode> edit) {
    JsonNode copy = DataSchemas.read(FILE).deepCopy();
    edit.accept((ObjectNode) copy.path("entries").get(0));
    return copy;
  }

  private static List<String> keywords(List<Error> errors) {
    return errors.stream().map(Error::getKeyword).toList();
  }

  @Test
  void schemaIsAValidDraft202012Schema() {
    JsonNode schemaDocument = DataSchemas.read("schema/" + SCHEMA);

    assertThat(schemaDocument.path("$schema").asString()).isEqualTo(DataSchemas.META_SCHEMA);
    assertThat(DataSchemas.metaSchemaErrors(schemaDocument)).isEmpty();
  }

  @Test
  void repositoryCurationFileConformsAndHasNoTakedowns() {
    JsonNode file = DataSchemas.read(FILE);

    assertThat(DataSchemas.errors(schema, file)).isEmpty();
    assertThat(file.path("entries").size()).isEqualTo(4);
    assertThat(file.path("entries"))
        .as("hide only for rights-holder requests or clearly wrong lines; none today")
        .noneSatisfy(entry -> assertThat(entry.path("hide").asBoolean(false)).isTrue());
    assertThat(EventCuration.parse(file)).hasSize(4);
  }

  static List<Arguments> brokenCopies() {
    return List.of(
        Arguments.of("required evidence_url missing", edit(e -> e.remove("evidence_url"))),
        Arguments.of("required date missing", edit(e -> e.remove("date"))),
        Arguments.of("required reason missing", edit(e -> e.remove("reason"))),
        Arguments.of("required reviewed_at missing", edit(e -> e.remove("reviewed_at"))),
        Arguments.of("no tone while hide is false", edit(e -> e.remove("tone"))),
        Arguments.of(
            "no tone and no hide (absent hide is not a takedown)",
            edit(
                e -> {
                  e.remove("tone");
                  e.remove("hide");
                })),
        Arguments.of(
            "neither primary_link nor summary_contains",
            edit(
                e -> {
                  e.putNull("primary_link");
                  e.putNull("summary_contains");
                })),
        Arguments.of("blank primary_link only", edit(e -> e.put("primary_link", " "))),
        Arguments.of("unknown tone", edit(e -> e.put("tone", "sad"))),
        Arguments.of("unknown scope", edit(e -> e.put("scope", "global"))),
        Arguments.of("hide not a boolean", edit(e -> e.put("hide", "no"))),
        Arguments.of("unknown property", edit(e -> e.put("note", "x"))),
        Arguments.of("date not YYYY-MM-DD", edit(e -> e.put("date", "1988-9-17"))),
        Arguments.of("date that does not exist", edit(e -> e.put("date", "2001-02-29"))),
        Arguments.of(
            "evidence_url not https",
            edit(e -> e.put("evidence_url", "http://ko.wikipedia.org/w/index.php?oldid=1"))),
        Arguments.of("blank reason", edit(e -> e.put("reason", "  "))));
  }

  private static Consumer<ObjectNode> edit(Consumer<ObjectNode> edit) {
    return edit;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("brokenCopies")
  void brokenCopyFailsTheSchemaAndTheServerLoader(String name, Consumer<ObjectNode> edit) {
    JsonNode broken = brokenCopy(edit);

    assertThat(DataSchemas.errors(schema, broken)).as(name).isNotEmpty();
    assertThatIllegalArgumentException().as(name).isThrownBy(() -> EventCuration.parse(broken));
  }

  @Test
  void missingRequiredFieldIsReportedAsRequired() {
    List<Error> errors = DataSchemas.errors(schema, brokenCopy(e -> e.remove("evidence_url")));

    assertThat(keywords(errors)).contains("required");
    assertThat(errors).anySatisfy(error -> assertThat(error.getMessage()).contains("evidence_url"));
  }

  @Test
  void missingToneWithoutHideIsReportedAsRequiredTone() {
    List<Error> errors = DataSchemas.errors(schema, brokenCopy(e -> e.remove("tone")));

    assertThat(errors).anySatisfy(error -> assertThat(error.getMessage()).contains("tone"));
  }

  @Test
  void takedownEntryWithoutToneConforms() {
    JsonNode takedown =
        brokenCopy(
            e -> {
              e.remove("tone");
              e.put("hide", true);
            });

    assertThat(DataSchemas.errors(schema, takedown)).isEmpty();
    assertThat(EventCuration.parse(takedown).getFirst().hide()).isTrue();
  }

  @Test
  void brokenRootsFailTheSchema() {
    JsonNode wrongVersion = DataSchemas.read(FILE).deepCopy();
    ((ObjectNode) wrongVersion).put("version", 2);
    JsonNode noEntries = DataSchemas.read(FILE).deepCopy();
    ((ObjectNode) noEntries).remove("entries");

    assertThat(DataSchemas.errors(schema, wrongVersion)).isNotEmpty();
    assertThat(DataSchemas.errors(schema, noEntries)).isNotEmpty();
  }
}
