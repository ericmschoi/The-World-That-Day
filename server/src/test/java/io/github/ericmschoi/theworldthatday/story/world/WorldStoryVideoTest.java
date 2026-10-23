package io.github.ericmschoi.theworldthatday.story.world;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsMediaLookup;
import io.github.ericmschoi.theworldthatday.media.pageimages.KoWikiPageImages;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4 T6 (3), (5), (7), (9), (10) and (12) through the service and the real {@link
 * CommonsMediaLookup}: the photo paths and the video sources share one deadline, tragic events are
 * never searched and show nothing, the day paths are off on a tragic or unavailable-page day, a
 * photo comes before its video, each path fails alone, and every path silent still takes one media
 * window. Commons, PageImages and the video sources are fakes (no HTTP); events, titles and files
 * are synthetic, on the leap day 2000-02-29.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryVideoTest {

  private static final LocalDate LEAP_DAY = FakeVideoSource.LEAP_DAY;
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");
  private static final Duration DEADLINE = Duration.ofMillis(600);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Verdict DOMESTIC = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);
  private static final Verdict WORLD = new Verdict(Tone.NORMAL, Basis.RULE_NO_SIGNAL, Scope.WORLD);
  private static final Verdict TRAGIC = new Verdict(Tone.TRAGIC, Basis.RULE_CLASS, Scope.WORLD);
  private static final String E1 = "kowiki-2000-02-29-00000001";
  private static final String E2 = "kowiki-2000-02-29-00000002";

  private boolean silentDay;
  private boolean silentPageImages;
  private boolean failedPageImages;

  private static String page(long id, String fileName) {
    return """
        {"pageid": %d, "ns": 6, "title": "File:%s", "imageinfo": [{
          "url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/S%d.jpg",
          "descriptionurl": "https://commons.wikimedia.org/wiki/File:S%d.jpg",
          "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/S%d.jpg/640px-S%d.jpg",
          "thumbwidth": 640, "thumbheight": 480, "mime": "image/jpeg",
          "extmetadata": {"Artist": {"value": "Synthetic Author"},
            "LicenseShortName": {"value": "CC0"}, "DateTimeOriginal": {"value": "2000-02-29"}}}]}
        """
        .formatted(id, fileName, id, id, id, id);
  }

  /** Never answers in time: sleeps until the deadline interrupts it, then throws. */
  private static <T> T sleepUntilInterrupted() {
    FakeVideoSource.silent();
    return null;
  }

  /** One day photo (commons-201); every asked file is commons-101. */
  private final CommonsClient commons =
      new CommonsClient() {
        @Override
        public Pages dayCategory(LocalDate date, Duration readTimeout) {
          if (silentDay) {
            return sleepUntilInterrupted();
          }
          return new Pages(List.of(JSON.readTree(page(201, "Synthetic day.jpg"))), false);
        }

        @Override
        public Files files(List<String> fileNames, Duration readTimeout) {
          return new Files(
              Map.of(fileNames.getFirst(), JSON.readTree(page(101, fileNames.getFirst()))), false);
        }
      };

  /** The article of event 1 has a free lead image. */
  private final KoWikiPageImages pageImages =
      (titles, readTimeout) -> {
        if (silentPageImages) {
          return sleepUntilInterrupted();
        }
        return failedPageImages
            ? Optional.empty()
            : Optional.of(Map.of("알파 행사", "Synthetic event.jpg"));
      };

  private final List<FakeVideoSource> sources = new CopyOnWriteArrayList<>();

  private static KoWikiEvent event(String id, String link, String qid) {
    return new KoWikiEvent(
        id,
        LEAP_DAY,
        "2000년",
        "사건",
        200L,
        "시험 사건 " + id.substring(25) + ".",
        linkList(link),
        qid,
        READ);
  }

  private static List<String> linkList(String link) {
    return link == null ? List.of() : List.of(link, "시험 문서 기타");
  }

  private WorldStoryResponse respond(
      List<KoWikiEvent> events, List<Verdict> verdicts, State datePage, State yearPage) {
    KoWikiDayResult day =
        new KoWikiDayResult(
            LEAP_DAY,
            events,
            List.of(
                new KoWikiDocStatus("2월 29일", 100L, READ, datePage),
                new KoWikiDocStatus("2000년", 200L, READ, yearPage)),
            0,
            0,
            false);
    MediaProperties media = new MediaProperties(List.of(), List.of());
    Clock clock = Clock.fixed(READ, ClockConfig.SERVICE_ZONE);
    CommonsMediaLookup lookup =
        new CommonsMediaLookup(
            commons,
            new CommonsFileMapper(new MediaFilter(media), new MediaNormalizer(media), clock),
            pageImages,
            new MediaLookupProperties(6, DEADLINE, 5),
            FakeVideoSource.search(3, sources.toArray(FakeVideoSource[]::new)));
    return new WorldStoryService(date -> day, found -> new Judgement(verdicts, false), lookup)
        .find(LEAP_DAY);
  }

  private WorldStoryResponse respond(List<KoWikiEvent> events, List<Verdict> verdicts) {
    return respond(events, verdicts, State.FRESH, State.FRESH);
  }

  /** A video for every event ID given, whatever the source was asked. */
  private FakeVideoSource source(String key, String... eventIds) {
    Candidate video =
        FakeVideoSource.commons(
            900001, "알파 행사 영상", day("2000-02-29"), EventLink.P10, Selection.Basis.COMMONS_P10);
    Map<String, List<Candidate>> byEvent = new HashMap<>();
    for (String id : eventIds) {
      byEvent.put(id, List.of(video));
    }
    FakeVideoSource source = new FakeVideoSource(key, events -> ok(byEvent));
    sources.add(source);
    return source;
  }

  private static List<String> ids(List<MediaItem> items) {
    return items.stream().map(item -> item.record().id()).toList();
  }

  private static List<String> askedIds(FakeVideoSource source) {
    List<String> ids = new ArrayList<>();
    source.asked.forEach(events -> events.forEach(e -> ids.add(e.eventId())));
    return ids;
  }

  // ---- (7), (12), (1): photo then video, selection on the video only ----

  @Test
  void eventMediaIsThePhotoThenTheVideoAndOnlyTheVideoHasASelection() {
    source("commons", E1);

    WorldStoryResponse response = respond(List.of(event(E1, "알파 행사", null)), List.of(DOMESTIC));

    List<MediaItem> media = response.events().getFirst().media();
    assertThat(ids(media)).containsExactly("commons-101", "commons-900001");
    assertThat(ids(response.dayMedia())).containsExactly("commons-201");
    assertThat(response.partial()).isFalse();
    JsonNode root = JSON.valueToTree(response);
    JsonNode items = root.path("events").get(0).path("media");
    assertThat(items.get(0).has("selection")).isFalse();
    assertThat(items.get(1).path("selection").path("basis").asString()).isEqualTo("commons_p10");
    assertThat(items.get(1).path("selection").path("score").asInt()).isEqualTo(130);
    assertThat(root.path("day_media").get(0).has("selection")).isFalse();
    assertThat(root.findValues("selection")).hasSize(1);
    assertThat(root.findValuesAsString("display_mode"))
        .containsExactlyInAnyOrder("image", "video_file", "image")
        .doesNotContain("link_only");
  }

  // ---- (5): tragic ----

  @Test
  void tragicEventsAreNeverSearchedAndShowNoVideoOnADayWithoutDayPaths() {
    FakeVideoSource a = source("alpha", E1, E2);
    FakeVideoSource b = source("beta", E1, E2);

    WorldStoryResponse response =
        respond(
            List.of(event(E1, "알파 행사", null), event(E2, "알파 행사", null)), List.of(TRAGIC, DOMESTIC));

    for (FakeVideoSource source : List.of(a, b)) {
      assertThat(askedIds(source)).containsExactly(E2);
      assertThat(source.dayPathsOff).containsExactly(true);
    }
    assertThat(response.events().get(0).id()).isEqualTo(E2);
    assertThat(ids(response.events().get(0).media())).contains("commons-900001");
    assertThat(response.events().get(1).id()).isEqualTo(E1);
    assertThat(response.events().get(1).media()).isEmpty();
    assertThat(response.dayMedia()).isEmpty();
  }

  @ParameterizedTest(name = "date page {0}, year page {1}: dayPathsOff {2}")
  @CsvSource({"FRESH, FRESH, false", "UNAVAILABLE, FRESH, true", "FRESH, UNAVAILABLE, true"})
  void anUnavailablePageTurnsEverySourcesDayPathsOff(State datePage, State yearPage, boolean off) {
    FakeVideoSource a = source("alpha");
    FakeVideoSource b = source("beta");

    respond(List.of(event(E1, "알파 행사", null)), List.of(DOMESTIC), datePage, yearPage);

    assertThat(a.dayPathsOff).containsExactly(off);
    assertThat(b.dayPathsOff).containsExactly(off);
  }

  // ---- (10), (4): the service fills the search inputs ----

  @Test
  void eachSourceGetsTheEventsTitleTextQidPlaceAndDate() {
    FakeVideoSource a = source("alpha");

    respond(
        List.of(event(E1, "알파 행사", "Q999999901"), event(E2, null, null)), List.of(WORLD, DOMESTIC));

    assertThat(a.asked)
        .containsExactly(
            List.of(
                new EventContext(E1, LEAP_DAY, "알파 행사", null, "시험 사건 1.", "Q999999901", "world")));
  }

  @Test
  void aDayWithoutTargetsAsksNoVideoSource() {
    FakeVideoSource a = source("alpha", E1);

    WorldStoryResponse none = respond(List.of(), List.of());
    WorldStoryResponse untitled = respond(List.of(event(E1, null, null)), List.of(DOMESTIC));

    assertThat(a.asked).isEmpty();
    assertThat(none.partial()).isFalse();
    assertThat(untitled.events().getFirst().media()).isEmpty();
  }

  // ---- (7): each path fails alone ----

  @Test
  void aFailedPhotoPathKeepsTheVideo() {
    failedPageImages = true;
    source("commons", E1);

    WorldStoryResponse response = respond(List.of(event(E1, "알파 행사", null)), List.of(DOMESTIC));

    assertThat(ids(response.events().getFirst().media())).containsExactly("commons-900001");
    assertThat(response.partial()).isTrue();
  }

  @Test
  void aFailedVideoSourceKeepsThePhotos(CapturedOutput output) {
    sources.add(
        new FakeVideoSource(
            "alpha",
            events -> {
              throw new IllegalStateException("synthetic");
            }));

    WorldStoryResponse response = respond(List.of(event(E1, "알파 행사", null)), List.of(DOMESTIC));

    assertThat(ids(response.events().getFirst().media())).containsExactly("commons-101");
    assertThat(ids(response.dayMedia())).containsExactly("commons-201");
    assertThat(response.partial()).isTrue();
    assertThat(output.getAll())
        .contains("Video search failed: source=alpha events=1 cause=IllegalStateException");
  }

  // ---- (9), (3): one window for every media path ----

  @Test
  void everyMediaPathSilentStillTakesOneWindow(CapturedOutput output) {
    silentDay = true;
    silentPageImages = true;
    sources.add(new FakeVideoSource("alpha", events -> FakeVideoSource.silent()));
    sources.add(new FakeVideoSource("beta", events -> FakeVideoSource.silent()));

    long start = System.nanoTime();
    WorldStoryResponse response = respond(List.of(event(E1, "알파 행사", null)), List.of(DOMESTIC));
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println(
        "M3a4 T6 (9) media segment, day category, PageImages and two video sources silent"
            + " (fakes): "
            + took.toMillis()
            + " ms");

    assertThat(took)
        .isLessThanOrEqualTo(DEADLINE.plus(Deadline.CANCEL_GRACE).plus(Duration.ofMillis(500)));
    assertThat(took).isGreaterThanOrEqualTo(DEADLINE);
    assertThat(response.events().getFirst().media()).isEmpty();
    assertThat(response.dayMedia()).isEmpty();
    assertThat(response.partial()).isTrue();
    assertThat(output.getAll().lines().filter(l -> l.contains("Video search failed")))
        .hasSize(2)
        .allMatch(l -> l.endsWith("events=1 cause=deadline"));
  }
}
