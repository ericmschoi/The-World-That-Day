package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.EXAMPLE_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.imageEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.linkOnly;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.sdk;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Rules R1 to R9 of plan M3a3 on synthetic records: a broken record is dropped, never changed. */
class MediaNormalizerTest {

  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);

  private static Optional<MediaItem> normalize(MediaNormalizer normalizer, Map<String, Object> m) {
    return normalizer.normalize(record(m), OLYMPICS);
  }

  static Stream<Arguments> validBases() {
    return Stream.of(
        arguments("image", image()),
        arguments("image_embed", imageEmbed()),
        arguments("video_file", videoFile()),
        arguments("video_embed", videoEmbed()),
        arguments("sdk", sdk()),
        arguments("link_only", linkOnly()));
  }

  @ParameterizedTest(name = "{0} passes unchanged with the example provider allowed")
  @MethodSource("validBases")
  void everyDisplayModePassesUnchanged(String mode, Map<String, Object> base) {
    MediaRecord input = record(base);

    Optional<MediaItem> item = EXAMPLE_NORMALIZER.normalize(input, OLYMPICS);

    assertThat(item).isPresent();
    assertThat(item.get().record()).isEqualTo(input);
    assertThat(item.get().record().displayMode().value()).isEqualTo(mode);
  }

  @Test
  void dateRelationIsFilledFromTheRequestedDate() {
    assertThat(normalize(DEFAULT_NORMALIZER, image()).orElseThrow().dateRelation())
        .isEqualTo(new DateRelation(Kind.SAME_DAY, 0));
    assertThat(
            DEFAULT_NORMALIZER
                .normalize(record(image()), LocalDate.of(2002, 6, 22))
                .orElseThrow()
                .dateRelation())
        .isEqualTo(new DateRelation(Kind.YEARS_APART, -14));
  }

  @Test
  void missingLicensePassesAsUnknown() {
    Map<String, Object> m = with(with(image(), "license", null), "license_status", "unknown");
    m.put("attribution", "No author information · Licence not verified · Wikimedia Commons");

    MediaItem item = normalize(DEFAULT_NORMALIZER, m).orElseThrow();

    assertThat(item.record().license()).isNull();
    assertThat(item.record().licenseStatus()).isEqualTo(MediaRecord.LicenseStatus.UNKNOWN);
    assertThat(item.record().displayMode()).isEqualTo(DisplayMode.IMAGE);
  }

  @Test
  void unknownCaptureTimeIsShownWithKindUnknown() {
    Map<String, Object> m = with(with(image(), "captured_at", null), "date_precision", null);

    MediaItem item = normalize(DEFAULT_NORMALIZER, m).orElseThrow();

    assertThat(item.record().capturedAt()).isNull();
    assertThat(item.record().displayMode()).isEqualTo(DisplayMode.IMAGE);
    assertThat(item.dateRelation()).isEqualTo(new DateRelation(Kind.UNKNOWN, 0));
  }

  @ParameterizedTest(name = "license_url [{0}] becomes null, the record stays")
  @MethodSource("malformedAddresses")
  void malformedLicenseUrlIsDroppedAlone(String licenseUrl) {
    MediaItem item =
        normalize(DEFAULT_NORMALIZER, with(image(), "license_url", licenseUrl)).orElseThrow();

    assertThat(item.record().licenseUrl()).isNull();
    assertThat(item.record().id()).isEqualTo("example-image");
  }

  @Test
  void wellFormedLicenseUrlIsKept() {
    String url = "https://creativecommons.org/publicdomain/mark/1.0/";

    assertThat(
            normalize(DEFAULT_NORMALIZER, with(image(), "license_url", url))
                .orElseThrow()
                .record()
                .licenseUrl())
        .isEqualTo(url);
  }

  static Stream<String> malformedAddresses() {
    return Stream.of("", "javascript:alert(1)", "ftp://example.test/a", "//example.test/a");
  }

  static Stream<Arguments> brokenRecords() {
    return Stream.of(
        // R1: source fields and other always-required fields
        arguments("R1 empty source_url", with(image(), "source_url", "")),
        arguments("R1 blank source_name", with(image(), "source_name", "  ")),
        arguments("R1 missing source_name", with(image(), "source_name", null)),
        arguments("R1 empty attribution", with(image(), "attribution", "")),
        arguments("R1 missing id", with(image(), "id", null)),
        arguments("R1 missing type", with(image(), "type", null)),
        arguments("R1 missing display_mode", with(image(), "display_mode", null)),
        arguments("R1 missing license_status", with(image(), "license_status", null)),
        arguments("R1 missing retrieved_at", with(image(), "retrieved_at", null)),
        arguments("R1 retrieved_at not a date-time", with(image(), "retrieved_at", "2026-10-13")),
        arguments("R1 no place", with(image(), "place_ref", null)),
        arguments("R1 place_ref pattern", with(image(), "place_ref", "Seoul")),
        arguments("R1 lat without lng", with(sdk(), "lng", null)),
        arguments("R1 lat out of range", with(sdk(), "lat", 91.0)),
        // R2 image
        arguments("R2 image without url", with(image(), "url", null)),
        arguments("R2 image without thumbnail_url", with(image(), "thumbnail_url", null)),
        // R3 image_embed
        arguments("R3 image_embed without embed_url", with(imageEmbed(), "embed_url", null)),
        arguments("R3 image_embed without provider", with(imageEmbed(), "provider", null)),
        arguments("R3 image_embed provider not allowed", with(imageEmbed(), "provider", "getty")),
        arguments("R3 image_embed with url", with(imageEmbed(), "url", image().get("url"))),
        arguments(
            "R3 image_embed with thumbnail_url",
            with(imageEmbed(), "thumbnail_url", image().get("thumbnail_url"))),
        // R4 video_file
        arguments("R4 video_file without url or sources", with(videoFile(), "video_sources", null)),
        arguments(
            "R4 video_file with empty sources", with(videoFile(), "video_sources", List.of())),
        arguments(
            "R4 video source with a non-video mime",
            with(
                videoFile(),
                "video_sources",
                List.of(
                    Map.of(
                        "url", "https://example.test/a.webm", "mime", "text/plain", "height", 1)))),
        // R5 video_embed
        arguments("R5 video_embed without embed_url", with(videoEmbed(), "embed_url", null)),
        arguments("R5 video_embed without provider", with(videoEmbed(), "provider", null)),
        arguments("R5 video_embed provider not allowed", with(videoEmbed(), "provider", "vimeo")),
        arguments("R5 video_embed with url", with(videoEmbed(), "url", image().get("url"))),
        // R6 sdk
        arguments("R6 sdk without render", with(sdk(), "render", null)),
        arguments("R6 sdk without provider", with(sdk(), "provider", null)),
        arguments("R6 sdk with url", with(sdk(), "url", image().get("url"))),
        arguments(
            "R6 sdk with thumbnail_url",
            with(sdk(), "thumbnail_url", image().get("thumbnail_url"))),
        // R7 link_only
        arguments("R7 link_only without reason", with(linkOnly(), "link_only_reason", null)),
        arguments("R7 link_only with url", with(linkOnly(), "url", image().get("url"))),
        arguments(
            "R7 link_only with embed_url",
            with(linkOnly(), "embed_url", "https://embed.example.test/image/1")),
        arguments("R7 reason on another mode", with(image(), "link_only_reason", "embed_disabled")),
        // R8 precision and captured_at shape
        arguments("R8 day with a month", with(image(), "captured_at", "1988-09")),
        arguments("R8 day that does not exist", with(image(), "captured_at", "1988-02-30")),
        arguments(
            "R8 month with a day",
            with(with(image(), "date_precision", "month"), "captured_at", "1988-09-17")),
        arguments(
            "R8 month 13",
            with(with(image(), "date_precision", "month"), "captured_at", "1988-13")),
        arguments(
            "R8 year with a day",
            with(with(image(), "date_precision", "year"), "captured_at", "1988-09-17")),
        arguments(
            "R8 approx with a day",
            with(with(image(), "date_precision", "approx"), "captured_at", "1988-09-17")),
        arguments("R8 current with a date", with(image(), "date_precision", "current")),
        arguments("R8 unknown precision with a date", with(image(), "date_precision", null)),
        arguments("R8 day without a date", with(image(), "captured_at", null)),
        // R9 addresses
        arguments("R9 javascript thumbnail_url", with(image(), "thumbnail_url", "javascript:x()")),
        arguments("R9 relative url", with(image(), "url", "/wiki/File:Example.jpg")),
        arguments("R9 source_url not http", with(image(), "source_url", "ftp://example.test/a")),
        arguments("R9 embed_url with a space", with(videoEmbed(), "embed_url", "https://a b")),
        arguments(
            "R9 video source url",
            with(
                videoFile(),
                "video_sources",
                List.of(Map.of("url", "data:video/webm", "mime", "video/webm", "height", 480)))),
        // sizes
        arguments("zero thumbnail width", with(image(), "thumbnail_width", 0)),
        arguments("negative duration", with(videoFile(), "duration_seconds", -1.0)),
        arguments("zero duration", with(videoFile(), "duration_seconds", 0.0)),
        // optional text is null or not blank, as the schema's nonEmptyString
        arguments("empty description", with(image(), "description", "")),
        arguments("empty license", with(image(), "license", " ")),
        arguments("empty provider", with(image(), "provider", "")),
        // retrieved_at is UTC to the second
        arguments(
            "R1 retrieved_at with an offset",
            with(image(), "retrieved_at", "2026-10-13T12:00:00+09:00")),
        arguments(
            "R1 retrieved_at with a fraction",
            with(image(), "retrieved_at", "2026-10-13T03:00:00.5Z")),
        arguments(
            "R1 retrieved_at without seconds", with(image(), "retrieved_at", "2026-10-13T03:00Z")),
        // PR #12 review: a leap second, on a day without one (plan M3a3a T2 (9))
        arguments(
            "R1 retrieved_at with second 60",
            with(image(), "retrieved_at", "2026-10-13T23:59:60Z")));
  }

  @Test
  void lastSecondOfTheDayPasses() {
    assertThat(normalize(DEFAULT_NORMALIZER, with(image(), "retrieved_at", "2026-10-13T23:59:59Z")))
        .isPresent();
  }

  /**
   * Plan M3a3a T2 (10): values made only of the space set (Unicode category Z included) are blank.
   * Kept out of {@link #brokenRecords} until the schema matches (T2b); T3 adds the parity cases.
   */
  @ParameterizedTest(name = "{0} of only spaces [{1}] is dropped")
  @MethodSource("unicodeBlankFields")
  void unicodeSpacesAreBlank(String field, String blank) {
    assertThat(normalize(EXAMPLE_NORMALIZER, with(image(), field, blank))).isEmpty();
  }

  static Stream<Arguments> unicodeBlankFields() {
    List<String> blanks = List.of("\u00A0", "\u2007", "\u202F", "\u3000", "\u00A0\u2007");
    Stream<Arguments> required =
        Stream.of("attribution", "id", "source_name")
            .flatMap(field -> blanks.stream().map(blank -> arguments(field, blank)));
    Stream<Arguments> optional =
        Stream.of(arguments("license", "\u00A0"), arguments("description", "\u00A0"));
    Stream<Arguments> existing =
        Stream.of(arguments("attribution", "  "), arguments("attribution", ""));
    return Stream.of(required, optional, existing).flatMap(s -> s);
  }

  @ParameterizedTest(name = "attribution [{0}] is kept")
  @ValueSource(strings = {"\u00A0작가", "Example\u00A0Author"})
  void textWithUnicodeSpacesIsKept(String attribution) {
    assertThat(normalize(DEFAULT_NORMALIZER, with(image(), "attribution", attribution)))
        .isPresent();
  }

  @ParameterizedTest(name = "{0} is dropped")
  @MethodSource("brokenRecords")
  void brokenRecordsAreDropped(String rule, Map<String, Object> broken) {
    assertThat(normalize(EXAMPLE_NORMALIZER, broken)).isEmpty();
  }

  @Test
  void videoFileWithOnlyAUrlPasses() {
    Map<String, Object> m = with(videoFile(), "video_sources", null);
    m.put("url", "https://upload.wikimedia.org/wikipedia/commons/a/ab/Example.webm");

    assertThat(normalize(DEFAULT_NORMALIZER, m)).isPresent();
  }

  @Test
  void nullZoomAndNullVideoHeightPass() {
    Map<String, Object> render = new HashMap<>();
    render.putAll(Map.of("provider", "kakao", "kind", "skyview", "lat", 37.5665, "lng", 126.978));
    render.put("zoom", null);
    Map<String, Object> source = new HashMap<>();
    source.putAll(Map.of("url", "https://example.test/a.webm", "mime", "video/webm"));
    source.put("height", null);

    assertThat(normalize(DEFAULT_NORMALIZER, with(sdk(), "render", render))).isPresent();
    assertThat(normalize(DEFAULT_NORMALIZER, with(videoFile(), "video_sources", List.of(source))))
        .isPresent();
  }

  @Test
  void hangulAddressPassesR9() {
    String url = "https://commons.wikimedia.org/wiki/File:서울_올림픽.jpg";

    assertThat(normalize(DEFAULT_NORMALIZER, with(image(), "source_url", url))).isPresent();
  }

  @Test
  void defaultSettingsDropEveryProviderEmbed() {
    assertThat(normalize(DEFAULT_NORMALIZER, imageEmbed())).isEmpty();
    assertThat(normalize(DEFAULT_NORMALIZER, videoEmbed())).isEmpty();
    assertThat(normalize(DEFAULT_NORMALIZER, image())).isPresent();
  }

  @Test
  void anAllowedProviderKeepsItsEmbeds() {
    assertThat(normalize(EXAMPLE_NORMALIZER, imageEmbed())).isPresent();
    assertThat(normalize(EXAMPLE_NORMALIZER, videoEmbed())).isPresent();
  }

  @Test
  void providersAreAllowedPerKind() {
    MediaNormalizer videoOnly =
        new MediaNormalizer(new MediaProperties(List.of(), List.of("example")));

    assertThat(normalize(videoOnly, imageEmbed())).isEmpty();
    assertThat(normalize(videoOnly, videoEmbed())).isPresent();
  }
}
