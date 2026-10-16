package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoEmbed;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaFilter.Reason;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a3b T0 (1) and (4), carried-over M3a3a review recommendations 1 and 3: blocklist values on
 * either side lose surrounding Unicode spaces ({@link MediaText#strip}), and a Commons id's first
 * letter written in lowercase also matches the capitalized file name. Names and IDs are synthetic;
 * spaces are written as escapes.
 */
class MediaFilterSpacesTest {

  private static MediaFilter filter(BlockEntry... entries) {
    return new MediaFilter(new MediaProperties(List.of(), List.of(), List.of(entries), List.of()));
  }

  private static Optional<Reason> commons(MediaFilter filter, String itemId, String author) {
    return filter.check(record(image()), new MediaOrigin("commons", itemId, author, null, null));
  }

  private static Optional<Reason> youtube(MediaFilter filter, String videoId, String channel) {
    return filter.check(
        record(videoEmbed()), new MediaOrigin("youtube", videoId, null, null, channel));
  }

  // ---- (1) surrounding Unicode spaces ----

  @ParameterizedTest(name = "author [{0}] is blocked")
  @ValueSource(strings = {"Example Author ", "　Example Author", "Example Author "})
  void sourceAuthorWithUnicodeSpacesIsBlocked(String author) {
    MediaFilter filter = filter(new BlockEntry("commons", null, "Example Author", null));

    assertThat(commons(filter, "Other.jpg", author)).contains(Reason.BLOCKED_AUTHOR);
  }

  @Test
  void configuredAuthorWithATrailingNoBreakSpaceBlocks() {
    BlockEntry entry = new BlockEntry("commons", null, "Example Author ", null);

    assertThat(entry.author()).isEqualTo("Example Author");
    assertThat(commons(filter(entry), "Other.jpg", "Example Author"))
        .contains(Reason.BLOCKED_AUTHOR);
  }

  @Test
  void channelWithATrailingNoBreakSpaceIsBlockedOnBothSides() {
    BlockEntry entry = new BlockEntry("youtube", null, null, "UCexampleexampleexample0 ");

    assertThat(entry.channel()).isEqualTo("UCexampleexampleexample0");
    assertThat(youtube(filter(entry), "BBBBBBBBBBB", "UCexampleexampleexample0"))
        .contains(Reason.BLOCKED_CHANNEL);
    MediaFilter plain = filter(new BlockEntry("youtube", null, null, "UCexampleexampleexample0"));
    assertThat(youtube(plain, "BBBBBBBBBBB", "UCexampleexampleexample0 "))
        .contains(Reason.BLOCKED_CHANNEL);
  }

  @Test
  void idWithATrailingNoBreakSpaceIsBlockedOnBothSides() {
    BlockEntry video = new BlockEntry("youtube", "AAAAAAAAAAA ", null, null);
    BlockEntry photo = new BlockEntry("commons", "Example photo.jpg ", null, null);

    assertThat(video.id()).isEqualTo("AAAAAAAAAAA");
    assertThat(youtube(filter(video), "AAAAAAAAAAA", null)).contains(Reason.BLOCKED_ID);
    assertThat(commons(filter(photo), "Example photo.jpg", null)).contains(Reason.BLOCKED_ID);
    MediaFilter plain =
        filter(
            new BlockEntry("youtube", "AAAAAAAAAAA", null, null),
            new BlockEntry("commons", "Example photo.jpg", null, null));
    assertThat(youtube(plain, "AAAAAAAAAAA ", null)).contains(Reason.BLOCKED_ID);
    assertThat(commons(plain, "Example photo.jpg ", null)).contains(Reason.BLOCKED_ID);
    assertThat(commons(plain, "File: Example_photo.jpg ", null)).contains(Reason.BLOCKED_ID);
  }

  @Test
  void sourceWithUnicodeSpacesStillFindsItsEntries() {
    MediaFilter filter = filter(new BlockEntry("commons", "Example photo.jpg", null, null));

    assertThat(
            filter.check(
                record(image()),
                new MediaOrigin(" commons　", "Example photo.jpg", null, null, null)))
        .contains(Reason.BLOCKED_ID);
  }

  @Test
  void innerSpacesStillMatter() {
    MediaFilter filter = filter(new BlockEntry("commons", null, "Example Author", null));

    assertThat(commons(filter, "Other.jpg", "Example Author")).isEmpty();
    assertThat(commons(filter, "Other.jpg", "Example Author 2 ")).isEmpty();
  }

  // ---- (4) Commons first letter ----

  @ParameterizedTest(name = "item [{0}] is blocked by a lowercase first letter")
  @ValueSource(strings = {"Example photo.jpg", "File:example_photo.jpg", "File:Example_photo.jpg"})
  void lowercaseFirstLetterMatchesTheCapital(String itemId) {
    MediaFilter filter = filter(new BlockEntry("commons", "example photo.jpg", null, null));

    assertThat(commons(filter, itemId, null)).contains(Reason.BLOCKED_ID);
  }

  @Test
  void onlyTheFirstLetterIgnoresCase() {
    MediaFilter filter = filter(new BlockEntry("commons", "example photo.jpg", null, null));

    assertThat(commons(filter, "Example Photo.jpg", null)).isEmpty();
    assertThat(commons(filter, "EXAMPLE PHOTO.JPG", null)).isEmpty();
    assertThat(commons(filter, "Fxample photo.jpg", null)).isEmpty();
  }

  @Test
  void firstLetterOutsideTheBasicPlaneAndWithoutCase() {
    // U+10428 DESERET SMALL LETTER LONG I; its capital is U+10400
    MediaFilter deseret = filter(new BlockEntry("commons", "𐐨xample.jpg", null, null));
    assertThat(commons(deseret, "𐐀xample.jpg", null)).contains(Reason.BLOCKED_ID);
    assertThat(commons(deseret, "𐐀Xample.jpg", null)).isEmpty();

    MediaFilter hangul = filter(new BlockEntry("commons", "예시 사진.jpg", null, null));
    assertThat(commons(hangul, "File:예시_사진.jpg", null)).contains(Reason.BLOCKED_ID);
  }

  @Test
  void otherSourcesCompareIdsWithCase() {
    MediaFilter filter = filter(new BlockEntry("youtube", "aAAAAAAAAAA", null, null));

    assertThat(youtube(filter, "AAAAAAAAAAA", null)).isEmpty();
    assertThat(youtube(filter, "aAAAAAAAAAA", null)).contains(Reason.BLOCKED_ID);
  }
}
