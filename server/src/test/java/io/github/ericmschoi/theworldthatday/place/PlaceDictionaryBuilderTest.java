package io.github.ericmschoi.theworldthatday.place;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ericmschoi.theworldthatday.place.PlaceDictionaryBuilder.Name;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Plan C2 T1 (1) and (2): rules B1 to B3 on the recorded SGC and CGNDB excerpts of {@code
 * src/test/resources/places/} (see its README). Cases the excerpt does not hold are made by adding
 * synthetic rows (CGNDB IDs starting with {@code ZZ}) to copies in a temp folder.
 */
class PlaceDictionaryBuilderTest {

  static final Path FIXTURES = Path.of("src/test/resources/places");

  /** Point of the Montréal override (CGNDB EHHUN). */
  static final double MONTREAL_LAT = 45.5088220;

  static final double MONTREAL_LNG = -73.5540770;
  static final double KM_PER_DEGREE_LAT = 6371.0088 * Math.PI / 180;

  @TempDir Path temp;
  Path structure;
  Path elements;
  Path cgndb;
  Path out;

  @BeforeEach
  void copyFixtures() throws IOException {
    structure = Files.copy(FIXTURES.resolve("sgc-structure-excerpt.csv"), temp.resolve("s.csv"));
    elements = Files.copy(FIXTURES.resolve("sgc-elements-excerpt.csv"), temp.resolve("e.csv"));
    cgndb = Files.copy(FIXTURES.resolve("cgn-excerpt.csv"), temp.resolve("cgn.csv"));
    out = Files.createDirectory(temp.resolve("out"));
    Files.copy(FIXTURES.resolve("overrides.csv"), out.resolve("overrides.csv"));
  }

  Map<String, Integer> build() throws IOException {
    return PlaceDictionaryBuilder.build(structure, elements, cgndb, out);
  }

  List<String> regions() throws IOException {
    return Files.readAllLines(out.resolve("regions.csv"), UTF_8);
  }

  List<String> neighbourhoods() throws IOException {
    return Files.readAllLines(out.resolve("neighbourhoods.csv"), UTF_8);
  }

  static void append(Path file, Charset charset, String... lines) throws IOException {
    Files.write(
        file, (String.join("\r\n", lines) + "\r\n").getBytes(charset), StandardOpenOption.APPEND);
  }

  /** A synthetic CGNDB row in the 16 columns of the real file. */
  static String cgndbRow(
      String id, String name, String concise, double lat, double lng, String pt) {
    return String.join(
        ",",
        id,
        name,
        "und",
        "Undetermined",
        "",
        "Synthetic",
        "Populated Place",
        concise,
        "synthetic",
        String.valueOf(lat),
        String.valueOf(lng),
        "",
        pt,
        "",
        "2026-11-05",
        "Synthetic test row");
  }

  /** A point {@code km} north of the Montréal override point. */
  static double northOfMontreal(double km) {
    return MONTREAL_LAT + km / KM_PER_DEGREE_LAT;
  }

