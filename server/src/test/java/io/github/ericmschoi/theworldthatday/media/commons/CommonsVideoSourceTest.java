package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.media.commons.WikidataVideoLinks.Links;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearchProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4b T3 (1), (2), (3), (6) and (7) on the source itself, with fake Wikidata and Commons
 * clients (no HTTP) that answer with the recorded 1969-07-20 pages (Apollo 11, a public historical
 * event) or synthetic ones. Event sentences are invented.
 */
class CommonsVideoSourceTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final Duration LOOKUP = Duration.ofSeconds(3);
  private static final String E1 = "kowiki-1969-07-20-00000001";
  private static final String E2 = "kowiki-1969-07-20-00000002";
  private static final Links APOLLO =
      new Links(List.of("AP11 FINAL APPROACH.ogv"), "Apollo 11", "Apollo 11");

  private static final EventContext MOON =
      new EventContext(E1, MOON_DAY, "아폴로 11호", null, "지어낸 문장: 달에 내렸다.", "Q43653", "world");
  private static final EventContext NO_QID =
      new EventContext(E2, MOON_DAY, "시험 사건", null, "지어낸 문장이다.", null, "country:KR");

  private final CommonsFileMapper mapper =
      new CommonsFileMapper(
          new MediaFilter(
              new MediaProperties(List.of(), List.of(), List.of(), List.of("Associated Press"))),
          new MediaNormalizer(new MediaProperties(List.of(), List.of())),
          new MutableClock(Instant.parse("2026-10-24T21:15:44Z"), ClockConfig.SERVICE_ZONE));

  static Pages pages(String fixture) {
    JsonNode pages =
        JSON.readTree(ActionApiCommonsVideoClientTest.fixture(fixture)).path("query").path("pages");
    return new Pages(StreamSupport.stream(pages.spliterator(), false).toList(), false);
  }

  /** Answers as set; records what was asked and on which threads. */
  static final class FakeWikimedia implements WikidataVideoLinks, CommonsVideoClient {

    Optional<Map<String, Links>> links = Optional.of(Map.of());
    Pages day = new Pages(List.of(), false);
    Map<String, Pages> categories = new HashMap<>();
    Pages files = new Pages(List.of(), false);
    boolean filesFailed;
    Duration delay = Duration.ZERO;
    RuntimeException thrown;
    final List<String> asked = new CopyOnWriteArrayList<>();
    final List<Thread> threads = new CopyOnWriteArrayList<>();

    private void call(String what) {
      asked.add(what);
      threads.add(Thread.currentThread());
      if (thrown != null) {
        throw thrown;
      }
      if (!delay.isZero()) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // as the HTTP client: a logged failure
        }
      }
    }

    @Override
    public Optional<Map<String, Links>> links(Collection<String> qids, Duration readTimeout) {
      call("links " + qids);
      return Thread.currentThread().isInterrupted() ? Optional.empty() : links;
    }

    @Override
    public Pages dateCategory(LocalDate date, Duration readTimeout) {
      call("day " + date);
      return Thread.currentThread().isInterrupted() ? new Pages(List.of(), true) : day;
    }

    @Override
    public Pages category(String category, Duration readTimeout) {
      call("category " + category);
      return categories.getOrDefault(category, new Pages(List.of(), false));
    }

    @Override
    public Files files(List<String> fileNames, Duration readTimeout) {
      call("files " + fileNames);
      Map<String, JsonNode> byName = new HashMap<>();
      for (JsonNode page : files.pages()) {
        byName.put(page.path("title").asString().substring(5), page);
      }
      return filesFailed ? new Files(Map.of(), true) : new Files(byName, false);
    }
  }

  private final FakeWikimedia wikimedia = new FakeWikimedia();

  private CommonsVideoSource source() {
    return new CommonsVideoSource(
        wikimedia, wikimedia, mapper, new MediaLookupProperties(6, LOOKUP, 5));
  }

  /** The recorded 1969-07-20 answers: P10, the Apollo 11 category and the date category. */
  private void recordedMoonDay(Links links) {
    wikimedia.links = Optional.of(Map.of("Q43653", links));
    wikimedia.files = pages("p10-files-1969-07-20.json");
    wikimedia.categories.put("Apollo 11", pages("category-apollo-11.json"));
    wikimedia.day = pages("date-category-1969-07-20.json");
  }

  private static List<String> ids(List<Candidate> candidates) {
    return candidates.stream().map(c -> c.item().record().id()).toList();
  }

  // ---- (2) candidates ----

  @Test
  void eachPathGivesItsLinkAndBasisAndTheDateCategoryGoesToEveryEvent() {
    recordedMoonDay(APOLLO);

    SourceResult result = source().find(MOON_DAY, List.of(MOON, NO_QID), false);

    assertThat(result.status()).isEqualTo(Status.OK);
    assertThat(result.englishLabels()).isEqualTo(Map.of(E1, "Apollo 11"));
    List<Candidate> moon = result.byEvent().get(E1);
    assertThat(moon)
        .filteredOn(c -> c.basis() == Basis.COMMONS_P10)
        .extracting(c -> c.item().record().id(), Candidate::eventLink)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("commons-7366097", EventLink.P10));
    assertThat(moon)
        .filteredOn(c -> c.basis() == Basis.COMMONS_CATEGORY)
        .hasSize(5)
        .allSatisfy(c -> assertThat(c.eventLink()).isEqualTo(EventLink.CATEGORY_DIRECT));
    assertThat(moon)
        .filteredOn(c -> c.basis() == Basis.COMMONS_DATE_CATEGORY)
        .extracting(c -> c.item().record().id(), Candidate::eventLink)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("commons-57080428", EventLink.NONE));
    assertThat(moon)
        .allSatisfy(
            c -> {
              assertThat(c.official()).isFalse();
              assertThat(c.explicitDates()).isEmpty();
              assertThat(c.dateTextExcluded()).isFalse();
              assertThat(c.item().record().placeRef()).isEqualTo("world");
            });
    // the event without a QID has the date category video only, at its own place
    List<Candidate> other = result.byEvent().get(E2);
    assertThat(ids(other)).containsExactly("commons-57080428");
    assertThat(other.getFirst().basis()).isEqualTo(Basis.COMMONS_DATE_CATEGORY);
    assertThat(other.getFirst().item().record().placeRef()).isEqualTo("country:KR");
    assertThat(wikimedia.asked)
        .containsExactlyInAnyOrder(
            "links [Q43653]",
            "day 1969-07-20",
            "files [AP11 FINAL APPROACH.ogv]",
            "category Apollo 11");
  }

  @Test
  void theMaterialsAreTheMappersAndTheUploadTimeNeverACapture() {
    recordedMoonDay(APOLLO);

    List<Candidate> moon = source().find(MOON_DAY, List.of(MOON), true).byEvent().get(E1);

    Candidate p10 = moon.stream().filter(c -> c.basis() == Basis.COMMONS_P10).findFirst().get();
    assertThat(p10.title()).isEqualTo("AP11 FINAL APPROACH.ogv");
    assertThat(p10.description()).startsWith("Apollo 11 - Landing on the Sea of Tranquility");
    assertThat(p10.originalCapture().capturedAt()).isEqualTo("1969-07-20");
    Candidate later =
        moon.stream()
            .filter(c -> c.title().equals("Historic Apollo 11 Moonwalk Footage.webm"))
            .findFirst()
            .get();
    assertThat(later.item().record().capturedAt()).isNull();
    assertThat(later.originalCapture().capturedAt()).isEqualTo("2019-07-21");
  }

  @Test
  void dayPathsOffAsksNoDateCategory() {
    recordedMoonDay(APOLLO);

    SourceResult result = source().find(MOON_DAY, List.of(MOON, NO_QID), true);

    assertThat(wikimedia.asked).noneMatch(a -> a.startsWith("day "));
    assertThat(result.byEvent().get(E2)).isEmpty();
    assertThat(result.byEvent().get(E1)).noneMatch(c -> c.basis() == Basis.COMMONS_DATE_CATEGORY);
  }

  @Test
  void eventsWithoutQidAndWithoutDayPathsAskNothingAndAreOk() {
    SourceResult result = source().find(MOON_DAY, List.of(NO_QID), true);

    assertThat(wikimedia.asked).isEmpty();
    assertThat(result).isEqualTo(new SourceResult(Map.of(E2, List.of()), Map.of(), Status.OK));
  }

  @Test
  void oneCategoryOfTwoEventsIsAskedOnceAndShared() {
    recordedMoonDay(APOLLO);
    wikimedia.links =
        Optional.of(
            Map.of("Q43653", APOLLO, "Q1", new Links(List.of(), "Apollo 11", "Synthetic label")));
    EventContext twin = new EventContext(E2, MOON_DAY, "시험 사건", null, "지어낸 문장이다.", "Q1", "world");

    SourceResult result = source().find(MOON_DAY, List.of(MOON, twin), true);

    assertThat(wikimedia.asked).filteredOn(a -> a.startsWith("category ")).hasSize(1);
    assertThat(result.byEvent().get(E2)).hasSize(5);
    assertThat(result.englishLabels()).containsEntry(E2, "Synthetic label");
  }

  // ---- (3) status ----

  @Test
  void anyFailedRequestMakesTheAnswerFailedAndEmpty() {
    recordedMoonDay(APOLLO);
    wikimedia.links = Optional.empty();
    assertThat(source().find(MOON_DAY, List.of(MOON), false))
        .isEqualTo(new SourceResult(Map.of(), Map.of(), Status.FAILED));
    assertThat(wikimedia.asked).noneMatch(a -> a.startsWith("files") || a.startsWith("category"));

    recordedMoonDay(APOLLO);
    wikimedia.day = new Pages(List.of(), true);
    assertThat(source().find(MOON_DAY, List.of(MOON), false).status()).isEqualTo(Status.FAILED);

    recordedMoonDay(APOLLO);
    wikimedia.categories.put("Apollo 11", new Pages(List.of(), true));
    assertThat(source().find(MOON_DAY, List.of(MOON), false).status()).isEqualTo(Status.FAILED);

    recordedMoonDay(APOLLO);
    wikimedia.filesFailed = true;
    assertThat(source().find(MOON_DAY, List.of(MOON), false).status()).isEqualTo(Status.FAILED);
  }

  @Test
  void noVideoAnywhereIsOk() {
    wikimedia.links = Optional.of(Map.of("Q43653", Links.NONE));

    assertThat(source().find(MOON_DAY, List.of(MOON), false))
        .isEqualTo(new SourceResult(Map.of(E1, List.of()), Map.of(), Status.OK));
  }

  @Test
  void anExceptionOfAnInnerRequestIsThrownForTheSearchToLog() {
    wikimedia.thrown = new IllegalStateException("synthetic");

    assertThatIllegalStateException()
        .isThrownBy(() -> source().find(MOON_DAY, List.of(MOON), false))
        .withMessage("synthetic");
  }

  // ---- (1) inner requests end with the outer search ----

  @Test
  void whenTheOuterDeadlinePassesNoInnerRequestThreadIsLeft() throws Exception {
    recordedMoonDay(APOLLO);
    wikimedia.delay = Duration.ofSeconds(30);
    Duration budget = Duration.ofMillis(300);

    long start = System.nanoTime();
    Deadline.Outcome<SourceResult> outcome;
    try (Deadline outer = Deadline.after(budget)) {
      outcome =
          outer.start("video-commons", () -> source().find(MOON_DAY, List.of(MOON), false)).await();
    }
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    assertThat(outcome)
        .isEqualTo(new TimedOut<>(true, new SourceResult(Map.of(), Map.of(), Status.FAILED)));
    assertThat(took).isLessThan(budget.plus(Deadline.CANCEL_GRACE));
    assertThat(wikimedia.threads).hasSize(2); // stage 1 only: links and the date category
    for (Thread thread : wikimedia.threads) {
      thread.join(Duration.ofSeconds(1));
      assertThat(thread.isAlive()).as(thread.getName()).isFalse();
    }
  }

  // ---- (6) and (7) with the search: 1969-07-20 ----

  /** The plan's settings (plan M3a4 "정확도 규칙"). */
  private static final VideoSearchProperties PLAN =
      new VideoSearchProperties(
          60,
          3,
          600,
          List.of(
              "재현",
              "기념",
              "기념식",
              "주년",
              "다시 보는",
              "추모",
              "anniversary",
              "reenactment",
              "montage",
              "highlights",
              "comparison",
              "memorial",
              "commemoration",
              "tribute"),
          List.of(
              "발사",
              "launch",
              "리허설",
              "rehearsal",
              "예행연습",
              "카운트다운",
              "countdown",
              "훈련",
              "training",
              "준비",
              "preparation"),
          List.of("the", "a", "an", "of", "in", "on", "at", "and", "for", "to", "및"));

  private static Videos search(CommonsVideoSource source, EventTarget target, boolean dayPathsOff) {
    VideoSearch search = new VideoSearch(List.of(source), new VideoAccuracy(PLAN), PLAN);
    try (Deadline deadline = Deadline.after(LOOKUP)) {
      return search.start(deadline, MOON_DAY, List.of(target), dayPathsOff).await();
    }
  }

  private static final EventTarget MOON_TARGET =
      new EventTarget(E1, "아폴로 11호", "world", "지어낸 문장: 달에 내렸다.", "Q43653");

  @Test
  void theMoonLandingGetsItsP10FileOfTheDay() {
    recordedMoonDay(APOLLO);

    Videos videos = search(source(), MOON_TARGET, false);

    MediaItem video = videos.byEvent().get(E1);
    assertThat(videos.failed()).isFalse();
    assertThat(video.record().id()).isEqualTo("commons-7366097");
    assertThat(video.selection()).isEqualTo(new Selection(Basis.COMMONS_P10, 130));
    assertThat(video.record().capturedAt()).isEqualTo("1969-07-20");
    assertThat(video.dateRelation().kind()).isEqualTo(DateRelation.Kind.SAME_DAY);
    assertThat(video.record().videoSources().getFirst().mime()).isEqualTo("video/webm");
  }

  @Test
  void launchVideoPassesAt70ButLosesToTheBestCategoryVideoOnRank() {
    recordedMoonDay(new Links(List.of(), "Apollo 11", "Apollo 11"));

    Videos videos = search(source(), MOON_TARGET, false);
    VideoAccuracy accuracy = new VideoAccuracy(PLAN);
    EventContext labelled = EventContext.of(MOON_TARGET, MOON_DAY).withEnglishLabel("Apollo 11");
    Candidate launch =
        source().find(MOON_DAY, List.of(labelled), true).byEvent().get(E1).stream()
            .filter(c -> c.title().contains("Launch"))
            .findFirst()
            .get();

    assertThat(videos.byEvent().get(E1).record().id()).isEqualTo("commons-71615");
    assertThat(videos.byEvent().get(E1).selection())
        .isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 130));
    assertThat(wikimedia.asked).noneMatch(a -> a.startsWith("files"));
    // same month 20 (July 16 in its text) + link 50 + words 30 + free 10 - launch 40: below 130
    assertThat(accuracy.judge(labelled, launch).score()).isEqualTo(70);
    assertThat(videos.byEvent().get(E1).record().id()).isNotEqualTo(launch.item().record().id());
  }

  @Test
  void anEnglishOnlyVideoAttachesWithTheRecordedLabelAndNotWithout() {
    JsonNode synthetic =
        JSON.readTree(
            """
            {"pageid": 9000002, "ns": 6, "title": "File:Synthetic Apollo 11 landing footage.webm",
             "videoinfo": [{"url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_landing.webm",
               "descriptionurl": "https://commons.wikimedia.org/wiki/File:Synthetic_landing.webm",
               "duration": 60, "mime": "video/webm", "mediatype": "VIDEO", "height": 360,
               "extmetadata": {"Artist": {"value": "Synthetic Author"},
                 "LicenseShortName": {"value": "CC0"}}}]}
            """);
    String recorded = SparqlRecorded.APOLLO_LABEL.englishLabel();
    wikimedia.categories.put("Apollo 11", new Pages(List.of(synthetic), false));

    wikimedia.links = Optional.of(Map.of("Q43653", new Links(List.of(), "Apollo 11", recorded)));
    Videos labelled = search(source(), MOON_TARGET, true);
    wikimedia.links = Optional.of(Map.of("Q43653", new Links(List.of(), "Apollo 11", null)));
    Videos unlabelled = search(source(), MOON_TARGET, true);

    assertThat(recorded).isEqualTo("Apollo 11");
    assertThat(labelled.byEvent().get(E1).selection())
        .isEqualTo(new Selection(Basis.COMMONS_CATEGORY, 90)); // link 50, words 30, free 10
    assertThat(unlabelled.byEvent()).isEmpty();
  }

  /** The English label recorded for Q43653 ({@code video-links-1969-07-20.json}). */
  static final class SparqlRecorded {
    static final Links APOLLO_LABEL = recorded();

    private static Links recorded() {
      JsonNode row =
          JSON.readTree(
                  io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest
                      .fixture("video-links-1969-07-20.json"))
              .path("results")
              .path("bindings")
              .get(0);
      return new Links(List.of(), null, row.path("label").path("value").asString());
    }
  }
}
