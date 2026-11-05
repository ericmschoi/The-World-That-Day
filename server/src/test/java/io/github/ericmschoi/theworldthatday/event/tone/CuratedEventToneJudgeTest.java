package io.github.ericmschoi.theworldthatday.event.tone;

import static io.github.ericmschoi.theworldthatday.event.tone.EventCurationTest.VALID_ENTRY;
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
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource.Signals;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryService;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a2 T3 (2), (3), (4) and plan C1 S3a T4 (4), (5): the tone and scope table with synthetic
 * QIDs (Q1 to Q9), Canada (Q16) as the domestic country and neutral English test sentences.
 * Wikidata is a stub that records what it was asked.
 */
class CuratedEventToneJudgeTest {

  private static final LocalDate DAY = LocalDate.of(2000, 2, 29);
  private static final Map<String, Signals> SIGNALS =
      Map.of(
          "Q1", new Signals(true, false, Set.of()),
          "Q2", new Signals(false, true, Set.of()),
          "Q3", new Signals(true, true, Set.of("Q30")),
          "Q4", new Signals(false, false, Set.of("Q16", "Q30")),
          "Q5", new Signals(false, false, Set.of("Q30")),
          "Q6", Signals.NONE);

  /** VALID_ENTRY of EventCurationTest for the English test page A. */
  private static final String ENTRY =
      VALID_ENTRY
          .replace("\"primary_link\": \"시험 문서 가\"", "\"primary_link\": \"Test page A\"")
          .replace("\"reason\": \"시험 항목\"", "\"reason\": \"Test entry\"")
          .replace(
              "https://ko.wikipedia.org/w/index.php?title=2000%EB%85%84&oldid=200",
              "https://en.wikipedia.org/w/index.php?title=2000&oldid=200");

  @TempDir Path dataDir;

  private final List<List<String>> asked = new ArrayList<>();
  private boolean wikidataDown;

  private final WikidataSignalSource wikidata =
      new WikidataSignalSource() {
        @Override
        public Optional<Map<String, Signals>> signals(Collection<String> qids) {
          asked.add(List.copyOf(qids));
          if (wikidataDown) {
            return Optional.empty();
          }
          // Like the SPARQL source: every queried (well-formed) ID is in the answer, never others.
          Map<String, Signals> answer = new LinkedHashMap<>();
          qids.stream()
              .filter(qid -> WikidataProperties.QID.matcher(qid).matches())
              .forEach(qid -> answer.put(qid, SIGNALS.getOrDefault(qid, Signals.NONE)));
          return Optional.of(answer);
        }
      };