  @Test
  void recordedExcerptGivesTheExpectedFiles() throws IOException {
    Map<String, Integer> counts = build();

    assertThat(Files.readAllBytes(out.resolve("regions.csv")))
        .isEqualTo(Files.readAllBytes(FIXTURES.resolve("expected-regions.csv")));
    assertThat(Files.readAllBytes(out.resolve("neighbourhoods.csv")))
        .isEqualTo(Files.readAllBytes(FIXTURES.resolve("expected-neighbourhoods.csv")));
    assertThat(counts)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("unique", 10, "override", 1, "none", 2, "neighbourhoods", 4, "shared", 0));
  }

  @Test
  void demoAndSameNameRowsHaveTheirCodesAndPoints() throws IOException {
    build();

    assertThat(regions())
        .first()
        .isEqualTo("csd_code,name,province_code,cd_name,lat,lng,cgndb_id,coord_rule");
    assertThat(regions())
        .contains(
            "4806016,Calgary,48,Division No. 6,51.0458333,-114.0574999,IAKID,unique",
            "1001519,St. John's,10,Division No. 1,47.4821970,-52.7970690,ABEFS,unique",
            "3539036,London,35,Middlesex,42.9675000,-81.2325000,FCAEN,unique",
            "1301006,Saint John,13,Saint John,45.2591670,-66.0388890,DAEGW,unique",
            "5955034,Fort St. John,59,Peace River,56.2525000,-120.8466670,JAGPB,unique",
            "5915022,Vancouver,59,Greater Vancouver,49.2611110,-123.1138890,JBRIK,unique",
            "2466023,Montréal,24,Montréal,45.5088220,-73.5540770,EHHUN,override");
    assertThat(regions().stream().filter(row -> row.contains(",Victoria,")).toList())
        .extracting(row -> row.substring(0, 7))
        .containsExactly("1001365", "1102032", "1103068", "4608042", "5917034");
    assertThat(neighbourhoods()).first().isEqualTo("cgndb_id,name,csd_code,province_code,lat,lng");
    assertThat(neighbourhoods())
        .contains("EQVRD,Hochelaga-Maisonneuve,2466023,24,45.5647222,-73.5388889")
        // The borough "Mercier–Hochelaga-Maisonneuve" is a TOWN, not a neighbourhood code.
        .noneMatch(row -> row.startsWith("ERCBM,"))
        // "Hochelaga" is an SGC place name with no CGNDB name.
        .noneMatch(row -> row.contains(",Hochelaga,"));
  }

  @Test
  void sameInputTwiceGivesTheSameBytesWhateverTheRowOrder() throws IOException {
    build();
    byte[] regions = Files.readAllBytes(out.resolve("regions.csv"));
    byte[] neighbourhoods = Files.readAllBytes(out.resolve("neighbourhoods.csv"));

    build();
    assertThat(Files.readAllBytes(out.resolve("regions.csv"))).isEqualTo(regions);
    assertThat(Files.readAllBytes(out.resolve("neighbourhoods.csv"))).isEqualTo(neighbourhoods);

    for (Path file : List.of(structure, elements, cgndb)) {
      Charset charset = file.equals(cgndb) ? UTF_8 : ISO_8859_1;
      List<String> lines = new ArrayList<>(Files.readAllLines(file, charset));
      List<String> body = new ArrayList<>(lines.subList(1, lines.size()));
      Collections.reverse(body);
      if (file.equals(structure)) {
        // Province and division rows must come before their CSDs; reverse within each level.
        body.sort((a, b) -> Character.compare(a.charAt(0), b.charAt(0)));
      }
      Files.write(
          file, Stream.concat(Stream.of(lines.getFirst()), body.stream()).toList(), charset);
    }
    build();
    assertThat(Files.readAllBytes(out.resolve("regions.csv"))).isEqualTo(regions);
    assertThat(Files.readAllBytes(out.resolve("neighbourhoods.csv"))).isEqualTo(neighbourhoods);
  }

  @Test
  void latin1SgcAndBomCgndbBecomeUtf8WithoutBomAndCarriageReturns() throws IOException {
    byte[] sgc = Files.readAllBytes(structure);
    byte[] montrealLatin1 = "Montréal".getBytes(ISO_8859_1);
    assertThat(indexOf(sgc, montrealLatin1)).as("fixture is ISO-8859-1").isPositive();
    assertThat(Files.readAllBytes(cgndb)).startsWith(0xEF, 0xBB, 0xBF);

    build();

    for (String file : List.of("regions.csv", "neighbourhoods.csv")) {
      byte[] bytes = Files.readAllBytes(out.resolve(file));
      assertThat(bytes[0]).isNotEqualTo((byte) 0xEF);
      assertThat(bytes).doesNotContain((byte) '\r');
      assertThat(indexOf(bytes, "Montréal".getBytes(UTF_8))).isPositive();
      assertThat(indexOf(bytes, montrealLatin1)).isNegative();
    }
  }

  @Test
  void cgndbWithoutBomGivesTheSameFiles() throws IOException {
    build();
    byte[] regions = Files.readAllBytes(out.resolve("regions.csv"));
    byte[] withBom = Files.readAllBytes(cgndb);
    Files.write(cgndb, java.util.Arrays.copyOfRange(withBom, 3, withBom.length));

    build();

    assertThat(Files.readAllBytes(out.resolve("regions.csv"))).isEqualTo(regions);
  }

  @Test
  void withoutOverrideMontrealHasNoPointAndNoNeighbourhoods() throws IOException {
    Files.delete(out.resolve("overrides.csv"));

    Map<String, Integer> counts = build();

    assertThat(regions()).contains("2466023,Montréal,24,Montréal,,,,none");
    assertThat(neighbourhoods())
        .containsExactly(
            "cgndb_id,name,csd_code,province_code,lat,lng",
            "FCZWN,Victoria,3521024,35,43.7727778,-79.8777777");
    assertThat(counts).containsEntry("none", 3).containsEntry("unique", 10);
  }

  @Test
  void twoCsdsWithOneNameInAProvinceGetNoPoint() throws IOException {
    build();
    // PEI has two CSDs named Victoria and one CGNDB village Victoria: neither gets it.
    assertThat(regions())
        .contains("1102032,Victoria,11,Queens,,,,none", "1103068,Victoria,11,Prince,,,,none");

    List<String> lines = Files.readAllLines(structure, ISO_8859_1);
    lines.removeIf(line -> line.contains(",1103068,"));
    Files.write(structure, lines, ISO_8859_1);
    lines = Files.readAllLines(elements, ISO_8859_1);
    lines.removeIf(line -> line.contains(",1103068,"));
    Files.write(elements, lines, ISO_8859_1);
    build();

    assertThat(regions())
        .contains("1102032,Victoria,11,Queens,46.2170240,-63.4931740,BACOS,unique");
    // Its own place name "Victoria" is the same CGNDB village, so no neighbourhood repeats it.
    assertThat(neighbourhoods()).noneMatch(row -> row.startsWith("BACOS,"));
  }

  @Test
  void neighbourhoodMustBeWithin25KmOfTheCsdPoint() {
    double inside = northOfMontreal(24.99);
    double outside = northOfMontreal(25.01);
    Name centre = new Name("EHHUN", "Montréal", "CITY", "24", "45.5088220", "-73.5540770");

    assertThat(distanceTo(centre, inside)).isLessThan(25.0);
    assertThat(distanceTo(centre, outside)).isGreaterThan(25.0);
  }

  @Test
  void neighbourhoodJustInsideTheRadiusIsKept() throws IOException {
    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAA", "Hochelaga", "UNP", northOfMontreal(24.99), MONTREAL_LNG, "Quebec"));

    build();

    assertThat(neighbourhoods()).anyMatch(row -> row.startsWith("ZZAAA,Hochelaga,2466023,24,"));
  }

  @Test
  void neighbourhoodJustOutsideTheRadiusIsLeftOut() throws IOException {
    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAA", "Hochelaga", "UNP", northOfMontreal(25.01), MONTREAL_LNG, "Quebec"));

    build();

    assertThat(neighbourhoods()).noneMatch(row -> row.startsWith("ZZAAA,"));
  }

  @Test
  void twoNearbyMatchesLeaveTheNameOut() throws IOException {
    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAB", "Maisonneuve", "UNP", northOfMontreal(10), MONTREAL_LNG, "Quebec"));

    build();

    assertThat(neighbourhoods()).noneMatch(row -> row.contains(",Maisonneuve,"));
    assertThat(neighbourhoods()).anyMatch(row -> row.startsWith("EQVRD,"));
  }

  @Test
  void onlyUnpHamletAndVillageNamesInTheSameProvinceCount() throws IOException {
    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAC", "Hochelaga", "LAKE", northOfMontreal(1), MONTREAL_LNG, "Quebec"),
        cgndbRow("ZZAAD", "Hochelaga", "UNP", northOfMontreal(2), MONTREAL_LNG, "Ontario"));
    build();
    assertThat(neighbourhoods()).noneMatch(row -> row.startsWith("ZZ"));

    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAE", "Hochelaga", "HAM", northOfMontreal(3), MONTREAL_LNG, "Quebec"));
    build();
    assertThat(neighbourhoods()).anyMatch(row -> row.startsWith("ZZAAE,Hochelaga,2466023,"));
  }

  @Test
  void nameNearTwoCsdsIsLeftOutAndCounted() throws IOException {
    append(structure, ISO_8859_1, "4,Census subdivision,2466007,Montréal-Est");
    append(cgndb, UTF_8, cgndbRow("ZZAAF", "Montréal-Est", "CITY", 45.632, -73.507, "Quebec"));
    append(elements, ISO_8859_1, "4,2466007,Montréal-Est,Place name(s),Maisonneuve");

    Map<String, Integer> counts = build();

    assertThat(neighbourhoods()).noneMatch(row -> row.startsWith("EIXQF,"));
    assertThat(counts).containsEntry("shared", 1).containsEntry("neighbourhoods", 3);
  }

  @Test
  void namesOutsideTheProvincesAreSkippedAndCounted() throws IOException {
    append(cgndb, UTF_8, cgndbRow("ZZAAG", "Victoria", "UNP", 50.0, -40.0, "International Waters"));

    assertThat(build()).containsEntry("skipped: International Waters", 1);
  }

  @Test
  void overrideOfAUniqueCsdFails() throws IOException {
    append(
        out.resolve("overrides.csv"), UTF_8, "4806016,IAKID,reason,https://example.org,2026-11-05");

    assertThatThrownBy(this::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("overrides.csv")
        .hasMessageContaining("4806016");
  }

  @Test
  void overrideWithAnUnknownOrForeignNameFails() throws IOException {
    Path overrides = out.resolve("overrides.csv");
    Files.writeString(
        overrides, "csd_code,cgndb_id,reason,evidence_url,reviewed_at\n2466023,ZZZZZ,r,u,d\n");
    assertThatThrownBy(this::build).hasMessageContaining("2466023");

    // Calgary's name is in Alberta, not in Quebec.
    Files.writeString(
        overrides, "csd_code,cgndb_id,reason,evidence_url,reviewed_at\n2466023,IAKID,r,u,d\n");
    assertThatThrownBy(this::build).hasMessageContaining("2466023");
  }

  @Test
  void brokenOverrideRowsFailWithTheirLine() throws IOException {
    Path overrides = out.resolve("overrides.csv");
    append(overrides, UTF_8, "2466023,EHHUN,again,https://example.org,2026-11-05");
    assertThatThrownBy(this::build).hasMessageStartingWith(overrides + ":3:");

    Files.writeString(
        overrides, "csd_code,cgndb_id,reason,evidence_url,reviewed_at\n2466023,EHHUN,,u,d\n");
    assertThatThrownBy(this::build).hasMessageStartingWith(overrides + ":2:");

    Files.writeString(
        overrides, "csd_code,cgndb_id,reason,evidence_url,reviewed_at\n9999999,EHHUN,r,u,d\n");
    assertThatThrownBy(this::build).hasMessageStartingWith(overrides + ":2:");
  }

  @Test
  void brokenSourceFilesFailWithPathAndLineButNoValue() throws IOException {
    append(
        cgndb,
        UTF_8,
        cgndbRow("ZZAAH", "Calgary", "CITY", 51.0, -114.0, "Alberta").replace("51.0", "north"));
    long line = Files.readAllLines(cgndb, UTF_8).size();
    assertThatThrownBy(this::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith(cgndb + ":" + line + ":")
        .hasMessageNotContaining("north")
        .hasMessageNotContaining("Calgary");

    Files.writeString(cgndb, "CGNDB ID,Geographical Name\n");
    assertThatThrownBy(this::build).hasMessageStartingWith(cgndb + ":1:");
  }

  @Test
  void structureAndElementRowsMustFit() throws IOException {
    append(elements, ISO_8859_1, "4,9999999,London,Place name(s),London");
    assertThatThrownBy(this::build).hasMessageStartingWith(elements + ":20:");

    Files.copy(
        FIXTURES.resolve("sgc-elements-excerpt.csv"),
        elements,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    append(structure, ISO_8859_1, "4,Census subdivision,353903,London");
    assertThatThrownBy(this::build).hasMessageStartingWith(structure + ":41:");

    assertThatThrownBy(
            () -> PlaceDictionaryBuilder.build(temp.resolve("missing.csv"), elements, cgndb, out))
        .isInstanceOf(NoSuchFileException.class);
  }

  @Test
  void builderCodeHasNoNetworkAccess() throws IOException {
    Path source = Path.of("src/main/java/io/github/ericmschoi/theworldthatday/place");
    for (String file : List.of("PlaceDictionaryBuilder.java", "PlaceCsv.java", "NameFold.java")) {
      assertThat(Files.readString(source.resolve(file)))
          .doesNotContain("java.net", "HttpClient", "RestClient", "URL", "Socket");
    }
  }

  private static double distanceTo(Name centre, double lat) {
    return PlaceDictionaryBuilder.distanceKm(
        centre, new Name("ZZ", "x", "UNP", "24", String.valueOf(lat), "-73.5540770"));
  }

  private static int indexOf(byte[] bytes, byte[] part) {
    outer:
    for (int i = 0; i <= bytes.length - part.length; i++) {
      for (int j = 0; j < part.length; j++) {
        if (bytes[i + j] != part[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }
}
