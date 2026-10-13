package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.wikitext;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.EMPTY_PAGEPROPS;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.PATH;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.pagepropsRequests;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.param;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.params;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPage;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubRecorded;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a1c T1 (3), (5)~(10) and the demo dates, with the real HTTP client, Action API clients and
 * parser against WireMock (recorded fixtures for the demo pages, synthetic ones for 2000-02-29).
 * Deduplication is T2, so every parsed line is still in the result here.
 */
class TwoPageKoWikiEventSourceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate OPENING = LocalDate.of(1988, 9, 17);
  private static final LocalDate QUARTER_FINAL = LocalDate.of(2002, 6, 22);
  private static final LocalDate QUIET = LocalDate.of(1995, 3, 15);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  /** Not about timeouts: 2 s read timeout so a slow machine never turns into a failure. */
  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  private KoWikiDayResult find(LocalDate date) {
    return parts.source().find(date);
  }

  private static void stubOpeningDay() {
    stubRecorded(wikimedia, "9월 17일");
    stubRecorded(wikimedia, "1988년");
    stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));
  }

  private static void stubLeapDay() {
    stubPage(wikimedia, "2월 29일", "synthetic/synthetic-02-29");
    stubPage(wikimedia, "2000년", "synthetic/synthetic-2000");
    stubPageprops(wikimedia, EMPTY_PAGEPROPS);
  }

  @ParameterizedTest(name = "{0} reads only [{1}] and [{2}]")
  @CsvSource({
    "1988-09-17, 9월 17일, 1988년",
    "2000-02-29, 2월 29일, 2000년",
    "1995-03-15, 3월 15일, 1995년"
  })
  void onlyTheTwoPagesOfTheDateAreRequestedAndEveryEventIsOfThatDate(
      LocalDate date, String dateTitle, String yearTitle) {
    stubOpeningDay();
    stubLeapDay();
    stubRecorded(wikimedia, "3월 15일");
    stubRecorded(wikimedia, "1995년");

    KoWikiDayResult result = find(date);

    assertThat(params(wikimedia, "page")).containsExactly(dateTitle, yearTitle);
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::title)
        .containsExactly(dateTitle, yearTitle);
    assertThat(result.events())
        .allSatisfy(event -> assertThat(event.date()).isEqualTo(date))
        .allSatisfy(event -> assertThat(event.sourceTitle()).isIn(dateTitle, yearTitle));
    assertThat(result.date()).isEqualTo(date);
  }

  @Test
  void openingDayKeepsEveryLineWithCanonicalLinksAndTheMainLinkItem() {
    stubOpeningDay();

    KoWikiDayResult result = find(OPENING);

    assertThat(result.docs())
        .extracting(KoWikiDocStatus::revision)
        .containsExactly(
            KoWikiFixtureReadme.rows().get("09-17-sections.json").revid(),
            KoWikiFixtureReadme.rows().get("1988-sections.json").revid());
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    KoWikiEvent first = result.events().getFirst();
    assertThat(first.sourceTitle()).isEqualTo("9월 17일");
    assertThat(first.sourceSection()).isEqualTo("사건");
    assertThat(first.text()).contains("서울 하계 올림픽");
    assertThat(first.linkTitles()).containsExactly("1988년 하계 올림픽");
    assertThat(first.wikidataQid()).isEqualTo("Q8470");
    assertThat(first.revision()).isEqualTo(42604413L);
    assertThat(first.retrievedAt()).isEqualTo(parts.clock.instant());
    KoWikiEvent culture = result.events().get(1);
    assertThat(culture.sourceSection()).isEqualTo("문화");
    assertThat(culture.linkTitles())
        .as("서울 is a redirect to 서울특별시")
        .containsExactly("1988년 하계 올림픽", "대한민국", "서울특별시");
    assertThat(result.events())
        .filteredOn(event -> event.text().contains("KBS 1TV"))
        .singleElement()
        .satisfies(event -> assertThat(event.wikidataQid()).isEqualTo("Q777278"));
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(result.degraded()).isFalse();
    assertThat(result.mergedCount()).isZero();
    assertThat(pagepropsRequests(wikimedia))
        .singleElement()
        .satisfies(
            request -> {
              assertThat(param(request, "titles"))
                  .isEqualTo("1988년 하계 올림픽|대한민국|서울|서울특별시|KBS 1TV|KBS 2TV");
              assertThat(request.getRequest().getHeader("User-Agent"))
                  .isEqualTo(KoWikiSourceFixture.USER_AGENT);
            });
  }

  @Test
  void quarterFinalLinksBothResolveToTheCupPage() {
    stubRecorded(wikimedia, "6월 22일");
    stubRecorded(wikimedia, "2002년");
    stubPageprops(wikimedia, read("pageprops-2002-06-22.json"));

    KoWikiDayResult result = find(QUARTER_FINAL);

    assertThat(result.events()).filteredOn(e -> e.text().contains("스페인")).hasSize(2);
    assertThat(result.events())
        .allSatisfy(
            event -> {
              assertThat(event.linkTitles()).containsExactly("2002년 FIFA 월드컵");
              assertThat(event.wikidataQid()).isEqualTo("Q47735");
            });
    assertThat(result.events().getFirst().sourceTitle()).isEqualTo("6월 22일");
  }

  @Test
  void quietDayHasNoEventsAndSendsNoLinkLookup() {
    stubRecorded(wikimedia, "3월 15일");
    stubRecorded(wikimedia, "1995년");

    KoWikiDayResult result = find(QUIET);

    assertThat(result.events()).isEmpty();
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(result.allUnavailable()).isFalse();
    assertThat(result.degraded()).isFalse();
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(pagepropsRequests(wikimedia)).isEmpty();
  }

  @Test
  void eventsWithoutLinksSendNoLinkLookup() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("2월 29일"))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));
    stubPage(wikimedia, "2000년", "synthetic/synthetic-2000");

    KoWikiDayResult result = find(LEAP_DAY);

    assertThat(result.events()).extracting(KoWikiEvent::text).containsExactly("시험 사건 다.");
    assertThat(result.events().getFirst().linkTitles()).isEmpty();
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(pagepropsRequests(wikimedia)).isEmpty();
    assertThat(result.linkResolutionFailed()).isFalse();
  }

  @Test
  void failedLinkLookupKeepsTheEventsWithoutItems() {
    stubRecorded(wikimedia, "9월 17일");
    stubRecorded(wikimedia, "1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiDayResult result = find(OPENING);
    List<String> expectedTexts = parsedTexts(OPENING, "09-17", "1988");

    assertThat(result.events()).extracting(KoWikiEvent::text).isEqualTo(expectedTexts);
    assertThat(result.events()).extracting(KoWikiEvent::wikidataQid).containsOnlyNulls();
    assertThat(result.events().get(1).linkTitles())
        .as("normalized but not resolved")
        .containsExactly("1988년 하계 올림픽", "대한민국", "서울");
    assertThat(result.linkResolutionFailed()).isTrue();
    assertThat(result.degraded()).isTrue();
    assertThat(result.allUnavailable()).isFalse();
  }

  @Test
  void idsHaveTheContractFormAndAreStableAcrossCalls() {
    stubOpeningDay();

    List<String> first = find(OPENING).events().stream().map(KoWikiEvent::id).toList();
    List<String> second = find(OPENING).events().stream().map(KoWikiEvent::id).toList();

    assertThat(first)
        .isNotEmpty()
        .allMatch(id -> id.matches("^kowiki-\\d{4}-\\d{2}-\\d{2}-[0-9a-f]{8}$"));
    assertThat(first).allMatch(id -> id.startsWith("kowiki-1988-09-17-"));
    assertThat(second).isEqualTo(first);
    assertThat(TwoPageKoWikiEventSource.id(OPENING, "9월 17일", "시험 사건 가"))
        .isEqualTo(TwoPageKoWikiEventSource.id(OPENING, "9월 17일", "시험 사건 가"))
        .isNotEqualTo(TwoPageKoWikiEventSource.id(OPENING, "1988년", "시험 사건 가"))
        .isNotEqualTo(TwoPageKoWikiEventSource.id(LEAP_DAY, "9월 17일", "시험 사건 가"));
  }

  @Test
  void idIsTheFirstEightHexDigitsOfTheSha256OfTitleAndText() throws Exception {
    byte[] hash =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest("2월 29일\n시험 사건 가".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertThat(TwoPageKoWikiEventSource.id(LEAP_DAY, "2월 29일", "시험 사건 가"))
        .isEqualTo("kowiki-2000-02-29-" + java.util.HexFormat.of().formatHex(hash, 0, 4));
  }

  @Test
  void onePageFailingLeavesTheOtherPagesEvents() {
    stubRecorded(wikimedia, "1988년");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("9월 17일"))
            .willReturn(aResponse().withStatus(503)));
    stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));

    KoWikiDayResult result = find(OPENING);

    assertThat(params(wikimedia, "page"))
        .as("a fast 503 is not a stall: the year page is still requested")
        .containsExactly("9월 17일", "9월 17일", "1988년");
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.UNAVAILABLE, State.FRESH);
    assertThat(result.docs().getFirst().revision()).isNull();
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.sourceTitle()).isEqualTo("1988년"));
    assertThat(result.degraded()).isTrue();
    assertThat(result.allUnavailable()).isFalse();
  }

  @Test
  void yearPageFailingLeavesTheDatePageEvents() {
    stubRecorded(wikimedia, "9월 17일");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("1988년"))
            .willReturn(aResponse().withStatus(503)));
    stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));

    KoWikiDayResult result = find(OPENING);

    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.FRESH, State.UNAVAILABLE);
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.sourceTitle()).isEqualTo("9월 17일"));
    assertThat(result.degraded()).isTrue();
  }

  @Test
  void bothPagesFailingIsAllUnavailable() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    KoWikiDayResult result = find(OPENING);

    assertThat(result.events()).isEmpty();
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.UNAVAILABLE);
    assertThat(result.allUnavailable()).isTrue();
    assertThat(result.degraded()).isTrue();
    assertThat(result.linkResolutionFailed()).isFalse();
  }

  @Test
  void bothPagesMissingIsNotAFailure() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));

    KoWikiDayResult result = find(LEAP_DAY);

    assertThat(result.events()).isEmpty();
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.ABSENT);
    assertThat(result.allUnavailable()).isFalse();
    assertThat(result.degraded()).isFalse();
    assertThat(result.skippedLines()).isZero();
  }

  @Test
  void skippedLinesAreTheSumOfBothPagesAndTextsAreTheParserOutput() {
    stubOpeningDay();

    KoWikiDayResult result = find(OPENING);

    int expectedSkipped = 0;
    for (String[] section :
        List.of(
            new String[] {"09-17-events.json", "DATE"},
            new String[] {"09-17-culture.json", "DATE"},
            new String[] {"1988-events.json", "YEAR"},
            new String[] {"1988-culture.json", "YEAR"})) {
      expectedSkipped +=
          parts
              .parser
              .parse(wikitext(section[0]), KoWikiPageKind.valueOf(section[1]), OPENING)
              .skippedLines();
    }
    assertThat(result.skippedLines()).isEqualTo(expectedSkipped);
    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .isEqualTo(parsedTexts(OPENING, "09-17", "1988"));
  }

  @Test
  void leapDayEventsComeFromTheLeapDayPagesWithTheirRevisions() {
    stubLeapDay();

    KoWikiDayResult result = find(LEAP_DAY);

    assertThat(result.events())
        .extracting(KoWikiEvent::text, KoWikiEvent::sourceTitle, KoWikiEvent::revision)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("시험 사건 가가 열렸다.", "2월 29일", 100L),
            org.assertj.core.groups.Tuple.tuple("시험 사건 나.", "2월 29일", 100L),
            org.assertj.core.groups.Tuple.tuple("시험 사건 다.", "2000년", 200L));
    assertThat(result.events().getFirst().linkTitles()).containsExactly("시험 문서 가");
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(result.linkResolutionFailed()).isFalse();
  }

  /** Parser output for the four sections in input order, character for character. */
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
}
