package io.github.ericmschoi.theworldthatday.media.commons;

import static io.github.ericmschoi.theworldthatday.media.commons.CommonsVideoSourceTest.pages;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsVideoSourceTest.FakeWikimedia;
import io.github.ericmschoi.theworldthatday.media.commons.WikidataVideoLinks.Links;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat.Restored;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4c T2 (1) and (2) on the recorded 1969-07-20 answers (Apollo 11, a public historical
 * event; fixtures of M3a4b): the source hands each candidate its {@code MediaFilter} input, and the
 * candidates of all three paths come back equal from the stored text.
 */
class SearchCacheFormatRecordedTest {

  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final String E1 = "kowiki-1969-07-20-00000001";
  private static final String E2 = "kowiki-1969-07-20-00000002";
  private static final EventContext MOON =
      new EventContext(E1, MOON_DAY, "아폴로 11호", null, "지어낸 문장: 달에 내렸다.", "Q43653", "world");
  private static final EventContext NO_QID =
      new EventContext(E2, MOON_DAY, "시험 사건", null, "지어낸 문장이다.", null, "country:KR");

  private final MediaFilter filter =
      new MediaFilter(
          new MediaProperties(List.of(), List.of(), List.of(), List.of("Associated Press")));
  private final MediaNormalizer normalizer =
      new MediaNormalizer(new MediaProperties(List.of(), List.of()));
  private final CommonsFileMapper mapper =
      new CommonsFileMapper(
          filter,
          normalizer,
          new MutableClock(Instant.parse("2026-10-24T21:15:44Z"), ClockConfig.SERVICE_ZONE));
  private final SearchCacheFormat format = new SearchCacheFormat(filter, normalizer);

  private SourceResult recordedMoonDay() {
    FakeWikimedia wikimedia = new FakeWikimedia();
    wikimedia.links =
        Optional.of(
            Map.of(
                "Q43653", new Links(List.of("AP11 FINAL APPROACH.ogv"), "Apollo 11", "Apollo 11")));
    wikimedia.files = pages("p10-files-1969-07-20.json");
    wikimedia.categories.put("Apollo 11", pages("category-apollo-11.json"));
    wikimedia.day = pages("date-category-1969-07-20.json");
    CommonsVideoSource source =
        new CommonsVideoSource(
            wikimedia, wikimedia, mapper, new MediaLookupProperties(6, Duration.ofSeconds(3), 5));
    return source.find(MOON_DAY, List.of(MOON, NO_QID), false);
  }

  @Test
  void everyCandidateCarriesItsCommonsOriginWithTheFileNameWithoutFilePrefix() {
    List<Candidate> moon = recordedMoonDay().byEvent().get(E1);

    Candidate p10 = moon.stream().filter(c -> c.basis() == Basis.COMMONS_P10).findFirst().get();
    assertThat(p10.origin())
        .isEqualTo(
            new MediaOrigin(
                "commons",
                "AP11 FINAL APPROACH.ogv",
                "René and Jonathan Cantin / Apollo Lunar Surface Journal",
                "Apollo Lunar Surface Journal",
                null));
    assertThat(moon)
        .allSatisfy(
            c -> {
              assertThat(c.origin().source()).isEqualTo("commons");
              assertThat(c.origin().itemId()).isEqualTo(c.title()).doesNotStartWith("File:");
            });
  }

  @Test
  void allThreePathsComeBackEqualFromTheStoredText() {
    SourceResult result = recordedMoonDay();
    List<Candidate> moon = result.byEvent().get(E1);
    assertThat(moon)
        .extracting(Candidate::basis)
        .contains(Basis.COMMONS_P10, Basis.COMMONS_CATEGORY, Basis.COMMONS_DATE_CATEGORY);

    String text = format.write("Apollo 11", moon).orElseThrow();
    Optional<Restored> restored = format.read(text, MOON_DAY);

    assertThat(restored).contains(new Restored("Apollo 11", moon));
    List<Candidate> other = result.byEvent().get(E2);
    assertThat(format.read(format.write(null, other).orElseThrow(), MOON_DAY))
        .contains(new Restored(null, other));
  }

  @Test
  void theStoredTextHoldsAddressesAndTextOnly() {
    List<Candidate> moon = recordedMoonDay().byEvent().get(E1);

    String text = format.write("Apollo 11", moon).orElseThrow();

    assertThat(text)
        .startsWith("{\"version\":1,")
        .contains("\"url\":\"https://upload.wikimedia.org/")
        .doesNotContain("\"selection\"", "\"date_relation\"", "\"item\"", "kowiki-");
  }
}
