package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.VideoSource;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.VideoResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a4b T2 (4) and (5): the video mapping of "미디어 레코드" on the recorded 1969-07-20 files
 * (Apollo 11, a public historical event) and on synthetic pages with made-up names and authors. The
 * photo mapping is covered, unchanged, by {@code CommonsFileMapperTest}.
 */
class CommonsFileMapperVideoTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);
  private static final String WORLD = "world";
  private static final List<String> CREDITS =
      List.of("연합뉴스", "Yonhap", "AP", "Associated Press", "Reuters", "AFP", "Getty", "EPA");

  private final CommonsFileMapper mapper =
      new CommonsFileMapper(
          new MediaFilter(new MediaProperties(List.of(), List.of(), List.of(), CREDITS)),
          new MediaNormalizer(new MediaProperties(List.of(), List.of())),
          new MutableClock(Instant.parse("2026-10-24T21:15:44.5Z"), ClockConfig.SERVICE_ZONE));

  private static List<JsonNode> pages(String fixture) {
    JsonNode pages =
        JSON.readTree(ActionApiCommonsVideoClientTest.fixture(fixture)).path("query").path("pages");
    return StreamSupport.stream(pages.spliterator(), false).toList();
  }

  private static JsonNode page(String fixture, String title) {
    return pages(fixture).stream()
        .filter(p -> p.path("title").asString().equals(title))
        .findFirst()
        .orElseThrow();
  }

  /** A valid synthetic video page: a CC0 WebM taken on 1969-07-20, with two derivatives. */
  private static ObjectNode synthetic() {
    return (ObjectNode)
        JSON.readTree(
            """
            {"pageid": 9000001, "ns": 6, "title": "File:Synthetic moon video.webm",
             "videoinfo": [{"url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_moon_video.webm",
               "descriptionurl": "https://commons.wikimedia.org/wiki/File:Synthetic_moon_video.webm",
               "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_moon_video.webm/640px--Synthetic_moon_video.webm.jpg",
               "thumbwidth": 640, "thumbheight": 360, "width": 1280, "height": 720,
               "duration": 95.5, "mime": "video/webm", "mediatype": "VIDEO",
               "extmetadata": {
                 "Artist": {"value": "<b>Synthetic</b> Author"},
                 "LicenseShortName": {"value": "CC0"},
                 "DateTimeOriginal": {"value": "1969-07-20"},
                 "ImageDescription": {"value": "<p>Synthetic &amp; invented description</p>"}},
               "derivatives": [
                 {"src": "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_moon_video.webm",
                  "type": "video/webm; codecs=\\"vp9, opus\\"", "width": 1280, "height": 720},
                 {"src": "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/ab/Synthetic_moon_video.webm/Synthetic_moon_video.webm.480p.vp9.webm",
                  "type": "video/webm; codecs=\\"vp9, opus\\"", "transcodekey": "480p.vp9.webm", "height": 480},
                 {"src": "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/ab/Synthetic_moon_video.webm/Synthetic_moon_video.webm.240p.mjpeg.mov",
                  "type": "video/quicktime", "transcodekey": "240p.mjpeg.mov", "height": 240},
                 {"src": "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/ab/Synthetic_moon_video.webm/Synthetic_moon_video.webm.240p.vp9.webm",
                  "type": "VIDEO/WEBM ; codecs=\\"vp9\\"", "transcodekey": "240p.vp9.webm", "height": 240},
                 {"src": "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/ab/Synthetic_moon_video.webm/Synthetic_moon_video.webm.ogg",
                  "type": "audio/ogg", "transcodekey": "ogg"},
                 {"type": "video/webm", "transcodekey": "360p.vp9.webm", "height": 360}]}]}
            """);
  }

  private static ObjectNode info(ObjectNode page) {
    return (ObjectNode) page.path("videoinfo").path(0);
  }

  private static ObjectNode meta(ObjectNode page, String key, String value) {
    ((ObjectNode) info(page).path("extmetadata")).putObject(key).put("value", value);
    return page;
  }

  // ---- recorded ----

  @Test
  void theRecordedP10FileIsAVideoFileOfTheDay() {
    VideoResult result =
        mapper.mapVideo(
            page("p10-files-1969-07-20.json", "File:AP11 FINAL APPROACH.ogv"), WORLD, MOON_DAY);

    assertThat(result.drop()).isNull();
    MediaRecord record = result.item().record();
    assertThat(record.id()).isEqualTo("commons-7366097");
    assertThat(record.type()).isEqualTo(Type.VIDEO);
    assertThat(record.displayMode()).isEqualTo(DisplayMode.VIDEO_FILE);
    assertThat(record.url())
        .isEqualTo(
            "https://upload.wikimedia.org/wikipedia/commons/1/19/AP11_FINAL_APPROACH.ogv"
                + "?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original");
    assertThat(record.capturedAt()).isEqualTo("1969-07-20");
    assertThat(record.datePrecision()).isEqualTo(DatePrecision.DAY);
    assertThat(result.item().dateRelation().kind()).isEqualTo(DateRelation.Kind.SAME_DAY);
    // the application/ogg original stays in url only: the 240p WebM, then the QuickTime file
    assertThat(record.videoSources())
        .containsExactly(
            new VideoSource(
                "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/19/AP11_FINAL_APPROACH.ogv/AP11_FINAL_APPROACH.ogv.240p.vp9.webm",
                "video/webm",
                240),
            new VideoSource(
                "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/19/AP11_FINAL_APPROACH.ogv/AP11_FINAL_APPROACH.ogv.360p.mpeg4.mov",
                "video/quicktime",
                240));
    assertThat(record.durationSeconds()).isEqualTo(270.0);
    assertThat(record.license()).isEqualTo("Public domain");
    assertThat(record.licenseStatus()).isEqualTo(LicenseStatus.FREE);
    assertThat(record.sourceName()).isEqualTo("Wikimedia Commons");
    assertThat(record.sourceUrl())
        .isEqualTo("https://commons.wikimedia.org/wiki/File:AP11_FINAL_APPROACH.ogv");
    assertThat(record.thumbnailUrl()).startsWith("https://thumb.wikimedia.org/");
    assertThat(record.thumbnailWidth()).isEqualTo(320);
    assertThat(record.attribution())
        .isEqualTo(
            "René and Jonathan Cantin / Apollo Lunar Surface Journal · Public domain"
                + " · Wikimedia Commons");
    assertThat(record.placeRef()).isEqualTo(WORLD);
    assertThat(record.lat()).isNull();
    assertThat(record.embedUrl()).isNull();
    assertThat(record.provider()).isNull();
    assertThat(record.render()).isNull();
    assertThat(record.linkOnlyReason()).isNull();
    assertThat(record.retrievedAt()).isEqualTo("2026-10-24T21:15:44Z");
    assertThat(result.title()).isEqualTo("AP11 FINAL APPROACH.ogv");
    assertThat(result.description())
        .isEqualTo(
            "Apollo 11 - Landing on the Sea of Tranquility - July 20, 1969, 16mm landing film.");
    assertThat(result.originalCapture())
        .isEqualTo(new CaptureTime("1969-07-20", DatePrecision.DAY));
  }

  @Test
  void recordedVideosHaveNoMimeParameterAndAWebmFirst() {
    List<JsonNode> all =
        List.of(
                pages("category-apollo-11.json"),
                pages("date-category-1969-07-20.json"),
                pages("p10-files-1969-07-20.json"))
            .stream()
            .flatMap(List::stream)
            .toList();

    assertThat(all).hasSize(7);
    for (JsonNode page : all) {
      VideoResult result = mapper.mapVideo(page, WORLD, MOON_DAY);
      String title = page.path("title").asString();
      assertThat(result.drop()).as(title).isNull();
      List<VideoSource> sources = result.item().record().videoSources();
      assertThat(sources).as(title).isNotEmpty();
      assertThat(sources.getFirst().mime()).as(title).isEqualTo("video/webm");
      assertThat(sources).as(title).allSatisfy(s -> assertThat(s.mime()).doesNotContain(";"));
      assertThat(sources)
          .as(title)
          .extracting(VideoSource::height)
          .isSortedAccordingTo(Integer::compare);
    }
  }

  @Test
  void aRecordedWebmOriginalComesLastAndTakenOnIsRead() {
    VideoResult result =
        mapper.mapVideo(
            page(
                "date-category-1969-07-20.json",
                "File:President Nixon speaking with astronauts Armstrong and Aldrin on the Moon.webm"),
            WORLD,
            MOON_DAY);

    MediaRecord record = result.item().record();
    assertThat(record.capturedAt()).isEqualTo("1969-07-20");
    assertThat(record.videoSources().getLast().url()).isEqualTo(record.url());
    assertThat(record.videoSources().getLast().mime()).isEqualTo("video/webm");
    assertThat(record.videoSources().getLast().height()).isEqualTo(480);
    assertThat(record.videoSources()).extracting(VideoSource::url).doesNotHaveDuplicates();
  }

  @Test
  void aRecordedCaptureAfterTheEventIsUnknownButKeptAsMaterial() {
    // "Historic Apollo 11 Moonwalk Footage" says DateTimeOriginal 2019-07-21 (an upload date)
    VideoResult result =
        mapper.mapVideo(
            page("category-apollo-11.json", "File:Historic Apollo 11 Moonwalk Footage.webm"),
            WORLD,
            MOON_DAY);

    assertThat(result.item().record().capturedAt()).isNull();
    assertThat(result.item().record().datePrecision()).isNull();
    assertThat(result.originalCapture())
        .isEqualTo(new CaptureTime("2019-07-21", DatePrecision.DAY));
  }

  // ---- synthetic ----

  @Test
  void derivativesAreOrderedByHeightThenWebmFirstAndTheOriginalLast() {
    MediaRecord record = mapper.mapVideo(synthetic(), WORLD, MOON_DAY).item().record();

    assertThat(record.videoSources())
        .extracting(VideoSource::mime, VideoSource::height)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("video/webm", 240),
            org.assertj.core.groups.Tuple.tuple("video/quicktime", 240),
            org.assertj.core.groups.Tuple.tuple("video/webm", 480),
            org.assertj.core.groups.Tuple.tuple("video/webm", 720));
    assertThat(record.videoSources().getLast().url()).isEqualTo(record.url());
    assertThat(record.durationSeconds()).isEqualTo(95.5);
    assertThat(record.attribution()).isEqualTo("Synthetic Author · CC0 · Wikimedia Commons");
    assertThat(record.description()).isEqualTo("Synthetic & invented description");
  }

  @Test
  void aCaptureAfterTheRequestedDateIsUnknownWithTheParsedValueAsMaterial() {
    VideoResult result =
        mapper.mapVideo(meta(synthetic(), "DateTimeOriginal", "2019-07-21"), WORLD, MOON_DAY);

    assertThat(result.item().record().capturedAt()).isNull();
    assertThat(result.item().record().datePrecision()).isNull();
    assertThat(result.item().dateRelation().kind()).isEqualTo(DateRelation.Kind.UNKNOWN);
    assertThat(result.originalCapture())
        .isEqualTo(new CaptureTime("2019-07-21", DatePrecision.DAY));
  }

  @Test
  void captureBoundariesKeepTheSameDayMonthAndYear() {
    Map<String, String> kept =
        Map.of(
            "1969-07-20",
            "1969-07-20",
            "1969-07",
            "1969-07",
            "1969",
            "1969",
            "1969-07-19",
            "1969-07-19");
    kept.forEach(
        (raw, at) ->
            assertThat(
                    mapper
                        .mapVideo(meta(synthetic(), "DateTimeOriginal", raw), WORLD, MOON_DAY)
                        .item()
                        .record()
                        .capturedAt())
                .as(raw)
                .isEqualTo(at));
    for (String later : List.of("1969-07-21", "1969-08", "1970")) {
      assertThat(
              mapper
                  .mapVideo(meta(synthetic(), "DateTimeOriginal", later), WORLD, MOON_DAY)
                  .item()
                  .record()
                  .capturedAt())
          .as(later)
          .isNull();
    }
    // leap day: the day itself is kept, the day after is not
    LocalDate leapDay = LocalDate.of(2000, 2, 29);
    assertThat(
            mapper
                .mapVideo(meta(synthetic(), "DateTimeOriginal", "2000-02-29"), WORLD, leapDay)
                .item()
                .record()
                .capturedAt())
        .isEqualTo("2000-02-29");
    assertThat(
            mapper
                .mapVideo(meta(synthetic(), "DateTimeOriginal", "2000-03-01"), WORLD, leapDay)
                .item()
                .record()
                .capturedAt())
        .isNull();
  }

  @Test
  void theUploadTimeIsNeverACaptureTime() {
    ObjectNode page = synthetic();
    ((ObjectNode) info(page).path("extmetadata")).remove("DateTimeOriginal");
    meta(page, "DateTime", "1969-07-20 00:00:00");

    VideoResult result = mapper.mapVideo(page, WORLD, MOON_DAY);

    assertThat(result.item().record().capturedAt()).isNull();
    assertThat(result.originalCapture()).isEqualTo(CaptureTime.UNKNOWN);
  }

  @Test
  void anAgencyAuthorIsFiltered() {
    assertThat(mapper.mapVideo(meta(synthetic(), "Artist", "Associated Press"), WORLD, MOON_DAY))
        .isEqualTo(new VideoResult(null, Drop.FILTERED, null, null, null));
  }

  @Test
  void onlyVideoMediaTypeIsKept() {
    ObjectNode audio = synthetic();
    info(audio).put("mediatype", "AUDIO").put("mime", "application/ogg");
    ObjectNode noType = synthetic();
    info(noType).remove("mediatype");

    assertThat(mapper.mapVideo(audio, WORLD, MOON_DAY).drop()).isEqualTo(Drop.MIME);
    assertThat(mapper.mapVideo(noType, WORLD, MOON_DAY).drop()).isEqualTo(Drop.MIME);
  }

  @Test
  void aMissingPageOrOneWithoutVideoInfoIsMissing() {
    ObjectNode missing =
        (ObjectNode) JSON.readTree("{\"ns\":6,\"title\":\"File:X.webm\",\"missing\":true}");
    ObjectNode imageInfoOnly = synthetic();
    imageInfoOnly.set("imageinfo", imageInfoOnly.remove("videoinfo"));

    assertThat(mapper.mapVideo(missing, WORLD, MOON_DAY).drop()).isEqualTo(Drop.MISSING);
    assertThat(mapper.mapVideo(imageInfoOnly, WORLD, MOON_DAY).drop()).isEqualTo(Drop.MISSING);
  }

  @Test
  void withoutAnyVideoDerivativeAVideoOriginalIsTheOnlySource() {
    ObjectNode page = synthetic();
    ((ArrayNode) info(page).path("derivatives")).removeAll();

    assertThat(mapper.mapVideo(page, WORLD, MOON_DAY).item().record().videoSources())
        .extracting(VideoSource::height)
        .containsExactly(720);

    info(page).put("mime", "application/ogg");
    MediaRecord ogg = mapper.mapVideo(page, WORLD, MOON_DAY).item().record();
    assertThat(ogg.videoSources()).isNull();
    assertThat(ogg.url()).isNotNull();
  }

  @Test
  void gpsPositionReplacesThePlace() {
    ObjectNode page = meta(meta(synthetic(), "GPSLatitude", "28.5"), "GPSLongitude", "-80.6");

    MediaRecord record = mapper.mapVideo(page, WORLD, MOON_DAY).item().record();

    assertThat(record.lat()).isEqualTo(28.5);
    assertThat(record.lng()).isEqualTo(-80.6);
    assertThat(record.placeRef()).isNull();
  }
}
