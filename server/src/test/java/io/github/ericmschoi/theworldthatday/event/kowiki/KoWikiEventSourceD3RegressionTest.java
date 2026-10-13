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
 * Plan M3a1c T5 (6): the revised rule D3 (T4) through the whole source, on 2000-02-29 pages made of
 * synthetic culture sections ({@code synthetic/dedup/}), with the real link lookup against
 * WireMock. The T3 repro (a one-link line no longer absorbs a different line of the same
 * tournament) and the different-country-pair case each keep two events; a control with a shared
 * country still merges.
 */
class KoWikiEventSourceD3RegressionTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  /** Date page 문화 and year page 문화 only (sections 1 and 3), both FRESH. */
  private KoWikiDayResult find(String dateCulture, String yearCulture) {
    stubPageprops(wikimedia, EMPTY_PAGEPROPS);
    KoWikiClient pages =
        title ->
            title.equals("2월 29일")
                ? new KoWikiPage(title, State.FRESH, 100L, null, dateCulture, READ)
                : new KoWikiPage(title, State.FRESH, 200L, null, yearCulture, READ);
    return parts.source(pages).find(LEAP_DAY);
  }

  private static String fixture(String name) {
    return KoWikiFixtures.read("synthetic/dedup/synthetic-" + name + ".wikitext");
  }

  @Test
  void oneLinkLineNoLongerAbsorbsAnotherLineOfTheSameTournament() {
    KoWikiDayResult result =
        find(fixture("one-link-date-culture"), fixture("one-link-year-culture"));

    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::sourceSection, KoWikiEvent::linkTitles)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("2월 29일", "문화", java.util.List.of("2002 시험 대회")),
            org.assertj.core.groups.Tuple.tuple(
                "2000년", "문화", java.util.List.of("2002년 시험 대회", "시험 팀 다", "시험 팀 라")));
    assertThat(result.mergedCount()).isZero();
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(pagepropsRequests(wikimedia)).hasSize(1);
  }

  @Test
  void differentCountryPairsOfTheSameTournamentStayTwoEvents() {
    KoWikiDayResult result =
        find(fixture("country-pair-date-culture"), fixture("country-pair-year-culture"));

    assertThat(result.events())
        .extracting(KoWikiEvent::linkTitles)
        .containsExactly(
            java.util.List.of("2002 시험 대회", "대한민국", "스페인"),
            java.util.List.of("2002년 시험 대회", "독일", "미국"));
    assertThat(result.mergedCount()).isZero();
  }

  @Test
  void controlWithASharedCountryStillMergesThroughTheSource() {
    KoWikiDayResult result =
        find(
            fixture("country-pair-date-culture"),
            "== 문화 ==\n* [[2월 29일]] - [[2002년 시험 대회]] 시험 경기 다 ([[스페인]]).\n");

    assertThat(result.events())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.sourceTitle()).isEqualTo("2월 29일");
              assertThat(event.text()).isEqualTo("2002 시험 대회 시험 경기 다 (대한민국, 스페인).");
            });
    assertThat(result.mergedCount()).isEqualTo(1);
  }
}
