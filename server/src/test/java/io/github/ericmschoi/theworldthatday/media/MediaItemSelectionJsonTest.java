package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.STORED_KEYS;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a4 T6 (2): {@code selection} is written only when set. Photo items keep the 26 stored keys
 * plus {@code date_relation}; a chosen video adds {@code selection: {basis, score}}, and without
 * the two API-only keys it passes {@code media.schema.json}. Synthetic records on 1988-09-17.
 */
class MediaItemSelectionJsonTest {

  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);

  private static MediaItem item(Map<String, Object> fields) {
    return DEFAULT_NORMALIZER.normalize(record(fields), OLYMPICS).orElseThrow();
  }

  private static List<String> keys(JsonNode node) {
    return new ArrayList<>(node.propertyNames());
  }

  @Test
  void anItemWithoutSelectionHasNoSelectionKey() {
    MediaItem video = item(videoFile());

    JsonNode node = JSON.valueToTree(video);

    List<String> expected = new ArrayList<>(STORED_KEYS);
    expected.add("date_relation");
    assertThat(keys(node)).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(video.selection()).isNull();
    assertThat(new MediaItem(video.record(), video.dateRelation())).isEqualTo(video);
  }

  @Test
  void aChosenVideoAddsSelectionWithAnIntegerScore() {
    MediaItem video = item(videoFile()).withSelection(new Selection(Basis.COMMONS_CATEGORY, 90));

    JsonNode node = JSON.valueToTree(video);

    List<String> expected = new ArrayList<>(STORED_KEYS);
    expected.add("date_relation");
    expected.add("selection");
    assertThat(keys(node)).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(keys(node.get("selection"))).containsExactlyInAnyOrder("basis", "score");
    assertThat(node.get("selection").get("basis").asString()).isEqualTo("commons_category");
    assertThat(node.get("selection").get("score").isInt()).isTrue();
    assertThat(node.get("selection").get("score").asInt()).isEqualTo(90);
  }

  @Test
  void withoutTheApiOnlyKeysAChosenVideoPassesTheSchema() {
    MediaItem video = item(videoFile()).withSelection(new Selection(Basis.COMMONS_P10, 130));
    ObjectNode node = (ObjectNode) JSON.valueToTree(video);

    node.remove(List.of("date_relation", "selection"));

    assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), node)).isEmpty();
    assertThat(JSON.treeToValue(node, MediaRecord.class)).isEqualTo(video.record());
  }

  @Test
  void withSelectionKeepsTheRecordAndDateRelation() {
    MediaItem photo = item(image());

    MediaItem chosen = photo.withSelection(new Selection(Basis.DAEHAN_NEWS, 60));

    assertThat(chosen.record()).isSameAs(photo.record());
    assertThat(chosen.dateRelation()).isEqualTo(photo.dateRelation());
    assertThat(chosen).isNotEqualTo(photo);
    assertThat(photo.selection()).isNull();
  }
}
