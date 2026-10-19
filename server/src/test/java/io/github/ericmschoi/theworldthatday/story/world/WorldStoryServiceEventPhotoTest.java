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
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a3d T1 (4), (5), (11) and (14) on the service: which events are asked for a photo (shown
 * {@code normal} events with a title, in answer order, placed by scope), when the day photos are
 * off (a shown {@code tragic} event, or one unavailable Wikipedia page), where the photos land, and
 * the partial flag. Synthetic events on the leap day.
 */
class WorldStoryServiceEventPhotoTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Verdict DOMESTIC = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict WORLD = new Verdict(Tone.NORMAL, Basis.RULE_NO_SIGNAL, Scope.WORLD);
  private static final Verdict UNKNOWN =
      new Verdict(Tone.NORMAL, Basis.RULE_NO_SIGNAL, Scope.UNKNOWN);
  private static final Verdict TRAGIC_DEFAULT =
      new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN);
  private static final Verdict TRAGIC = new Verdict(Tone.TRAGIC, Basis.RULE_CLASS, Scope.WORLD);
  private static final Verdict HIDDEN =
      new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC, true);

  /** Records each call and answers {@code answer}; {@code dayMedia} is never the path taken. */
  private static final class Recording implements MediaLookup {
    final List<List<EventTarget>> targets = new ArrayList<>();
    final List<Boolean> tragicDays = new ArrayList<>();
    WorldMedia answer = new WorldMedia(Map.of(), List.of(), false);

    @Override
    public DayMedia dayMedia(LocalDate date, boolean tragicDay) {
      throw new AssertionError("the service calls lookup");
    }

    @Override
    public WorldMedia lookup(LocalDate date, List<EventTarget> asked, boolean tragicDay) {
      targets.add(asked);
      tragicDays.add(tragicDay);
      return answer;
    }
  }

  private final Recording media = new Recording();

  private static KoWikiEvent event(int i, List<String> links) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-0000000" + i,
        LEAP_DAY,
        "2000년",
        "사건",
        200L,
        "시험 사건 " + i + ".",
        links,
        null,
        READ);
  }

  private static KoWikiEvent event(int i) {
    return event(i, List.of("시험 문서 " + i, "시험 문서 기타"));
  }

  private WorldStoryResponse respond(List<KoWikiEvent> events, List<Verdict> verdicts) {
    return respond(events, verdicts, State.FRESH, State.FRESH);
  }

  private WorldStoryResponse respond(
      List<KoWikiEvent> events, List<Verdict> verdicts, State datePage, State yearPage) {
    KoWikiDayResult day =
        new KoWikiDayResult(
            LEAP_DAY,
            events,
            List.of(
                new KoWikiDocStatus("2월 29일", 100L, READ, datePage),
                new KoWikiDocStatus("2000년", 200L, READ, yearPage)),
            0,
            0,
            false);
    return new WorldStoryService(date -> day, found -> new Judgement(verdicts, false), media)
        .find(LEAP_DAY);
  }

  private static MediaItem photo(long id) {
    MediaRecord record =
        new MediaRecord(
            "commons-" + id,
            Type.PHOTO,
            DisplayMode.IMAGE,
            "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic.jpg/640px-Synthetic.jpg",
            640,
            480,
            null,
            null,
            null,
            null,
            null,
            null,
            "Wikimedia Commons",
            "https://commons.wikimedia.org/wiki/File:Synthetic.jpg",
            "CC0",
            LicenseStatus.FREE,
            null,
            "Synthetic Author · CC0 · Wikimedia Commons",
            null,
            "1998",
            MediaRecord.DatePrecision.YEAR,
            null,
            null,
            "country:KR",
            "2026-01-01T00:00:00Z");
    return new MediaItem(record, new DateRelation(DateRelation.Kind.YEARS_APART, -2));
  }

  // ---- T1 (4), (5): who is asked ----

  @Test
  void shownNormalEventsWithATitleAreAskedInAnswerOrderAndPlacedByScope() {
    respond(
        List.of(event(1), event(2), event(3), event(4, List.of())),
        List.of(DOMESTIC, WORLD, UNKNOWN, DOMESTIC));

    assertThat(media.targets)
        .containsExactly(
            List.of(
                new MediaLookup.EventTarget("kowiki-2000-02-29-00000001", "시험 문서 1", "country:KR"),
                new MediaLookup.EventTarget("kowiki-2000-02-29-00000002", "시험 문서 2", "world"),
                new MediaLookup.EventTarget("kowiki-2000-02-29-00000003", "시험 문서 3", "unknown")));
  }

  @Test
  void tragicAndHiddenEventsAreNeverAsked() {
    respond(
        List.of(event(1), event(2), event(3), event(4)),
        List.of(TRAGIC_DEFAULT, DOMESTIC, TRAGIC, HIDDEN));

    assertThat(media.targets.getFirst())
        .extracting(MediaLookup.EventTarget::title)
        .containsExactly("시험 문서 2");
    assertThat(media.tragicDays).containsExactly(true);
  }

  // ---- T1 (14): decision 2 (b) ----

  @ParameterizedTest(name = "date page {0}, year page {1}: day photos off = {2}")
  @CsvSource({
    "FRESH, FRESH, false",
    "UNAVAILABLE, FRESH, true",
    "FRESH, UNAVAILABLE, true",
    "STALE, FRESH, false",
    "FRESH, STALE, false",
    "ABSENT, FRESH, false",
    "CACHED, ABSENT, false"
  })
  void anUnavailablePageTurnsTheDayPhotosOffButNotTheEventPhotos(
      State datePage, State yearPage, boolean off) {
    respond(List.of(event(1)), List.of(DOMESTIC), datePage, yearPage);

    assertThat(media.tragicDays).containsExactly(off);
    assertThat(media.targets.getFirst())
        .extracting(MediaLookup.EventTarget::title)
        .containsExactly("시험 문서 1");
  }

  // ---- where the photos land, partial ----

  @Test
  void photosLandOnTheirEventsAndTragicEventsStayEmpty() {
    media.answer =
        new MediaLookup.WorldMedia(
            Map.of("kowiki-2000-02-29-00000002", List.of(photo(7))), List.of(photo(8)), false);

    WorldStoryResponse response =
        respond(List.of(event(1), event(2), event(3)), List.of(TRAGIC, DOMESTIC, DOMESTIC));

    assertThat(response.events())
        .extracting(Event::id)
        .containsExactly(
            "kowiki-2000-02-29-00000002",
            "kowiki-2000-02-29-00000003",
            "kowiki-2000-02-29-00000001");
    assertThat(response.events().get(0).media()).containsExactly(photo(7));
    assertThat(response.events().get(1).media()).isEmpty();
    assertThat(response.events().get(2).media()).isEmpty();
    assertThat(response.dayMedia()).containsExactly(photo(8));
    assertThat(response.partial()).isFalse();
  }

  @Test
  void failedMediaLookupKeepsTheEventsAndMarksThePartial() {
    media.answer = new MediaLookup.WorldMedia(Map.of(), List.of(), true);

    WorldStoryResponse response = respond(List.of(event(1)), List.of(DOMESTIC));

    assertThat(response.events()).hasSize(1);
    assertThat(response.partial()).isTrue();
  }

  // ---- T1 (11): the default lookup ----

  @Test
  void defaultLookupAsksTheDayPhotosOnlyAndLeavesEventMediaEmpty() {
    List<Boolean> asked = new ArrayList<>();
    MediaLookup dayOnly =
        (date, tragicDay) -> {
          asked.add(tragicDay);
          return new MediaLookup.DayMedia(List.of(photo(9)), true);
        };

    MediaLookup.WorldMedia world =
        dayOnly.lookup(
            LEAP_DAY, List.of(new MediaLookup.EventTarget("e", "시험 문서", "country:KR")), true);

    assertThat(asked).containsExactly(true);
    assertThat(world.eventMedia()).isEmpty();
    assertThat(world.dayMedia()).containsExactly(photo(9));
    assertThat(world.failed()).isTrue();
  }
}
