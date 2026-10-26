package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat.Restored;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a4c T2 (2) to (5): the stored text of a cache row with synthetic candidates (made-up file
 * names, IDs and addresses; dates are boundaries and the leap day 2000-02-29). Reading filters and
 * normalizes again, and unreadable text is absent, never an exception.
 */
@ExtendWith(OutputCaptureExtension.class)
class SearchCacheFormatTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final String FILE = "Example test video.webm";
  private static final MediaOrigin ORIGIN =
      new MediaOrigin("commons", FILE, "Example Author", null, null);

  /** No capture time, no description and no stated date. */
  private static final Candidate PLAIN =
      new Candidate(
          VideoCandidates.commonsVideo("commons-900001", 95.0, null),
          FILE,
          null,
          CaptureTime.UNKNOWN,
          List.of(),
          EventLink.NONE,
          false,
          false,
          Basis.COMMONS_DATE_CATEGORY,
          ORIGIN);

  /** Every field set: a day capture, a stated date, an official embed with a channel. */
  private static final Candidate FULL =
      new Candidate(
          VideoCandidates.youtubeVideo(
              "AAAAAAAAAAA", 120.0, new CaptureTime("2000-02-29", DatePrecision.DAY), LEAP_DAY),
          "지어낸 제목 2000-02-29",
          "지어낸 설명이다.",
          new CaptureTime("2000-02", DatePrecision.MONTH),
          List.of(LEAP_DAY, LocalDate.of(2000, 3, 1)),
          EventLink.CATEGORY_DIRECT,
          true,
          true,
          Basis.YOUTUBE_OFFICIAL,
          new MediaOrigin("youtube", "AAAAAAAAAAA", null, null, "UCxxxxxxxxxxxxxxxxxxxxxx"));

  private static SearchCacheFormat format(List<BlockEntry> blocklist) {
    return new SearchCacheFormat(
        new MediaFilter(
            new MediaProperties(
                List.of(), List.of("youtube"), blocklist, List.of("AP", "Associated Press"))),
        VideoCandidates.NORMALIZER);
  }

  private final SearchCacheFormat format = format(List.of());

  private String written(String label, Candidate... candidates) {
    return format.write(label, List.of(candidates)).orElseThrow();
  }

  // ---- (2) round trip ----

  @Test
  void syntheticCandidatesComeBackEqual() {
    String text = written("Synthetic label", PLAIN, FULL);

    assertThat(format.read(text, LEAP_DAY))
        .contains(new Restored("Synthetic label", List.of(PLAIN, FULL)));
  }

  @Test
  void noCandidatesAndNoLabelAreStoredToo() {
    assertThat(format.read(written(null), LEAP_DAY)).contains(new Restored(null, List.of()));
  }

  @Test
  void theSelectionIsNeverStoredAndTheRestoredItemHasNone() {
    MediaItem chosen = PLAIN.item().withSelection(new Selection(Basis.COMMONS_DATE_CATEGORY, 70));
    Candidate selected =
        new Candidate(
            chosen,
            PLAIN.title(),
            null,
            null,
            null,
            PLAIN.eventLink(),
            false,
            false,
            PLAIN.basis(),
            ORIGIN);

    String text = written(null, selected);

    assertThat(text).doesNotContain("selection", "date_relation", "\"score\"");
    assertThat(format.read(text, LEAP_DAY).orElseThrow().candidates())
        .singleElement()
        .satisfies(c -> assertThat(c.item().selection()).isNull());
  }

  // ---- (3) filtered and normalized again ----

  @Test
  void aFileBlockedAfterItWasStoredIsLeftOut() {
    String text = written(null, PLAIN, FULL);
    SearchCacheFormat blocked = format(List.of(new BlockEntry("commons", FILE, null, null)));

    assertThat(blocked.read(text, LEAP_DAY).orElseThrow().candidates()).containsExactly(FULL);
  }

  @Test
  void anAgencyAuthorIsLeftOut() {
    Candidate agency =
        new Candidate(
            PLAIN.item(),
            "Other test video.webm",
            null,
            null,
            null,
            EventLink.NONE,
            false,
            false,
            Basis.COMMONS_SEARCH,
            new MediaOrigin("commons", "Other test video.webm", "Associated Press", null, null));

    String text = written(null, agency, FULL);

    assertThat(format.read(text, LEAP_DAY).orElseThrow().candidates()).containsExactly(FULL);
  }

  @Test
  void aRecordThatNowBreaksARuleIsLeftOut() {
    String text =
        written(null, PLAIN, FULL)
            .replace(
                "\"source_url\":\"https://commons.wikimedia.org/wiki/File:Example_test_video.webm\"",
                "\"source_url\":null");

    assertThat(format.read(text, LEAP_DAY).orElseThrow().candidates()).containsExactly(FULL);
  }

  @Test
  void retrievedAtStaysAndTheDateRelationFollowsTheRowDate() {
    LocalDate later = LocalDate.of(2001, 2, 28);

    Candidate restored = format.read(written(null, FULL), later).orElseThrow().candidates().get(0);

    assertThat(restored.item().record().retrievedAt()).isEqualTo("2026-10-18T00:00:00Z");
    assertThat(restored.item().dateRelation())
        .isEqualTo(DateRelation.of("2000-02-29", DatePrecision.DAY, later))
        .isNotEqualTo(FULL.item().dateRelation());
  }

  // ---- (4) unreadable text is absent ----

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      value = {
        "unknown version | \"version\":1 | \"version\":2",
        "broken JSON | \"candidates\":[ | \"candidates\":[{{",
        "missing basis | ,\"basis\":\"commons_date_category\" | ''",
        "missing record field | \"license_url\":\"https://creativecommons.org/licenses/by-sa/4.0\", | ''",
        "missing origin field | \"credit\":null, | ''",
        "unknown basis | \"basis\":\"commons_date_category\" | \"basis\":\"other\"",
        "capture breaks R8 | \"original_captured_at\":\"2000-02\" | \"original_captured_at\":\"2000-02-30\"",
        "capture shape breaks R8 | \"original_captured_at\":\"2000-02\" | \"original_captured_at\":\"2000\"",
        "null stated date | [\"2000-02-29\", | [null,",
        "null candidate | \"candidates\":[ | \"candidates\":[null,",
        "unknown key | \"version\":1, | \"version\":1,\"extra\":true,"
      })
  void unreadableTextIsAbsentAndNeverThrows(String name, String from, String to) {
    String text = written("Synthetic label", PLAIN, FULL);
    assertThat(text).contains(from);

    assertThat(format.read(text.replace(from, to), LEAP_DAY)).as(name).isEmpty();
  }

  @ParameterizedTest(name = "null {0}")
  @CsvSource({"record", "origin", "event_link", "basis", "explicit_dates"})
  void aNullRequiredValueIsAbsent(String key) {
    JsonMapper json = JsonMapper.builder().build();
    ObjectNode row = (ObjectNode) json.readTree(written("Synthetic label", PLAIN, FULL));
    ((ObjectNode) row.path("candidates").path(1)).putNull(key);

    assertThat(format.read(json.writeValueAsString(row), LEAP_DAY)).isEmpty();
  }

  @Test
  void truncatedTextIsAbsent() {
    String text = written("Synthetic label", PLAIN, FULL);

    assertThat(format.read(text.substring(0, text.length() - 1), LEAP_DAY)).isEmpty();
    assertThat(format.read(text.substring(0, text.length() / 2), LEAP_DAY)).isEmpty();
  }

  @Test
  void missingCandidatesNullAndEmptyTextAreAbsent() {
    assertThat(format.read("{\"version\":1,\"english_label\":null}", LEAP_DAY)).isEmpty();
    assertThat(format.read("{\"version\":1,\"english_label\":null,\"candidates\":null}", LEAP_DAY))
        .isEmpty();
    assertThat(format.read(null, LEAP_DAY)).isEmpty();
    assertThat(format.read("", LEAP_DAY)).isEmpty();
  }

  @Test
  void readingLogsNothingOfTheContent(CapturedOutput output) {
    String text = written("Synthetic label", PLAIN, FULL);
    format(List.of(new BlockEntry("commons", FILE, null, null))).read(text, LEAP_DAY);
    format.read(text.replace("\"version\":1", "\"version\":2"), LEAP_DAY);
    format.read(text.replace("\"original_captured_at\":\"2000-02\"", "\"x\":1"), LEAP_DAY);

    assertThat(output.getAll())
        .doesNotContain(FILE, "Synthetic label", "2000-02-29", "AAAAAAAAAAA", "지어낸");
  }

  // ---- (5) no origin, no row ----

  @Test
  void anEventWithACandidateWithoutOriginIsNotWritten() {
    Candidate withoutOrigin =
        new Candidate(
            PLAIN.item(),
            FILE,
            null,
            null,
            null,
            EventLink.NONE,
            false,
            false,
            Basis.COMMONS_DATE_CATEGORY);

    assertThat(withoutOrigin.origin()).isNull();
    assertThat(format.write(null, List.of(FULL, withoutOrigin))).isEmpty();
    assertThat(format.write(null, List.of(withoutOrigin))).isEmpty();
    assertThat(format.write(null, null)).isEmpty();
    assertThat(format.write(null, List.of(FULL))).isPresent();
  }

  @Test
  void theTextStartsWithTheVersion() {
    assertThat(written(null)).isEqualTo("{\"version\":1,\"english_label\":null,\"candidates\":[]}");
    assertThat(Optional.of(SearchCacheFormat.VERSION)).contains(1);
  }
}
