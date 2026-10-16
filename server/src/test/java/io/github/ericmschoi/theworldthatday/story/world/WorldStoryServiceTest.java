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
        "kowiki-2000-02-29-0000000" + suffix,
        LEAP_DAY,
        page,
        section,
        page.equals("2월 29일") ? 100L : 200L,
        "시험 사건 " + suffix + ".",
        List.of(links),
        links.length == 0 ? null : "Q" + suffix,
        READ);
  }

  private static KoWikiDayResult day(List<KoWikiEvent> events) {
    return new KoWikiDayResult(
        LEAP_DAY,
        events,
        List.of(
            new KoWikiDocStatus("2월 29일", 100L, READ, State.FRESH),
            new KoWikiDocStatus("2000년", 200L, READ, State.CACHED)),
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
    KoWikiEvent source = event("1", "2월 29일", "사건", "시험 문서 가", "시험 (문서) 나/다");

    Event event = respond(List.of(source), new DefaultEventToneJudge()).events().getFirst();

    assertThat(event.id()).isEqualTo(source.id());
    assertThat(event.date()).isEqualTo("2000-02-29");
    assertThat(event.title()).isEqualTo("시험 문서 가");
    assertThat(event.summary()).isEqualTo("시험 사건 1.");
    assertThat(event.summaryLang()).isEqualTo("ko");
    assertThat(event.summaryModified()).isFalse();
    assertThat(event.textLicense()).isEqualTo("CC BY-SA 4.0");
    assertThat(event.textLicenseUrl())
        .isEqualTo("https://creativecommons.org/licenses/by-sa/4.0/deed.ko");
    assertThat(event.sourceName()).isEqualTo("한국어 위키백과");
    assertThat(event.sourceTitle()).isEqualTo("2월 29일");
    assertThat(event.sourceUrl()).isEqualTo("https://ko.wikipedia.org/wiki/2%EC%9B%94_29%EC%9D%BC");
    assertThat(event.sourceSection()).isEqualTo("사건");
    assertThat(event.sourceRevision()).isEqualTo(100L);
    assertThat(event.wikidataQid()).isEqualTo("Q1");
    assertThat(event.relatedLinks())
        .containsExactly(
            new Link(
                "시험 문서 가",
                "https://ko.wikipedia.org/wiki/%EC%8B%9C%ED%97%98_%EB%AC%B8%EC%84%9C_%EA%B0%80"),
            new Link(
                "시험 (문서) 나/다",
                "https://ko.wikipedia.org/wiki/%EC%8B%9C%ED%97%98_%28%EB%AC%B8%EC%84%9C%29_%EB%82%98%2F%EB%8B%A4"));
    assertThat(event.media()).isEmpty();
    assertThat(event.retrievedAt()).as("seconds, UTC").isEqualTo("2026-01-01T00:00:00Z");
  }

  @Test
  void eventWithoutLinksHasNoTitleNoQidAndNoRelatedLinks() {
    Event event =
        respond(List.of(event("2", "2000년", "문화")), new DefaultEventToneJudge())
            .events()
            .getFirst();

    assertThat(event.title()).isNull();
    assertThat(event.wikidataQid()).isNull();
    assertThat(event.relatedLinks()).isEmpty();
    assertThat(event.sourceUrl()).isEqualTo("https://ko.wikipedia.org/wiki/2000%EB%85%84");
  }

  @Test
  void placeholderJudgeGivesEveryEventTheConservativeVerdict() {
    WorldStoryResponse response =
        respond(
            List.of(event("1", "2월 29일", "사건", "가"), event("2", "2000년", "문화")),
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
            event("1", "2월 29일", "사건", "가"),
            event("2", "2월 29일", "문화", "나"),
            event("3", "2000년", "사건", "다"),
            event("4", "2000년", "문화", "라"));
    EventToneJudge judge = found -> new Judgement(List.of(TRAGIC, NORMAL, TRAGIC, NORMAL), false);

    WorldStoryResponse response = respond(events, judge);

    assertThat(response.events())
        .extracting(Event::summary)
        .containsExactly("시험 사건 2.", "시험 사건 4.", "시험 사건 1.", "시험 사건 3.");
    assertThat(response.events())
        .extracting(Event::tone, Event::toneBasis, Event::scope)
        .first()
        .isEqualTo(org.assertj.core.groups.Tuple.tuple("normal", "curated", "domestic"));
  }

  @Test
  void aFailedSupportingSourceOfTheJudgeMakesThePagePartial() {
    EventToneJudge failing = found -> new Judgement(List.of(TRAGIC), true);

    WorldStoryResponse response = respond(List.of(event("1", "2월 29일", "사건", "가")), failing);

    assertThat(response.partial()).isTrue();
    assertThat(response.notice()).isNull();
  }

  @Test
  void aJudgeThatLosesAnEventIsAProgrammingError() {
    EventToneJudge broken = found -> new Judgement(List.of(), false);

    assertThatThrownBy(() -> respond(List.of(event("1", "2월 29일", "사건", "가")), broken))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void pageUrlEncodesSpacesAsUnderscoresAndEverythingElseAsUtf8() {
    assertThat(WorldStoryResponse.pageUrl("9월 17일"))
        .isEqualTo("https://ko.wikipedia.org/wiki/9%EC%9B%94_17%EC%9D%BC");
    assertThat(WorldStoryResponse.pageUrl("KBS 1TV"))
        .isEqualTo("https://ko.wikipedia.org/wiki/KBS_1TV");
    assertThat(WorldStoryResponse.pageUrl("A&B?C#D"))
        .isEqualTo("https://ko.wikipedia.org/wiki/A%26B%3FC%23D");
  }
}
