package io.github.ericmschoi.theworldthatday.media.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaOrigin;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.media.MediaText;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3b T1 (4), (5), (6) and (8): every row of "Commons 사진 규칙" on synthetic file pages. File
 * names, authors and IDs are made up; dates are demo, historical and boundary dates. Spaces are
 * written as escapes.
 */
class CommonsFileMapperTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);
  private static final Predicate<String> JPEG = "image/jpeg"::equals;
  private static final String KR = "country:KR";
  private static final String URL =
      "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_day_photo.jpg"
          + "?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=original";
  private static final String THUMB =
      "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_day_photo.jpg/"
          + "960px-Synthetic_day_photo.jpg?utm_source=commons.wikimedia.org&utm_content=thumbnail";
  private static final String PAGE_URL =
      "https://commons.wikimedia.org/wiki/File:Synthetic_day_photo.jpg";
  private static final List<String> CREDITS =
      List.of("연합뉴스", "Yonhap", "AP", "Associated Press", "Reuters", "AFP", "Getty", "EPA");

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T09:30:15.789Z"), ClockConfig.SERVICE_ZONE);
  private final MediaNormalizer normalizer =
      new MediaNormalizer(new MediaProperties(List.of(), List.of()));

  private CommonsFileMapper mapper(BlockEntry... blocked) {
    MediaProperties properties =
        new MediaProperties(List.of(), List.of(), List.of(blocked), CREDITS);
    return new CommonsFileMapper(new MediaFilter(properties), normalizer, clock);
  }

  /** A valid synthetic file page: a Public domain JPEG taken on 1988-09-17. */
  private static ObjectNode page() {
    return (ObjectNode)
        JSON.readTree(
            """
            {"pageid": 1000001, "ns": 6, "title": "File:Synthetic day photo.jpg",
             "imagerepository": "local",
             "imageinfo": [{"url": "%s", "descriptionurl": "%s", "thumburl": "%s",
               "thumbwidth": 640, "thumbheight": 427, "mime": "image/jpeg",
               "extmetadata": {
                 "Artist": {"value": "Synthetic Author", "source": "commons-desc-page"},
                 "Credit": {"value": "Synthetic Credit", "source": "commons-desc-page"},
                 "LicenseShortName": {"value": "Public domain"},
                 "DateTimeOriginal": {"value": "1988-09-17"},
                 "ImageDescription": {"value": "합성 설명"},
                 "Restrictions": {"value": ""}}}]}
            """
                .formatted(URL, PAGE_URL, THUMB));
  }

  /** {@code page} with one {@code extmetadata} value set, or removed when {@code value} is null. */
  private static ObjectNode meta(ObjectNode page, String key, String value) {
    ObjectNode meta = (ObjectNode) page.path("imageinfo").path(0).path("extmetadata");
    if (value == null) {
      meta.remove(key);
    } else {
      meta.putObject(key).put("value", value);
    }
    return page;
  }

  private Result map(ObjectNode page) {
    return mapper().map(page, JPEG, KR, OLYMPICS);
  }

  private MediaRecord kept(ObjectNode page) {
    Result result = map(page);
    assertThat(result.drop()).isNull();
    return result.kept().orElseThrow().record();
  }

  private static void assertPassesSchema(MediaItem item) {
    ObjectNode written = JSON.valueToTree(item);
    written.remove("date_relation");
    assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), written)).isEmpty();
  }

  // ---- fields ----

  @Test
  void basePageGivesAPhotoWithEveryField() {
    MediaItem item = map(page()).kept().orElseThrow();
    MediaRecord r = item.record();

    assertThat(r.id()).isEqualTo("commons-1000001");
    assertThat(r.type()).isEqualTo(Type.PHOTO);
    assertThat(r.displayMode()).isEqualTo(DisplayMode.IMAGE);
    assertThat(r.thumbnailWidth()).isEqualTo(640);
    assertThat(r.thumbnailHeight()).isEqualTo(427);
    assertThat(r.sourceName()).isEqualTo("Wikimedia Commons");
    assertThat(r.license()).isEqualTo("Public domain");
    assertThat(r.licenseStatus()).isEqualTo(LicenseStatus.FREE);
    assertThat(r.licenseUrl()).isNull();
    assertThat(r.attribution()).isEqualTo("Synthetic Author · Public domain · Wikimedia Commons");
    assertThat(r.description()).isEqualTo("합성 설명");
    assertThat(r.capturedAt()).isEqualTo("1988-09-17");
    assertThat(r.datePrecision()).isEqualTo(DatePrecision.DAY);
    assertThat(r.lat()).isNull();
    assertThat(r.lng()).isNull();
    assertThat(r.placeRef()).isEqualTo(KR);
    assertThat(r.retrievedAt()).isEqualTo("2026-01-01T09:30:15Z");
    assertThat(r.videoSources()).isNull();
    assertThat(r.embedUrl()).isNull();
    assertThat(r.provider()).isNull();
    assertThat(r.render()).isNull();
    assertThat(r.linkOnlyReason()).isNull();
    assertThat(item.dateRelation()).isEqualTo(new DateRelation(DateRelation.Kind.SAME_DAY, 0));
    assertPassesSchema(item);
  }

  /** Carried-over review recommendation 5: addresses are copied character for character. */
  @Test
  void addressesAreCopiedExactly() {
    ObjectNode page = meta(page(), "LicenseUrl", "https://creativecommons.org/licenses/by-sa/3.0");
    meta(page, "LicenseShortName", "CC BY-SA 3.0");

    MediaRecord r = kept(page);

    assertThat(r.url()).isEqualTo(URL);
    assertThat(r.thumbnailUrl()).isEqualTo(THUMB);
    assertThat(r.sourceUrl()).isEqualTo(PAGE_URL);
    assertThat(r.licenseUrl()).isEqualTo("https://creativecommons.org/licenses/by-sa/3.0");
  }

  // ---- dropped pages ----

  @Test
  void missingPageOrPageWithoutFileInfoIsDropped() {
    ObjectNode missing =
        (ObjectNode)
            JSON.readTree("{\"ns\":6,\"title\":\"File:Synthetic local.jpg\",\"missing\":true}");
    ObjectNode noInfo = page();
    noInfo.remove("imageinfo");

    assertThat(map(missing).drop()).isEqualTo(Drop.MISSING);
    assertThat(map(noInfo).drop()).isEqualTo(Drop.MISSING);
    assertThat(map(missing).kept()).isEmpty();
  }

  @Test
  void nonFreeFileIsDroppedButAnEmptyOrFalseValueIsNot() {
    assertThat(map(meta(page(), "NonFree", "true")).drop()).isEqualTo(Drop.NON_FREE);
    assertThat(map(meta(page(), "NonFree", "")).drop()).isNull();
    assertThat(map(meta(page(), "NonFree", "false")).drop()).isNull();
  }

  @ParameterizedTest(name = "MIME {0} is dropped")
  @ValueSource(strings = {"image/png", "image/svg+xml", "application/pdf", "video/webm"})
  void onlyTheCallersMimeTypesAreKept(String mime) {
    ObjectNode page = page();
    ((ObjectNode) page.path("imageinfo").path(0)).put("mime", mime);

    assertThat(map(page).drop()).isEqualTo(Drop.MIME);
  }

  @Test
  void blockedFileNameIsDroppedByTheFilter() {
    Result result =
        mapper(new BlockEntry("commons", "synthetic day photo.jpg", null, null))
            .map(page(), JPEG, KR, OLYMPICS);

    assertThat(result.drop()).isEqualTo(Drop.FILTERED);
  }

  @Test
  void wireAgencyCreditIsDroppedByTheFilter() {
    ObjectNode page = meta(page(), "Credit", "AP Photo/Synthetic Shooter");
    meta(page, "Artist", null);

    assertThat(map(page).drop()).isEqualTo(Drop.FILTERED);
  }

  @Test
  void recordThatBreaksARuleIsDropped() {
    ObjectNode noUrl = page();
    ((ObjectNode) noUrl.path("imageinfo").path(0)).remove("url");
    ObjectNode badPage = page();
    ((ObjectNode) badPage.path("imageinfo").path(0)).put("descriptionurl", "ftp://example.test/x");

    assertThat(map(noUrl).drop()).isEqualTo(Drop.RULE);
    assertThat(map(badPage).drop()).isEqualTo(Drop.RULE);
  }

  // ---- filter input and order ----

  @Test
  void filterGetsThePlainOriginAndRunsBeforeTheNormalizer() {
    MediaFilter filter = mock(MediaFilter.class);
    when(filter.check(any(), any())).thenReturn(Optional.empty());
    MediaNormalizer spied = spy(normalizer);
    ObjectNode page =
        meta(page(), "Artist", "<a href=\"https://example.test/u\">Synthetic&nbsp;Author</a>");
    meta(page, "Credit", "<span lang=\"ko\">합성 출처</span>");

    Result result = new CommonsFileMapper(filter, spied, clock).map(page, JPEG, KR, OLYMPICS);

    assertThat(result.kept()).isPresent();
    ArgumentCaptor<MediaOrigin> origin = ArgumentCaptor.forClass(MediaOrigin.class);
    InOrder order = inOrder(filter, spied);
    order.verify(filter).check(any(MediaRecord.class), origin.capture());
    order.verify(spied).normalize(any(MediaRecord.class), any(LocalDate.class));
    assertThat(origin.getValue())
        .isEqualTo(
            new MediaOrigin(
                "commons", "Synthetic day photo.jpg", "Synthetic Author", "합성 출처", null));
  }

  // ---- license ----

  @Test
  void allowedLicenseIsFreeAndAnotherIsOther() {
    assertThat(kept(meta(page(), "LicenseShortName", "CC BY-SA 3.0")).licenseStatus())
        .isEqualTo(LicenseStatus.FREE);
    assertThat(kept(meta(page(), "LicenseShortName", "KOGL Type 1")).licenseStatus())
        .isEqualTo(LicenseStatus.FREE);
    MediaRecord other = kept(meta(page(), "LicenseShortName", "CC BY-NC 4.0"));
    assertThat(other.licenseStatus()).isEqualTo(LicenseStatus.OTHER);
    assertThat(other.attribution())
        .isEqualTo("Synthetic Author · CC BY-NC 4.0 · Wikimedia Commons");
  }

  @ParameterizedTest(name = "license [{0}] is shown as unknown")
  @ValueSource(strings = {"", "&nbsp;", " 　", "<span></span>"})
  void emptyLicenseIsShownAsUnknown(String license) {
    MediaRecord r = kept(meta(page(), "LicenseShortName", license));

    assertThat(r.license()).isNull();
    assertThat(r.licenseStatus()).isEqualTo(LicenseStatus.UNKNOWN);
    assertThat(r.attribution())
        .isEqualTo("Synthetic Author · Licence not verified · Wikimedia Commons");
  }

  @Test
  void missingLicenseIsShownAsUnknown() {
    MediaRecord r = kept(meta(page(), "LicenseShortName", null));

    assertThat(r.license()).isNull();
    assertThat(r.licenseStatus()).isEqualTo(LicenseStatus.UNKNOWN);
  }

  @Test
  void restrictionsAreShownNotFiltered() {
    assertThat(map(meta(page(), "Restrictions", "personality|trademarked")).kept()).isPresent();
  }

  // ---- author and description ----

  @Test
  void authorFallsBackFromArtistToCreditToNoAuthor() {
    assertThat(kept(meta(page(), "Artist", null)).attribution()).startsWith("Synthetic Credit · ");
    assertThat(kept(meta(page(), "Artist", "&nbsp;")).attribution())
        .startsWith("Synthetic Credit · ");
    ObjectNode neither = meta(meta(page(), "Artist", null), "Credit", " ");
    assertThat(kept(neither).attribution())
        .isEqualTo("No author information · Public domain · Wikimedia Commons");
  }

  @Test
  void artistHtmlBecomesPlainText() {
    ObjectNode page =
        meta(
            page(),
            "Artist",
            "<a rel=\"mw:WikiLink\" href=\"https://commons.wikimedia.org/wiki/User:Synthetic\">"
                + "Synthetic&nbsp;&amp;\tAuthor</a>\n<span>(talk)</span>");

    assertThat(kept(page).attribution())
        .isEqualTo("Synthetic & Author (talk) · Public domain · Wikimedia Commons");
  }

  @Test
  void descriptionIsCutAtTwoHundredCodePoints() {
    String face = new String(Character.toChars(0x1F600));
    String exactly = "가".repeat(199) + face;

    assertThat(kept(meta(page(), "ImageDescription", exactly + "나")).description())
        .isEqualTo(exactly);
    assertThat(kept(meta(page(), "ImageDescription", exactly)).description()).isEqualTo(exactly);
    assertThat(kept(meta(page(), "ImageDescription", "<p>　</p>")).description()).isNull();
    assertThat(kept(meta(page(), "ImageDescription", null)).description()).isNull();
  }

  // ---- position ----

  @Test
  void gpsInRangeGivesCoordinatesWithoutAPlace() {
    ObjectNode page = meta(meta(page(), "GPSLatitude", "37.5665"), "GPSLongitude", "126.978");

    MediaRecord r = kept(page);

    assertThat(r.lat()).isEqualTo(37.5665);
    assertThat(r.lng()).isEqualTo(126.978);
    assertThat(r.placeRef()).isNull();
  }

  @Test
  void gpsOutOfRangeHalfOrMissingKeepsTheCallersPlace() {
    ObjectNode outOfRange = meta(meta(page(), "GPSLatitude", "91"), "GPSLongitude", "126.978");
    ObjectNode badLongitude = meta(meta(page(), "GPSLatitude", "37.5"), "GPSLongitude", "180.5");
    ObjectNode half = meta(page(), "GPSLatitude", "37.5");
    ObjectNode unreadable = meta(meta(page(), "GPSLatitude", "NaN"), "GPSLongitude", "x");

    for (ObjectNode page : List.of(outOfRange, badLongitude, half, unreadable, page())) {
      MediaRecord r = kept(page);
      assertThat(r.lat()).isNull();
      assertThat(r.lng()).isNull();
      assertThat(r.placeRef()).isEqualTo(KR);
    }
    assertThat(mapper().map(page(), JPEG, "event:synthetic", OLYMPICS).kept().orElseThrow())
        .extracting(item -> item.record().placeRef())
        .isEqualTo("event:synthetic");
  }

  // ---- capture time ----

  @Test
  void twoDatesInDateTimeOriginalAreUnknownButShown() {
    MediaItem item =
        mapper()
            .map(
                meta(
                    page(),
                    "DateTimeOriginal",
                    "Taken on 17 September 1988<br>2013-05-05 21:10:18"),
                JPEG,
                KR,
                OLYMPICS)
            .kept()
            .orElseThrow();

    assertThat(item.record().capturedAt()).isNull();
    assertThat(item.record().datePrecision()).isNull();
    assertThat(item.dateRelation().kind()).isEqualTo(DateRelation.Kind.UNKNOWN);
    assertPassesSchema(item);
  }

  @Test
  void englishTakenOnTextIsReadAsADay() {
    MediaRecord r = kept(meta(page(), "DateTimeOriginal", "Taken on&nbsp;17 September 1988"));

    assertThat(r.capturedAt()).isEqualTo("1988-09-17");
    assertThat(r.datePrecision()).isEqualTo(DatePrecision.DAY);
  }

  /** Known parser limit (plan M3a3a risk 4): the Korean wording stays unknown. */
  @Test
  void koreanLocalizedDateIsUnknown() {
    String korean =
        "촬영 날짜:<span typeof=\"mw:Entity\"> </span>1988년 9월 17일"
            + "<link rel=\"mw:PageProp/Category\" href=\"./Category:Synthetic\">";

    assertThat(kept(meta(page(), "DateTimeOriginal", korean)).capturedAt()).isNull();
  }

  @Test
  void leapDayAndOtherYearsRelateToTheRequestedDate() {
    ObjectNode leap = meta(page(), "DateTimeOriginal", "2000-02-29");
    CommonsFileMapper mapper = mapper();

    assertThat(mapper.map(leap, JPEG, KR, LocalDate.of(2000, 2, 29)).item().dateRelation())
        .isEqualTo(new DateRelation(DateRelation.Kind.SAME_DAY, 0));
    assertThat(mapper.map(leap, JPEG, KR, LocalDate.of(2000, 2, 1)).item().dateRelation().kind())
        .isEqualTo(DateRelation.Kind.SAME_MONTH);
    assertThat(mapper.map(leap, JPEG, KR, LocalDate.of(2004, 2, 29)).item().dateRelation())
        .isEqualTo(new DateRelation(DateRelation.Kind.YEARS_APART, -4)); // taken 4 years before;
  }

  // ---- plain text uses the MediaText space set ----

  @Test
  void plainTextCollapsesExactlyTheMediaTextSpaces() {
    List<String> mismatches = new ArrayList<>();
    for (int c = 0; c <= 0xFFFF; c++) {
      if (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE) {
        continue;
      }
      String ch = Character.toString(c);
      boolean collapsed = "a b".equals(CommonsFileMapper.plain("a" + ch + ch + "b"));
      if (collapsed != MediaText.isBlank(ch)) {
        mismatches.add(String.format("U+%04X", c));
      }
    }
    assertThat(mismatches).isEmpty();
    assertThat(CommonsFileMapper.plain(null)).isNull();
    assertThat(CommonsFileMapper.plain(" <br>　")).isNull();
  }
}