  private static KoWikiEvent event(String text, String qid, String... links) {
    return new KoWikiEvent(
        "enwiki-2000-02-29-0000000" + (qid == null ? "0" : qid.substring(1)),
        DAY,
        "2000",
        "Events",
        200L,
        text,
        List.of(links),
        qid,
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  private CuratedEventToneJudge judge(String entries) {
    return new CuratedEventToneJudge(
        EventCurationTest.load(EventCurationTest.write(dataDir, entries)), wikidata);
  }

  private Verdict only(CuratedEventToneJudge judge, KoWikiEvent event) {
    return judge.judge(List.of(event)).verdicts().getFirst();
  }

  @Test
  void curationWinsOverKeywordAndWikidata() {
    Verdict verdict =
        only(judge(ENTRY), event("A fire breaks out at Test page A.", "Q1", "Test page A"));

    assertThat(verdict).isEqualTo(new Verdict(Tone.NORMAL, Basis.CURATED, Scope.WORLD, false));
  }

  @Test
  void keywordWinsOverWikidata() {
    Verdict verdict =
        only(judge(""), event("An earthquake hits Test area A.", "Q6", "Test area A"));

    assertThat(verdict.tone()).isEqualTo(Tone.TRAGIC);
    assertThat(verdict.basis()).isEqualTo(Basis.RULE_KEYWORD);
  }

  @Test
  void wikidataRulesInOrder() {
    CuratedEventToneJudge judge = judge("");

    assertThat(only(judge, event("Test event A.", "Q1", "A")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_CLASS);
    assertThat(only(judge, event("Test event B.", "Q2", "B")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_DEATHS);
    assertThat(only(judge, event("Test event C.", "Q3", "C")).basis())
        .as("class before deaths")
        .isEqualTo(Basis.RULE_CLASS);
    assertThat(only(judge, event("Test event D.", "Q6", "D")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.NORMAL, Basis.RULE_NO_SIGNAL);
    assertThat(only(judge, event("Test event E.", "Q9", "E")))
        .as("asked but missing from the answer: evaluated, no signal")
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.NORMAL, Basis.RULE_NO_SIGNAL);
  }

  @Test
  void withoutQidTheVerdictIsConservative() {
    Judgement judgement = judge("").judge(List.of(event("Test event A.", null)));

    assertThat(judgement.verdicts().getFirst())
        .isEqualTo(new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN, false));
    assertThat(judgement.partial()).isFalse();
    assertThat(asked).containsExactly(List.of());
  }

  @Test
  void wikidataDownIsConservativeAndPartial() {
    wikidataDown = true;

    Judgement judgement =
        judge(ENTRY)
            .judge(
                List.of(
                    event("Test event A.", "Q6", "A"), event("Test event.", "Q4", "Test page A")));

    assertThat(judgement.verdicts())
        .containsExactly(
            new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN, false),
            new Verdict(Tone.NORMAL, Basis.CURATED, Scope.WORLD, false));
    assertThat(judgement.partial()).isTrue();
  }

  @Test
  void scopeComesFromCurationThenCountries() {
    CuratedEventToneJudge judge = judge("");

    assertThat(only(judge, event("Test event A.", "Q4", "A")).scope()).isEqualTo(Scope.DOMESTIC);
    assertThat(only(judge, event("Test event B.", "Q5", "B")).scope()).isEqualTo(Scope.WORLD);
    assertThat(only(judge, event("Test event C.", "Q6", "C")).scope()).isEqualTo(Scope.UNKNOWN);
    assertThat(only(judge, event("Test event D.", null)).scope()).isEqualTo(Scope.UNKNOWN);
    assertThat(only(judge(ENTRY), event("Test event E.", "Q4", "Test page A")).scope())
        .as("curated world although P17 holds Canada")
        .isEqualTo(Scope.WORLD);
    assertThat(
            only(
                    judge(ENTRY.replace("\"scope\": \"world\", ", "")),
                    event("Test event E.", "Q4", "Test page A"))
                .scope())
        .as("curated tone without scope: scope from P17")
        .isEqualTo(Scope.DOMESTIC);
  }

  @Test
  void oneRequestWithEveryQidOfTheDayExceptHiddenOnes() {
    String hide =
        ENTRY
            .replace("\"primary_link\": \"Test page A\"", "\"primary_link\": \"Test page hidden\"")
            .replace("\"hide\": false", "\"hide\": true");
    CuratedEventToneJudge judge = judge(ENTRY + "," + hide);

    Judgement judgement =
        judge.judge(
            List.of(
                event("Test event A.", "Q4", "Test page A"),
                event("Test event B.", "Q5", "B"),
                event("Test event C.", "Q5", "C"),
                event("Test event D.", null),
                event("Test event E.", "Q7", "Test page hidden")));

    assertThat(asked).containsExactly(List.of("Q4", "Q5"));
    assertThat(judgement.verdicts())
        .extracting(Verdict::hidden)
        .containsExactly(false, false, false, false, true);
  }

  @Test
  void keywordsAreThePlanList() {
    List<String> words =
        List.of(
            "kill",
            "kills",
            "killed",
            "killing",
            "killings",
            "dead",
            "death",
            "deaths",
            "die",
            "dies",
            "died",
            "dying",
            "crash",
            "crashes",
            "crashed",
            "earthquake",
            "earthquakes",
            "massacre",
            "massacres",
            "massacred",
            "shoot",
            "shoots",
            "shooting",
            "shootings",
            "shot",
            "bomb",
            "bombs",
            "bombed",
            "bombing",
            "bombings",
            "terror",
            "terrorist",
            "terrorists",
            "terrorism",
            "attack",
            "attacks",
            "attacked",
            "murder",
            "murders",
            "murdered",
            "disaster",
            "disasters",
            "explosion",
            "explosions",
            "explode",
            "explodes",
            "exploded",
            "drown",
            "drowns",
            "drowned",
            "drowning",
            "collapse",
            "collapses",
            "collapsed",
            "genocide",
            "assassinate",
            "assassinates",
            "assassinated",
            "assassination",
            "hostage",
            "hostages",
            "victim",
            "victims",
            "tsunami",
            "famine",
            "epidemic",
            "pandemic",
            "riot",
            "riots",
            "fire",
            "fires",
            "war",
            "wars",
            "coup",
            "sank",
            "sinks",
            "sinking",
            "derail",
            "derails",
            "derailed",
            "derailment",
            "collide",
            "collides",
            "collided",
            "collision",
            "hurricane",
            "typhoon",
            "cyclone",
            "flood",
            "floods",
            "flooding",
            "invade",
            "invades",
            "invaded",
            "invasion",
            "executed",
            "execution",
            "executions");
    CuratedEventToneJudge judge = judge("");

    for (String word : words) {
      assertThat(only(judge, event("Test " + word + " sentence.", "Q6", "A")).basis())
          .as(word)
          .isEqualTo(Basis.RULE_KEYWORD);
    }
  }

  @ParameterizedTest(name = "\"{0}\" is a keyword: {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "Test event A: 3 people Killed.|true",
        "KILLED in test event A.|true",
        "A test coup.|true",
        "Test event A (war).|true",
        "Test event A, war-time.|true",
        "Test player A is skilled.|false",
        "Test event A in Warsaw.|false",
        "Test award A for software.|false",
        "Test event A is unkilled.|false",
        "Test ceasefire A.|false",
        "Test event A was shotgun-free.|false"
      })
  void keywordsMatchWholeWordsInAnyCase(String text, boolean keyword) {
    Verdict verdict = only(judge(""), event(text, "Q6", "A"));

    assertThat(verdict.basis() == Basis.RULE_KEYWORD).isEqualTo(keyword);
  }

  /** T4 (4): the recorded 1980-04-12 crash line is tragic by keyword ("killed"). */
  @Test
  void recordedCrashLineIsTragicByKeyword() {
    KoWikiEvent crash =
        event(
            "Transbrasil Flight 303, a Boeing 727, crashes on approach to Hercílio Luz"
                + " International Airport in Florianópolis, Brazil. Fifty-five out of the 58"
                + " people on board are killed.",
            "Q6",
            "Transbrasil Flight 303");

    assertThat(only(judge(""), crash))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_KEYWORD);
  }

