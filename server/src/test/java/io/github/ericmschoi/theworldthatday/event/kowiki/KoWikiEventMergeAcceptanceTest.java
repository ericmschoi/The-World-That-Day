package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.PATH;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubRecorded;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Candidate;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventDeduplicator.Result;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a1c T3: acceptance checks of T1/T2 that the backend tests left open. The demo-date rows
 * through the whole source (which lines were merged, nothing else lost), the second clause of the
 * 2002-06-22 row ("another match of the same day stays") as a neutral synthetic case, rule D3 in
 * the direction where the later line holds the specific main link, and the log rule (T2 (5)) for a
 * whole day at DEBUG, not only for the deduplicator on its own.
 */
@ExtendWith(OutputCaptureExtension.class)
class KoWikiEventMergeAcceptanceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate OPENING = LocalDate.of(1988, 9, 17);
  private static final LocalDate QUARTER_FINAL = LocalDate.of(2002, 6, 22);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  private final KoWikiEventDeduplicator deduplicator =
      new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6));

  private static void stubOpeningDay() {
    stubRecorded(wikimedia, "9월 17일");
    stubRecorded(wikimedia, "1988년");
    stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));
  }

  private static void stubQuarterFinalDay() {
    stubRecorded(wikimedia, "6월 22일");
    stubRecorded(wikimedia, "2002년");
    stubPageprops(wikimedia, read("pageprops-2002-06-22.json"));
  }

  /** Parser output of the four recorded sections, in input order. */
  private List<String> parsedTexts(LocalDate date, String datePrefix, String yearPrefix) {
    List<String> texts = new ArrayList<>();
    for (String[] section :
        List.of(
            new String[] {datePrefix + "-events.json", "DATE"},
            new String[] {datePrefix + "-culture.json", "DATE"},
            new String[] {yearPrefix + "-events.json", "YEAR"},
            new String[] {yearPrefix + "-culture.json", "YEAR"})) {
      parts
          .parser
          .parse(wikitext(section[0]), KoWikiPageKind.valueOf(section[1]), date)
          .events()
          .stream()
          .map(ParsedEvent::text)
          .forEach(texts::add);
    }
    return texts;
  }

  /** A line of {@code section} (0 date 사건, 1 문화, 2 year 사건, 3 문화) on the leap-day pages. */
  private static Candidate line(int section, String text, String... links) {
    List<Link> parsed = Arrays.stream(links).map(title -> new Link(title, null)).toList();
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
            null,
            READ);
    return new Candidate(event, section, parsed);
  }

  @Test
  void openingDayRemovesOnlyOpeningLinesAndKeepsEveryOtherLine() {
    stubOpeningDay();

    KoWikiDayResult result = parts.source().find(OPENING);
    List<String> parsed = parsedTexts(OPENING, "09-17", "1988");
    List<String> kept = result.events().stream().map(KoWikiEvent::text).toList();
    List<String> removed = new ArrayList<>(parsed);
    kept.forEach(removed::remove);

    assertThat(removed)
        .as("every removed line is about the opening of the 1988 games")
        .hasSize(result.mergedCount())
        .isNotEmpty()
        .allSatisfy(text -> assertThat(text).contains("올림픽"));
    assertThat(result.events())
        .as("lines without the games link are never merged into the opening")
        .filteredOn(event -> !event.linkTitles().contains("1988년 하계 올림픽"))
        .extracting(KoWikiEvent::text)
        .containsExactlyElementsOf(parsed.stream().filter(text -> !text.contains("올림픽")).toList());
    assertThat(result.events()).extracting(KoWikiEvent::id).doesNotHaveDuplicates();
  }

  @Test
  void quarterFinalLineKeepsTheOtherMatchOfTheDayItCarries() {
    stubQuarterFinalDay();

    KoWikiDayResult result = parts.source().find(QUARTER_FINAL);

    assertThat(result.events())
        .as("the kept line is unchanged (D5), so the other match it mentions is not lost")
        .filteredOn(event -> event.text().contains("스페인"))
        .singleElement()
        .satisfies(event -> assertThat(event.text()).contains("세네갈"));
    assertThat(result.events().size() + result.mergedCount())
        .isEqualTo(parsedTexts(QUARTER_FINAL, "06-22", "2002").size());
  }

  @Test
  void anotherMatchOfTheSameDayStaysWhileTheDuplicateMerges() {
    Candidate matchA = line(1, "시험 대회 8강 경기 가에서 시험 팀 가가 이겼다.", "2002 시험 대회", "시험 팀 가");
    Candidate matchB = line(1, "시험 대회 8강 경기 나에서 시험 팀 나가 이겼다.", "2002 시험 대회", "시험 팀 나");
    Candidate againA = line(3, "시험 팀 가가 2002년 시험 대회 8강에서 이겼다.", "2002년 시험 대회", "시험 팀 가");

    Result result = deduplicator.dedup(List.of(matchA, matchB, againA));

    assertThat(result.events()).containsExactly(matchA.event(), matchB.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d3MergesWhenOnlyTheLaterLineHasTheSpecificMainLink() {
    Candidate earlier = line(1, "대한민국에서 시험 대회가 열렸다.", "대한민국", "시험 대회");
    Candidate later = line(2, "시험 대회 개막.", "시험 대회");

    Result result = deduplicator.dedup(List.of(earlier, later));

    assertThat(result.events()).containsExactly(earlier.event());
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void d3BothMainLinksGenericAndOnlyAPageLinkSharedStayApart() {
    Candidate earlier = line(0, "대한민국에서 시험 사건 가.", "대한민국", "2000년");
    Candidate later = line(3, "서울에서 시험 사건 나.", "서울", "2000년", "대한민국");

    assertThat(deduplicator.dedup(List.of(earlier, later)).mergedCount()).isZero();
  }

  @Test
  void wholeDaysAtDebugLogNoEventTextDateOrFullId(CapturedOutput output) {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    try {
      stubOpeningDay();
      parts.source().find(OPENING);

      wikimedia.resetAll();
      stubRecorded(wikimedia, "6월 22일");
      stubRecorded(wikimedia, "2002년");
      wikimedia.stubFor(
          get(urlPathEqualTo(PATH))
              .withQueryParam("action", equalTo("query"))
              .willReturn(aResponse().withStatus(503)));
      parts.source().find(QUARTER_FINAL);
    } finally {
      app.setLevel(before);
    }

    String logs = output.getAll();
    assertThat(logs).as("the dedup ran at DEBUG").contains("rule=D3");
    assertThat(logs)
        .doesNotContain("1988-09-17")
        .doesNotContain("2002-06-22")
        .doesNotContain("kowiki-19")
        .doesNotContain("kowiki-20")
        .doesNotContain("올림픽")
        .doesNotContain("KBS")
        .doesNotContain("스페인")
        .doesNotContain("승부차기");
  }
}
