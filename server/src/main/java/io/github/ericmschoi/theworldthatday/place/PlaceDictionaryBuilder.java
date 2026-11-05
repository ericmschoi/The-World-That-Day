package io.github.ericmschoi.theworldthatday.place;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.github.ericmschoi.theworldthatday.place.PlaceCsv.Row;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Builds {@code regions.csv} and {@code neighbourhoods.csv} of {@code data/places/} (plan C2, B1 to
 * B3) from the SGC 2021 CSVs (Windows-1252), the CGNDB CSV (UTF-8 with a byte order mark) and the
 * curated {@code overrides.csv} of the output folder, reading local files only. Against guessing, a
 * CSD sharing its folded name with another CSD of its province gets no {@code unique} point, and no
 * CSD's point nor a name near two CSDs becomes a neighbourhood.
 */
public final class PlaceDictionaryBuilder {

  /** B2 radius around the CSD point. No official basis (plan C2, risk 2). */
  static final double NEIGHBOURHOOD_RADIUS_KM = 25.0;

  static final Set<String> REGION_CODES =
      Set.of("CITY", "TOWN", "VILG", "HAM", "UTM", "LTM", "STM", "MUN1", "MUN2");
  static final Set<String> NEIGHBOURHOOD_CODES = Set.of("UNP", "HAM", "VILG");
  static final List<String> REGIONS =
      List.of("csd_code,name,province_code,cd_name,lat,lng,cgndb_id,coord_rule".split(","));
  static final List<String> NEIGHBOURHOODS =
      List.of("cgndb_id,name,csd_code,province_code,lat,lng".split(","));
  static final List<String> OVERRIDES =
      List.of("csd_code,cgndb_id,reason,evidence_url,reviewed_at".split(","));
  private static final List<String> STRUCTURE =
      List.of("Level,Hierarchical structure,Code,Class title".split(","));
  private static final List<String> ELEMENTS =
      List.of("Level,Code,Element Type Label English,Element Description English".split(","));
  private static final List<String> CGNDB =
      List.of(
          "CGNDB ID,Geographical Name,Concise Code,Latitude,Longitude,Province - Territory"
              .split(","));
  private static final Pattern CSD_CODE = Pattern.compile("[0-9]{7}");

  /** A current CGNDB name; {@code province} is the SGC code, coordinates are the file's text. */
  record Name(String id, String name, String concise, String province, String lat, String lng) {
    List<String> row(String csdCode) {
      return List.of(id, name, csdCode, province, lat, lng);
    }
  }

  private final Map<String, String> provinces = new HashMap<>();
  private final Map<String, String> divisions = new HashMap<>();
  private final Map<String, String> csds = new TreeMap<>();
  private final Map<String, String> overrides = new TreeMap<>();
  private final Map<String, List<Name>> names = new HashMap<>();
  private final Map<String, Name> byId = new HashMap<>();
  private final Map<String, Name> points = new HashMap<>();
  private final Set<Name> pointNames = new HashSet<>();
  private final Map<Name, TreeSet<String>> hits = new HashMap<>();
  private final Map<String, Integer> counts = new TreeMap<>();

  private PlaceDictionaryBuilder() {}

