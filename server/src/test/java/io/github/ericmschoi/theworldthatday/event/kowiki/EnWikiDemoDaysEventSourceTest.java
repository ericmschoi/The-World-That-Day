package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.params;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubPageprops;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.stubRecorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Plan C1 S3a T3 (2) and T4 (3) with D3m: the recorded English date and year pages of the four demo
 * dates and the video sample 1969-07-20 through the whole source (real HTTP client, parser, link
 * lookup and deduplication against WireMock). The rows are the S3a column of the plan's demo table;
 * tone and scope are T5's and the world story's.
 */
class EnWikiDemoDaysEventSourceTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  private KoWikiDayResult find(String date, String dateTitle, String yearTitle) {
    stubRecorded(wikimedia, dateTitle);
    stubRecorded(wikimedia, yearTitle);
    stubPageprops(wikimedia, read("pageprops-" + date + ".json"));
    KoWikiDayResult result = parts.source().find(LocalDate.parse(date));
    assertThat(params(wikimedia, "page")).containsExactly(dateTitle, yearTitle);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(result.linkResolutionFailed()).isFalse();
    assertThat(result.degraded()).isFalse();
    assertThat(result.events())
        .allSatisfy(event -> assertThat(event.date()).isEqualTo(LocalDate.parse(date)))
        .allSatisfy(event -> assertThat(event.sourceSection()).isEqualTo("Events"));
    return result;
  }

  @Test
  void calgaryOpeningIsOneRangeStartFromTheYearPage() {
    KoWikiDayResult result = find("1988-02-13", "February 13", "1988");

    assertThat(result.events())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.text())
                  .isEqualTo("The 1988 Winter Olympics are held in Calgary, Alberta, Canada.");
              assertThat(event.sourceTitle()).isEqualTo("1988");
              assertThat(event.dateKind()).isEqualTo(DateKind.RANGE_START);
              assertThat(event.rangeEnd()).isEqualTo(LocalDate.of(1988, 2, 28));
              assertThat(event.linkTitles())
                  .containsExactly("1988 Winter Olympics", "Calgary", "Alberta");
              assertThat(event.wikidataQid()).isEqualTo("Q9658");
              assertThat(event.id()).isEqualTo("enwiki-1988-02-13-d716c4f7");
            });
    assertThat(result.mergedCount()).isZero();
    assertThat(result.skippedLines()).isEqualTo(5);
  }

  @Test
  void montrealOpeningMergesByItsMainLinkAndEastTimorStaysTwice() {
    KoWikiDayResult result = find("1976-07-17", "July 17", "1976");

    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::wikidataQid, KoWikiEvent::dateKind)
        .containsExactly(
            tuple("July 17", "Q574", DateKind.DAY),
            tuple("July 17", "Q8444", DateKind.DAY),
            tuple("1976", "Q574", DateKind.DAY));
    assertThat(result.events())
        .as("the opening once: the year page line has the same main link (D3m)")
        .filteredOn(event -> event.linkTitles().contains("1976 Summer Olympics"))
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.text()).startsWith("The opening of the Summer Olympics in Montreal");
              assertThat(event.linkTitles().getFirst()).isEqualTo("1976 Summer Olympics");
            });
    assertThat(result.events())
        .as("East Timor twice: its main link is a country, a generic link (no D3m)")
        .filteredOn(event -> event.linkTitles().getFirst().equals("Timor-Leste"))
        .extracting(KoWikiEvent::text)
        .containsExactly(
            "East Timor is annexed and becomes the 27th province of Indonesia.",
            "East Timor is declared the 27th province of Indonesia.");
    assertThat(result.events()).extracting(KoWikiEvent::rangeEnd).containsOnlyNulls();
    assertThat(result.mergedCount()).isEqualTo(1);
    assertThat(result.skippedLines()).isEqualTo(7);
    assertThat(params(wikimedia, "titles"))
        .as("the lookup asks for the README titles of pageprops-1976-07-17.json")
        .containsExactly(
            "East Timor|Indonesian invasion of East Timor|Provinces of Indonesia|Indonesia"
                + "|1976 Summer Olympics|Montreal|New Zealand|IOC|South Africa|Apartheid"
                + "|Indonesian occupation of East Timor");
  }

  @Test
  void vancouverOpeningIsOneRangeStartFromTheYearPage() {
    KoWikiDayResult result = find("2010-02-12", "February 12", "2010");

    assertThat(result.events())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.text())
                  .isEqualTo(
                      "The 2010 Winter Olympics are held in Vancouver and Whistler, Canada.");
              assertThat(event.sourceTitle()).isEqualTo("2010");
              assertThat(event.dateKind()).isEqualTo(DateKind.RANGE_START);
              assertThat(event.rangeEnd()).isEqualTo(LocalDate.of(2010, 2, 28));
              assertThat(event.wikidataQid()).isEqualTo("Q9674");
            });
    assertThat(result.mergedCount()).isZero();
    assertThat(result.skippedLines()).isEqualTo(2);
  }

  @Test
  void liberianCoupIsOnceNextToTheCrashAndTerryFox() {
    KoWikiDayResult result = find("1980-04-12", "April 12", "1980");

    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::wikidataQid)
        .containsExactly(
            tuple("April 12", "Q27628137"),
            tuple("April 12", "Q7833816"),
            tuple("April 12", "Q295053"));
    assertThat(result.events().getFirst().text())
        .isEqualTo("The Americo-Liberian government of Liberia is violently deposed.");
    assertThat(result.events())
        .extracting(event -> event.linkTitles().getFirst())
        .containsExactly("1980 Liberian coup d'état", "Transbrasil Flight 303", "Terry Fox");
    assertThat(result.mergedCount()).as("both year page lines merged").isEqualTo(2);
    assertThat(result.skippedLines()).isZero();
  }

  @Test
  void moonLandingIsOnceNextToTheCeaseFireAndTheTour() {
    KoWikiDayResult result = find("1969-07-20", "July 20", "1969");

    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::wikidataQid)
        .containsExactly(
            tuple("July 20", "Q46611"), tuple("July 20", "Q783"), tuple("1969", "Q836884"));
    assertThat(result.events())
        .filteredOn(event -> event.linkTitles().contains("Apollo 11"))
        .as("the landing once")
        .singleElement()
        .satisfies(
            event -> assertThat(event.text()).startsWith("Apollo program: Apollo 11's crew"));
    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .anySatisfy(text -> assertThat(text).contains("cease fire"))
        .anySatisfy(text -> assertThat(text).startsWith("1969 Tour de France:"));
    assertThat(result.mergedCount()).isEqualTo(1);
    assertThat(result.skippedLines()).isEqualTo(6);
  }

  @Test
  void everyDemoDayHasUniqueEnwikiIds() {
    for (List<String> day :
        List.of(
            List.of("1988-02-13", "February 13", "1988"),
            List.of("1976-07-17", "July 17", "1976"),
            List.of("2010-02-12", "February 12", "2010"),
            List.of("1980-04-12", "April 12", "1980"),
            List.of("1969-07-20", "July 20", "1969"))) {
      wikimedia.resetAll();
      KoWikiDayResult result = find(day.get(0), day.get(1), day.get(2));

      assertThat(result.events())
          .isNotEmpty()
          .extracting(KoWikiEvent::id)
          .doesNotHaveDuplicates()
          .allMatch(id -> id.startsWith("enwiki-" + day.get(0) + "-"))
          .allMatch(id -> id.matches("^enwiki-\\d{4}-\\d{2}-\\d{2}-[0-9a-f]{8}$"));
    }
  }
}
