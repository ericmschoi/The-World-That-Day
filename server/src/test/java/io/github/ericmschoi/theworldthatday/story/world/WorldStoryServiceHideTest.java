package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a2 T3 (4): curated takedowns leave the response, do not change {@code partial}, and are
 * logged at DEBUG with the ID hash only (synthetic events).
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryServiceHideTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Verdict SHOWN = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict HIDDEN =
      new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC, true);

  private static KoWikiEvent event(String hash, String text) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-" + hash,
        LEAP_DAY,
        "2000년",
        "문화",
        200L,
        text,
        List.of("시험 문서"),
        null,
        READ);
  }

  private static WorldStoryResponse respond(
      List<KoWikiEvent> events, State yearPage, List<Verdict> verdicts) {
    KoWikiDayResult day =
        new KoWikiDayResult(
            LEAP_DAY,
            events,
            List.of(
                new KoWikiDocStatus("2월 29일", null, null, State.ABSENT),
                new KoWikiDocStatus("2000년", 200L, READ, yearPage)),
            0,
            0,
            false);
    EventToneJudge judge = found -> new Judgement(verdicts, false);
    return new WorldStoryService(date -> day, judge, (date, tragicDay) -> DayMedia.NONE)
        .find(LEAP_DAY);
  }

  @Test
  void hiddenEventLeavesTheResponseAndOnlyItsHashIsLogged(CapturedOutput output) {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    WorldStoryResponse response;
    try {
      response =
          respond(
              List.of(event("0000000a", "시험 사건 가."), event("0000000b", "시험 사건 나.")),
              State.FRESH,
              List.of(HIDDEN, SHOWN));
    } finally {
      app.setLevel(before);
    }

    assertThat(response.events()).extracting(Event::summary).containsExactly("시험 사건 나.");
    assertThat(response.notice()).isNull();
    assertThat(response.partial()).isFalse();
    assertThat(output.getAll())
        .contains("count=1", "0000000a")
        .doesNotContain("0000000b")
        .doesNotContain("kowiki-")
        .doesNotContain("2000-02-29")
        .doesNotContain("시험 사건");
  }

  @Test
  void everyEventHiddenIsNoEventsFoundAndPartialStaysAsTheSourceSaid() {
    List<KoWikiEvent> events = List.of(event("0000000a", "시험 사건 가."));

    WorldStoryResponse fresh = respond(events, State.FRESH, List.of(HIDDEN));
    WorldStoryResponse stale = respond(events, State.STALE, List.of(HIDDEN));

    assertThat(fresh.events()).isEmpty();
    assertThat(fresh.notice()).isEqualTo(WorldStoryResponse.Notice.NO_EVENTS_FOUND);
    assertThat(fresh.partial()).as("a takedown is not a failure").isFalse();
    assertThat(stale.events()).isEmpty();
    assertThat(stale.partial()).isTrue();
  }
}