  /** S3a review: the recorded 1918 line of July 17 is tragic by keyword ("are executed"). */
  @Test
  void recordedExecutionLineIsTragicByKeyword() {
    KoWikiEvent execution =
        event(
            "Tsar Nicholas II of Russia and his immediate family and retainers are executed by"
                + " Bolshevik Chekists at the Ipatiev House in Yekaterinburg, Russia.",
            "Q6",
            "Nicholas II of Russia");

    assertThat(only(judge(""), execution))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_KEYWORD);
  }

  @Test
  void malformedQidThatWasNeverQueriedIsConservative() {
    CuratedEventToneJudge judge = judge("");

    Judgement judgement = judge.judge(List.of(event("Test event A.", "Q0", "A")));

    assertThat(judgement.verdicts().getFirst())
        .as("rule 6: no usable QID")
        .isEqualTo(new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN, false));
    assertThat(judgement.partial()).isFalse();
  }

  private static final String HIDE_ENTRY =
      ENTRY.replace("\"tone\": \"normal\", ", "").replace("\"hide\": false", "\"hide\": true");

  @ParameterizedTest(name = "takedown listed {0}")
  @CsvSource({"after the tone entry, false", "before the tone entry, true"})
  void takedownWinsOverAToneEntryForTheSameEvent(String order, boolean hideFirst) {
    assertThat(HIDE_ENTRY).contains("\"hide\": true").doesNotContain("\"tone\"");
    CuratedEventToneJudge judge =
        judge(hideFirst ? HIDE_ENTRY + "," + ENTRY : ENTRY + "," + HIDE_ENTRY);
    List<KoWikiEvent> events =
        List.of(event("Test event A.", "Q4", "Test page A"), event("Test event B.", "Q5", "B"));

    // Fixed per state of the year page, not derived from the source result.
    Map<State, Boolean> partialFor = Map.of(State.FRESH, false, State.STALE, true);
    for (State yearPage : List.of(State.FRESH, State.STALE)) {
      asked.clear();
      KoWikiDayResult day =
          new KoWikiDayResult(
              DAY,
              events,
              List.of(
                  new KoWikiDocStatus("February 29", null, null, State.ABSENT),
                  new KoWikiDocStatus("2000", 200L, Instant.EPOCH, yearPage)),
              0,
              0,
              false);

      WorldStoryResponse response =
          new WorldStoryService(date -> day, judge, (date, tragicDay) -> DayMedia.NONE).find(DAY);

      assertThat(response.events())
          .extracting(WorldStoryResponse.Event::summary)
          .containsExactly("Test event B.");
      assertThat(asked).as("the hidden event's QID is not sent").containsExactly(List.of("Q5"));
      assertThat(response.partial())
          .as("year page %s: a takedown never makes the answer partial on its own", yearPage)
          .isEqualTo(partialFor.get(yearPage));
    }
  }
}
