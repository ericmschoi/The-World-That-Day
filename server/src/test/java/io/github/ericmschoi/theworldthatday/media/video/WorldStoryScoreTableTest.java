package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.commonsVideo;
import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.youtubeVideo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventSource;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Basis;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.TextDates;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsMediaLookup;
import io.github.ericmschoi.theworldthatday.media.pageimages.KoWikiPageImages;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryController;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a4 T7 (4): the nine rows of the "점수 확인" table, run through {@code GET /api/story/world}
 * (MockMvc, the real controller, service, {@link CommonsMediaLookup} and {@link VideoSearch}) with
 * one fake video source that answers a single synthetic candidate per request. Expected values are
 * the plan's table, written out here, never read back from the code. The newsreel and KTV titles
 * are the strings written in {@code docs/research/video-sources.md}; file names, video IDs and
 * addresses are made up. The item's {@code captured_at} is what the source's adapter would set
 * (plan "촬영 시점 규칙"): the original capture date when it is not after the event, the issue year
 * ({@code approx}) for a newsreel, the date written in the title for the KTV rows. A dropped
 * candidate's {@code Reject} is read from the DEBUG count line. No HTTP.
 */
@WebMvcTest(WorldStoryController.class)
@Import({WorldStoryService.class, WorldStoryScoreTableTest.Stubs.class})
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryScoreTableTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Instant READ = Instant.parse("2026-01-01T00:00:00Z");

  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final LocalDate OLYMPICS_DAY = LocalDate.of(1988, 9, 17);
  private static final LocalDate ASIAN_GAMES_DAY = LocalDate.of(1986, 9, 20);

  private static final String APOLLO_ID = "kowiki-1969-07-20-00000001";
  private static final String OLYMPICS_ID = "kowiki-1988-09-17-00000001";
  private static final String ASIAN_GAMES_ID = "kowiki-1986-09-20-00000001";

  private static final Verdict WORLD = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.WORLD);
  private static final Verdict DOMESTIC = new Verdict(Tone.NORMAL, Basis.CURATED, Scope.DOMESTIC);

  /** The answer of the one fake source, and the event the source service finds. */
  static final AtomicReference<SourceResult> ANSWER = new AtomicReference<>();

  static final AtomicReference<KoWikiEvent> EVENT = new AtomicReference<>();
  static final AtomicReference<Verdict> VERDICT = new AtomicReference<>(DOMESTIC);

  @Autowired private MockMvc mvc;

  @TestConfiguration(proxyBeanMethods = false)
  static class Stubs {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-10-23T03:00:00Z"), ClockConfig.SERVICE_ZONE);
    }

    @Bean
    KoWikiEventSource eventSource() {
      return date -> {
        KoWikiEvent event = EVENT.get();
        return new KoWikiDayResult(
            date,
            event == null ? List.of() : List.of(event),
            List.of(
                new KoWikiDocStatus("date page", 100L, READ, State.FRESH),
                new KoWikiDocStatus("year page", 200L, READ, State.FRESH)),
            0,
            0,
            false);
      };
    }

    @Bean
    EventToneJudge toneJudge() {
      return found -> new Judgement(found.stream().map(e -> VERDICT.get()).toList(), false);
    }

    @Bean
    MediaLookup mediaLookup() {
      MediaProperties media = new MediaProperties(List.of(), List.of());
      CommonsClient noPhotos =
          new CommonsClient() {
            @Override
            public Pages dayCategory(LocalDate date, Duration readTimeout) {
              return new Pages(List.of(), false);
            }

            @Override
            public Files files(List<String> fileNames, Duration readTimeout) {
              return new Files(Map.of(), false);
            }
          };
      KoWikiPageImages noImages = (titles, readTimeout) -> Optional.of(Map.of());
      FakeVideoSource source = new FakeVideoSource("commons", events -> ANSWER.get());
      return new CommonsMediaLookup(
          noPhotos,
          new CommonsFileMapper(
              new MediaFilter(media),
              new MediaNormalizer(media),
              Clock.fixed(READ, ClockConfig.SERVICE_ZONE)),
          noImages,
          new MediaLookupProperties(6, Duration.ofSeconds(3), 5),
          FakeVideoSource.search(3, source));
    }
  }

  private Level before;

  @BeforeEach
  void debugOn() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(VideoSearch.class);
    before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
  }

  @AfterEach
  void debugOff() {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(VideoSearch.class)).setLevel(before);
    ANSWER.set(null);
    EVENT.set(null);
    VERDICT.set(DOMESTIC);
  }

  // ---- the three events ----

  /** An event with its main link title; the sentence is invented. */
  private enum Scene {
    APOLLO(MOON_DAY, APOLLO_ID, "아폴로 11호", "Apollo 11", "시험 문장: 우주인이 달에 내렸다.", WORLD),
    OLYMPICS(OLYMPICS_DAY, OLYMPICS_ID, "1988년 하계 올림픽", null, "시험 문장: 서울에서 개막했다.", DOMESTIC),
    ASIAN_GAMES(
        ASIAN_GAMES_DAY, ASIAN_GAMES_ID, "1986년 아시안 게임", null, "시험 문장: 서울에서 개막했다.", DOMESTIC);

    final LocalDate date;
    final String id;
    final String title;
    final String englishLabel;
    final String summary;
    final Verdict verdict;

    Scene(
        LocalDate date,
        String id,
        String title,
        String englishLabel,
        String summary,
        Verdict verdict) {
      this.date = date;
      this.id = id;
      this.title = title;
      this.englishLabel = englishLabel;
      this.summary = summary;
      this.verdict = verdict;
    }

    KoWikiEvent event() {
      return new KoWikiEvent(
          id,
          date,
          "date page",
          "사건",
          100L,
          summary,
          List.of(title, "시험 문서 기타"),
          "Q999999901",
          READ);
    }

    SourceResult answer(Candidate... candidates) {
      return new SourceResult(
          Map.of(id, List.of(candidates)),
          englishLabel == null ? Map.of() : Map.of(id, englishLabel),
          Status.OK);
    }
  }

  // ---- the nine synthetic candidates ----

  private static CaptureTime day(String date) {
    return new CaptureTime(date, DatePrecision.DAY);
  }

  private static Candidate commons(
      String title,
      String description,
      CaptureTime original,
      CaptureTime itemCapture,
      EventLink link,
      Selection.Basis basis,
      LocalDate requested) {
    return commons(900001, title, description, original, itemCapture, link, basis);
  }

  private static Candidate commons(
      long n,
      String title,
      String description,
      CaptureTime original,
      CaptureTime itemCapture,
      EventLink link,
      Selection.Basis basis) {
    LocalDate requested = MOON_DAY;
    return new Candidate(
        commonsVideo("commons-" + n, 95.0, itemCapture, requested),
        title,
        description,
        original,
        List.of(),
        link,
        false,
        false,
        basis);
  }

  /** An official channel video (a newsreel): the issue year as {@code approx}, the issue date. */
  private static Candidate newsreel(
      String videoId, String title, LocalDate issue, LocalDate requested) {
    return new Candidate(
        youtubeVideo(
            videoId,
            300.0,
            new CaptureTime(String.valueOf(issue.getYear()), DatePrecision.APPROX),
            requested),
        title,
        null,
        null,
        List.of(issue),
        EventLink.NONE,
        true,
        true,
        Selection.Basis.DAEHAN_NEWS);
  }

  /** An official channel video dated only by its title, as the YouTube adapter will read it. */
  private static Candidate ktv(String videoId, String title, LocalDate requested) {
    CaptureTime written = TextDates.find(title).getFirst();
    return new Candidate(
        youtubeVideo(videoId, 300.0, written, requested),
        title,
        null,
        null,
        List.of(),
        EventLink.NONE,
        true,
        false,
        Selection.Basis.YOUTUBE_OFFICIAL);
  }

  /** What the world API must show for a row. */
  private record Expect(
      boolean attached,
      int score,
      String reject,
      String capturedAt,
      String precision,
      String kind,
      String basis) {

    static Expect attached(
        int score, String capturedAt, String precision, String kind, String basis) {
      return new Expect(true, score, null, capturedAt, precision, kind, basis);
    }

    static Expect dropped(int score, String reject) {
      return new Expect(false, score, reject, null, null, null, null);
    }
  }

  static Stream<Arguments> rows() {
    return Stream.of(
        Arguments.of(
            "1 AP11 FINAL APPROACH.ogv",
            Scene.APOLLO,
            commons(
                "AP11 FINAL APPROACH.ogv",
                "Apollo 11",
                day("1969-07-20"),
                day("1969-07-20"),
                EventLink.P10,
                Selection.Basis.COMMONS_P10,
                MOON_DAY),
            Expect.attached(130, "1969-07-20", "day", "same_day", "commons_p10")),
        Arguments.of(
            "2 Historic Apollo 11 Moonwalk Footage",
            Scene.APOLLO,
            commons(
                "Historic Apollo 11 Moonwalk Footage",
                null,
                day("2019-07-21"),
                null,
                EventLink.CATEGORY_DIRECT,
                Selection.Basis.COMMONS_CATEGORY,
                MOON_DAY),
            Expect.attached(90, null, null, "unknown", "commons_category")),
        Arguments.of(
            "3 Jack King's Apollo 11 Launch Commentary",
            Scene.APOLLO,
            commons(
                "Jack King's Apollo 11 Launch Commentary",
                null,
                day("2015-06-12"),
                null,
                EventLink.CATEGORY_DIRECT,
                Selection.Basis.COMMONS_CATEGORY,
                MOON_DAY),
            Expect.dropped(50, "LOW_SCORE")),
        Arguments.of(
            "4 Nixon speaking with astronauts on the Moon",
            Scene.APOLLO,
            commons(
                "Nixon speaking with astronauts on the Moon",
                "Apollo 11",
                day("1969-07-20"),
                day("1969-07-20"),
                EventLink.NONE,
                Selection.Basis.COMMONS_DATE_CATEGORY,
                MOON_DAY),
            Expect.attached(80, "1969-07-20", "day", "same_day", "commons_date_category")),
        Arguments.of(
            "5 Mónica Mihánovich experiencia",
            Scene.APOLLO,
            commons(
                "Mónica Mihánovich experiencia",
                null,
                null,
                null,
                EventLink.CATEGORY_DIRECT,
                Selection.Basis.COMMONS_CATEGORY,
                MOON_DAY),
            Expect.dropped(60, "NO_MATCH")),
        Arguments.of(
            "6 대한뉴스 제 1715호-서울 올림픽 개막",
            Scene.OLYMPICS,
            newsreel("AAAAAAAAAAA", "대한뉴스 제 1715호-서울 올림픽 개막", OLYMPICS_DAY, OLYMPICS_DAY),
            Expect.attached(90, "1988", "approx", "approximate", "daehan_news")),
        Arguments.of(
            "7 대한뉴스 제 736호-사람이 달에 내리다-아폴로 11호 제1신-",
            Scene.APOLLO,
            newsreel(
                "BBBBBBBBBBB",
                "대한뉴스 제 736호-사람이 달에 내리다-아폴로 11호 제1신-",
                LocalDate.of(1969, 7, 26),
                MOON_DAY),
            Expect.attached(70, "1969", "approx", "approximate", "daehan_news")),
        Arguments.of(
            "8 1986년 서울아시안게임 개막식",
            Scene.ASIAN_GAMES,
            ktv("JJJJJJJJJJJ", "1986년 서울아시안게임 개막식", ASIAN_GAMES_DAY),
            Expect.attached(60, "1986", "year", "same_year", "youtube_official")),
        Arguments.of(
            "9 1986년 9월 20일 오늘 #제10회 #아시안게임 #개막",
            Scene.ASIAN_GAMES,
            ktv("KKKKKKKKKKK", "1986년 9월 20일 오늘 #제10회 #아시안게임 #개막", ASIAN_GAMES_DAY),
            Expect.attached(90, "1986-09-20", "day", "same_day", "youtube_official")));
  }

  private JsonNode world(Scene scene, SourceResult answer) throws Exception {
    EVENT.set(scene.event());
    VERDICT.set(scene.verdict);
    ANSWER.set(answer);
    String body =
        mvc.perform(get("/api/story/world").param("date", scene.date.toString()))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return JSON.readTree(body);
  }

  @ParameterizedTest(name = "row {0}")
  @MethodSource("rows")
  void theScoreTableRowThroughTheWorldApi(
      String row, Scene scene, Candidate candidate, Expect expect, CapturedOutput output)
      throws Exception {
    JsonNode root = world(scene, scene.answer(candidate));

    JsonNode media = root.path("events").get(0).path("media");
    String line;
    if (expect.attached()) {
      assertThat(media.size()).isEqualTo(1);
      JsonNode video = media.get(0);
      assertThat(video.path("type").asString()).isEqualTo("video");
      assertThat(video.path("display_mode").asString()).isNotEqualTo("link_only");
      assertThat(video.path("source_name").asString()).isNotBlank();
      assertThat(video.path("selection").path("score").isIntegralNumber()).isTrue();
      assertThat(video.path("selection").path("score").asInt()).isEqualTo(expect.score());
      assertThat(video.path("selection").path("basis").asString()).isEqualTo(expect.basis());
      assertThat(video.path("captured_at").isNull() ? null : video.path("captured_at").asString())
          .isEqualTo(expect.capturedAt());
      assertThat(
              video.path("date_precision").isNull()
                  ? null
                  : video.path("date_precision").asString())
          .isEqualTo(expect.precision());
      assertThat(video.path("date_relation").path("kind").asString()).isEqualTo(expect.kind());
      assertThat(output.getAll()).doesNotContain("Video candidates rejected");
      line =
          "attached | score %d | captured_at %s | %s | selection %s"
              .formatted(
                  expect.score(),
                  video.path("captured_at"),
                  video.path("date_relation").path("kind").asString(),
                  video.path("selection"));
    } else {
      assertThat(media.isEmpty()).isTrue();
      assertThat(output.getAll())
          .contains("Video candidates rejected: {" + expect.reject() + "=1}");
      line = "dropped | judged score %d | Reject %s".formatted(expect.score(), expect.reject());
    }
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(root.findValuesAsString("display_mode")).doesNotContain("link_only");
    System.out.println("M3a4 T7 (4) row " + row + " | " + line);
  }

  @Test
  void rowsOneToFiveTogetherKeepOnlyTheStrongestFreeVideo() throws Exception {
    // The five Commons rows with a file each; the table's titles, dates and paths.
    Candidate row1 =
        commons(
            900001,
            "AP11 FINAL APPROACH.ogv",
            "Apollo 11",
            day("1969-07-20"),
            day("1969-07-20"),
            EventLink.P10,
            Selection.Basis.COMMONS_P10);
    Candidate row2 =
        commons(
            900002,
            "Historic Apollo 11 Moonwalk Footage",
            null,
            day("2019-07-21"),
            null,
            EventLink.CATEGORY_DIRECT,
            Selection.Basis.COMMONS_CATEGORY);
    Candidate row3 =
        commons(
            900003,
            "Jack King's Apollo 11 Launch Commentary",
            null,
            day("2015-06-12"),
            null,
            EventLink.CATEGORY_DIRECT,
            Selection.Basis.COMMONS_CATEGORY);
    Candidate row4 =
        commons(
            900004,
            "Nixon speaking with astronauts on the Moon",
            "Apollo 11",
            day("1969-07-20"),
            day("1969-07-20"),
            EventLink.NONE,
            Selection.Basis.COMMONS_DATE_CATEGORY);
    Candidate row5 =
        commons(
            900005,
            "Mónica Mihánovich experiencia",
            null,
            null,
            null,
            EventLink.CATEGORY_DIRECT,
            Selection.Basis.COMMONS_CATEGORY);

    JsonNode root = world(Scene.APOLLO, Scene.APOLLO.answer(row5, row4, row3, row2, row1));

    JsonNode media = root.path("events").get(0).path("media");
    assertThat(media.size()).as("one video per event").isEqualTo(1);
    assertThat(media.get(0).path("id").asString()).isEqualTo("commons-900001");
    assertThat(media.get(0).path("selection").path("score").asInt()).isEqualTo(130);
    assertThat(media.get(0).path("selection").path("basis").asString()).isEqualTo("commons_p10");
  }
}
