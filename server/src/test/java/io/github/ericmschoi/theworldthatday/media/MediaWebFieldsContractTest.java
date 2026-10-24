package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a4 T7 (6): the media item the API writes for a chosen video has exactly the fields of
 * {@code Media} in {@code web/src/media/types.ts} (read from the repository), {@code selection}
 * being the only optional one, and the nested {@code date_relation}, {@code video_sources} and
 * {@code selection} objects and the date relation kinds match too. The server value is built from a
 * synthetic record on 1988-09-17 and written by the real mapper, not from a hand-kept key list.
 */
class MediaWebFieldsContractTest {

  private static String types;

  @BeforeAll
  static void readTypes() throws IOException {
    Path file = Path.of("").toAbsolutePath().getParent().resolve("web/src/media/types.ts");
    assertThat(file).as("web/src/media/types.ts").isRegularFile();
    types = Files.readString(file, StandardCharsets.UTF_8);
  }

  /** The text between the braces of {@code export type <name> = {}. */
  private static String body(String name) {
    Matcher m =
        Pattern.compile("export type " + name + " = \\{(.*?)\\n?\\}", Pattern.DOTALL)
            .matcher(types);
    assertThat(m.find()).as("type %s in types.ts", name).isTrue();
    return m.group(1);
  }

  /** Field names of a multi-line object type; optional ones (with {@code ?}) apart. */
  private static List<String> fields(String name, boolean optional) {
    List<String> names = new ArrayList<>();
    Matcher lines = Pattern.compile("(?m)^ {2}(\\w+)(\\?)?:").matcher(body(name));
    while (lines.find()) {
      if ((lines.group(2) != null) == optional) {
        names.add(lines.group(1));
      }
    }
    return names;
  }

  /** Field names of a one-line object type such as {@code { kind: X; years: number }}. */
  private static List<String> inlineFields(String name) {
    List<String> names = new ArrayList<>();
    Matcher m = Pattern.compile("(\\w+)\\??:").matcher(body(name));
    while (m.find()) {
      names.add(m.group(1));
    }
    return names;
  }

  private static List<String> keys(JsonNode node) {
    return new ArrayList<>(node.propertyNames());
  }

  private static JsonNode chosenVideo() {
    MediaItem video =
        DEFAULT_NORMALIZER
            .normalize(record(videoFile()), LocalDate.of(1988, 9, 17))
            .orElseThrow()
            .withSelection(new Selection(Basis.COMMONS_P10, 130));
    return JSON.valueToTree(video);
  }

  @Test
  void aChosenVideoHasExactlyTheFieldsOfTheWebMediaType() {
    JsonNode video = chosenVideo();

    List<String> required = fields("Media", false);
    List<String> optional = fields("Media", true);

    assertThat(optional).containsExactly("selection");
    assertThat(keys(video)).containsExactlyInAnyOrderElementsOf(concat(required, optional));
    assertThat(required).hasSize(27).doesNotContain("selection");
  }

  @Test
  void aPhotoHasTheWebFieldsWithoutTheOptionalSelection() {
    MediaItem photo =
        DEFAULT_NORMALIZER
            .normalize(record(MediaRecords.image()), LocalDate.of(1988, 9, 17))
            .orElseThrow();

    JsonNode node = JSON.valueToTree(photo);

    assertThat(keys(node)).containsExactlyInAnyOrderElementsOf(fields("Media", false));
  }

  @Test
  void theNestedObjectsMatchTheirWebTypes() {
    JsonNode video = chosenVideo();

    assertThat(keys(video.get("date_relation")))
        .containsExactlyInAnyOrderElementsOf(inlineFields("DateRelation"));
    assertThat(keys(video.get("video_sources").get(0)))
        .containsExactlyInAnyOrderElementsOf(inlineFields("VideoSource"));
    assertThat(keys(video.get("selection")))
        .containsExactlyInAnyOrderElementsOf(fields("MediaSelection", false));
    assertThat(video.get("selection").get("score").isIntegralNumber()).isTrue();
  }

  @Test
  void theDateRelationKindsMatchTheWebUnion() {
    Matcher union =
        Pattern.compile("export type DateRelationKind =((?:\\s*\\|\\s*'[a-z_]+')+)").matcher(types);
    assertThat(union.find()).isTrue();
    List<String> web = new ArrayList<>();
    Matcher quoted = Pattern.compile("'([a-z_]+)'").matcher(union.group(1));
    while (quoted.find()) {
      web.add(quoted.group(1));
    }

    assertThat(Arrays.stream(DateRelation.Kind.values()).map(DateRelation.Kind::value))
        .containsExactlyInAnyOrderElementsOf(web);
  }

  private static List<String> concat(List<String> a, List<String> b) {
    List<String> all = new ArrayList<>(a);
    all.addAll(b);
    return all;
  }
}
