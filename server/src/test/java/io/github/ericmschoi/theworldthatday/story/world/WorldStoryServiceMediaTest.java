package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a3b T2 (5), (6) and (11) on the service: when day photos are looked up, how a failed
 * lookup marks the answer partial, and that event media stays empty. Synthetic events.
 */
class WorldStoryServiceMediaTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Verdict NORMAL = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict TRAGIC_DEFAULT =
      new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN);
  private static final Verdict TRAGIC_HIDDEN =
      new Verdict(Tone.TRAGIC, Basis.CURATED, Scope.DOMESTIC, true);

  /** Records each call and answers {@code answer}. */
  private static final class Recording implements MediaLookup {
    final List<Boolean> tragicDays = new ArrayList<>();
    DayMedia answer = DayMedia.NONE;

    @Override
    public DayMedia dayMedia(LocalDate date, boolean tragicDay) {
      tragicDays.add(tragicDay);
      return answer;
    }
  }

  private final Recording media = new Recording();

  private static KoWikiEvent event(String hash) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-" + hash,
        LEAP_DAY,
        "2000년",
        "사건",
        200L,
        "시험 사건 " + hash + ".",
        List.of("시험 문서"),
        null,
        READ);
  }

  private WorldStoryResponse respond(List<Verdict> verdicts) {
    List<KoWikiEvent> events = new ArrayList<>();
    for (int i = 0; i < verdicts.size(); i++) {
      events.add(event("0000000" + i));
    }
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
    return new WorldStoryService(date -> day, found -> new Judgement(verdicts, false), media)
        .find(LEAP_DAY);
  }

  @Test
  void shownTragicEventMakesATragicDay() {
    respond(List.of(NORMAL, TRAGIC_DEFAULT));

    assertThat(media.tragicDays).containsExactly(true);
  }

  @Test
  void hiddenTragicEventDoesNotCount() {
    respond(List.of(NORMAL, TRAGIC_HIDDEN));
    respond(List.of(TRAGIC_HIDDEN));

    assertThat(media.tragicDays).containsExactly(false, false);
  }

  @Test
  void dayWithoutEventsStillLooksForPhotos() {
    WorldStoryResponse response = respond(List.of());

    assertThat(media.tragicDays).containsExactly(false);
    assertThat(response.notice()).isEqualTo(WorldStoryResponse.Notice.NO_EVENTS_FOUND);
  }

  @Test
  void failedLookupKeepsTheEventsAndMarksThePartial() {
    media.answer = DayMedia.FAILED;

    WorldStoryResponse response = respond(List.of(NORMAL, NORMAL));

    assertThat(response.events()).hasSize(2);
    assertThat(response.dayMedia()).isEmpty();
    assertThat(response.partial()).isTrue();
    assertThat(response.events()).allSatisfy(event -> assertThat(event.media()).isEmpty());
  }

  @Test
  void emptyLookupIsNotPartial() {
    WorldStoryResponse response = respond(List.of(NORMAL));

    assertThat(response.partial()).isFalse();
    assertThat(response.events()).extracting(Event::media).containsOnly(List.of());
  }
}
