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
 * Plan M3a1c T1 (2): events come in input order (date page 사건 → 문화 → year page 사건 → 문화), with
 * synthetic pages served by an in-memory client and resolver (no HTTP at all).
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
            case "2월 29일" ->
                new KoWikiPage(
                    title,
                    State.FRESH,
                    100L,
                    "== 사건 ==\n* [[2000년]] - 시험 사건 가1 [[시험_문서 가]].\n"
                        + "* [[1999년]] - 다른 해.\n* [[2000년]] - 시험 사건 가2.",
                    "== 문화 ==\n* [[2000년]] - 시험 사건 나 [[시험 문서 나|별칭]] [[시험 문서 마]].\n" + "* {{틀}} 건너뜀",
                    DATE_READ);
            case "2000년" ->
                new KoWikiPage(
                    title,
                    State.CACHED,
                    200L,
                    "== 사건 ==\n* [[2월 29일]] - 시험 사건 다 [[시험 문서 다]].",
                    "== 문화 ==\n* [[2월 29일]]\n** 시험 사건 라.\n** [http://example.test 외부]",
                    YEAR_READ);
            default -> throw new AssertionError("unexpected page " + title);
          };

  private final KoWikiLinkResolver resolver =
      titles -> {
        lookups.add(titles);
        return Optional.of(
            Map.of(
                "시험 문서 가", new Link("시험 문서 가", "Q1"),
                "시험 문서 나", new Link("시험 문서 마", "Q2"),
                "시험 문서 마", new Link("시험 문서 마", "Q2"),
                "시험 문서 다", new Link("시험 문서 다", null)));
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
            "시험 사건 가1 시험 문서 가.",
            "시험 사건 가2.",
            "시험 사건 나 별칭 시험 문서 마.",
            "시험 사건 다 시험 문서 다.",
            "시험 사건 라.");
    assertThat(result.events())
        .extracting(KoWikiEvent::sourceTitle, KoWikiEvent::sourceSection)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("2월 29일", "사건"),
            org.assertj.core.groups.Tuple.tuple("2월 29일", "사건"),
            org.assertj.core.groups.Tuple.tuple("2월 29일", "문화"),
            org.assertj.core.groups.Tuple.tuple("2000년", "사건"),
            org.assertj.core.groups.Tuple.tuple("2000년", "문화"));
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
            new KoWikiDocStatus("2월 29일", 100L, DATE_READ, State.FRESH),
            new KoWikiDocStatus("2000년", 200L, YEAR_READ, State.CACHED));
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

    assertThat(lookups).containsExactly(List.of("시험 문서 가", "시험 문서 나", "시험 문서 마", "시험 문서 다"));
    assertThat(result.events().get(0).linkTitles()).containsExactly("시험 문서 가");
    assertThat(result.events().get(2).linkTitles())
        .as("시험 문서 나 resolves to the same page as 시험 문서 마")
        .containsExactly("시험 문서 마");
    assertThat(result.events().get(2).wikidataQid()).isEqualTo("Q2");
    assertThat(result.events().get(3).wikidataQid()).isNull();
    assertThat(result.events().get(4).linkTitles()).isEmpty();
    assertThat(result.events().get(4).wikidataQid()).isNull();
  }
}
