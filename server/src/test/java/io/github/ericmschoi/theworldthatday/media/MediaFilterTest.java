package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.imageEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.linkOnly;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.with;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.MediaFilter.Reason;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Blocklist and wire agency exclusion of plan M3a3a T2 (2) and (4), on synthetic records. File
 * names, authors and IDs are made up; the excluded credits are the plan's initial list.
 */
class MediaFilterTest {

  static final List<String> INITIAL_CREDITS =
      List.of(
          "연합뉴스",
          "Yonhap",
          "뉴시스",
          "Newsis",
          "AP",
          "Associated Press",
          "Reuters",
          "AFP",
          "Getty",
          "EPA");

  private static final List<BlockEntry> BLOCKLIST =
      List.of(
          new BlockEntry("commons", "Example photo.jpg", null, null),
          new BlockEntry("commons", null, "Example Author", null),
          new BlockEntry("youtube", "AAAAAAAAAAA", null, null),
          new BlockEntry("youtube", null, null, "UCexampleexampleexample0"));

  private final MediaFilter filter =
      new MediaFilter(new MediaProperties(List.of(), List.of(), BLOCKLIST, INITIAL_CREDITS));

  private static MediaOrigin commons(String itemId, String author, String credit) {
    return new MediaOrigin("commons", itemId, author, credit, null);
  }

  private static MediaOrigin youtube(String videoId, String channel) {
    return new MediaOrigin("youtube", videoId, null, null, channel);
  }

  private Object check(Map<String, Object> fields, MediaOrigin origin) {
    return filter.check(record(fields), origin).orElse(null);
  }

  // blocklist

  @ParameterizedTest(name = "Commons file [{0}] is blocked")
  @ValueSource(
      strings = {
        "Example photo.jpg",
        "Example_photo.jpg",
        " Example_photo.jpg ",
        "File:Example photo.jpg"
      })
  void commonsFileNameIgnoresUnderscoresAndSpaces(String itemId) {
    assertThat(check(image(), commons(itemId, null, null))).isEqualTo(Reason.BLOCKED_ID);
  }

  @Test
  void anotherCommonsFileIsKept() {
    assertThat(check(image(), commons("Example photo 2.jpg", null, null))).isNull();
    assertThat(check(image(), commons("example photo.jpg", null, null))).isNull();
  }

  @ParameterizedTest(name = "author [{0}] is blocked")
  @ValueSource(strings = {"Example Author", "example author", "EXAMPLE AUTHOR", " Example Author "})
  void authorMatchesWholeTextIgnoringCase(String author) {
    assertThat(check(image(), commons("Other.jpg", author, null))).isEqualTo(Reason.BLOCKED_AUTHOR);
  }

  @Test
  void authorMustMatchCompletely() {
    assertThat(check(image(), commons("Other.jpg", "Example Author 2", null))).isNull();
    assertThat(check(image(), commons("Other.jpg", "Example", null))).isNull();
  }

  @Test
  void youtubeVideoAndChannelAreBlocked() {
    assertThat(check(videoEmbed(), youtube("AAAAAAAAAAA", null))).isEqualTo(Reason.BLOCKED_ID);
    assertThat(check(videoEmbed(), youtube("BBBBBBBBBBB", "UCexampleexampleexample0")))
        .isEqualTo(Reason.BLOCKED_CHANNEL);
    // video IDs are case-sensitive
    assertThat(check(videoEmbed(), youtube("aaaaaaaaaaa", "UCother"))).isNull();
  }

  @Test
  void anotherSourceIsKept() {
    assertThat(
            check(
                image(),
                new MediaOrigin("flickr", "Example photo.jpg", "Example Author", null, null)))
        .isNull();
    assertThat(check(image(), new MediaOrigin("commons", "AAAAAAAAAAA", null, null, null)))
        .isNull();
    assertThat(
            check(
                videoEmbed(),
                new MediaOrigin("commons", null, null, null, "UCexampleexampleexample0")))
        .isNull();
  }

