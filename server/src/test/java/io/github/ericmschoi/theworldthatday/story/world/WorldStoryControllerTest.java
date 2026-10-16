package io.github.ericmschoi.theworldthatday.story.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventSource;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.DefaultEventToneJudge;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Plan M3a2 T2 (2), (3), (6), (7): date validation and error format, and how {@link
 * KoWikiDayResult} states become 503, {@code partial} and {@code notice}. The event source is a
 * stub that returns a hand-built result; "today" is 2026-10-10 in Asia/Seoul.
 */
@WebMvcTest(WorldStoryController.class)
@Import({
  WorldStoryService.class,
  DefaultEventToneJudge.class,
  WorldStoryControllerTest.Stubs.class
})
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryControllerTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

  @Autowired private MockMvc mvc;
  @Autowired private StubSource source;

  @TestConfiguration(proxyBeanMethods = false)
  static class Stubs {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ClockConfig.SERVICE_ZONE);
    }

    @Bean
    StubSource stubSource() {
      return new StubSource();
    }

    @Bean
    MediaLookup noDayMedia() {
      return (date, tragicDay) -> DayMedia.NONE;
    }
  }

  /** Returns {@link #next} for the requested date and records the dates it was asked for. */
  static final class StubSource implements KoWikiEventSource {
    final AtomicReference<KoWikiDayResult> next = new AtomicReference<>();
    final List<LocalDate> asked = new ArrayList<>();

    @Override
    public KoWikiDayResult find(LocalDate date) {
      asked.add(date);
      KoWikiDayResult result = next.get();
      return new KoWikiDayResult(
          date,
          result.events(),
          result.docs(),
          result.skippedLines(),
          result.mergedCount(),
          result.linkResolutionFailed());
    }
  }

  @BeforeEach
  void reset() {
    source.asked.clear();
    source.next.set(day(List.of(), State.ABSENT, State.ABSENT, false));
  }

  private static KoWikiDocStatus doc(String title, State state) {
    boolean content = state != State.ABSENT && state != State.UNAVAILABLE;
    return new KoWikiDocStatus(title, content ? 100L : null, content ? READ : null, state);
  }

  private static KoWikiDayResult day(
      List<KoWikiEvent> events, State datePage, State yearPage, boolean linksFailed) {
    return new KoWikiDayResult(
        LEAP_DAY,
        events,
        List.of(doc("2월 29일", datePage), doc("2000년", yearPage)),
        0,
        0,
        linksFailed);
  }

  private static KoWikiEvent event(String page, String section, String text, String link) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-" + String.format("%08x", text.hashCode()),
        LEAP_DAY,
        page,
        section,
        page.equals("2월 29일") ? 100L : 200L,
        text,
        link == null ? List.of() : List.of(link),
        link == null ? null : "Q1",
        READ);
  }

  private ResultActions world(String query) throws Exception {
    return mvc.perform(get("/api/story/world" + query));
  }

  private void expectProblem(String query, int status, String code, String... notEchoed)
      throws Exception {
    MvcResult result =
        world(query)
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith(PROBLEM))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.status").value(status))
            .andExpect(jsonPath("$.instance").value("/api/story/world"))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").isString())
            .andExpect(jsonPath("$.detail").isString())
            .andReturn();
    String body = result.getResponse().getContentAsString();
    for (String input : notEchoed) {
      assertThat(body).as("the body never echoes the input").doesNotContain(input);
    }
  }

  @Test
  void missingDateIsRequired() throws Exception {
    expectProblem("", 400, "DATE_REQUIRED");
    expectProblem("?date=", 400, "DATE_REQUIRED");
    assertThat(source.asked).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "1988-9-17, DATE_FORMAT",
    "2001-02-29, DATE_NOT_EXIST",
    "1988-02-30, DATE_NOT_EXIST",
    "2026-10-11, DATE_IN_FUTURE"
  })
  void invalidDatesAreProblemsThatDoNotEchoTheInput(String date, String code, CapturedOutput output)
      throws Exception {
    expectProblem("?date=" + date, 400, code, date, date.replace("-", ""));

    assertThat(source.asked).as("no source call for a bad date").isEmpty();
    assertThat(output.getAll()).as("400s are not logged").doesNotContain(date).doesNotContain(code);
  }

  @Test
  void todayIsAllowed() throws Exception {
    world("?date=2026-10-10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-10-10"));
    assertThat(source.asked).containsExactly(LocalDate.of(2026, 10, 10));
  }

  @Test
  void leapDayIsAValidDate() throws Exception {
    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2000-02-29"));
  }

  @Test
  void bothPagesUnavailableIs503WithOneWarnLineWithoutTheDate(CapturedOutput output)
      throws Exception {
    source.next.set(day(List.of(), State.UNAVAILABLE, State.UNAVAILABLE, false));

    expectProblem("?date=2000-02-29", 503, "UPSTREAM_UNAVAILABLE", "2000-02-29");

    String logs = output.getAll();
    assertThat(logs.lines().filter(line -> line.contains(" WARN ")))
        .singleElement()
        .satisfies(line -> assertThat(line).contains("source=kowiki", "UNAVAILABLE,UNAVAILABLE"));
    assertThat(logs).doesNotContain("2000-02-29").doesNotContain("02-29").doesNotContain("2월 29일");
  }

  @ParameterizedTest(name = "date page {0}, year page {1} -> 200, partial")
  @CsvSource({
    "UNAVAILABLE, FRESH",
    "FRESH, UNAVAILABLE",
    "STALE, UNAVAILABLE",
    "UNAVAILABLE, STALE"
  })
  void oneUnavailablePageWithAnyCopyOfTheOtherIsPartial(State datePage, State yearPage)
      throws Exception {
    List<KoWikiEvent> events = List.of(event("2000년", "사건", "시험 사건 다.", "시험 문서 다"));
    source.next.set(day(events, datePage, yearPage, false));

    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath("$.events.length()").value(1))
        .andExpect(jsonPath("$.notice").value(nullValue()));
  }

  @Test
  void bothPagesAbsentIsNoEventsFoundAndNotPartial() throws Exception {
    source.next.set(day(List.of(), State.ABSENT, State.ABSENT, false));

    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.events").isEmpty())
        .andExpect(jsonPath("$.day_media").isEmpty())
        .andExpect(jsonPath("$.notice.code").value("NO_EVENTS_FOUND"))
        .andExpect(jsonPath("$.partial").value(false));
  }

  @Test
  void staleOnlyPagesAnswerWithTheOldCopyAsPartial() throws Exception {
    source.next.set(day(List.of(), State.STALE, State.STALE, false));

    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notice.code").value("NO_EVENTS_FOUND"))
        .andExpect(jsonPath("$.partial").value(true));
  }

  @Test
  void failedLinkLookupIsPartial() throws Exception {
    List<KoWikiEvent> events = List.of(event("2월 29일", "사건", "시험 사건 가.", null));
    source.next.set(day(events, State.FRESH, State.CACHED, true));

    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(true))
        .andExpect(jsonPath("$.events[0].wikidata_qid").value(nullValue()));
  }

  @Test
  void everyEventIsPassedThroughWhateverItsPageOrSection() throws Exception {
    List<KoWikiEvent> events =
        List.of(
            event("2월 29일", "문화", "시험 사건 나.", "시험 문서 나"),
            event("2000년", "사건", "시험 사건 다", "시험 문서 다"),
            event("2000년", "문화", "시험 채널에서 시험 항목 24개를 시험했다.", "시험 채널"));
    source.next.set(day(events, State.FRESH, State.FRESH, false));

    world("?date=2000-02-29")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events.length()").value(events.size()))
        .andExpect(jsonPath("$.events[0].source_title").value("2월 29일"))
        .andExpect(jsonPath("$.events[0].source_section").value("문화"))
        .andExpect(jsonPath("$.events[1].source_title").value("2000년"))
        .andExpect(jsonPath("$.events[1].source_section").value("사건"))
        .andExpect(jsonPath("$.events[1].summary").value("시험 사건 다"))
        .andExpect(jsonPath("$.events[2].source_title").value("2000년"))
        .andExpect(jsonPath("$.events[2].source_section").value("문화"))
        .andExpect(jsonPath("$.events[2].summary").value("시험 채널에서 시험 항목 24개를 시험했다."))
        .andExpect(jsonPath("$.notice").value(nullValue()))
        .andExpect(jsonPath("$.partial").value(false));
  }
}
