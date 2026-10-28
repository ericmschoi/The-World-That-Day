package io.github.ericmschoi.theworldthatday.place;

import io.github.ericmschoi.theworldthatday.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The region dictionary {@code <app.data-dir>/places/regions.json} (plan M2a): provinces ({@code
 * sido}), cities, counties and districts ({@code sigungu}) and the districts of a city ({@code
 * gu}), from the legal-dong code list. Read once at startup, like {@code EventCuration}: a missing
 * file logs one WARN and the dictionary is empty; a file that breaks a rule stops startup with the
 * path and the rule in the message. Messages name entries by index only, never by code or name.
 */
@Component
public class RegionDictionary {

  static final String FILE = "places/regions.json";
  private static final Logger log = LoggerFactory.getLogger(RegionDictionary.class);
  private static final Set<String> ROOT_KEYS = Set.of("version", "source", "regions");
  private static final Set<String> SOURCE_KEYS = Set.of("name", "url", "terms", "retrieved_at");
  private static final Set<String> REGION_KEYS =
      Set.of("code", "level", "name", "short_name", "parent", "aliases");
  private static final Pattern CODE = Pattern.compile("[0-9]{10}");
  private static final Pattern HTTPS_URL = Pattern.compile("https://\\S+");

  /** The level of a region; {@code parent} is the level its parent must have. */
  public enum Level {
    SIDO("sido", null),
    SIGUNGU("sigungu", "sido"),
    GU("gu", "sigungu");

    private final String value;
    private final String parent;

    Level(String value, String parent) {
      this.value = value;
      this.parent = parent;
    }

    public String value() {
      return value;
    }
  }

  /**
   * One region. {@code shortName} is present only for {@code sido}; {@code parentCode} is empty
   * only for {@code sido}.
   */
  public record Region(
      String code,
      Level level,
      String name,
      Optional<String> shortName,
      Optional<String> parentCode,
      List<String> aliases) {}

  /** The dictionary's data source, shown with every answer that used it. */
  public record Source(String name, String url) {}

  /** Regions in code order and the source; an empty dictionary has no source. */
  record Contents(List<Region> regions, Optional<Source> source) {}

  private final List<Region> regions;
  private final Map<String, Region> byCode;
  private final Optional<Source> source;

  public RegionDictionary(AppProperties properties, JsonMapper json) {
    Path file = properties.dataPath().resolve(FILE);
    if (!Files.isRegularFile(file)) {
      log.warn("Region dictionary file not found, the dictionary is empty: path={}", file);
      this.regions = List.of();
      this.byCode = Map.of();
      this.source = Optional.empty();
      return;
    }
    Contents contents;
    try {
      contents = parse(json.readTree(file.toFile()));
    } catch (JacksonException e) {
      throw new IllegalStateException("Region dictionary file is not JSON: " + file, e);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Region dictionary file is invalid: " + file + ": " + e.getMessage(), e);
    }
    this.regions = contents.regions();
    Map<String, Region> map = new LinkedHashMap<>();
    regions.forEach(region -> map.put(region.code(), region));
    this.byCode = Map.copyOf(map);
    this.source = contents.source();
    log.info("Region dictionary loaded: regions={}", regions.size());
  }

  /** Every region in ascending code order. */
  public List<Region> regions() {
    return regions;
  }

  public Optional<Region> byCode(String code) {
    return Optional.ofNullable(byCode.get(code));
  }

  /** Empty only when the file is missing. */
  public Optional<Source> source() {
    return source;
  }

  static Contents parse(JsonNode root) {
    check(root.isObject(), "the root must be an object");
    keys(root, ROOT_KEYS, "the root");
    check(
        root.path("version").isIntegralNumber() && root.path("version").asInt() == 1,
        "version must be 1");
    JsonNode sourceNode = root.path("source");
    check(sourceNode.isObject(), "source must be an object");
    keys(sourceNode, SOURCE_KEYS, "source");
    String sourceName = text(sourceNode, "name", "source");
    String sourceUrl = text(sourceNode, "url", "source");
    check(HTTPS_URL.matcher(sourceUrl).matches(), "source.url must be an https URL");
    check(root.path("regions").isArray(), "regions must be an array");

    List<Region> regions = new ArrayList<>();
    Map<String, Integer> indexByCode = new LinkedHashMap<>();
    int index = 0;
    for (JsonNode node : root.path("regions")) {
      String at = "regions[" + index + "]";
      check(node.isObject(), at + " must be an object");
      keys(node, REGION_KEYS, at);
      String code = node.path("code").isString() ? node.path("code").asString() : "";
      check(CODE.matcher(code).matches(), at + ".code must be 10 digits");
      check(
          indexByCode.putIfAbsent(code, index) == null,
          at + ".code repeats the code of regions[" + indexByCode.get(code) + "]");
      Level level = level(node, at);
      String name = text(node, "name", at);
      Optional<String> shortName = Optional.empty();
      if (level == Level.SIDO) {
        shortName = Optional.of(text(node, "short_name", at));
      } else {
        check(!node.has("short_name"), at + ".short_name is only for sido");
      }
      check(node.has("parent"), at + " needs parent");
      Optional<String> parent = Optional.empty();
      if (level == Level.SIDO) {
        check(node.get("parent").isNull(), at + ".parent must be null for sido");
      } else {
        check(node.get("parent").isString(), at + ".parent must be the code of a " + level.parent);
        parent = Optional.of(node.get("parent").asString());
      }
      regions.add(new Region(code, level, name, shortName, parent, aliases(node, at)));
      index++;
    }

    for (int i = 0; i < regions.size(); i++) {
      Region region = regions.get(i);
      if (region.parentCode().isEmpty()) {
        continue;
      }
      String at = "regions[" + i + "]";
      Integer parentIndex = indexByCode.get(region.parentCode().get());
      check(parentIndex != null, at + ".parent is not a code in regions");
      Level parentLevel = regions.get(parentIndex).level();
      check(
          parentLevel.value().equals(region.level().parent),
          at + ".parent must be a " + region.level().parent + ", not a " + parentLevel.value());
    }

    regions.sort(Comparator.comparing(Region::code));
    return new Contents(List.copyOf(regions), Optional.of(new Source(sourceName, sourceUrl)));
  }

  private static Level level(JsonNode node, String at) {
    String value = node.path("level").isString() ? node.path("level").asString() : "";
    for (Level level : Level.values()) {
      if (level.value().equals(value)) {
        return level;
      }
    }
    throw new IllegalArgumentException(at + ".level must be sido, sigungu or gu");
  }

  private static List<String> aliases(JsonNode node, String at) {
    check(node.path("aliases").isArray(), at + ".aliases must be an array");
    List<String> aliases = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    int index = 0;
    for (JsonNode alias : node.path("aliases")) {
      String aliasAt = at + ".aliases[" + index++ + "]";
      check(
          alias.isString() && !alias.asString().isBlank(), aliasAt + " must be a non-empty string");
      check(seen.add(alias.asString()), aliasAt + " repeats an alias of the same region");
      aliases.add(alias.asString());
    }
    return List.copyOf(aliases);
  }

  private static void keys(JsonNode node, Set<String> allowed, String at) {
    for (String key : node.propertyNames()) {
      check(allowed.contains(key), at + " has an unknown property " + key);
    }
  }

  /** A string with a non-space character. */
  private static String text(JsonNode node, String key, String at) {
    JsonNode value = node.path(key);
    check(
        value.isString() && !value.asString().isBlank(),
        at + "." + key + " must be a non-empty string");
    return value.asString();
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
