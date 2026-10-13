package io.github.ericmschoi.theworldthatday.event.tone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.tone.EventCuration.Entry;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a2 T3 (1): the curation file under {@code app.data-dir}, with the rules of {@code
 * data/schema/event-curation.schema.json}. Broken files are synthetic copies in a temp folder.
 */
@ExtendWith(OutputCaptureExtension.class)
class EventCurationTest {

  static final JsonMapper JSON = JsonMapper.builder().build();
  static final String VALID_ENTRY =
      """
      {"date": "2000-02-29", "primary_link": "시험 문서 가", "summary_contains": null,
       "tone": "normal", "scope": "world", "hide": false, "reason": "시험 항목",
       "evidence_url": "https://ko.wikipedia.org/w/index.php?title=2000%EB%85%84&oldid=200",
       "reviewed_at": "2026-10-13"}""";

  @TempDir Path dataDir;

  /** Writes {@code curation/event-tone.json} with {@code entries} and returns its data folder. */
  static Path write(Path dataDir, String entries) {
    try {
      Path file = dataDir.resolve(EventCuration.FILE);
      Files.createDirectories(file.getParent());
      Files.writeString(file, "{\"version\": 1, \"entries\": [" + entries + "]}");
      return dataDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static EventCuration load(Path dataDir) {
    return new EventCuration(new AppProperties(dataDir.toString()), JSON);
  }

  private static KoWikiEvent event(LocalDate date, String text, String... links) {
    return new KoWikiEvent(
        "kowiki-" + date + "-00000001",
        date,
        "2000년",
        "사건",
        200L,
        text,
        List.of(links),
        null,
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void defaultDataDirHoldsTheFourDemoEntriesWithoutTakedowns() {
    EventCuration curation = load(Path.of("../data"));

    assertThat(curation.entries()).hasSize(4);
    assertThat(curation.entries()).noneMatch(Entry::hide);
    assertThat(curation.entries())
        .allSatisfy(
            entry -> {
              assertThat(entry.tone()).contains(Tone.NORMAL);
              assertThat(entry.scope()).contains(Scope.DOMESTIC);
            });
    assertThat(curation.entries())
        .extracting(entry -> entry.primaryLink().orElseThrow())
        .containsExactly("1988년 하계 올림픽", "KBS 1TV", "KBS 2TV", "2002년 FIFA 월드컵");
  }

  @Test
  void missingFileWarnsAndCuratesNothing(CapturedOutput output) {
    EventCuration curation = load(dataDir.resolve("nowhere"));

    assertThat(curation.entries()).isEmpty();
    assertThat(output.getAll()).contains("WARN").contains("Event curation file not found");
  }

  @Test
  void matchesByPrimaryLinkIgnoringUnderscoresOrBySummaryText() {
    LocalDate day = LocalDate.of(2000, 2, 29);
    EventCuration curation =
        load(
            write(
                dataDir,
                VALID_ENTRY
                    + ","
                    + VALID_ENTRY
                        .replace("\"primary_link\": \"시험 문서 가\"", "\"primary_link\": null")
                        .replace("\"summary_contains\": null", "\"summary_contains\": \"시험 문구\"")
                        .replace("\"tone\": \"normal\"", "\"tone\": \"tragic\"")));

    assertThat(curation.match(event(day, "아무 문장.", "시험_문서_가", "다른 문서")))
        .map(Entry::tone)
        .contains(Optional.of(Tone.NORMAL));
    assertThat(curation.match(event(day, "시험 문구가 든 문장.", "다른 문서")))
        .map(Entry::tone)
        .contains(Optional.of(Tone.TRAGIC));
    assertThat(curation.match(event(day, "아무 문장.", "다른 문서", "시험 문서 가")))
        .as("only the main (first) link counts")
        .isEmpty();
    assertThat(curation.match(event(day.plusDays(1), "아무 문장.", "시험 문서 가")))
        .as("another date")
        .isEmpty();
    assertThat(curation.match(event(day, "아무 문장."))).as("no links, no text match").isEmpty();
  }

  @Test
  void takedownEntryNeedsNoTone() {
    EventCuration curation =
        load(
            write(
                dataDir,
                VALID_ENTRY
                    .replace("\"tone\": \"normal\", ", "")
                    .replace("\"hide\": false", "\"hide\": true")));

    assertThat(curation.entries().getFirst().hide()).isTrue();
    assertThat(curation.entries().getFirst().tone()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"date\": \"2000-02-29\", ->", // date missing
        "\"reason\": \"시험 항목\",->", // reason missing
        "\"reason\": \"시험 항목\"->\"reason\": \"  \"", // blank reason
        "\"evidence_url\": \"https://ko.wikipedia.org/w/index.php?title=2000%EB%85%84&oldid=200\",->",
        "https://ko->http://ko", // evidence not https
        "\"reviewed_at\": \"2026-10-13\"->\"reviewed_at\": \"2026-10-13\", \"note\": \"x\"",
        "\"tone\": \"normal\", ->", // no tone and not hidden
        "\"tone\": \"normal\"->\"tone\": \"sad\"",
        "\"tone\": \"normal\"->\"tone\": null",
        "\"scope\": \"world\"->\"scope\": \"global\"",
        "\"hide\": false->\"hide\": \"no\"",
        "\"date\": \"2000-02-29\"->\"date\": \"2001-02-29\"", // not a date
        "\"date\": \"2000-02-29\"->\"date\": \"2000-2-29\"",
        "\"primary_link\": \"시험 문서 가\"->\"primary_link\": null", // nothing to match by
        "\"primary_link\": \"시험 문서 가\"->\"primary_link\": \" \"",
      })
  void brokenEntryIsRejected(String edit) {
    String[] change = edit.split("->", -1);
    assertThat(VALID_ENTRY).contains(change[0]);
    write(dataDir, VALID_ENTRY.replace(change[0], change[1]));

    assertThatThrownBy(() -> load(dataDir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Event curation file is invalid");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"version\": 2, \"entries\": []}",
        "{\"entries\": []}",
        "{\"version\": 1}",
        "{\"version\": 1, \"entries\": [], \"extra\": true}",
        "[]",
        "{\"version\": 1, \"entries\": [1]}",
        "not json"
      })
  void brokenFileIsRejected(String content) throws IOException {
    Path file = dataDir.resolve(EventCuration.FILE);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);

    assertThatThrownBy(() -> load(dataDir)).isInstanceOf(IllegalStateException.class);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AppProperties.class)
  @Import(EventCuration.class)
  static class CurationOnly {}

  @Test
  void brokenFileStopsTheContextAndAMissingOneDoesNot() {
    ApplicationContextRunner runner =
        new ApplicationContextRunner()
            .withBean(JsonMapper.class, () -> JSON)
            .withUserConfiguration(CurationOnly.class);
    write(dataDir, VALID_ENTRY.replace("\"reason\": \"시험 항목\",", ""));

    runner
        .withPropertyValues("app.data-dir=" + dataDir)
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .rootCause()
                  .hasMessageContaining("needs reason");
            });
    runner
        .withPropertyValues("app.data-dir=" + dataDir.resolve("nowhere"))
        .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EventCuration.class));
  }
}
