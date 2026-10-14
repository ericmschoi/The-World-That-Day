package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.JSON;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.networknt.schema.Schema;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Plan M3a3a T3 (3): the parser table once more, row by row, through the parser, the normalizer
 * (rule R8) and the schema. Whatever {@link CaptureTimeParser} returns, put into a record, is kept
 * unchanged by {@link MediaNormalizer} for every demo and boundary date and passes the schema. The
 * license names of the license table go the same way through {@link LicenseClassifier}. Dates are
 * demo and boundary dates only (CLAUDE.md 7.6).
 */
class CaptureTimeParityTest {

  private static final Schema SCHEMA = DataSchemas.schema("media.schema.json");

  private static final List<LocalDate> REQUESTED =
      List.of(LocalDate.of(1988, 9, 17), LocalDate.of(2002, 6, 22), LocalDate.of(2000, 2, 29));

  /** Parser table rows: raw text, expected {@code captured_at}, expected precision. */
  static Stream<Arguments> table() {
    return Stream.of(
        row("1988-09-17", "1988-09-17", DatePrecision.DAY),
        row("1988-09-17 12:34:56", "1988-09-17", DatePrecision.DAY),
        row("1988:09:17 12:34:56", "1988-09-17", DatePrecision.DAY),
        row("17 September 1988", "1988-09-17", DatePrecision.DAY),
        row("September 17, 1988", "1988-09-17", DatePrecision.DAY),
        row("Taken on 17 September 1988", "1988-09-17", DatePrecision.DAY),
        row("17 September 1988 (Exif)", "1988-09-17", DatePrecision.DAY),
        row(
            "<time datetime=\"1988-09-17\">17 September 1988</time>",
            "1988-09-17",
            DatePrecision.DAY),
        row("<time datetime=\"2002-06-22\">22 June 2002</time>", "2002-06-22", DatePrecision.DAY),
        row("2000-02-29", "2000-02-29", DatePrecision.DAY),
        row("September 1988", "1988-09", DatePrecision.MONTH),
        row("1988-09", "1988-09", DatePrecision.MONTH),
        row("1988", "1988", DatePrecision.YEAR),
        row("c. 1988", "1988", DatePrecision.APPROX),
        row("circa 1988", "1988", DatePrecision.APPROX),
        row("ca. 1988", "1988", DatePrecision.APPROX),
        row("around 1988", "1988", DatePrecision.APPROX),
        row("1988-09-17 / 1988-09-18", null, null),
        row("1988-1990", null, null),
        row("1980s", null, null),
        row("not a date", null, null),
        row("", null, null),
        row(null, null, null),
        row("1988-02-30", null, null),
        row("1999-02-29", null, null));
  }

  private static Arguments row(String raw, String capturedAt, DatePrecision precision) {
    return arguments(raw, capturedAt, precision);
  }

  private static Map<String, Object> withCaptureTime(CaptureTime time) {
    Map<String, Object> fields = image();
    fields.put("captured_at", time.capturedAt());
    fields.put(
        "date_precision", time.datePrecision() == null ? null : time.datePrecision().value());
    return fields;
  }

  @ParameterizedTest(name = "[{0}] -> {1} {2}: kept by R8, passes the schema")
  @MethodSource("table")
  void everyParserResultPassesTheNormalizerAndTheSchema(
      String raw, String capturedAt, DatePrecision precision) {
    CaptureTime time = CaptureTimeParser.parse(raw);
    assertThat(time).isEqualTo(new CaptureTime(capturedAt, precision));

    Map<String, Object> fields = withCaptureTime(time);
    assertThat(DataSchemas.errors(SCHEMA, JSON.valueToTree(fields))).isEmpty();
    for (LocalDate requested : REQUESTED) {
      MediaItem item = DEFAULT_NORMALIZER.normalize(record(fields), requested).orElseThrow();
      assertThat(item.record().capturedAt()).isEqualTo(capturedAt);
      assertThat(item.record().datePrecision()).isEqualTo(precision);
      assertThat(item.dateRelation()).isNotNull();
    }
  }

  @Test
  void theTableCoversEveryPrecisionTheParserCanReturn() {
    List<DatePrecision> seen = new ArrayList<>();
    table().forEach(a -> seen.add((DatePrecision) a.get()[2]));

    assertThat(seen)
        .containsAll(
            Arrays.stream(DatePrecision.values()).filter(p -> p != DatePrecision.CURRENT).toList())
        .doesNotContain(DatePrecision.CURRENT)
        .contains((DatePrecision) null);
  }

  /** The demo strings of the plan's "데모 입력 3개로 확인할 것" section. */
  @Test
  void demoStringsOfThePlan() {
    assertThat(CaptureTimeParser.parse("1988:09:17 12:34:56"))
        .isEqualTo(new CaptureTime("1988-09-17", DatePrecision.DAY));
    assertThat(CaptureTimeParser.parse("<time datetime=\"2002-06-22\">22 June 2002</time>"))
        .isEqualTo(new CaptureTime("2002-06-22", DatePrecision.DAY));
    assertThat(LicenseClassifier.classify("KOGL Type 1")).isEqualTo(LicenseStatus.FREE);
  }

  static Stream<Arguments> licenses() {
    return Stream.of(
        arguments("Public domain", LicenseStatus.FREE),
        arguments("CC0", LicenseStatus.FREE),
        arguments("CC0 1.0", LicenseStatus.FREE),
        arguments("CC BY 4.0", LicenseStatus.FREE),
        arguments("CC BY-SA 3.0", LicenseStatus.FREE),
        arguments("CC BY-SA 2.0 kr", LicenseStatus.FREE),
        arguments("KOGL Type 1", LicenseStatus.FREE),
        arguments("CC BY-NC 4.0", LicenseStatus.OTHER),
        arguments("CC BY-ND 2.0", LicenseStatus.OTHER),
        arguments("CC BY-NC-SA 3.0", LicenseStatus.OTHER),
        arguments("KOGL Type 3", LicenseStatus.OTHER),
        arguments("provider-terms", LicenseStatus.OTHER),
        arguments("Example Made-up License", LicenseStatus.OTHER),
        arguments(null, LicenseStatus.UNKNOWN));
  }

  /** A record with the classifier's status passes the normalizer and the schema. */
  @ParameterizedTest(name = "license [{0}] -> {1}: passes both")
  @MethodSource("licenses")
  void everyLicenseStatusPassesTheNormalizerAndTheSchema(String license, LicenseStatus expected) {
    LicenseStatus status = LicenseClassifier.classify(license);
    assertThat(status).isEqualTo(expected);

    Map<String, Object> fields = image();
    fields.put("license", license);
    fields.put("license_status", status.value());

    assertThat(DataSchemas.errors(SCHEMA, JSON.valueToTree(fields))).isEmpty();
    MediaItem item =
        DEFAULT_NORMALIZER.normalize(record(fields), REQUESTED.getFirst()).orElseThrow();
    assertThat(item.record().licenseStatus()).isEqualTo(expected);
  }
}
