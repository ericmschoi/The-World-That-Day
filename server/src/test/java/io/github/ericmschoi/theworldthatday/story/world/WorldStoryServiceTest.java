package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.DefaultEventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Link;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mapping of {@link KoWikiDayResult} to the response, without Spring (plan M3a2 T2). */
class WorldStoryServiceTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00.123456789Z");
  private static final Verdict NORMAL = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict TRAGIC = new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN);

  private static KoWikiEvent event(String suffix, String page, String section, String... links) {
    return new KoWikiEvent(
        "enwiki-2000-02-29-0000000" + suffix,
        LEAP_DAY,
        page,
        section,
        page.equals("February 29") ? 100L : 200L,
        "Test event " + suffix + ".",
        List.of(links),
        links.length == 0 ? null : "Q" + suffix,
        READ);
  }

  private static KoWikiDayResult day(List<KoWikiEvent> events) {
    return new KoWikiDayResult(
        LEAP_DAY,
        events,
        List.of(
            new KoWikiDocStatus("February 29", 100L, READ, State.FRESH),
            new KoWikiDocStatus("2000", 200L, READ, State.CACHED)),
        0,
        0,
        false);
  }

  private static WorldStoryResponse respond(List<KoWikiEvent> events, EventToneJudge judge) {
    return new WorldStoryService(date -> day(events), judge, (date, tragicDay) -> DayMedia.NONE)
        .find(LEAP_DAY);
  }

  @Test
  void eventFieldsComeFromTheSourceEventUnchanged() {
    KoWikiEvent source = event("1", "February 29", "Events", "Test page A", "Test (page) B/C");

    Event event = respond(List.of(source), new DefaultEventToneJudge()).events().getFirst();

    assertThat(event.id()).isEqualTo(source.id());
    assertThat(event.date()).isEqualTo("2000-02-29");
    assertThat(event.title()).isEqualTo("Test page A");
    assertThat(event.summary()).isEqualTo("Test event 1.");
    assertThat(event.summaryLang()).isEqualTo("en");
    assertThat(event.summaryModified()).isFalse();
    assertThat(event.textLicense()).isEqualTo("CC BY-SA 4.0");
    assertThat(event.textLicenseUrl())
        .isEqualTo("https://creativecommons.org/licenses/by-sa/4.0/deed.en");
    assertThat(event.sourceName()).isEqualTo("English Wikipedia");
    assertThat(event.sourceTitle()).isEqualTo("February 29");
    assertThat(event.sourceUrl()).isEqualTo("https://en.wikipedia.org/wiki/February_29");
    assertThat(event.sourceSection()).isEqualTo("Events");
    assertThat(event.dateKind()).isEqualTo("day");
    assertThat(event.rangeEnd()).isNull();
    assertThat(event.sourceRevision()).isEqualTo(100L);
    assertThat(event.wikidataQid()).isEqualTo("Q1");
    assertThat(event.relatedLinks())
        .containsExactly(
            new Link("Test page A", "https://en.wikipedia.org/wiki/Test_page_A"),
            new Link("Test (page) B/C", "https://en.wikipedia.org/wiki/Test_%28page%29_B%2FC"));
    assertThat(event.media()).isEmpty();
    assertThat(event.retrievedAt()).as("seconds, UTC").isEqualTo("2026-01-01T00:00:00Z");
  }

  @Test
  void eventWithoutLinksHasNoTitleNoQidAndNoRelatedLinks() {
    Event event =
        respond(List.of(event("2", "2000", "Events")), new DefaultEventToneJudge())
            .events()
            .getFirst();

    assertThat(event.title()).isNull();
    assertThat(event.wikidataQid()).isNull();
    assertThat(event.relatedLinks()).isEmpty();
    assertThat(event.sourceUrl()).isEqualTo("https://en.wikipedia.org/wiki/2000");
  }

  @Test
  void placeholderJudgeGivesEveryEventTheConservativeVerdict() {
    WorldStoryResponse response =
        respond(
            List.of(event("1", "February 29", "Events", "A"), event("2", "2000", "Events")),
            new DefaultEventToneJudge());

    assertThat(response.events())
        .allSatisfy(
            event -> {
              assertThat(event.tone()).isEqualTo("tragic");
              assertThat(event.toneBasis()).isEqualTo("default");
              assertThat(event.scope()).isEqualTo("unknown");
            });
    assertThat(response.partial()).isFalse();
  }

  @Test
  void normalEventsComeFirstAndEachToneKeepsTheSourceOrder() {
    List<KoWikiEvent> events =
        List.of(
            event("1", "February 29", "Events", "A"),
            event("2", "February 29", "Events", "B"),
            event("3", "2000", "Events", "C"),
            event("4", "2000", "Events", "D"));
    EventToneJudge judge = found -> new Judgement(List.of(TRAGIC, NORMAL, TRAGIC, NORMAL), false);

    WorldStoryResponse response = respond(events, judge);

    assertThat(response.events())
        .extracting(Event::summary)
        .containsExactly("Test event 2.", "Test event 4.", "Test event 1.", "Test event 3.");
    assertThat(response.events())
        .extracting(Event::tone, Event::toneBasis, Event::scope)
        .first()
        .isEqualTo(org.assertj.core.groups.Tuple.tuple("normal", "curated", "domestic"));
  }

  @Test
  void aFailedSupportingSourceOfTheJudgeMakesThePagePartial() {
    EventToneJudge failing = found -> new Judgement(List.of(TRAGIC), true);

    WorldStoryResponse response =
        respond(List.of(event("1", "February 29", "Events", "A")), failing);

    assertThat(response.partial()).isTrue();
    assertThat(response.notice()).isNull();
  }

  @Test
  void aJudgeThatLosesAnEventIsAProgrammingError() {
    EventToneJudge broken = found -> new Judgement(List.of(), false);

    assertThatThrownBy(() -> respond(List.of(event("1", "February 29", "Events", "A")), broken))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void pageUrlEncodesSpacesAsUnderscoresAndEverythingElseAsUtf8() {
    assertThat(WorldStoryResponse.pageUrl("February 13"))
        .isEqualTo("https://en.wikipedia.org/wiki/February_13");
    assertThat(WorldStoryResponse.pageUrl("1980 Liberian coup d'état"))
        .isEqualTo("https://en.wikipedia.org/wiki/1980_Liberian_coup_d%27%C3%A9tat");
    assertThat(WorldStoryResponse.pageUrl("A&B?C#D"))
        .isEqualTo("https://en.wikipedia.org/wiki/A%26B%3FC%23D");
  }
}
