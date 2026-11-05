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
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plan M3a1c T1 (3), (5)~(10) and plan C1 S3a T2/T3 on English pages, with the real HTTP client,
 * Action API clients and parser against WireMock (recorded fixtures for the demo day 1976-07-17,
 * synthetic ones for 2002-06-22, 1995-03-15 and 2000-02-29). Deduplication runs too: the
 * expectations below are after merging. EnWikiDemoDaysEventSourceTest covers every demo day.
 */
class TwoPageKoWikiEventSourceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate OPENING = LocalDate.of(1976, 7, 17);
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
    stubRecorded(wikimedia, "July 17");
    stubRecorded(wikimedia, "1976");
    stubPageprops(wikimedia, read("pageprops-1976-07-17.json"));
  }

  private static void stubLeapDay() {
    stubPage(wikimedia, "February 29", "synthetic/synthetic-02-29");
    stubPage(wikimedia, "2000", "synthetic/synthetic-2000");
    stubPageprops(wikimedia, EMPTY_PAGEPROPS);
  }

  @ParameterizedTest(name = "{0} reads only [{1}] and [{2}]")
  @CsvSource({
    "1976-07-17, July 17, 1976",
    "2000-02-29, February 29, 2000",
    "1995-03-15, March 15, 1995"
  })
  void onlyTheTwoPagesOfTheDateAreRequestedAndEveryEventIsOfThatDate(
      LocalDate date, String dateTitle, String yearTitle) {
    stubOpeningDay();
    stubLeapDay();
    stubRecorded(wikimedia, "March 15");
    stubPage(wikimedia, "1995", "synthetic/synthetic-1995");

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
  void openingDayKeepsTheOpeningOnceWithCanonicalLinksAndTheMainLinkItem() {
    stubOpeningDay();

    KoWikiDayResult result = find(OPENING);

    assertThat(result.docs())
        .extracting(KoWikiDocStatus::revision)
        .containsExactly(EnWikiPages.revid("07-17"), EnWikiPages.revid("1976"))
        .containsExactly(1375524665L, 1382023488L);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    KoWikiEvent opening = result.events().get(1);
    assertThat(opening.sourceTitle()).isEqualTo("July 17");
    assertThat(opening.sourceSection()).isEqualTo("Events");
    assertThat(opening.text()).contains("Summer Olympics in Montreal");
    assertThat(opening.linkTitles())
        .as("canonical titles: IOC is a redirect")
        .containsExactly(
            "1976 Summer Olympics",
            "Montreal",
            "New Zealand",
            "International Olympic Committee",
            "South Africa",
            "Apartheid");
    assertThat(opening.wikidataQid()).isEqualTo("Q8444");
    assertThat(opening.revision()).isEqualTo(1375524665L);
    assertThat(opening.retrievedAt()).isEqualTo(parts.clock.instant());
    assertThat(result.events())
        .as("demo 1976-07-17: the opening exactly once")
        .filteredOn(
            event ->
                event.text().contains("Summer Olympics")
                    || event.linkTitles().contains("1976 Summer Olympics"))
        .containsExactly(opening);
    assertThat(result.mergedCount()).isEqualTo(1);
    assertThat(result.events().getFirst())
        .satisfies(
            event -> {
              assertThat(event.linkTitles().getFirst()).isEqualTo("Timor-Leste");
              assertThat(event.wikidataQid()).as("East Timor redirects").isEqualTo("Q574");
            });
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(result.degraded()).isFalse();
    assertThat(pagepropsRequests(wikimedia))
        .singleElement()
        .satisfies(
            request -> {
              assertThat(param(request, "titles"))
                  .isEqualTo(
                      "East Timor|Indonesian invasion of East Timor|Provinces of Indonesia"
                          + "|Indonesia|1976 Summer Olympics|Montreal|New Zealand|IOC"
                          + "|South Africa|Apartheid|Indonesian occupation of East Timor");
              assertThat(request.getRequest().getHeader("User-Agent"))
                  .isEqualTo(KoWikiSourceFixture.USER_AGENT);
            });
  }

  @Test
  void quarterFinalAppearsOnceWithTheCupPageAndItsItem() {
    stubPage(wikimedia, "June 22", "synthetic/synthetic-06-22");
    stubPage(wikimedia, "2002", "synthetic/synthetic-2002");
    stubPageprops(wikimedia, read("synthetic/synthetic-pageprops-2002-06-22.json"));

    KoWikiDayResult result = find(QUARTER_FINAL);

    assertThat(result.events())
        .as("2002-06-22: the match exactly once, the year line links a redirect of the cup")
        .filteredOn(e -> e.text().contains("quarter-final"))
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.sourceTitle()).isEqualTo("June 22");
              assertThat(event.sourceSection()).isEqualTo("Events");
              assertThat(event.linkTitles()).containsExactly("2002 FIFA World Cup");
              assertThat(event.wikidataQid()).isEqualTo("Q47735");
            });
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void quarterFinalMergesByTitleAloneWhenTheLinkLookupFails() {
    stubPage(wikimedia, "June 22", "synthetic/synthetic-06-22");
    WikiPageStubs.stubPage(
        wikimedia,
        "2002",
        2002L,
        "==Events==\n* [[June 22]] – Test quarter-final B of the [[2002 FIFA World Cup]] is"
            + " decided on penalties.");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiDayResult result = find(QUARTER_FINAL);

    assertThat(result.linkResolutionFailed()).isTrue();
    assertThat(result.events())
        .as("the same title on both pages reads as one link")
        .filteredOn(e -> e.text().contains("quarter-final"))
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.text()).contains("test quarter-final A");
              assertThat(event.linkTitles()).containsExactly("2002 FIFA World Cup");
              assertThat(event.wikidataQid()).isNull();
            });
    assertThat(result.mergedCount()).isEqualTo(1);
  }

  @Test
  void redirectTitleNeedsTheLinkLookupToMerge() {
    stubPage(wikimedia, "June 22", "synthetic/synthetic-06-22");
    stubPage(wikimedia, "2002", "synthetic/synthetic-2002");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiDayResult result = find(QUARTER_FINAL);

    assertThat(result.linkResolutionFailed()).isTrue();
    assertThat(result.events())
        .as(
            "without the lookup, 2002 World Cup is not known to be 2002 FIFA World Cup (D4: shown twice)")
        .filteredOn(e -> e.text().contains("quarter-final"))
        .extracting(event -> event.linkTitles().getFirst())
        .containsExactly("2002 FIFA World Cup", "2002 World Cup");
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void quietDayHasNoEventsAndSendsNoLinkLookup() {
    stubRecorded(wikimedia, "March 15");
    stubPage(wikimedia, "1995", "synthetic/synthetic-1995");

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
            .withQueryParam("page", equalTo("February 29"))
            .willReturn(okJson(read("synthetic/synthetic-missingtitle.json"))));
    stubPage(wikimedia, "2000", "synthetic/synthetic-2000");

    KoWikiDayResult result = find(LEAP_DAY);

    assertThat(result.events()).extracting(KoWikiEvent::text).containsExactly("Test event C.");
    assertThat(result.events().getFirst().linkTitles()).isEmpty();
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(pagepropsRequests(wikimedia)).isEmpty();
    assertThat(result.linkResolutionFailed()).isFalse();
  }

  @Test
  void failedLinkLookupKeepsTheEventsWithoutItems() {
    stubRecorded(wikimedia, "July 17");
    stubRecorded(wikimedia, "1976");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(aResponse().withStatus(503)));

    KoWikiDayResult result = find(OPENING);
    List<String> expectedTexts = parsedTexts(OPENING, "07-17", "1976");

    assertThat(expectedTexts)
        .containsSubsequence(result.events().stream().map(KoWikiEvent::text).toList());
    assertThat(result.events()).extracting(KoWikiEvent::wikidataQid).containsOnlyNulls();
    assertThat(result.events().getFirst().linkTitles())
        .as("titles as written, not resolved")
        .containsExactly(
            "East Timor",
            "Indonesian invasion of East Timor",
            "Provinces of Indonesia",
            "Indonesia");
    assertThat(result.events())
        .as("titles alone still merge the opening")
        .filteredOn(event -> event.linkTitles().contains("1976 Summer Olympics"))
        .hasSize(1);
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
        .allMatch(id -> id.matches("^enwiki-\\d{4}-\\d{2}-\\d{2}-[0-9a-f]{8}$"));
    assertThat(first).allMatch(id -> id.startsWith("enwiki-1976-07-17-"));
    assertThat(second).isEqualTo(first);
    assertThat(TwoPageKoWikiEventSource.id(OPENING, "July 17", "Test event A"))
        .isEqualTo(TwoPageKoWikiEventSource.id(OPENING, "July 17", "Test event A"))
        .isNotEqualTo(TwoPageKoWikiEventSource.id(OPENING, "1976", "Test event A"))
        .isNotEqualTo(TwoPageKoWikiEventSource.id(LEAP_DAY, "July 17", "Test event A"));
  }

  @Test
  void idIsTheFirstEightHexDigitsOfTheSha256OfTitleAndText() throws Exception {
    byte[] hash =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest("February 29\nTest event A".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertThat(TwoPageKoWikiEventSource.id(LEAP_DAY, "February 29", "Test event A"))
        .isEqualTo("enwiki-2000-02-29-" + java.util.HexFormat.of().formatHex(hash, 0, 4));
  }

  @Test
  void onePageFailingLeavesTheOtherPagesEvents() {
    stubRecorded(wikimedia, "1976");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("July 17"))
            .willReturn(aResponse().withStatus(503)));
    stubPageprops(wikimedia, read("pageprops-1976-07-17.json"));

    KoWikiDayResult result = find(OPENING);

    assertThat(params(wikimedia, "page"))
        .as("a fast 503 is not a stall: the year page is still requested")
        .containsExactly("July 17", "July 17", "1976");
    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.UNAVAILABLE, State.FRESH);
    assertThat(result.docs().getFirst().revision()).isNull();
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.sourceTitle()).isEqualTo("1976"));
    assertThat(result.degraded()).isTrue();
    assertThat(result.allUnavailable()).isFalse();
  }

  @Test
  void yearPageFailingLeavesTheDatePageEvents() {
    stubRecorded(wikimedia, "July 17");
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo("1976"))
            .willReturn(aResponse().withStatus(503)));
    stubPageprops(wikimedia, read("pageprops-1976-07-17.json"));

    KoWikiDayResult result = find(OPENING);

    assertThat(result.docs())
        .extracting(KoWikiDocStatus::state)
        .containsExactly(State.FRESH, State.UNAVAILABLE);
    assertThat(result.events())
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.sourceTitle()).isEqualTo("July 17"));
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
            new String[] {"07-17-events.json", "DATE"},
            new String[] {"1976-events.json", "YEAR"})) {
      expectedSkipped +=
          parts
              .parser
              .parse(wikitext(section[0]), KoWikiPageKind.valueOf(section[1]), OPENING)
              .skippedLines();
    }
    assertThat(result.skippedLines()).isEqualTo(expectedSkipped);
    List<String> parsed = parsedTexts(OPENING, "07-17", "1976");
    assertThat(parsed)
        .as("kept texts are parser output, character for character, in input order")
        .containsSubsequence(result.events().stream().map(KoWikiEvent::text).toList());
    assertThat(result.events().size() + result.mergedCount()).isEqualTo(parsed.size());
  }

  @Test
  void leapDayEventsComeFromTheLeapDayPagesWithTheirRevisions() {
    stubLeapDay();

    KoWikiDayResult result = find(LEAP_DAY);

    assertThat(result.events())
        .extracting(KoWikiEvent::text, KoWikiEvent::sourceTitle, KoWikiEvent::revision)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Test event A opens.", "February 29", 100L),
            org.assertj.core.groups.Tuple.tuple("Test event B.", "February 29", 100L),
            org.assertj.core.groups.Tuple.tuple("Test event C.", "2000", 200L));
    assertThat(result.events().getFirst().linkTitles()).containsExactly("Test page A");
    assertThat(result.events().getFirst().wikidataQid()).isNull();
    assertThat(result.linkResolutionFailed()).isFalse();
  }

  /** Parser output for the two Events sections in input order, character for character. */
  private List<String> parsedTexts(LocalDate date, String datePrefix, String yearPrefix) {
    List<String> texts = new ArrayList<>();
    for (String[] section :
        List.of(
            new String[] {datePrefix + "-events.json", "DATE"},
            new String[] {yearPrefix + "-events.json", "YEAR"})) {
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
