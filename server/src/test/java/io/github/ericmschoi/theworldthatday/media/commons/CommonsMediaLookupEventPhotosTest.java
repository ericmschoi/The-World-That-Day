package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.DayMedia;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.WorldMedia;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.pageimages.ActionApiKoWikiPageImages;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3d T1 (3), (6), (7), (8), (9) and (15) on the lookup itself: event photos through the
 * real PageImages and Commons adapters against WireMock, the ±5-year rule, duplicates, the day
 * photos without the event photos, failures and the one shared deadline. Requested date 2000-02-29
 * (a leap day boundary). Article titles, file names and authors are synthetic.
 */
@ExtendWith(OutputCaptureExtension.class)
class CommonsMediaLookupEventPhotosTest {

  @RegisterExtension
  static final WireMockExtension wiki =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String KOWIKI = WikimediaWireMockTest.KOWIKI_PATH;
  private static final String COMMONS = WikimediaWireMockTest.COMMONS_PATH;
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final Duration DEADLINE = Duration.ofMillis(600);
  private static final Duration SILENT = Duration.ofSeconds(4);

  /** The plan's media window bound: lookup-deadline + CANCEL_GRACE + 1s of slack. */
  private static final Duration WINDOW =
      DEADLINE.plus(Deadline.CANCEL_GRACE).plus(Duration.ofSeconds(1));

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);

  private CommonsMediaLookup lookup(int dayLimit, int maxYearsApart) {
    WikimediaProperties shared =
        new WikimediaProperties(
            WikimediaWireMockTest.TEST_USER_AGENT,
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            Duration.ofSeconds(5),
            URI.create(wiki.baseUrl() + KOWIKI));
    WikimediaHttpClient http = new WikimediaHttpClient(shared, clock);
    JsonMapper json = JsonMapper.builder().build();
    MediaProperties media = new MediaProperties(List.of(), List.of());
    return new CommonsMediaLookup(
        new ActionApiCommonsClient(
            http, new CommonsProperties(URI.create(wiki.baseUrl() + COMMONS)), json),
        new CommonsFileMapper(new MediaFilter(media), new MediaNormalizer(media), clock),
        new ActionApiKoWikiPageImages(http, shared, json),
        new MediaLookupProperties(dayLimit, DEADLINE, maxYearsApart));
  }

  private CommonsMediaLookup lookup() {
    return lookup(6, 5);
  }

  /** A day without a Commons category unless a test stubs one. */
  @BeforeEach
  void emptyDayCategory() {
    dayCategory(okJson("{\"batchcomplete\":true}"));
  }

  // ---- synthetic answers ----

  private static String page(long id, String fileName, String taken) {
    return """
        {"pageid": %d, "ns": 6, "title": "File:%s", "imageinfo": [{
          "url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/S%d.jpg",
          "descriptionurl": "https://commons.wikimedia.org/wiki/File:S%d.jpg",
          "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/S%d.jpg/640px-S%d.jpg",
          "thumbwidth": 640, "thumbheight": 480, "mime": "image/jpeg",
          "extmetadata": {"Artist": {"value": "Synthetic Author"},
            "LicenseShortName": {"value": "CC0"}, "DateTimeOriginal": {"value": "%s"}}}]}
        """
        .formatted(id, fileName, id, id, id, id, taken);
  }

  private static String pages(List<String> pages) {
    return "{\"batchcomplete\":true,\"query\":{\"pages\":[" + String.join(",", pages) + "]}}";
  }

  /** PageImages: each {@code title=file} pair is a free lead image. */
  private static void leadImages(ResponseDefinitionBuilder response) {
    wiki.stubFor(
        get(urlPathEqualTo(KOWIKI))
            .withQueryParam("prop", equalTo("pageimages"))
            .willReturn(response));
  }

  private static void leadImages(String... titleEqualsFile) {
    List<String> found = new ArrayList<>();
    for (String pair : titleEqualsFile) {
      String[] parts = pair.split("=", 2);
      found.add(
          "{\"pageid\":1,\"ns\":0,\"title\":\"%s\",\"pageimage\":\"%s\"}"
              .formatted(parts[0], parts[1]));
    }
    leadImages(okJson(pages(found)));
  }

  private static void fileBatch(ResponseDefinitionBuilder response) {
    wiki.stubFor(
        get(urlPathEqualTo(COMMONS)).withQueryParam("titles", matching(".+")).willReturn(response));
  }

  private static void dayCategory(ResponseDefinitionBuilder response) {
    wiki.stubFor(
        get(urlPathEqualTo(COMMONS))
            .withQueryParam("generator", equalTo("categorymembers"))
            .willReturn(response));
  }

  /** Seven passing same-day photos, ids commons-101 to commons-107, titles in that order. */
  private static List<String> sevenDayPhotos() {
    return IntStream.rangeClosed(1, 7)
        .mapToObj(i -> page(100 + i, "Synthetic day " + i + ".jpg", "2000-02-29"))
        .toList();
  }

  private static EventTarget target(String id, String title) {
    return new EventTarget(id, title, "country:KR");
  }

  private static List<ServeEvent> served(String path) {
    return wiki.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(path + "?"))
        .toList();
  }

  private static long fileBatchRequests() {
    return served(COMMONS).stream()
        .filter(event -> event.getRequest().queryParameter("titles").isPresent())
        .count();
  }

  private static long dayCategoryRequests() {
    return served(COMMONS).stream()
        .filter(event -> event.getRequest().queryParameter("generator").isPresent())
        .count();
  }

  private static List<String> askedTitles() {
    return served(KOWIKI).stream()
        .map(event -> event.getRequest().queryParameter("titles").firstValue())
        .flatMap(titles -> Arrays.stream(titles.split("\\|")))
        .toList();
  }

  private static List<String> ids(List<MediaItem> items) {
    return items.stream().map(item -> item.record().id()).toList();
  }

  private static Map<String, String> photoIds(WorldMedia media) {
    return media.eventMedia().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFirst().record().id()));
  }

  private static List<String> failures(CapturedOutput output) {
    return output
        .getAll()
        .lines()
        .filter(l -> l.contains("Wikimedia call failed"))
        .map(l -> l.substring(l.indexOf("Wikimedia call failed")))
        .toList();
  }

  /** Five events whose lead images were taken in 1994, 1995, 2005, 2006 and at an unknown time. */
  private static List<EventTarget> fiveYearTargets() {
    leadImages(
        "시험 문서 1994=Synthetic 1994.jpg",
        "시험 문서 1995=Synthetic 1995.jpg",
        "시험 문서 2000=Synthetic 2000.jpg",
        "시험 문서 2005=Synthetic 2005.jpg",
        "시험 문서 2006=Synthetic 2006.jpg",
        "시험 문서 미상=Synthetic unknown.jpg");
    fileBatch(
        okJson(
            pages(
                List.of(
                    page(1994, "Synthetic 1994.jpg", "1994"),
                    page(1995, "Synthetic 1995.jpg", "1995"),
                    page(2000, "Synthetic 2000.jpg", "2000"),
                    page(2005, "Synthetic 2005.jpg", "2005"),
                    page(2006, "Synthetic 2006.jpg", "2006"),
                    page(9999, "Synthetic unknown.jpg", "")))));
    return List.of(
        target("e1994", "시험 문서 1994"),
        target("e1995", "시험 문서 1995"),
        target("e2000", "시험 문서 2000"),
        target("e2005", "시험 문서 2005"),
        target("e2006", "시험 문서 2006"),
        target("eunknown", "시험 문서 미상"));
  }

  // ---- T1 (3): ±5 years ----

  @Test
  void photosAtMostFiveYearsApartAndUnknownOnesAreKept(CapturedOutput output) {
    List<EventTarget> targets = fiveYearTargets();
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CommonsMediaLookup.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    WorldMedia media;
    try {
      media = lookup().lookup(LEAP_DAY, targets, false);
    } finally {
      logger.setLevel(before);
    }

    assertThat(media.failed()).isFalse();
    assertThat(photoIds(media))
        .containsOnlyKeys("e1995", "e2000", "e2005", "eunknown")
        .containsEntry("e1995", "commons-1995")
        .containsEntry("e2005", "commons-2005");
    assertThat(media.eventMedia().get("e1995").getFirst().dateRelation())
        .isEqualTo(new DateRelation(Kind.YEARS_APART, -5));
    assertThat(media.eventMedia().get("e2005").getFirst().dateRelation())
        .isEqualTo(new DateRelation(Kind.YEARS_APART, 5));
    MediaItem unknown = media.eventMedia().get("eunknown").getFirst();
    assertThat(unknown.dateRelation().kind()).isEqualTo(Kind.UNKNOWN);
    assertThat(unknown.record().capturedAt()).as("never the requested date").isNull();
    assertThat(media.eventMedia().values()).allSatisfy(list -> assertThat(list).hasSize(1));
    assertThat(output.getAll())
        .contains("Event photos dropped: {YEARS=2}")
        .doesNotContain("시험 문서", "Synthetic", "2000-02-29", "File:");
  }

  @Test
  void maxYearsApartZeroKeepsTheSameYearAndUnknownOnly() {
    WorldMedia media = lookup(6, 0).lookup(LEAP_DAY, fiveYearTargets(), false);

    assertThat(photoIds(media)).containsOnlyKeys("e2000", "eunknown");
    assertThat(media.eventMedia().get("e2000").getFirst().dateRelation())
        .isEqualTo(new DateRelation(Kind.SAME_YEAR, 0));
  }

  // ---- placing, JPEG, display ----

  @Test
  void photoIsShownAsAnImageAtTheEventsPlace() {
    leadImages("시험 문서 세계=Synthetic world.jpg", "시험 문서 미상=Synthetic place.jpg");
    fileBatch(
        okJson(
            pages(
                List.of(
                    page(11, "Synthetic world.jpg", "2000-02-29"),
                    page(12, "Synthetic place.jpg", "2000-02")))));

    WorldMedia media =
        lookup()
            .lookup(
                LEAP_DAY,
                List.of(
                    new EventTarget("w", "시험 문서 세계", "world"),
                    new EventTarget("u", "시험 문서 미상", "unknown")),
                true);

    MediaItem world = media.eventMedia().get("w").getFirst();
    assertThat(world.record().placeRef()).isEqualTo("world");
    assertThat(world.record().displayMode().name()).isEqualTo("IMAGE");
    assertThat(world.dateRelation().kind()).isEqualTo(Kind.SAME_DAY);
    assertThat(media.eventMedia().get("u").getFirst().record().placeRef()).isEqualTo("unknown");
    assertThat(media.eventMedia().get("u").getFirst().dateRelation().kind())
        .isEqualTo(Kind.SAME_MONTH);
  }

  @Test
  void svgOrMissingLeadImageIsDroppedWithoutAFailure() {
    leadImages("시험 문서 로고=Synthetic logo.svg", "시험 문서 없음=Synthetic local.jpg");
    fileBatch(
        okJson(
            pages(
                List.of(
                    page(21, "Synthetic logo.svg", "2000").replace("image/jpeg", "image/svg+xml"),
                    "{\"ns\":6,\"title\":\"File:Synthetic local.jpg\",\"missing\":true}"))));

    WorldMedia media =
        lookup().lookup(LEAP_DAY, List.of(target("a", "시험 문서 로고"), target("b", "시험 문서 없음")), true);

    assertThat(media.eventMedia()).isEmpty();
    assertThat(media.failed()).isFalse();
  }

  // ---- T1 (6): duplicates and the day photos ----

  @Test
  void sameFileOnTwoEventsGoesToTheFirstOnly() {
    leadImages("시험 문서 가=Synthetic shared.jpg", "시험 문서 나=Synthetic shared.jpg");
    fileBatch(okJson(pages(List.of(page(31, "Synthetic shared.jpg", "2000")))));

    WorldMedia media =
        lookup()
            .lookup(
                LEAP_DAY, List.of(target("first", "시험 문서 가"), target("second", "시험 문서 나")), true);

    assertThat(photoIds(media)).containsExactlyEntriesOf(Map.of("first", "commons-31"));
    assertThat(fileBatchRequests()).isEqualTo(1);
    assertThat(served(COMMONS).getFirst().getRequest().queryParameter("titles").firstValue())
        .isEqualTo("File:Synthetic shared.jpg");
  }

  @Test
  void eventPhotoLeavesTheDayPhotosBeforeTheyAreCut() {
    dayCategory(okJson(pages(sevenDayPhotos())));
    leadImages("시험 문서 가=Synthetic day 3.jpg");
    fileBatch(okJson(pages(List.of(page(103, "Synthetic day 3.jpg", "2000-02-29")))));

    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);

    assertThat(photoIds(media)).containsExactlyEntriesOf(Map.of("e", "commons-103"));
    assertThat(ids(media.dayMedia()))
        .containsExactly(
            "commons-101",
            "commons-102",
            "commons-104",
            "commons-105",
            "commons-106",
            "commons-107");
    assertThat(media.failed()).isFalse();
  }

  @Test
  void failedEventPathLeavesTheDayPhotosAsBefore() {
    dayCategory(okJson(pages(sevenDayPhotos())));
    leadImages(aResponse().withStatus(503));

    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);
    wiki.resetRequests();
    DayMedia dayOnly = lookup().dayMedia(LEAP_DAY, false);

    assertThat(media.eventMedia()).isEmpty();
    assertThat(media.failed()).isTrue();
    assertThat(ids(media.dayMedia()))
        .isEqualTo(ids(dayOnly.items()))
        .containsExactly(
            "commons-101",
            "commons-102",
            "commons-103",
            "commons-104",
            "commons-105",
            "commons-106");
    assertThat(dayOnly.failed()).isFalse();
    assertThat(served(KOWIKI)).as("dayMedia asks no PageImages").isEmpty();
  }

  @Test
  void dayMediaLimitZeroOrATragicDayStillFindsEventPhotos() {
    dayCategory(okJson(pages(sevenDayPhotos())));
    leadImages("시험 문서 가=Synthetic event.jpg");
    fileBatch(okJson(pages(List.of(page(41, "Synthetic event.jpg", "2000")))));

    WorldMedia off = lookup(0, 5).lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);
    WorldMedia tragic = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), true);

    assertThat(photoIds(off)).containsExactlyEntriesOf(Map.of("e", "commons-41"));
    assertThat(photoIds(tragic)).containsExactlyEntriesOf(Map.of("e", "commons-41"));
    assertThat(off.dayMedia()).isEmpty();
    assertThat(tragic.dayMedia()).isEmpty();
    assertThat(dayCategoryRequests()).isZero();
  }

  // ---- T1 (7): failures ----

  static Stream<Arguments> eventPathFailures() {
    ResponseDefinitionBuilder lead =
        okJson(pages(List.of("{\"title\":\"시험 문서 가\",\"pageimage\":\"Synthetic.jpg\"}")));
    ResponseDefinitionBuilder file = okJson(pages(List.of(page(51, "Synthetic.jpg", "2000"))));
    return Stream.of(
        Arguments.of("PageImages 503", aResponse().withStatus(503), file),
        Arguments.of("PageImages error", okJson("{\"error\":{\"info\":\"x\"}}"), file),
        Arguments.of("file batch 503", lead, aResponse().withStatus(503)),
        Arguments.of("file batch without query", lead, okJson("{\"batchcomplete\":true}")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("eventPathFailures")
  void failedEventPathEmptiesOnlyTheEventPhotos(
      String name,
      ResponseDefinitionBuilder lead,
      ResponseDefinitionBuilder file,
      CapturedOutput output) {
    dayCategory(okJson(pages(sevenDayPhotos())));
    leadImages(lead);
    fileBatch(file);

    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);

    assertThat(media.eventMedia()).isEmpty();
    assertThat(media.dayMedia()).hasSize(6);
    assertThat(media.failed()).isTrue();
    assertThat(failures(output)).as("one failure, one WARN line").hasSize(1);
  }

  // ---- T1 (8): request counts ----

  @Test
  void noLeadImageSendsNoFileBatchAndIsNotAFailure() {
    leadImages(okJson(WikimediaWireMockTest.NO_PAGE_IMAGES));

    WorldMedia media =
        lookup().lookup(LEAP_DAY, List.of(target("a", "시험 문서 가"), target("b", "시험 문서 나")), false);

    assertThat(media.failed()).isFalse();
    assertThat(media.eventMedia()).isEmpty();
    assertThat(served(KOWIKI)).hasSize(1);
    assertThat(askedTitles()).containsExactly("시험 문서 가", "시험 문서 나");
    assertThat(fileBatchRequests()).isZero();
    assertThat(dayCategoryRequests()).isEqualTo(1);
  }

  @Test
  void noTargetsSendNoPageImagesRequest() {
    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(), false);

    assertThat(media.failed()).isFalse();
    assertThat(served(KOWIKI)).isEmpty();
    assertThat(served(COMMONS)).hasSize(1);
  }

  @Test
  void fiftyOneTargetsAreOneRequestWithTheFirstFifty() {
    leadImages(okJson(WikimediaWireMockTest.NO_PAGE_IMAGES));
    List<EventTarget> targets =
        IntStream.rangeClosed(1, 51).mapToObj(i -> target("e" + i, "시험 문서 " + i)).toList();

    lookup().lookup(LEAP_DAY, targets, true);

    assertThat(served(KOWIKI)).hasSize(1);
    assertThat(askedTitles()).hasSize(50).doesNotContain("시험 문서 51");
  }

  // ---- T1 (9): one end time for both paths ----

  @Test
  void silentPageImagesLeavesTheDayPhotosWithinOneWindow(CapturedOutput output) {
    dayCategory(okJson(pages(sevenDayPhotos())));
    leadImages(
        okJson(WikimediaWireMockTest.NO_PAGE_IMAGES).withFixedDelay((int) SILENT.toMillis()));

    long start = System.nanoTime();
    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took).isLessThan(WINDOW);
    assertThat(media.failed()).isTrue();
    assertThat(media.dayMedia()).hasSize(6);
    assertThat(failures(output))
        .hasSize(1)
        .allSatisfy(
            line ->
                assertThat(line)
                    .containsAnyOf(
                        "source=kowiki title=kowiki pageimages of 1",
                        "source=media title=event photos of 1"));
  }

  @Test
  void silentDayCategoryLeavesTheEventPhoto(CapturedOutput output) {
    dayCategory(okJson(pages(sevenDayPhotos())).withFixedDelay((int) SILENT.toMillis()));
    leadImages("시험 문서 가=Synthetic event.jpg");
    fileBatch(okJson(pages(List.of(page(61, "Synthetic event.jpg", "2000")))));

    long start = System.nanoTime();
    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took).isLessThan(WINDOW);
    assertThat(media.failed()).isTrue();
    assertThat(media.dayMedia()).isEmpty();
    assertThat(photoIds(media)).containsExactlyEntriesOf(Map.of("e", "commons-61"));
    assertThat(failures(output))
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).contains("source=commons title=commons day category"));
  }

  @Test
  void bothPathsSilentStillTakeOneWindowWithOneWarningEach(CapturedOutput output) {
    dayCategory(okJson(pages(sevenDayPhotos())).withFixedDelay((int) SILENT.toMillis()));
    leadImages(
        okJson(WikimediaWireMockTest.NO_PAGE_IMAGES).withFixedDelay((int) SILENT.toMillis()));

    long start = System.nanoTime();
    WorldMedia media = lookup().lookup(LEAP_DAY, List.of(target("e", "시험 문서 가")), false);
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(took).as("not two media windows").isLessThan(WINDOW);
    assertThat(media.failed()).isTrue();
    assertThat(media.dayMedia()).isEmpty();
    assertThat(media.eventMedia()).isEmpty();
    List<String> lines = failures(output);
    assertThat(lines).hasSize(2);
    assertThat(lines).filteredOn(l -> l.contains("title=commons day category")).hasSize(1);
    assertThat(lines)
        .filteredOn(
            l ->
                l.contains("title=kowiki pageimages of 1") || l.contains("title=event photos of 1"))
        .hasSize(1);
    assertThat(output.getAll()).doesNotContain("시험 문서", "Synthetic", "2000-02-29", "File:");
  }
}
