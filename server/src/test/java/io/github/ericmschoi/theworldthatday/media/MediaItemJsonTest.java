package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.EXAMPLE_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.STORED_KEYS;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.sdk;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/** The JSON of a {@link MediaItem}: every stored field plus {@code date_relation}, snake_case. */
class MediaItemJsonTest {

  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);

  private static JsonNode json(MediaNormalizer normalizer, Map<String, Object> fields) {
    return JSON.valueToTree(normalizer.normalize(record(fields), OLYMPICS).orElseThrow());
  }

  private static List<String> keys(JsonNode node) {
    return new ArrayList<>(node.propertyNames());
  }

  @Test
  void keysAreTheStoredFieldsPlusDateRelationIncludingNulls() {
    JsonNode node = json(DEFAULT_NORMALIZER, image());

    List<String> expected = new ArrayList<>(STORED_KEYS);
    expected.add("date_relation");
    assertThat(keys(node)).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(node.get("video_sources").isNull()).isTrue();
    assertThat(node.get("render").isNull()).isTrue();
    assertThat(node.get("license_url").isNull()).isTrue();
    assertThat(node.get("display_mode").asString()).isEqualTo("image");
    assertThat(node.get("license_status").asString()).isEqualTo("free");
    assertThat(node.get("date_precision").asString()).isEqualTo("day");
  }

  @Test
  void dateRelationAlwaysCarriesYears() {
    JsonNode relation = json(DEFAULT_NORMALIZER, image()).get("date_relation");

    assertThat(keys(relation)).containsExactlyInAnyOrder("kind", "years");
    assertThat(relation.get("kind").asString()).isEqualTo("same_day");
    assertThat(relation.get("years").asInt()).isZero();
  }

  @Test
  void nestedTypesAreSnakeCase() {
    JsonNode source = json(DEFAULT_NORMALIZER, videoFile()).get("video_sources").get(0);
    assertThat(keys(source)).containsExactlyInAnyOrder("url", "mime", "height");
    assertThat(source.get("mime").asString()).isEqualTo("video/webm");

    JsonNode render = json(EXAMPLE_NORMALIZER, sdk()).get("render");
    assertThat(keys(render)).containsExactlyInAnyOrder("provider", "kind", "lat", "lng", "zoom");
    assertThat(render.get("kind").asString()).isEqualTo("roadview");
  }

  @Test
  void writtenItemWithoutDateRelationReadsBackAsTheSameRecord() {
    MediaRecord input = record(videoFile());
    JsonNode node = JSON.valueToTree(DEFAULT_NORMALIZER.normalize(input, OLYMPICS).orElseThrow());
    ((tools.jackson.databind.node.ObjectNode) node).remove("date_relation");

    assertThat(JSON.treeToValue(node, MediaRecord.class)).isEqualTo(input);
  }

  @ParameterizedTest(name = "{0} carries @JsonNaming(SnakeCaseStrategy)")
  @ValueSource(
      classes = {
        MediaItem.class,
        MediaRecord.class,
        VideoSource.class,
        Render.class,
        DateRelation.class
      })
  void everyMediaTypeNamesItsOwnFieldsInSnakeCase(Class<?> type) {
    assertThat(type.getAnnotation(JsonNaming.class).value()).isEqualTo(SnakeCaseStrategy.class);
  }

  @Test
  void unknownFieldsAndValuesAreRejectedOnRead() {
    Map<String, Object> extra = MediaRecords.with(image(), "date_relation", Map.of());
    assertThatThrownBy(() -> record(extra)).isInstanceOf(RuntimeException.class);

    Map<String, Object> badMode = MediaRecords.with(image(), "display_mode", "embed");
    assertThatThrownBy(() -> record(badMode)).isInstanceOf(RuntimeException.class);
  }
}
