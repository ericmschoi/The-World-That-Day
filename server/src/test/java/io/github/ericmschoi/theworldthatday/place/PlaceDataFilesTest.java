package io.github.ericmschoi.theworldthatday.place;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.ericmschoi.theworldthatday.place.PlaceCsv.Row;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Plan C2 T3: the real data/places/ files (formats, keys, ranges, demo rows, overrides). */
class PlaceDataFilesTest {

  static final Path DIR = Path.of("").toAbsolutePath().getParent().resolve("data/places");
  static final Set<String> PROVINCES =
      Set.of("10", "11", "12", "13", "24", "35", "46", "47", "48", "59", "60", "61", "62");
  static final long MAX_BYTES = 4L * 1024 * 1024;

  static List<String> columns(String file) {
    return switch (file) {
      case "provinces.csv" -> List.of("code", "name", "abbr", "aliases", "lat", "lng", "cgndb_id");
      case "regions.csv" ->
          List.of(
              "csd_code",
              "name",
              "province_code",
              "cd_name",
              "lat",
              "lng",
              "cgndb_id",
              "coord_rule");
      case "neighbourhoods.csv" ->
          List.of("cgndb_id", "name", "csd_code", "province_code", "lat", "lng");
      case "overrides.csv" ->
          List.of("csd_code", "cgndb_id", "reason", "evidence_url", "reviewed_at");
      default -> throw new IllegalArgumentException(file);
    };
  }

  static final List<String> FILES =
      List.of("provinces.csv", "regions.csv", "neighbourhoods.csv", "overrides.csv");

  static List<Row> rows(String file) throws IOException {
    List<Row> rows = new ArrayList<>();
    PlaceCsv.read(DIR.resolve(file), UTF_8, columns(file), rows::add);
    return rows;
  }

  @Test
  void headersAreExactlyThePlannedColumns() throws IOException {
    for (String file : FILES) {
      String first = Files.readAllLines(DIR.resolve(file), UTF_8).get(0);
      assertThat(first).as(file).isEqualTo(String.join(",", columns(file)));
    }
  }

