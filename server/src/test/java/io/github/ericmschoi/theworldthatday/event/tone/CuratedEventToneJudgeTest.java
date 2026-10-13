package io.github.ericmschoi.theworldthatday.event.tone;

import static io.github.ericmschoi.theworldthatday.event.tone.EventCurationTest.VALID_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource.Signals;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Plan M3a2 T3 (2), (3), (4): the tone and scope table with synthetic QIDs (Q1 to Q9) and neutral
 * test sentences. Wikidata is a stub that records what it was asked.
 */
class CuratedEventToneJudgeTest {

  private static final LocalDate DAY = LocalDate.of(2000, 2, 29);
  private static final Map<String, Signals> SIGNALS =
      Map.of(
          "Q1", new Signals(true, false, Set.of()),
          "Q2", new Signals(false, true, Set.of()),
          "Q3", new Signals(true, true, Set.of("Q17")),
          "Q4", new Signals(false, false, Set.of("Q884", "Q17")),
          "Q5", new Signals(false, false, Set.of("Q17")),
          "Q6", Signals.NONE);

  @TempDir Path dataDir;

  private final List<List<String>> asked = new ArrayList<>();
  private boolean wikidataDown;

  private final WikidataSignalSource wikidata =
      new WikidataSignalSource() {
        @Override
        public Optional<Map<String, Signals>> signals(Collection<String> qids) {
          asked.add(List.copyOf(qids));
          return wikidataDown ? Optional.empty() : Optional.of(SIGNALS);
        }
      };

  private static KoWikiEvent event(String text, String qid, String... links) {
    return new KoWikiEvent(
        "kowiki-2000-02-29-0000000" + (qid == null ? "0" : qid.substring(1)),
        DAY,
        "2000년",
        "사건",
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
    Verdict verdict = only(judge(VALID_ENTRY), event("시험 문서 가에서 화재가 났다.", "Q1", "시험 문서 가"));

    assertThat(verdict).isEqualTo(new Verdict(Tone.NORMAL, Basis.CURATED, Scope.WORLD, false));
  }

  @Test
  void keywordWinsOverWikidata() {
    Verdict verdict = only(judge(""), event("시험 지역에 지진이 있었다.", "Q6", "시험 지역"));

    assertThat(verdict.tone()).isEqualTo(Tone.TRAGIC);
    assertThat(verdict.basis()).isEqualTo(Basis.RULE_KEYWORD);
  }

  @Test
  void wikidataRulesInOrder() {
    CuratedEventToneJudge judge = judge("");

    assertThat(only(judge, event("시험 사건 가.", "Q1", "가")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_CLASS);
    assertThat(only(judge, event("시험 사건 나.", "Q2", "나")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.TRAGIC, Basis.RULE_DEATHS);
    assertThat(only(judge, event("시험 사건 다.", "Q3", "다")).basis())
        .as("class before deaths")
        .isEqualTo(Basis.RULE_CLASS);
    assertThat(only(judge, event("시험 사건 라.", "Q6", "라")))
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.NORMAL, Basis.RULE_NO_SIGNAL);
    assertThat(only(judge, event("시험 사건 마.", "Q9", "마")))
        .as("asked but missing from the answer: evaluated, no signal")
        .extracting(Verdict::tone, Verdict::basis)
        .containsExactly(Tone.NORMAL, Basis.RULE_NO_SIGNAL);
  }

  @Test
  void withoutQidTheVerdictIsConservative() {
    Judgement judgement = judge("").judge(List.of(event("시험 사건 가.", null)));

    assertThat(judgement.verdicts().getFirst())
        .isEqualTo(new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN, false));
    assertThat(judgement.partial()).isFalse();
    assertThat(asked).containsExactly(List.of());
  }

  @Test
  void wikidataDownIsConservativeAndPartial() {
    wikidataDown = true;

    Judgement judgement =
        judge(VALID_ENTRY)
            .judge(List.of(event("시험 사건 가.", "Q6", "가"), event("시험 사건.", "Q4", "시험 문서 가")));

    assertThat(judgement.verdicts())
        .containsExactly(
            new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN, false),
            new Verdict(Tone.NORMAL, Basis.CURATED, Scope.WORLD, false));
    assertThat(judgement.partial()).isTrue();
  }

  @Test
  void scopeComesFromCurationThenCountries() {
    CuratedEventToneJudge judge = judge("");

    assertThat(only(judge, event("시험 사건 가.", "Q4", "가")).scope()).isEqualTo(Scope.DOMESTIC);
    assertThat(only(judge, event("시험 사건 나.", "Q5", "나")).scope()).isEqualTo(Scope.WORLD);
    assertThat(only(judge, event("시험 사건 다.", "Q6", "다")).scope()).isEqualTo(Scope.UNKNOWN);
    assertThat(only(judge, event("시험 사건 라.", null)).scope()).isEqualTo(Scope.UNKNOWN);
    assertThat(only(judge(VALID_ENTRY), event("시험 사건 마.", "Q4", "시험 문서 가")).scope())
        .as("curated world although P17 holds Korea")
        .isEqualTo(Scope.WORLD);
    assertThat(
            only(
                    judge(VALID_ENTRY.replace("\"scope\": \"world\", ", "")),
                    event("시험 사건 마.", "Q4", "시험 문서 가"))
                .scope())
        .as("curated tone without scope: scope from P17")
        .isEqualTo(Scope.DOMESTIC);
  }

  @Test
  void oneRequestWithEveryQidOfTheDayExceptHiddenOnes() {
    String hide =
        VALID_ENTRY
            .replace("\"primary_link\": \"시험 문서 가\"", "\"primary_link\": \"시험 문서 숨김\"")
            .replace("\"hide\": false", "\"hide\": true");
    CuratedEventToneJudge judge = judge(VALID_ENTRY + "," + hide);

    Judgement judgement =
        judge.judge(
            List.of(
                event("시험 사건 가.", "Q4", "시험 문서 가"),
                event("시험 사건 나.", "Q5", "나"),
                event("시험 사건 다.", "Q5", "다"),
                event("시험 사건 라.", null),
                event("시험 사건 마.", "Q7", "시험 문서 숨김")));

    assertThat(asked).containsExactly(List.of("Q4", "Q5"));
    assertThat(judgement.verdicts())
        .extracting(Verdict::hidden)
        .containsExactly(false, false, false, false, true);
  }

  @Test
  void keywordsAreThePlanList() {
    List<String> words =
        List.of(
            "사망", "숨지", "희생", "참사", "사고", "추락", "침몰", "충돌", "탈선", "붕괴", "폭발", "화재", "지진", "해일",
            "쓰나미", "태풍", "홍수", "테러", "총격", "학살", "암살", "피살", "살해", "전쟁", "공습", "폭격", "침공", "내전",
            "분쟁", "쿠데타", "처형");
    CuratedEventToneJudge judge = judge("");

    for (String word : words) {
      assertThat(only(judge, event("시험 " + word + " 문장.", "Q6", "가")).basis())
          .as(word)
          .isEqualTo(Basis.RULE_KEYWORD);
    }
  }
}
