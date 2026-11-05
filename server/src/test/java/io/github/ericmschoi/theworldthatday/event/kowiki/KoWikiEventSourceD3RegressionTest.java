package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.EMPTY_PAGEPROPS;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.pagepropsRequests;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Plan M3a1c T5 (6) and plan C1 S3a T4 (D3m): the merge rules through the whole source, on
 * 2000-02-29 pages made of synthetic English Events sections ({@code synthetic/dedup/}), with the
 * real link lookup against WireMock. Under D3m the one-link line of the T3 repro merges the year
 * page line with the same main link (approved expectation change) but never a line of its own page;
 * the different-country-pair case keeps two events (D3 (iii) place conflict); a control with a
 * shared country still merges.
 */
class KoWikiEventSourceD3RegressionTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  /** The Events sections of the date page and the year page, both FRESH. */
  private KoWikiDayResult find(String dateEvents, String yearEvents) {
    stubPageprops(wikimedia, EMPTY_PAGEPROPS);
    KoWikiClient pages =
        title ->
            title.equals("February 29")
                ? new KoWikiPage(title, State.FRESH, 100L, dateEvents, null, READ)
                : new KoWikiPage(title, State.FRESH, 200L, yearEvents, null, READ);
    return parts.source(pages).find(LEAP_DAY);
  }

  private static String fixture(String name) {
    return KoWikiFixtures.read("synthetic/dedup/synthetic-" + name + ".wikitext");
  }

  @Test
  void oneLinkLineMergesTheYearLineWithTheSameMainLink() {
    KoWikiDayResult result = find(fixture("one-link-date"), fixture("one-link-year"));

    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::sourceSection, KoWikiEvent::linkTitles)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                "February 29", "Events", java.util.List.of("2002 Test Cup")));
    assertThat(result.events().getFirst().text()).isEqualTo("2002 Test Cup test match A is held.");
    assertThat(result.mergedCount()).isEqualTo(1);
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(pagepropsRequests(wikimedia)).hasSize(1);
  }

  @Test
  void oneLinkLineNeverAbsorbsALineOfItsOwnPage() {
    String yearLine =
        fixture("one-link-year").replace("* [[February 29]] –", "*[[2000]] &ndash;").strip();
    KoWikiDayResult result =
        find(fixture("one-link-date") + yearLine.replace("==Events==", ""), "==Events==\n");

    assertThat(result.events())
        .extracting(KoWikiEvent::linkTitles)
        .containsExactly(
            java.util.List.of("2002 Test Cup"),
            java.util.List.of("2002 Test Cup", "Test team C", "Test team D"));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void differentCountryPairsOfTheSameTournamentStayTwoEvents() {
    KoWikiDayResult result = find(fixture("country-pair-date"), fixture("country-pair-year"));

    assertThat(result.events())
        .extracting(KoWikiEvent::linkTitles)
        .containsExactly(
            java.util.List.of("2002 Test Cup", "South Korea", "Spain"),
            java.util.List.of("2002 Test Cup", "Germany", "United States"));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void controlWithASharedCountryStillMergesThroughTheSource() {
    KoWikiDayResult result =
        find(
            fixture("country-pair-date"),
            "==Events==\n* [[February 29]] – [[2002 Test Cup]] test match E ([[Spain]]).\n");

    assertThat(result.events())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.sourceTitle()).isEqualTo("February 29");
              assertThat(event.text())
                  .isEqualTo("2002 Test Cup test match C (South Korea, Spain).");
            });
    assertThat(result.mergedCount()).isEqualTo(1);
  }
}