  @Test
  void noBomAndNoCarriageReturns() throws IOException {
    for (String file : FILES) {
      byte[] bytes = Files.readAllBytes(DIR.resolve(file));
      assertThat(bytes.length).as(file).isGreaterThan(3);
      boolean bom =
          (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
      assertThat(bom).as("BOM in " + file).isFalse();
      assertThat(new String(bytes, UTF_8)).as(file).doesNotContain("﻿");
    }
  }

  @Test
  void totalSizeIsAtMostFourMegabytes() throws IOException {
    long total = 0;
    try (var stream = Files.list(DIR)) {
      for (Path file : stream.toList()) {
        total += Files.size(file);
      }
    }
    assertThat(total).isLessThanOrEqualTo(MAX_BYTES);
  }

  @Test
  void thirteenProvincesWithUniqueCodes() throws IOException {
    List<Row> rows = rows("provinces.csv");
    assertThat(rows).extracting(r -> r.get("code")).containsExactlyInAnyOrderElementsOf(PROVINCES);
    assertThat(rows).extracting(r -> r.get("abbr")).doesNotHaveDuplicates();
    rows.forEach(r -> assertPoint(r, true));
  }

  @Test
  void regionIdsAreUniqueAndCodesAgreeWithTheProvince() throws IOException {
    Set<String> seen = new HashSet<>();
    for (Row r : rows("regions.csv")) {
      String csd = r.get("csd_code");
      assertThat(csd).as("line %d", r.line()).matches("\\d{7}");
      assertThat(seen.add(csd)).as("duplicate csd_code at line %d", r.line()).isTrue();
      assertThat(PROVINCES).as("line %d", r.line()).contains(r.get("province_code"));
      assertThat(csd.substring(0, 2)).as("line %d", r.line()).isEqualTo(r.get("province_code"));
      assertThat(r.get("name")).as("line %d", r.line()).isNotBlank();
      assertPoint(r, false);
      String rule = r.get("coord_rule");
      assertThat(rule).as("line %d", r.line()).isIn("unique", "override", "none");
      boolean hasPoint = !r.get("lat").isEmpty();
      assertThat(hasPoint)
          .as("coord_rule vs point, line %d", r.line())
          .isEqualTo(!rule.equals("none"));
      assertThat(r.get("cgndb_id").isEmpty())
          .as("cgndb_id, line %d", r.line())
          .isEqualTo(!hasPoint);
    }
  }

  @Test
  void neighbourhoodIdsAreUniqueAndTheirCsdExists() throws IOException {
    Set<String> csds = new HashSet<>();
    for (Row r : rows("regions.csv")) {
      csds.add(r.get("csd_code"));
    }
    Set<String> seen = new HashSet<>();
    for (Row r : rows("neighbourhoods.csv")) {
      assertThat(r.get("cgndb_id")).as("line %d", r.line()).isNotBlank();
      assertThat(seen.add(r.get("cgndb_id"))).as("duplicate id at line %d", r.line()).isTrue();
      assertThat(r.get("name")).as("line %d", r.line()).isNotBlank();
      assertThat(r.get("csd_code")).as("line %d", r.line()).matches("\\d{7}");
      assertThat(csds).as("line %d", r.line()).contains(r.get("csd_code"));
      assertThat(PROVINCES).as("line %d", r.line()).contains(r.get("province_code"));
      assertThat(r.get("csd_code").substring(0, 2))
          .as("line %d", r.line())
          .isEqualTo(r.get("province_code"));
      assertThat(r.get("lat")).as("line %d", r.line()).isNotEmpty();
      assertPoint(r, true);
    }
  }

  static void assertPoint(Row r, boolean required) {
    String lat = r.get("lat");
    String lng = r.get("lng");
    assertThat(lat.isEmpty())
        .as("lat and lng both present or both empty, line %d", r.line())
        .isEqualTo(lng.isEmpty());
    if (lat.isEmpty()) {
      assertThat(required).as("point required, line %d", r.line()).isFalse();
      return;
    }
    assertThat(Double.parseDouble(lat)).as("lat, line %d", r.line()).isBetween(41.0, 84.0);
    assertThat(Double.parseDouble(lng)).as("lng, line %d", r.line()).isBetween(-142.0, -52.0);
  }

  @Test
  void everyOverrideHasAnEvidenceUrlAndPointsAtARegionWithTheOverrideRule() throws IOException {
    Pattern url = Pattern.compile("https://\\S+");
    List<Row> regions = rows("regions.csv");
    List<Row> overrides = rows("overrides.csv");
    assertThat(overrides).isNotEmpty();
    Set<String> seen = new HashSet<>();
    for (Row o : overrides) {
      assertThat(o.get("evidence_url")).as("line %d", o.line()).matches(url);
      assertThat(o.get("reason")).as("line %d", o.line()).isNotBlank();
      assertThat(o.get("reviewed_at")).as("line %d", o.line()).matches("\\d{4}-\\d{2}-\\d{2}");
      assertThat(seen.add(o.get("csd_code"))).as("duplicate override line %d", o.line()).isTrue();
      Row region =
          regions.stream()
              .filter(r -> r.get("csd_code").equals(o.get("csd_code")))
              .findFirst()
              .orElseThrow();
      assertThat(region.get("coord_rule")).as("line %d", o.line()).isEqualTo("override");
      assertThat(region.get("cgndb_id")).as("line %d", o.line()).isEqualTo(o.get("cgndb_id"));
    }
  }

  @Test
  void noTextColumnContainsControlCharacters() throws IOException {
    Pattern control = Pattern.compile("\\p{Cc}");
    for (String file : FILES) {
      for (Row r : rows(file)) {
        for (String column : columns(file)) {
          assertThat(control.matcher(r.get(column)).find())
              .as("control character in %s column %s, line %d", file, column, r.line())
              .isFalse();
        }
      }
    }
  }

  @Test
  void regionsAreSortedByCsdCode() throws IOException {
    String previous = null;
    for (Row r : rows("regions.csv")) {
      String csd = r.get("csd_code");
      if (previous != null) {
        assertThat(csd).as("order at line %d", r.line()).isGreaterThan(previous);
      }
      previous = csd;
    }
  }

  @Test
  void neighbourhoodsAreSortedByCsdCodeThenCgndbId() throws IOException {
    String prevCsd = null;
    String prevId = null;
    for (Row r : rows("neighbourhoods.csv")) {
      String csd = r.get("csd_code");
      String id = r.get("cgndb_id");
      if (prevCsd != null) {
        int byCsd = csd.compareTo(prevCsd);
        assertThat(byCsd).as("csd_code order at line %d", r.line()).isGreaterThanOrEqualTo(0);
        if (byCsd == 0) {
          assertThat(id).as("cgndb_id order at line %d", r.line()).isGreaterThan(prevId);
        }
      }
      prevCsd = csd;
      prevId = id;
    }
  }

  // demo expectations from the plan

  static Row region(String csd) throws IOException {
    return rows("regions.csv").stream()
        .filter(r -> r.get("csd_code").equals(csd))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no region " + csd));
  }

  static void assertRegion(String csd, String name, String province, double lat, double lng)
      throws IOException {
    Row r = region(csd);
    assertThat(r.get("name")).isEqualTo(name);
    assertThat(r.get("province_code")).isEqualTo(province);
    assertThat(Double.parseDouble(r.get("lat"))).isCloseTo(lat, within(0.001));
    assertThat(Double.parseDouble(r.get("lng"))).isCloseTo(lng, within(0.001));
  }

  @Test
  void demoRegionsHaveTheirCodesAndPoints() throws IOException {
    assertRegion("4806016", "Calgary", "48", 51.0458, -114.0575);
    assertRegion("5915022", "Vancouver", "59", 49.2611, -123.1139);
    assertRegion("1001519", "St. John's", "10", 47.4822, -52.7971);
    assertRegion("3539036", "London", "35", 42.9675, -81.2325);
    assertRegion("1301006", "Saint John", "13", 45.2592, -66.0389);
  }

  @Test
  void fiveVictoriaCsdsExistInTheExpectedProvinces() throws IOException {
    List<Row> victorias =
        rows("regions.csv").stream().filter(r -> r.get("name").equals("Victoria")).toList();
    assertThat(victorias)
        .extracting(r -> r.get("csd_code"))
        .containsExactly("1001365", "1102032", "1103068", "4608042", "5917034");
    assertThat(victorias)
        .extracting(r -> r.get("province_code"))
        .containsExactly("10", "11", "11", "46", "59");
  }

  @Test
  void hochelagaMaisonneuveIsANeighbourhoodOfMontreal() throws IOException {
    Row r =
        rows("neighbourhoods.csv").stream()
            .filter(n -> n.get("cgndb_id").equals("EQVRD"))
            .findFirst()
            .orElseThrow();
    assertThat(r.get("name")).isEqualTo("Hochelaga-Maisonneuve");
    assertThat(r.get("csd_code")).isEqualTo("2466023");
    assertThat(r.get("province_code")).isEqualTo("24");
    assertThat(Double.parseDouble(r.get("lat"))).isCloseTo(45.5647, within(0.001));
    assertThat(Double.parseDouble(r.get("lng"))).isCloseTo(-73.5389, within(0.001));
    assertThat(region("2466023").get("name")).isEqualTo("Montréal");
  }
}
