package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4 T6 (10), checked field by field: {@link EventTarget#equals} compares the event ID,
 * title and place only, so a test that compares targets cannot see a wrong {@code summary} or
 * {@code wikidataQid}. This one reads every field of every target the service hands to the media
 * lookup. Synthetic events on the leap day 2000-02-29.
 */
class WorldStoryEventTargetInputsTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Verdict DOMESTIC = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict WORLD = new Verdict(Tone.NORMAL, Basis.RULE_NO_SIGNAL, Scope.WORLD);

  private static KoWikiEvent event(String id, String text, String link, String qid) {
    return new KoWikiEvent(
        "enwiki-2000-02-29-" + id,
        LEAP_DAY,
        "2000",
        "Events",
        200L,
        text,
        link == null ? List.of() : List.of(link, "Test page other"),
        qid,
        READ);
  }

  @Test
  void everyTargetCarriesItsEventsTextAndWikidataIdNotJustItsIdTitleAndPlace() {
    List<List<EventTarget>> asked = new ArrayList<>();
    MediaLookup lookup =
        new MediaLookup() {
          @Override
          public DayMedia dayMedia(LocalDate date, boolean tragicDay) {
            throw new AssertionError("the service calls lookup");
          }

          @Override
          public WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay) {
            asked.add(targets);
            return new WorldMedia(Map.of(), List.of(), false);
          }
        };
    KoWikiDayResult day =
        new KoWikiDayResult(
            LEAP_DAY,
            List.of(
                event("00000001", "Test event A opens.", "Test page A", "Q999999901"),
                event("00000002", "Test event B opens.", "Test page B", null),
                event("00000003", "Test event C opens.", null, "Q999999903")),
            List.of(
                new KoWikiDocStatus("February 29", 100L, READ, State.FRESH),
                new KoWikiDocStatus("2000", 200L, READ, State.FRESH)),
            0,
            0,
            false);

    new WorldStoryService(
            date -> day, found -> new Judgement(List.of(DOMESTIC, WORLD, DOMESTIC), false), lookup)
        .find(LEAP_DAY);

    assertThat(asked).hasSize(1);
    assertThat(asked.getFirst())
        .extracting(
            EventTarget::eventId,
            EventTarget::title,
            EventTarget::placeRef,
            EventTarget::summary,
            EventTarget::wikidataQid)
        .containsExactly(
            tuple(
                "enwiki-2000-02-29-00000001",
                "Test page A",
                "country:CA",
                "Test event A opens.",
                "Q999999901"),
            tuple(
                "enwiki-2000-02-29-00000002", "Test page B", "world", "Test event B opens.", null));
  }

  @Test
  void targetsWithOtherTextOrWikidataIdAreEqualButTheirFieldsDiffer() {
    EventTarget a = new EventTarget("e", "Test page", "country:CA", "Sentence A", "Q999999901");
    EventTarget b = new EventTarget("e", "Test page", "country:CA", "Sentence B", "Q999999902");

    // The documented behaviour: equality ignores the two search inputs ...
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(new EventTarget("e", "Test page", "country:CA"));
    // ... so every check that has to see them reads the fields, as the test above does.
    assertThat(a.summary()).isNotEqualTo(b.summary());
    assertThat(a.wikidataQid()).isNotEqualTo(b.wikidataQid());
    // The three fields that do count still tell targets apart.
    assertThat(a)
        .isNotEqualTo(new EventTarget("f", "Test page", "country:CA", "Sentence A", "Q999999901"));
    assertThat(a)
        .isNotEqualTo(new EventTarget("e", "Other page", "country:CA", "Sentence A", "Q999999901"));
    assertThat(a)
        .isNotEqualTo(new EventTarget("e", "Test page", "world", "Sentence A", "Q999999901"));
  }
}
