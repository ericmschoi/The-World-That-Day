package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.VideoResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a4b T2 (4), "차단·통신사 제외·R1~R9는 사진과 같은 경로", for the safeguards the video mapping test does
 * not reach: the blocklist (by file name and by author, safeguard 5), a non-free file, and a video
 * without a source page (no {@code source_url}, rule R2: not shown). Synthetic pages with made-up
 * names; the date is the sample date 1969-07-20.
 */
class CommonsFileMapperVideoSafeguardsTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final LocalDate MOON_DAY = LocalDate.of(1969, 7, 20);

  private static CommonsFileMapper mapper(List<BlockEntry> blocklist) {
    MediaProperties media = new MediaProperties(List.of(), List.of(), blocklist, List.of());
    return new CommonsFileMapper(
        new MediaFilter(media),
        new MediaNormalizer(media),
        new MutableClock(Instant.parse("2026-10-24T21:15:44Z"), ClockConfig.SERVICE_ZONE));
  }

  private static ObjectNode page() {
    return (ObjectNode)
        JSON.readTree(
            """
            {"pageid": 9000001, "ns": 6, "title": "File:Synthetic moon video.webm",
             "videoinfo": [{"url": "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_moon_video.webm",
               "descriptionurl": "https://commons.wikimedia.org/wiki/File:Synthetic_moon_video.webm",
               "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_moon_video.webm/640px--Synthetic_moon_video.webm.jpg",
               "thumbwidth": 640, "thumbheight": 360, "duration": 95.5,
               "mime": "video/webm", "mediatype": "VIDEO",
               "extmetadata": {
                 "Artist": {"value": "Synthetic Author"},
                 "LicenseShortName": {"value": "CC0"},
                 "DateTimeOriginal": {"value": "1969-07-20"}}}]}
            """);
  }

  @Test
  void theSyntheticPageIsKeptWithoutABlocklistEntry() {
    assertThat(mapper(List.of()).mapVideo(page(), "world", MOON_DAY).drop()).isNull();
  }

  @Test
  void aBlockedFileNameIsLeftOut() {
    CommonsFileMapper mapper =
        mapper(List.of(new BlockEntry("commons", "Synthetic moon video.webm", null, null)));

    VideoResult result = mapper.mapVideo(page(), "world", MOON_DAY);

    assertThat(result.drop()).isEqualTo(Drop.FILTERED);
    assertThat(result.item()).isNull();
  }

  @Test
  void aBlockedAuthorIsLeftOut() {
    CommonsFileMapper mapper =
        mapper(List.of(new BlockEntry("commons", null, "Synthetic Author", null)));

    assertThat(mapper.mapVideo(page(), "world", MOON_DAY).drop()).isEqualTo(Drop.FILTERED);
  }

  @Test
  void aBlockEntryOfAnotherSourceOrFileLeavesItAlone() {
    CommonsFileMapper mapper =
        mapper(
            List.of(
                new BlockEntry("youtube", "Synthetic moon video.webm", null, null),
                new BlockEntry("commons", "Another file.webm", null, null)));

    assertThat(mapper.mapVideo(page(), "world", MOON_DAY).drop()).isNull();
  }

  @Test
  void aNonFreeFileIsLeftOut() {
    ObjectNode page = page();
    ((ObjectNode) page.path("videoinfo").path(0).path("extmetadata"))
        .putObject("NonFree")
        .put("value", "true");

    assertThat(mapper(List.of()).mapVideo(page, "world", MOON_DAY).drop()).isEqualTo(Drop.NON_FREE);
  }

  @Test
  void aVideoWithoutItsSourcePageIsNotShown() {
    ObjectNode page = page();
    ((ObjectNode) page.path("videoinfo").path(0)).remove("descriptionurl");

    VideoResult result = mapper(List.of()).mapVideo(page, "world", MOON_DAY);

    assertThat(result.item()).isNull();
    assertThat(result.drop()).isEqualTo(Drop.RULE);
  }
}
