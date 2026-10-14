package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.CuratedEventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventCuration;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource.Signals;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a2 T3 (4) end to end without mocks of the judge: a curation file with a takedown entry
 * (synthetic, neutral lines) read by the real {@link EventCuration}, judged by the real {@link
 * CuratedEventToneJudge} and answered by {@link WorldStoryService}. The hidden event leaves the
 * response, is not asked of Wikidata, does not make the answer partial, and only its ID hash is
 * logged.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryCuratedHideTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  @TempDir Path dataDir;
  private final List<Collection<String>> asked = new ArrayList<>();

  private static final String ENTRY =
      """
      {"date": "2000-02-29", "primary_link": "%s", "summary_contains": null, %s
       "reason": "시험 항목", "reviewed_at": "2026-10-13",
       "evidence_url": "https://ko.wikipedia.org/w/index.php?title=2000%%EB%%85%%84&oldid=200"}""";

  private void curation(String... entries) throws IOException {
    Path file = dataDir.resolve("curation/event-tone.json");
    Files.createDirectories(file.getParent());
    Files.writeString(
        file,
        "{\"version\": 1, \"entries\": [" + String.join(",", entries) + "]}",
        StandardCharsets.UTF_8);
  }

  private static KoWikiEvent event(String hash, String text, String link, String qid) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-" + hash, LEAP_DAY, "2000년", "사건", 200L, text, List.of(link), qid, READ);
  }

  private WorldStoryResponse respond(List<KoWikiEvent> events) {
    EventCuration curation =
        new EventCuration(new AppProperties(dataDir.toString()), JsonMapper.builder().build());
    CuratedEventToneJudge judge =
        new CuratedEventToneJudge(
            curation,
            qids -> {
              asked.add(List.copyOf(qids));
              // Like the SPARQL source: every queried (well-formed) ID is in the answer.
              Map<String, Signals> answer = new LinkedHashMap<>();
              qids.stream()
                  .filter(qid -> WikidataProperties.QID.matcher(qid).matches())
                  .forEach(qid -> answer.put(qid, Signals.NONE));
              return Optional.of(answer);
            });
    KoWikiDayResult day =
        new KoWikiDayResult(
            LEAP_DAY,
            events,
            List.of(
                new KoWikiDocStatus("2월 29일", 100L, READ, State.FRESH),
                new KoWikiDocStatus("2000년", 200L, READ, State.FRESH)),
            0,
            0,
            false);
    return new WorldStoryService(date -> day, judge).find(LEAP_DAY);
  }

  @Test
  void takedownEntryWithoutToneRemovesOnlyItsEventAndLogsOnlyTheHash(CapturedOutput output)
      throws IOException {
    curation(
        ENTRY.formatted("시험 문서 가", "\"hide\": true,"),
        ENTRY.formatted("시험 문서 나", "\"tone\": \"normal\", \"scope\": \"world\","));
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    WorldStoryResponse response;
    try {
      response =
          respond(
              List.of(
                  event("0000000a", "시험 사건 가.", "시험 문서 가", "Q1"),
                  event("0000000b", "시험 사건 나.", "시험 문서 나", "Q2"),
                  event("0000000c", "시험 사건 다.", "시험 문서 다", "Q3")));
    } finally {
      app.setLevel(before);
    }

    assertThat(response.events())
        .extracting(Event::summary)
        .containsExactly("시험 사건 나.", "시험 사건 다.");
    assertThat(response.events().getFirst().toneBasis()).isEqualTo("curated");
    assertThat(response.events().getFirst().scope()).isEqualTo("world");
    assertThat(response.notice()).isNull();
    assertThat(response.partial()).as("a takedown is not a failure").isFalse();
    assertThat(asked)
        .as("the hidden event's QID is not asked")
        .containsExactly(List.of("Q2", "Q3"));
    assertThat(output.getAll())
        .contains("World events hidden by curation: count=1", "0000000a")
        .doesNotContain("0000000b")
        .doesNotContain("kowiki-")
        .doesNotContain("2000-02-29")
        .doesNotContain("시험 사건");
  }

  @Test
  void takedownOfEveryEventIsNoEventsFoundAndNotPartial() throws IOException {
    curation(ENTRY.formatted("시험 문서 가", "\"hide\": true,"));

    WorldStoryResponse response = respond(List.of(event("0000000a", "시험 사건 가.", "시험 문서 가", "Q1")));

    assertThat(response.events()).isEmpty();
    assertThat(response.notice()).isEqualTo(WorldStoryResponse.Notice.NO_EVENTS_FOUND);
    assertThat(response.partial()).isFalse();
    assertThat(asked).containsExactly(List.of());
  }
}