  public static void main(String[] args) throws IOException {
    try {
      if (args.length != 4) {
        throw new IllegalArgumentException("usage: <structure> <elements> <cgndb> <out folder>");
      }
      Path[] paths = {Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3])};
      System.out.println(
          "Place dictionary written: " + build(paths[0], paths[1], paths[2], paths[3]));
    } catch (IllegalArgumentException e) {
      System.err.println("Place dictionary not built: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Writes both files and returns counts for the README: CSDs per {@code coord_rule}, {@code
   * neighbourhoods}, names left out as {@code shared} by two CSDs, and CGNDB rows outside the 13
   * provinces and territories ({@code skipped: <name>}).
   */
  public static Map<String, Integer> build(Path structure, Path elements, Path cgndb, Path out)
      throws IOException {
    PlaceDictionaryBuilder builder = new PlaceDictionaryBuilder();
    PlaceCsv.read(structure, PlaceCsv.WINDOWS_1252, STRUCTURE, builder::structure);
    Path overrideFile = out.resolve("overrides.csv");
    if (Files.isRegularFile(overrideFile)) {
      PlaceCsv.read(overrideFile, UTF_8, OVERRIDES, builder::override);
    }
    PlaceCsv.read(cgndb, UTF_8, CGNDB, builder::cgndb);
    List<List<String>> regions = builder.regions(overrideFile);
    PlaceCsv.read(elements, PlaceCsv.WINDOWS_1252, ELEMENTS, builder::element);
    Map<String, List<String>> neighbourhoods = new TreeMap<>();
    for (Map.Entry<Name, TreeSet<String>> hit : builder.hits.entrySet()) {
      if (hit.getValue().size() == 1) {
        Name name = hit.getKey();
        String code = hit.getValue().first();
        neighbourhoods.put(code + "," + name.id(), name.row(code));
      }
    }
    builder.counts.put("neighbourhoods", neighbourhoods.size());
    builder.counts.put("shared", builder.hits.size() - neighbourhoods.size());
    // B3: rows sorted by code, one fixed format.
    Files.createDirectories(out);
    PlaceCsv.write(out.resolve("regions.csv"), REGIONS, regions);
    PlaceCsv.write(
        out.resolve("neighbourhoods.csv"), NEIGHBOURHOODS, List.copyOf(neighbourhoods.values()));
    return builder.counts;
  }

  private void structure(Row row) {
    String code = row.get("Code");
    switch (row.get("Level")) {
      case "2" -> provinces.put(row.get("Class title"), code);
      case "3" -> divisions.put(code, row.get("Class title"));
      case "4" -> {
        if (!CSD_CODE.matcher(code).matches()
            || !provinces.containsValue(code.substring(0, 2))
            || !divisions.containsKey(code.substring(0, 4))) {
          throw row.fail("CSD code needs 7 digits after its province and division rows");
        }
        csds.put(code, row.get("Class title"));
      }
      default -> {}
    }
  }

  private void override(Row row) {
    if (OVERRIDES.stream().anyMatch(column -> row.get(column).isEmpty())
        || !csds.containsKey(row.get("csd_code"))
        || overrides.put(row.get("csd_code"), row.get("cgndb_id")) != null) {
      throw row.fail("needs every column, a known csd_code and no csd_code twice");
    }
  }

  private void cgndb(Row row) {
    String province = provinces.get(row.get("Province - Territory"));
    String id = row.get("CGNDB ID");
    String concise = row.get("Concise Code");
    if (province == null) {
      counts.merge("skipped: " + row.get("Province - Territory"), 1, Integer::sum);
    } else if (REGION_CODES.contains(concise)
        || NEIGHBOURHOOD_CODES.contains(concise)
        || overrides.containsValue(id)) {
      String lat = row.get("Latitude");
      String lng = row.get("Longitude");
      if (!degrees(lat, 90) || !degrees(lng, 180)) {
        throw row.fail("Latitude and Longitude must be decimal degrees");
      }
      Name name = new Name(id, row.get("Geographical Name"), concise, province, lat, lng);
      names.computeIfAbsent(key(province, name.name()), k -> new ArrayList<>()).add(name);
      byId.put(id, name);
    }
  }

  /** B1: a CSD point from one matching name or the curated override; otherwise none. */
  private List<List<String>> regions(Path overrideFile) {
    Map<String, Integer> sameName = new HashMap<>();
    csds.forEach((code, name) -> sameName.merge(key(code, name), 1, Integer::sum));
    List<List<String>> regions = new ArrayList<>();
    for (Map.Entry<String, String> csd : csds.entrySet()) {
      String code = csd.getKey();
      List<Name> found = matches(key(code, csd.getValue()), REGION_CODES);
      boolean unique = found.size() == 1 && sameName.get(key(code, csd.getValue())) == 1;
      String overrideId = overrides.get(code);
      Name point = unique ? found.getFirst() : byId.get(overrideId);
      if (overrideId != null
          && (unique || point == null || !point.province().equals(code.substring(0, 2)))) {
        throw new IllegalArgumentException(
            overrideFile + ": override of CSD " + code + " is not needed or not in its province");
      }
      String rule = unique ? "unique" : point != null ? "override" : "none";
      counts.merge(rule, 1, Integer::sum);
      List<String> row = new ArrayList<>(List.of(code, csd.getValue(), code.substring(0, 2)));
      row.add(divisions.get(code.substring(0, 4)));
      row.addAll(
          point == null ? List.of("", "", "") : List.of(point.lat(), point.lng(), point.id()));
      row.add(rule);
      regions.add(row);
      if (point != null) {
        points.put(code, point);
        pointNames.add(point);
      }
    }
    return regions;
  }

  /** B2: a place name of a CSD with a point that matches one CGNDB name near that point. */
  private void element(Row row) {
    String code = row.get("Code");
    if (!"4".equals(row.get("Level"))
        || !"Place name(s)".equals(row.get("Element Type Label English"))) {
      return;
    }
    if (!csds.containsKey(code)) {
      throw row.fail("Code is not a CSD of the structure file");
    }
    Name centre = points.get(code);
    List<Name> found =
        centre == null
            ? List.of()
            : matches(key(code, row.get("Element Description English")), NEIGHBOURHOOD_CODES)
                .stream()
                .filter(name -> distanceKm(centre, name) <= NEIGHBOURHOOD_RADIUS_KM)
                .toList();
    if (found.size() == 1 && !pointNames.contains(found.getFirst())) {
      hits.computeIfAbsent(found.getFirst(), k -> new TreeSet<>()).add(code);
    }
  }

  private List<Name> matches(String key, Set<String> codes) {
    return names.getOrDefault(key, List.of()).stream()
        .filter(name -> codes.contains(name.concise()))
        .toList();
  }

  /** Province code (the first two digits of a code) and folded name. */
  private static String key(String code, String name) {
    return code.substring(0, 2) + "|" + NameFold.fold(name);
  }

  private static boolean degrees(String text, double limit) {
    try {
      return Math.abs(Double.parseDouble(text)) <= limit;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /** Great-circle distance on a sphere of radius 6,371.0088 km (mean Earth radius). */
  static double distanceKm(Name a, Name b) {
    double lat1 = Math.toRadians(Double.parseDouble(a.lat()));
    double lat2 = Math.toRadians(Double.parseDouble(b.lat()));
    double dLng = Math.toRadians(Double.parseDouble(b.lng()) - Double.parseDouble(a.lng()));
    double h =
        Math.pow(Math.sin((lat2 - lat1) / 2), 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
    return 2 * 6371.0088 * Math.asin(Math.sqrt(h));
  }
}
