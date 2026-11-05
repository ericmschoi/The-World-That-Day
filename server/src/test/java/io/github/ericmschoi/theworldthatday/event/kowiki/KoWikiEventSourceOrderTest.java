package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a1c T1 (2) and plan C1 S3a T2: events come in input order (date page Events → year page
 * Events), with synthetic English pages served by an in-memory client and resolver (no HTTP).
 */
class KoWikiEventSourceOrderTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Instant DATE_READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant YEAR_READ = Instant.parse("2025-12-30T00:00:00Z");

  private final WikimediaProperties properties =
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
  private final WikimediaHttpClient http =
      new WikimediaHttpClient(properties, new MutableClock(DATE_READ, ClockConfig.SERVICE_ZONE));
  private final List<List<String>> lookups = new ArrayList<>();

  private final KoWikiClient pages =
      title ->
          switch (title) {
            case "February 29" ->
                new KoWikiPage(
                    title,
                    State.FRESH,
                    100L,
                    "==Events==\n*[[2000]] &ndash; Test event A1 [[Test_page A]].\n"
                        + "*[[1999]] &ndash; Another year.\n*[[2000]] &ndash; Test event A2.\n"
                        + "*[[2000]] &ndash; Test event B [[Test page B|alias]] [[Test page E]].\n"
                        + "* {{Template}} skipped",
                    null,
                    DATE_READ);
            case "2000" ->
                new KoWikiPage(
                    title,
                    State.CACHED,
                    200L,
                    "==Events==\n* [[February 29]] – Test event C [[Test page C]].\n"
                        + "* [[February 29]]\n** Test event D.\n** [http://example.test external]",
                    null,
                    YEAR_READ);
            default -> throw new AssertionError("unexpected page " + title);
          };

  private final KoWikiLinkResolver resolver =
      titles -> {
        lookups.add(titles);
        return Optional.of(
            Map.of(
                "Test page A", new Link("Test page A", "Q1"),
                "Test page B", new Link("Test page E", "Q2"),
                "Test page E", new Link("Test page E", "Q2"),
                "Test page C", new Link("Test page C", null)));
      };

  @Test
  void eventsKeepInputOrderAcrossBothPagesAndSections() {
    KoWikiDayResult result =
        new TwoPageKoWikiEventSource(
                pages,
                resolver,
                new WikitextEventParser(),
                http,
                properties,
                new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6)))
            .find(LEAP_DAY);

    assertThat(result.events())
        .extracting(KoWikiEvent::text)
        .containsExactly(
            "Test event A1 Test page A.",
            "Test event A2.",
            "Test event B alias Test page E.",
            "Test event C Test page C.",
            "Test event D.");
    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::sourceSection)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("February 29", "Events"),
            org.assertj.core.groups.Tuple.tuple("February 29", "Events"),
            org.assertj.core.groups.Tuple.tuple("February 29", "Events"),
            org.assertj.core.groups.Tuple.tuple("2000", "Events"),
            org.assertj.core.groups.Tuple.tuple("2000", "Events"));
    assertThat(result.events())
        .extracting(KoWikiEvent::revision, KoWikiEvent::retrievedAt)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(100L, DATE_READ),
            org.assertj.core.groups.Tuple.tuple(100L, DATE_READ),
            org.assertj.core.groups.Tuple.tuple(100L, DATE_READ),
            org.assertj.core.groups.Tuple.tuple(200L, YEAR_READ),
            org.assertj.core.groups.Tuple.tuple(200L, YEAR_READ));
    assertThat(result.docs())
        .containsExactly(
            new KoWikiDocStatus("February 29", 100L, DATE_READ, State.FRESH),
            new KoWikiDocStatus("2000", 200L, YEAR_READ, State.CACHED));
    assertThat(result.skippedLines()).as("one template line + one external link line").isEqualTo(2);
    assertThat(result.degraded()).as("CACHED is not degraded").isFalse();
  }

  @Test
  void linksAreNormalizedOnceAndResolvedToCanonicalTitlesWithoutDuplicates() {
    KoWikiDayResult result =
        new TwoPageKoWikiEventSource(
                pages,
                resolver,
                new WikitextEventParser(),
                http,
                properties,
                new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6)))
            .find(LEAP_DAY);

    assertThat(lookups)
        .containsExactly(List.of("Test page A", "Test page B", "Test page E", "Test page C"));
    assertThat(result.events().get(0).linkTitles()).containsExactly("Test page A");
    assertThat(result.events().get(2).linkTitles())
        .as("Test page B resolves to the same page as Test page E")
        .containsExactly("Test page E");
    assertThat(result.events().get(2).wikidataQid()).isEqualTo("Q2");
    assertThat(result.events().get(3).wikidataQid()).isNull();
    assertThat(result.events().get(4).linkTitles()).isEmpty();
    assertThat(result.events().get(4).wikidataQid()).isNull();
  }
}
