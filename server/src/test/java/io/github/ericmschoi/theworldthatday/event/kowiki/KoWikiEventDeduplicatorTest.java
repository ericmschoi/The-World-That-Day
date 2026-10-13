package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Candidate;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Result;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1c T2 (2)~(5): one synthetic case per rule D1~D6, link normalization and the log rule.
 * Sentences are neutral test lines ("시험 대회 A조 경기 가"), never real events. Sections: 0 date page 사건,
 * 1 문화, 2 year page 사건, 3 문화.
 */
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventDeduplicatorTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private final KoWikiEventDeduplicator deduplicator =
      new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6));

  /** A line of {@code section}; a link is {@code "title"} or {@code "title=Q123"}. */
  private static Candidate line(int section, String text, String... links) {
    List<Link> parsed =
        Arrays.stream(links)
            .map(l -> l.contains("=") ? l.split("=") : new String[] {l, null})
            .map(parts -> new Link(parts[0], parts[1]))
            .toList();
    String page = section < 2 ? "2월 29일" : "2000년";
    KoWikiEvent event =
        new KoWikiEvent(
            TwoPageKoWikiEventSource.id(LEAP_DAY, page, text),
            LEAP_DAY,
            page,
            section % 2 == 0 ? "사건" : "문화",
            section < 2 ? 100L : 200L,
            text,
            parsed.stream().map(Link::title).toList(),
            parsed.isEmpty() ? null : parsed.getFirst().wikidataQid(),
            READ);
    return new Candidate(event, section, parsed);
  }

  private Result dedup(Candidate... lines) {
    return deduplicator.dedup(List.of(lines));
  }

  private static List<KoWikiEvent> events(Candidate... lines) {
    return Arrays.stream(lines).map(Candidate::event).toList();
  }

  @Test
  void d1LinesOfOneSectionNeverMergeEvenWithTheSameMainLink() {
    Candidate a = line(0, "시험 대회 1경기가 열렸다.", "시험 대회");
    Candidate b = line(0, "시험 대회 2경기가 열렸다.", "시험 대회");

    Result result = dedup(a, b);

    assertThat(result.events()).isEqualTo(events(a, b));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d1ExceptionIdenticalLineTwiceOnOnePageIsOneEventSoIdsStayUnique() {
    Candidate a = line(0, "시험 사건 가.", "시험 문서 가");
    Candidate twice = line(0, "시험 사건 가.", "시험 문서 가");
    Candidate otherSection = line(1, "시험 사건 가.");

    Result result = dedup(a, twice, otherSection);

    assertThat(result.events()).containsExactly(a.event());
    assertThat(result.mergedCount()).isEqualTo(2);
    assertThat(result.events()).extracting(KoWikiEvent::id).doesNotHaveDuplicates();
  }

  @Test
  void d2SameTextApartFromSpacesAndPunctuationMerges() {
    Candidate date = line(0, "시험 사건 가가 열렸다.");
    Candidate year = line(2, "시험 사건 가가  열렸다");

    Result result = dedup(date, year);

    assertThat(result.events()).containsExactly(date.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d2DifferentWordsStayApart() {
    Result result = dedup(line(0, "시험 사건 가가 열렸다."), line(2, "시험 사건 나가 열렸다."));

    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d3LineWithAGenericMainLinkMergesThroughTheSharedSpecificLink() {
    Candidate date = line(0, "시험 대회가 개막했다.", "시험 대회");
    Candidate year = line(3, "대한민국에서 시험 대회가 열리다.", "대한민국", "시험 대회");

    Result result = dedup(date, year);

    assertThat(result.events()).containsExactly(date.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d3OnlyGenericLinksSharedStayApart() {
    Candidate a = line(0, "대한민국에서 시험 사건 가.", "대한민국", "시험 문서 가");
    Candidate b = line(2, "대한민국에서 시험 사건 나.", "대한민국", "시험 문서 나");
    Candidate c = line(3, "대한민국에서 시험 사건 다.", "대한민국");

    Result result = dedup(a, b, c);

    assertThat(result.events()).isEqualTo(events(a, b, c));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d3CounterexampleSharingOnlyTheTournamentStaysApart() {
    Candidate a = line(1, "시험 대회 A조 경기 가", "시험 대회", "시험 팀 가");
    Candidate b = line(2, "시험 대회 A조 경기 나", "시험 대회", "시험 팀 나");
    Candidate c = line(3, "시험 대회 A조 경기 다", "시험 대회", "시험 팀 다", "시험 팀 라");

    Result result = dedup(a, b, c);

    assertThat(result.events()).isEqualTo(events(a, b, c));
  }

  @Test
  void d3SharedMainLinkWithEnoughOverlapMerges() {
    Candidate a = line(1, "시험 대회 A조 경기 가", "시험 대회", "시험 팀 가", "시험 팀 나");
    Candidate b = line(2, "시험 팀 가가 시험 대회에서 이겼다", "시험 대회", "시험 팀 가");

    assertThat(dedup(a, b).events()).containsExactly(a.event());
  }

  @Test
  void d3PageLinksAreNeitherMainLinksNorCounted() {
    Candidate a = line(0, "시험 사건 가.", "1999년", "시험 문서 가");
    Candidate b = line(2, "시험 사건 나.", "1999년", "시험 문서 나");
    Candidate c = line(3, "시험 사건 다.", "2월 28일");
    Candidate d = line(1, "시험 사건 라.", "2월 28일");

    assertThat(dedup(a, b, c, d).mergedCount()).isZero();
  }

  @Test
  void d4LineMatchingTwoEarlierEventsIsNotMerged() {
    Candidate first = line(0, "시험 사건 가.", "시험 문서 가");
    Candidate second = line(0, "시험 사건 나.", "시험 문서 나");
    Candidate both = line(2, "시험 사건 가와 나.", "시험 문서 가", "시험 문서 나");

    Result result = dedup(first, second, both);

    assertThat(result.events()).isEqualTo(events(first, second, both));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void d5TheFirstLineIsKeptUnchanged() {
    Candidate culture = line(1, "시험 대회가 개막하였다.", "시험 대회");
    Candidate year = line(2, "시험 대회 개막.", "시험 대회=Q7");
    Candidate yearCulture = line(3, "시험 대회 개막식.", "시험 대회", "시험 장소");

    Result result = dedup(culture, year, yearCulture);

    assertThat(result.events()).singleElement().isSameAs(culture.event());
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(result.mergedCount()).isEqualTo(2);
  }

  @Test
  void spellingsWithAndWithoutTheYearSuffixMergeWithoutItems() {
    Candidate date = line(1, "시험 대회에서 시험 팀 가가 이겼다.", "2002 시험 대회");
    Candidate year = line(3, "시험 팀 가가 시험 대회 8강에서 이겼다.", "2002년 시험 대회");

    assertThat(dedup(date, year).events()).containsExactly(date.event());
  }

  @Test
  void differentTitlesWithTheSameItemMerge() {
    Candidate date = line(0, "시험 사건 가.", "시험 문서 가=Q9");
    Candidate year = line(2, "시험 사건 가 다른 표기.", "시험 문서 나=Q9");

    assertThat(dedup(date, year).events()).containsExactly(date.event());
    assertThat(KoWikiEventDeduplicator.sameLink(new Link("가", "Q1"), new Link("나", "Q2")))
        .isFalse();
    assertThat(KoWikiEventDeduplicator.titleKey("2002년 시험 대회")).isEqualTo("2002 시험 대회");
    assertThat(KoWikiEventDeduplicator.titleKey("2002년")).isEqualTo("2002년");
  }

  @Test
  void mergedCountIsTheNumberOfRemovedLines() {
    Candidate[] lines = {
      line(0, "시험 대회 개막.", "시험 대회"),
      line(0, "시험 사건 나.", "시험 문서 나"),
      line(1, "시험 대회가 열렸다.", "시험 대회", "대한민국"),
      line(2, "시험 사건 나", "시험 문서 다"),
      line(2, "시험 사건 라.", "시험 문서 라"),
      line(3, "시험 대회 개막식.", "대한민국", "시험 대회")
    };

    Result result = dedup(lines);

    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .containsExactly("시험 대회 개막.", "시험 사건 나.", "시험 사건 라.");
    assertThat(result.mergedCount()).isEqualTo(3).isEqualTo(lines.length - result.events().size());
  }

  @Test
  void debugLogsNameTheRuleAndIdHashButNoTextOrDate(CapturedOutput output) {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(KoWikiEventDeduplicator.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      Candidate first = line(0, "시험 사건 가.", "시험 문서 가");
      Candidate second = line(0, "시험 사건 나.", "시험 문서 나");
      Candidate d2 = line(2, "시험 사건 가");
      Candidate d3 = line(3, "시험 문서 나와 다.", "시험 문서 나");
      Candidate d4 = line(1, "시험 사건 가와 나.", "시험 문서 가", "시험 문서 나");
      dedup(first, second, d2, d3, d4);
    } finally {
      logger.setLevel(before);
    }

    String hashFirst = line(0, "시험 사건 가.").event().id().substring(18);
    assertThat(output.getAll())
        .contains("rule=D2 kept=" + hashFirst)
        .contains("rule=D3")
        .contains("rule=D4")
        .doesNotContain("시험")
        .doesNotContain("2000-02-29")
        .doesNotContain("kowiki-");
  }

  @Test
  void genericLinksAreLoadedWithoutCommentsOrBlankLines() {
    assertThat(deduplicator.genericLinks())
        .contains("대한민국", "서울특별시", "서울", "미국", "도쿄")
        .doesNotContain("", "1988년 하계 올림픽", "2002년 FIFA 월드컵")
        .noneMatch(title -> title.startsWith("#") || !title.equals(title.strip()));
  }

  @ParameterizedTest(name = "min-link-overlap {0} is rejected")
  @ValueSource(doubles = {0, -0.1, 1.01, Double.NaN})
  void overlapOutsideZeroToOneIsRejected(double overlap) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new KoWikiDedupProperties(overlap))
        .withMessageContaining("wikimedia.dedup.min-link-overlap");
  }

  /** The counterexample fixture through the whole source, with the link lookup failing. */
  @Test
  void counterexampleFixtureStaysTwoEventsThroughTheSource() {
    WikimediaProperties properties =
        new WikimediaProperties(
            "",
            Duration.ofSeconds(2),
            Duration.ofSeconds(4),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create("http://localhost:9/w/api.php"));
    KoWikiClient pages =
        title ->
            title.equals("2월 29일")
                ? new KoWikiPage(
                    title,
                    State.FRESH,
                    100L,
                    null,
                    KoWikiFixtures.read(
                        "synthetic/dedup/synthetic-counterexample-date-culture.wikitext"),
                    READ)
                : new KoWikiPage(
                    title,
                    State.FRESH,
                    200L,
                    KoWikiFixtures.read(
                        "synthetic/dedup/synthetic-counterexample-year-events.wikitext"),
                    null,
                    READ);
    KoWikiEventSource source =
        new TwoPageKoWikiEventSource(
            pages,
            titles -> Optional.empty(),
            new WikitextEventParser(),
            new WikimediaHttpClient(properties, new MutableClock(READ, ClockConfig.SERVICE_ZONE)),
            properties,
            deduplicator);

    KoWikiDayResult result = source.find(LEAP_DAY);

    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .containsExactly("시험 대회 A조 경기 가에서 시험 팀 가가 이겼다.", "시험 대회 A조 경기 나에서 시험 팀 나가 이겼다.");
    assertThat(result.mergedCount()).isZero();
    assertThat(result.linkResolutionFailed()).isTrue();
  }
}