  @Test
  void blocklistAppliesToEveryDisplayMode() {
    assertThat(check(imageEmbed(), commons("Example photo.jpg", null, null)))
        .isEqualTo(Reason.BLOCKED_ID);
    assertThat(check(linkOnly(), commons(null, "Example Author", null)))
        .isEqualTo(Reason.BLOCKED_AUTHOR);
  }

  // excluded credits

  @ParameterizedTest(name = "credit [{0}] on an image is excluded")
  @ValueSource(
      strings = {
        "AP Photo/Example Photographer",
        "ap photo",
        "(AP)",
        "ⓒ연합뉴스",
        "사진=연합뉴스 제공",
        "Yonhap News",
        "Reuters/Example",
        "Photo by Example / Getty Images",
        "EPA-EFE",
        "Associated Press",
        "뉴시스",
      })
  void excludedCreditOnAnImageIsDropped(String credit) {
    assertThat(check(image(), commons("Other.jpg", null, credit)))
        .isEqualTo(Reason.EXCLUDED_CREDIT);
  }

  @ParameterizedTest(name = "credit [{0}] is kept")
  @ValueSource(
      strings = {
        "Japan Example Society",
        "Apple",
        "Example Author",
        "CAPTAIN Example",
        "Reutersville",
        "AP2",
        "AP통신", // known limitation, plan M3a3a D1 (b)
        "EPA연합",
      })
  void wordsThatOnlyContainAnEntryAreKept(String credit) {
    assertThat(check(image(), commons("Other.jpg", null, credit))).isNull();
  }

  @Test
  void excludedCreditIsFoundInEveryField() {
    assertThat(check(with(image(), "source_name", "AP Images"), commons("Other.jpg", null, null)))
        .isEqualTo(Reason.EXCLUDED_CREDIT);
    assertThat(
            check(
                with(image(), "attribution", "Example · AP Photo · Wikimedia Commons"),
                commons("Other.jpg", null, null)))
        .isEqualTo(Reason.EXCLUDED_CREDIT);
    assertThat(check(image(), commons("Other.jpg", "Reuters staff", null)))
        .isEqualTo(Reason.EXCLUDED_CREDIT);
    assertThat(check(image(), commons("Other.jpg", null, null))).isNull();
  }

  @Test
  void videoFileIsChecked() {
    assertThat(check(videoFile(), commons("Other.webm", null, "AFP")))
        .isEqualTo(Reason.EXCLUDED_CREDIT);
  }

  @Test
  void providerEmbedsAreNotChecked() {
    Map<String, Object> image = with(imageEmbed(), "attribution", "Getty Images · provider-terms");
    Map<String, Object> video = with(videoEmbed(), "source_name", "연합뉴스");

    assertThat(check(image, new MediaOrigin("getty", "1", null, "AP Photo", null))).isNull();
    assertThat(check(video, youtube("CCCCCCCCCCC", "UCother"))).isNull();
  }

  @Test
  void blockReasonComesBeforeTheCredit() {
    assertThat(check(image(), commons("Example photo.jpg", null, "AP Photo")))
        .isEqualTo(Reason.BLOCKED_ID);
  }

  // never throws

  @Test
  void missingInputsNeverThrow() {
    assertThat(filter.check(null, null)).isEmpty();
    assertThat(filter.check(record(image()), null)).isEmpty();
    assertThat(filter.check(null, commons("Example photo.jpg", null, null)))
        .contains(Reason.BLOCKED_ID);
    assertThat(filter.check(record(image()), new MediaOrigin(null, null, null, null, null)))
        .isEmpty();
    assertThat(filter.check(record(with(image(), "display_mode", null)), null)).isEmpty();
  }

  @Test
  void emptySettingsKeepEverything() {
    MediaFilter none = new MediaFilter(new MediaProperties(List.of(), List.of()));

    assertThat(none.check(record(image()), commons("Example photo.jpg", "Example Author", "AP")))
        .isEmpty();
  }

  @Test
  void reasonNamesAreSnakeCase() {
    assertThat(List.of(Reason.values()).stream().map(Reason::value))
        .containsExactly("blocked_id", "blocked_author", "blocked_channel", "excluded_credit");
  }
}
