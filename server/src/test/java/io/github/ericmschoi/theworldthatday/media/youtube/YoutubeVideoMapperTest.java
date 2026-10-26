package io.github.ericmschoi.theworldthatday.media.youtube;

import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.CHANNEL;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.SECOND_CHANNEL;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.item;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.mapper;
import static io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems.titled;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.SearchCacheFormat;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeVideoMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeVideoMapper.Result;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a5a T4 (1) to (5) and (7): the checks, the record and the capture time of {@link
 * YoutubeVideoMapper} on synthetic {@code videos.list} items ({@code
 * fixtures/youtube/videos-1986-09-20.json}, changed field by field here). Dates are the 1986-09-20
 * and 1988-09-17 sample dates (public historical events) and the leap day. No request, no storage.
 */
@ExtendWith(OutputCaptureExtension.class)
class YoutubeVideoMapperTest {

  static final LocalDate ASIAN_GAMES_DAY = LocalDate.of(1986, 9, 20);
  static final LocalDate OLYMPICS_DAY = LocalDate.of(1988, 9, 17);
  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);
  private static final String CLIP = "Abc123xyz_0";

  static final EventContext ASIAN_GAMES = event(ASIAN_GAMES_DAY, "1986년 아시안 게임");

  private final YoutubeVideoMapper mapper = mapper();

  static EventContext event(LocalDate date, String title) {
    return new EventContext(
        "kowiki-" + date + "-00000001", date, title, null, "시험 문장: 서울에서 열렸다.", null, "country:KR");
  }

  private Candidate kept(ObjectNode item, EventContext event) {
    Result result = mapper.map(item, event);
    assertThat(result.drop()).isNull();
    return result.candidate();
  }

  private static ObjectNode with(ObjectNode item, Consumer<ObjectNode> change) {
    change.accept(item);
    return item;
  }

  private static ObjectNode node(ObjectNode item, String part) {
    return (ObjectNode) item.path(part);
  }

  private static ObjectNode recorded(ObjectNode item, String recordingDate) {
    node(item, "recordingDetails").put("recordingDate", recordingDate);
    return item;
  }

  // ---- (1) checks ----

  static Stream<Arguments> drops() {
    return Stream.of(
        Arguments.of(
            "not embeddable",
            with(item(CLIP), i -> node(i, "status").put("embeddable", false)),
            Drop.NOT_EMBEDDABLE),
        Arguments.of(
            "embeddable missing",
            with(item(CLIP), i -> node(i, "status").remove("embeddable")),
            Drop.NOT_EMBEDDABLE),
        Arguments.of(
            "embeddable as text",
            with(item(CLIP), i -> node(i, "status").put("embeddable", "true")),
            Drop.NOT_EMBEDDABLE),
        Arguments.of(
            "private",
            with(item(CLIP), i -> node(i, "status").put("privacyStatus", "private")),
            Drop.NOT_PUBLIC),
        Arguments.of(
            "unlisted",
            with(item(CLIP), i -> node(i, "status").put("privacyStatus", "unlisted")),
            Drop.NOT_PUBLIC),
        Arguments.of(
            "blocked KR",
            with(
                item(CLIP),
                i ->
                    node(i, "contentDetails")
                        .putObject("regionRestriction")
                        .putArray("blocked")
                        .add("JP")
                        .add("KR")),
            Drop.REGION),
        Arguments.of(
            "allowed JP",
            with(
                item(CLIP),
                i ->
                    node(i, "contentDetails")
                        .putObject("regionRestriction")
                        .putArray("allowed")
                        .add("JP")),
            Drop.REGION),
        Arguments.of(
            "allowed none",
            with(
                item(CLIP),
                i -> node(i, "contentDetails").putObject("regionRestriction").putArray("allowed")),
            Drop.REGION),
        Arguments.of(
            "age restricted",
            with(
                item(CLIP),
                i ->
                    node(i, "contentDetails")
                        .putObject("contentRating")
                        .put("ytRating", "ytAgeRestricted")),
            Drop.AGE_RESTRICTED),
        Arguments.of(
            "other channel",
            with(item(CLIP), i -> node(i, "snippet").put("channelId", "UCsyntheticChannelId0009")),
            Drop.CHANNEL),
        Arguments.of(
            "no channel",
            with(item(CLIP), i -> node(i, "snippet").remove("channelId")),
            Drop.CHANNEL),
        Arguments.of("ID of 10", with(item(CLIP), i -> i.put("id", "Abc123xyz_")), Drop.ID),
        Arguments.of("ID of 12", with(item(CLIP), i -> i.put("id", "Abc123xyz_00")), Drop.ID),
        Arguments.of("ID with a slash", with(item(CLIP), i -> i.put("id", "Abc123xyz/0")), Drop.ID),
        Arguments.of(
            "colour caption",
            titled(CLIP, "[📺컬러 복원] 해외 소식 | 시험 꼭지 [대한뉴스 698호] 📽️1968년 11월 2일 방영", ""),
            Drop.COLOR_RESTORED),
        Arguments.of(
            "colour words only", titled(CLIP, "1986년 서울아시안게임 개막식 (컬러복원)", ""), Drop.COLOR_RESTORED),
        Arguments.of(
            "newsreel tag only",
            titled(CLIP, "1986년 서울아시안게임 개막식 [대한뉴스 제 1611호]", ""),
            Drop.COLOR_RESTORED),
        Arguments.of(
            "R1 blank channel name",
            with(item(CLIP), i -> node(i, "snippet").put("channelTitle", "　")),
            Drop.RULE),
        Arguments.of(
            "R9 thumbnail not http",
            with(
                item(CLIP),
                i ->
                    ((ObjectNode) i.path("snippet").path("thumbnails").path("medium"))
                        .put("url", "ftp://i.ytimg.com/vi/x.jpg")),
            Drop.RULE),
        Arguments.of(
            "shape zero width",
            with(
                item(CLIP),
                i ->
                    ((ObjectNode) i.path("snippet").path("thumbnails").path("medium"))
                        .put("width", 0)),
            Drop.RULE));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("drops")
  void eachDropRuleLeavesNoCandidate(String name, ObjectNode item, Drop drop) {
    Result result = mapper.map(item, ASIAN_GAMES);

    assertThat(result.drop()).isEqualTo(drop);
    assertThat(result.candidate()).isNull();
  }

  @Test
  void anotherRegionOrKoreaAllowedKeepsTheVideo() {
    kept(
        with(
            item(CLIP),
            i ->
                node(i, "contentDetails")
                    .putObject("regionRestriction")
                    .putArray("blocked")
                    .add("JP")),
        ASIAN_GAMES);
    kept(
        with(
            item(CLIP),
            i ->
                node(i, "contentDetails")
                    .putObject("regionRestriction")
                    .putArray("allowed")
                    .add("JP")
                    .add("KR")),
        ASIAN_GAMES);
    kept(
        with(
            item(CLIP),
            i -> node(i, "contentDetails").putObject("contentRating").put("ytRating", "ytUnrated")),
        ASIAN_GAMES);
  }

  @Test
  void theBlocklistTakesDownAVideoByIdAndByChannel() {
    YoutubeVideoMapper byId =
        mapper(
            List.of(CHANNEL),
            "http://localhost:5173",
            List.of(new BlockEntry("youtube", CLIP, null, null)),
            List.of("youtube"));
    YoutubeVideoMapper byChannel =
        mapper(
            List.of(CHANNEL, SECOND_CHANNEL),
            "http://localhost:5173",
            List.of(new BlockEntry("youtube", null, null, SECOND_CHANNEL)),
            List.of("youtube"));
    YoutubeVideoMapper otherSource =
        mapper(
            List.of(CHANNEL),
            "http://localhost:5173",
            List.of(new BlockEntry("commons", CLIP, null, null)),
            List.of("youtube"));

    assertThat(byId.map(item(CLIP), ASIAN_GAMES).drop()).isEqualTo(Drop.FILTERED);
    assertThat(byId.map(item("Abc123xyz_2"), ASIAN_GAMES).drop()).isNull();
    assertThat(byChannel.map(item("Abc123xyz_4"), ASIAN_GAMES).drop()).isEqualTo(Drop.FILTERED);
    assertThat(byChannel.map(item(CLIP), ASIAN_GAMES).drop()).isNull();
    assertThat(otherSource.map(item(CLIP), ASIAN_GAMES).drop()).isNull();
  }

  @Test
  void withoutYoutubeAmongTheEmbedProvidersRuleR5DropsIt() {
    YoutubeVideoMapper noProvider =
        mapper(List.of(CHANNEL), "http://localhost:5173", List.of(), List.of());

    assertThat(noProvider.map(item(CLIP), ASIAN_GAMES).drop()).isEqualTo(Drop.RULE);
  }

  @Test
  void dropsAreLoggedAsCountsByReasonOnly(CapturedOutput output) {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(YoutubeVideoMapper.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      List<Candidate> candidates =
          mapper.candidates(
              List.of(
                  with(item(CLIP), i -> node(i, "status").put("privacyStatus", "private")),
                  item("Abc123xyz_2"),
                  with(
                      item("Abc123xyz_3"),
                      i ->
                          node(i, "contentDetails")
                              .putObject("regionRestriction")
                              .putArray("blocked")
                              .add("KR")),
                  item("Abc123xyz_4"),
                  item(CLIP)),
              ASIAN_GAMES);

      assertThat(candidates)
          .extracting(c -> c.item().record().id())
          .containsExactly("youtube-Abc123xyz_2", "youtube-Abc123xyz_0");
      assertThat(output).contains("YouTube videos dropped: {CHANNEL=1, NOT_PUBLIC=1, REGION=1}");
      assertThat(output).doesNotContain("Abc123xyz", CHANNEL, SECOND_CHANNEL, "1986", "아시안", "KTV");
    } finally {
      logger.setLevel(before);
    }
  }

  @Test
  void nothingDroppedLogsNothing(CapturedOutput output) {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(YoutubeVideoMapper.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      assertThat(mapper.candidates(List.of(item(CLIP)), ASIAN_GAMES)).hasSize(1);
      assertThat(mapper.candidates(List.of(), ASIAN_GAMES)).isEmpty();
      assertThat(output).doesNotContain("YouTube videos dropped");
    } finally {
      logger.setLevel(before);
    }
  }

  // ---- (2), (3), (7) record ----

  @Test
  void theRecordHasEveryValueOfTheRuleTable() {
    MediaRecord record = kept(item(CLIP), ASIAN_GAMES).item().record();

    assertThat(record.id()).isEqualTo("youtube-Abc123xyz_0");
    assertThat(record.type()).isEqualTo(Type.VIDEO);
    assertThat(record.displayMode()).isEqualTo(DisplayMode.VIDEO_EMBED);
    assertThat(record.url()).isNull();
    assertThat(record.thumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/Abc123xyz_0/mqdefault.jpg");
    assertThat(record.thumbnailWidth()).isEqualTo(320);
    assertThat(record.thumbnailHeight()).isEqualTo(180);
    assertThat(record.videoSources()).isNull();
    assertThat(record.durationSeconds()).isEqualTo(117.0);
    assertThat(record.provider()).isEqualTo("youtube");
    assertThat(record.render()).isNull();
    assertThat(record.linkOnlyReason()).isNull();
    assertThat(record.sourceName()).isEqualTo("KTV 아카이브");
    assertThat(record.sourceUrl()).isEqualTo("https://www.youtube.com/watch?v=Abc123xyz_0");
    assertThat(record.license()).isEqualTo("provider-terms");
    assertThat(record.licenseStatus()).isEqualTo(LicenseStatus.OTHER);
    assertThat(record.licenseUrl()).isEqualTo("https://www.youtube.com/t/terms");
    assertThat(record.attribution()).isEqualTo("KTV 아카이브 · YouTube");
    assertThat(record.description()).isEqualTo("시험용 개막식 영상 설명");
    assertThat(record.capturedAt()).isEqualTo("1986");
    assertThat(record.datePrecision()).isEqualTo(DatePrecision.YEAR);
    assertThat(record.lat()).isNull();
    assertThat(record.lng()).isNull();
    assertThat(record.placeRef()).isEqualTo("country:KR");
    assertThat(record.retrievedAt()).isEqualTo("2026-10-25T03:00:00Z");
  }

  @Test
  void thePlaceIsTheEventsOwn() {
    EventContext world =
        new EventContext(
            "kowiki-1986-09-20-00000002",
            ASIAN_GAMES_DAY,
            "1986년 아시안 게임",
            null,
            null,
            null,
            "world");

    assertThat(kept(item(CLIP), world).item().record().placeRef()).isEqualTo("world");
  }

  @Test
  void theEmbedAddressHasTheExactShapeWithoutAutoplayOrMute() {
    String embed = kept(item(CLIP), ASIAN_GAMES).item().record().embedUrl();

    assertThat(embed)
        .isEqualTo(
            "https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=http%3A%2F%2Flocalhost%3A5173");
    assertThat(embed).doesNotContain("autoplay", "mute");
  }

  @Test
  void theOriginFollowsTheWebOriginSetting() {
    YoutubeVideoMapper elsewhere =
        mapper(List.of(CHANNEL), "https://example.org:8443", List.of(), List.of("youtube"));

    assertThat(elsewhere.map(item(CLIP), ASIAN_GAMES).candidate().item().record().embedUrl())
        .isEqualTo(
            "https://www.youtube-nocookie.com/embed/Abc123xyz_0?playsinline=1&rel=0&enablejsapi=1&origin=https%3A%2F%2Fexample.org%3A8443");
  }

  @Test
  void durationsAreReadAsIsoSeconds() {
    assertThat(YoutubeVideoMapper.seconds("PT1M57S")).isEqualTo(117.0);
    assertThat(YoutubeVideoMapper.seconds("PT10M1S")).isEqualTo(601.0);
    assertThat(YoutubeVideoMapper.seconds("PT1H")).isEqualTo(3600.0);
    assertThat(YoutubeVideoMapper.seconds("PT1S")).isEqualTo(1.0);
    assertThat(YoutubeVideoMapper.seconds("P0D")).isNull();
    assertThat(YoutubeVideoMapper.seconds("PT0S")).isNull();
    assertThat(YoutubeVideoMapper.seconds(null)).isNull();
    assertThat(YoutubeVideoMapper.seconds("1:57")).isNull();
    assertThat(YoutubeVideoMapper.seconds("")).isNull();
  }

  @Test
  void anUnknownLengthKeepsTheItemWithoutDuration() {
    Candidate broken =
        kept(with(item(CLIP), i -> node(i, "contentDetails").put("duration", "P0D")), ASIAN_GAMES);
    Candidate missing =
        kept(with(item(CLIP), i -> node(i, "contentDetails").remove("duration")), ASIAN_GAMES);
    Candidate longer =
        kept(
            with(item(CLIP), i -> node(i, "contentDetails").put("duration", "PT10M1S")),
            ASIAN_GAMES);

    assertThat(broken.item().record().durationSeconds()).isNull();
    assertThat(missing.item().record().durationSeconds()).isNull();
    assertThat(longer.item().record().durationSeconds()).isEqualTo(601.0);
  }

  @Test
  void theDescriptionIsItsFirstLineCutAt200CodePoints() {
    String line = "가".repeat(199) + "😀" + "나";

    assertThat(YoutubeVideoMapper.firstLine(line + "\n둘째 줄")).isEqualTo("가".repeat(199) + "😀");
    assertThat(YoutubeVideoMapper.firstLine("가".repeat(200))).isEqualTo("가".repeat(200));
    assertThat(YoutubeVideoMapper.firstLine(" 첫 줄 \r\n둘째")).isEqualTo("첫 줄");
    assertThat(YoutubeVideoMapper.firstLine("")).isNull();
    assertThat(YoutubeVideoMapper.firstLine("　\n둘째 줄")).isNull();
    assertThat(YoutubeVideoMapper.firstLine(null)).isNull();
    assertThat(
            kept(titled(CLIP, "1986년 서울아시안게임 개막식", ""), ASIAN_GAMES).item().record().description())
        .isNull();
  }

  @Test
  void everyRecordFollowsTheMediaPrinciples() {
    List<Candidate> candidates =
        mapper(
                List.of(CHANNEL, SECOND_CHANNEL),
                "http://localhost:5173",
                List.of(),
                List.of("youtube"))
            .candidates(
                List.of(item(CLIP), item("Abc123xyz_2"), item("Abc123xyz_3"), item("Abc123xyz_4")),
                ASIAN_GAMES);

    assertThat(candidates).hasSize(4);
    for (Candidate candidate : candidates) {
      MediaRecord record = candidate.item().record();
      assertThat(record.sourceName()).isNotBlank();
      assertThat(record.sourceUrl()).startsWith("https://www.youtube.com/watch?v=");
      assertThat(record.license()).isEqualTo("provider-terms");
      assertThat(record.attribution()).isEqualTo(record.sourceName() + " · YouTube");
      assertThat(record.displayMode()).isEqualTo(DisplayMode.VIDEO_EMBED);
      assertThat(record.linkOnlyReason()).isNull();
      assertThat(record.url()).isNull();
      assertThat(record.videoSources()).isNull();
      assertThat(record.embedUrl()).startsWith("https://www.youtube-nocookie.com/embed/");
    }
  }

  // ---- (4) candidate ----

  @Test
  void theCandidateIsAnOfficialYoutubeVideoWithItsOrigin() {
    Candidate candidate = kept(item(CLIP), ASIAN_GAMES);

    assertThat(candidate.official()).isTrue();
    assertThat(candidate.eventLink()).isEqualTo(EventLink.NONE);
    assertThat(candidate.explicitDates()).isEmpty();
    assertThat(candidate.basis()).isEqualTo(Basis.YOUTUBE_OFFICIAL);
    assertThat(candidate.title()).isEqualTo("1986년 서울아시안게임 개막식");
    assertThat(candidate.description()).isEqualTo("시험용 개막식 영상 설명\n둘째 줄");
    assertThat(candidate.dateTextExcluded()).isFalse();
    assertThat(candidate.origin()).isEqualTo(new MediaOrigin("youtube", CLIP, null, null, CHANNEL));
  }

  @Test
  void theResultCacheStoresTheCandidateAndReadsItBack() {
    Candidate candidate = kept(item(CLIP), ASIAN_GAMES);
    MediaProperties media = new MediaProperties(List.of(), List.of("youtube"));
    SearchCacheFormat format =
        new SearchCacheFormat(new MediaFilter(media), new MediaNormalizer(media));

    String stored = format.write(null, List.of(candidate)).orElseThrow();

    assertThat(format.read(stored, ASIAN_GAMES_DAY).orElseThrow().candidates())
        .containsExactly(candidate);
  }

  // ---- (5) capture time ----

  @Test
  void aRecordingDateNotAfterTheEventIsTheCaptureTime() {
    Candidate candidate = kept(recorded(item(CLIP), "1986-09-20T00:00:00Z"), ASIAN_GAMES);

    assertThat(candidate.item().record().capturedAt()).isEqualTo("1986-09-20");
    assertThat(candidate.item().record().datePrecision()).isEqualTo(DatePrecision.DAY);
    assertThat(candidate.originalCapture())
        .isEqualTo(new CaptureTime("1986-09-20", DatePrecision.DAY));
  }

  @Test
  void aLaterRecordingDateFallsThroughToTheTitle() {
    Candidate candidate = kept(recorded(item(CLIP), "1986-09-21T00:00:00Z"), ASIAN_GAMES);

    assertThat(candidate.item().record().capturedAt()).isEqualTo("1986");
    assertThat(candidate.item().record().datePrecision()).isEqualTo(DatePrecision.YEAR);
    assertThat(candidate.originalCapture())
        .isEqualTo(new CaptureTime("1986-09-21", DatePrecision.DAY));
  }

  @Test
  void anUnreadableRecordingDateIsUnknown() {
    Candidate candidate = kept(recorded(item(CLIP), "1986-09"), ASIAN_GAMES);

    assertThat(candidate.originalCapture()).isEqualTo(CaptureTime.UNKNOWN);
    assertThat(candidate.item().record().capturedAt()).isEqualTo("1986");
  }

  @Test
  void aYearWithoutTheKoreanSuffixIsNoDate() {
    // TextDates reads no bare number as a date, so the plan's 「1986 서울아시안게임 개막식」 gives no
    // capture time; the year comes with 「1986년 …」, as in the research title [122].
    Candidate candidate = kept(titled(CLIP, "1986 서울아시안게임 개막식", "시험용 설명"), ASIAN_GAMES);

    assertThat(candidate.item().record().capturedAt()).isNull();
    assertThat(candidate.item().record().datePrecision()).isNull();
  }

  @Test
  void withoutADateInTheTitleTheDescriptionsFirstDateIsUsed() {
    Candidate candidate =
        kept(titled(CLIP, "서울아시안게임 개막식", "1986년 9월 20일 잠실에서 열린 개막식, 2023년 복원"), ASIAN_GAMES);

    assertThat(candidate.item().record().capturedAt()).isEqualTo("1986-09-20");
    assertThat(candidate.item().record().datePrecision()).isEqualTo(DatePrecision.DAY);
  }

  @Test
  void onlyALaterDateInTheDescriptionLeavesTheCaptureTimeUnknown() {
    Candidate candidate = kept(titled(CLIP, "서울아시안게임 개막식", "2023년 복원"), ASIAN_GAMES);

    assertThat(candidate.item().record().capturedAt()).isNull();
    assertThat(candidate.item().record().datePrecision()).isNull();
    assertThat(candidate.item().dateRelation())
        .isEqualTo(new DateRelation(DateRelation.Kind.UNKNOWN, 0));
  }

  @Test
  void aRetrospectiveEditHasNoCaptureTimeButKeepsItsDateText() {
    Candidate candidate =
        mapper(
                List.of(CHANNEL, SECOND_CHANNEL),
                "http://localhost:5173",
                List.of(),
                List.of("youtube"))
            .map(recorded(item("Abc123xyz_4"), "1986-09-20T00:00:00Z"), ASIAN_GAMES)
            .candidate();

    assertThat(candidate.item().record().capturedAt()).isNull();
    assertThat(candidate.item().record().datePrecision()).isNull();
    assertThat(candidate.dateTextExcluded()).isFalse();
    assertThat(candidate.item().record().sourceName()).isEqualTo("시험용 둘째 채널");
  }

  @Test
  void aNewsreelTitleHasNoCaptureTimeAndExcludesItsDateText() {
    EventContext olympics = event(OLYMPICS_DAY, "1988년 하계 올림픽");
    Candidate candidate = kept(titled(CLIP, "대한뉴스 제 1715호-서울 올림픽 개막", "제작일: 1988-09-17"), olympics);

    assertThat(candidate.item().record().capturedAt()).isNull();
    assertThat(candidate.item().record().datePrecision()).isNull();
    assertThat(candidate.dateTextExcluded()).isTrue();
    assertThat(candidate.title()).isEqualTo("대한뉴스 제 1715호-서울 올림픽 개막");
  }

  @Test
  void publishedAtIsNeverRead() {
    Result before = mapper.map(titled(CLIP, "서울아시안게임 개막식", "시험용 설명"), ASIAN_GAMES);
    ObjectNode moved = titled(CLIP, "서울아시안게임 개막식", "시험용 설명");
    node(moved, "snippet").put("publishedAt", "1986-09-20T00:00:00Z");
    ObjectNode gone = titled(CLIP, "서울아시안게임 개막식", "시험용 설명");
    node(gone, "snippet").remove("publishedAt");

    assertThat(mapper.map(moved, ASIAN_GAMES)).isEqualTo(before);
    assertThat(mapper.map(gone, ASIAN_GAMES)).isEqualTo(before);
    assertThat(before.candidate().item().record().capturedAt()).isNull();
  }

  @Test
  void leapDayBoundariesOfTheCaptureTime() {
    EventContext leap = event(LEAP_DAY, "시험 사건");

    assertThat(
            kept(recorded(titled(CLIP, "시험 영상", ""), "2000-02-29T23:59:59Z"), leap)
                .item()
                .record()
                .capturedAt())
        .isEqualTo("2000-02-29");
    Candidate nextDay =
        kept(recorded(titled(CLIP, "2000년 2월 시험 영상", ""), "2000-03-01T00:00:00Z"), leap);
    assertThat(nextDay.item().record().capturedAt()).isEqualTo("2000-02");
    assertThat(nextDay.item().record().datePrecision()).isEqualTo(DatePrecision.MONTH);
    assertThat(kept(titled(CLIP, "2000년 3월 시험 영상", "2001년 시험"), leap).item().record().capturedAt())
        .isNull();
    assertThat(kept(titled(CLIP, "1999년 시험 영상", ""), leap).item().record().capturedAt())
        .isEqualTo("1999");
  }
}
