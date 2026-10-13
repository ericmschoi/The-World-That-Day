package io.github.ericmschoi.theworldthatday.event.tone;

import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Curated tone, scope and takedowns ({@code hide}) from {@code <app.data-dir>/curation/
 * event-tone.json}, read once at startup (plan M3a2). A missing file logs a WARN and nothing is
 * curated; a file that breaks the rules of {@code data/schema/event-curation.schema.json} stops
 * startup.
 */
@Component
public class EventCuration {

  static final String FILE = "curation/event-tone.json";
  private static final Logger log = LoggerFactory.getLogger(EventCuration.class);
  private static final Set<String> ROOT_KEYS = Set.of("version", "entries");
  private static final Set<String> ENTRY_KEYS =
      Set.of(
          "date",
          "primary_link",
          "summary_contains",
          "tone",
          "scope",
          "hide",
          "reason",
          "evidence_url",
          "reviewed_at");
  private static final List<String> REQUIRED =
      List.of("date", "evidence_url", "reason", "reviewed_at");
  private static final Pattern ISO_DATE =
      Pattern.compile("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])");
  private static final Pattern HTTPS_URL = Pattern.compile("https://\\S+");
  private static final DateTimeFormatter STRICT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

  /** One entry; {@code tone} is empty only when {@code hide} is true. */
  public record Entry(
      LocalDate date,
      Optional<String> primaryLink,
      Optional<String> summaryContains,
      Optional<Tone> tone,
      Optional<Scope> scope,
      boolean hide) {

    boolean matches(KoWikiEvent event) {
      return date.equals(event.date())
          && (primaryLink
                  .filter(
                      link ->
                          !event.linkTitles().isEmpty()
                              && same(link, event.linkTitles().getFirst()))
                  .isPresent()
              || summaryContains.filter(event.text()::contains).isPresent());
    }

    private static boolean same(String a, String b) {
      return a.replace('_', ' ').equals(b.replace('_', ' '));
    }
  }

  private final List<Entry> entries;

  public EventCuration(AppProperties properties, JsonMapper json) {
    Path file = properties.dataPath().resolve(FILE);
    if (!Files.isRegularFile(file)) {
      log.warn("Event curation file not found, nothing is curated: path={}", file);
      this.entries = List.of();
      return;
    }
    try {
      this.entries = List.copyOf(parse(json.readTree(file.toFile())));
    } catch (JacksonException e) {
      throw new IllegalStateException("Event curation file is not JSON: " + file, e);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Event curation file is invalid: " + file + ": " + e.getMessage(), e);
    }
    log.info("Event curation loaded: entries={}", entries.size());
  }

  public List<Entry> entries() {
    return entries;
  }

  /** The first matching entry that is not a takedown (it gives the curated tone and scope). */
  public Optional<Entry> match(KoWikiEvent event) {
    return entries.stream().filter(entry -> !entry.hide() && entry.matches(event)).findFirst();
  }

  /** Whether any matching entry takes the event down; a takedown wins over every other entry. */
  public boolean hidden(KoWikiEvent event) {
    return entries.stream().anyMatch(entry -> entry.hide() && entry.matches(event));
  }

  static List<Entry> parse(JsonNode root) {
    check(root.isObject(), "the root must be an object");
    keys(root, ROOT_KEYS, "the root");
    check(
        root.path("version").isIntegralNumber() && root.path("version").asInt() == 1,
        "version must be 1");
    check(root.path("entries").isArray(), "entries must be an array");
    List<Entry> entries = new ArrayList<>();
    int index = 0;
    for (JsonNode node : root.path("entries")) {
      String at = "entries[" + index++ + "]";
      check(node.isObject(), at + " must be an object");
      keys(node, ENTRY_KEYS, at);
      for (String key : REQUIRED) {
        check(node.has(key), at + " needs " + key);
      }
      LocalDate date = date(node, "date", at);
      date(node, "reviewed_at", at);
      check(text(node, "reason").isPresent(), at + ".reason must be a non-empty string");
      check(
          node.path("evidence_url").isString()
              && HTTPS_URL.matcher(node.path("evidence_url").asString()).matches(),
          at + ".evidence_url must be an https URL");
      Optional<String> link = optionalText(node, "primary_link", at);
      Optional<String> contains = optionalText(node, "summary_contains", at);
      check(
          link.isPresent() || contains.isPresent(), at + " needs primary_link or summary_contains");
      Optional<Tone> tone = choice(node, "tone", Tone.values(), Tone::value, at);
      Optional<Scope> scope = choice(node, "scope", Scope.values(), Scope::value, at);
      check(!node.has("hide") || node.get("hide").isBoolean(), at + ".hide must be a boolean");
      boolean hide = node.path("hide").asBoolean(false);
      check(hide || tone.isPresent(), at + " needs tone unless hide is true");
      entries.add(new Entry(date, link, contains, tone, scope, hide));
    }
    return entries;
  }

  private static void keys(JsonNode node, Set<String> allowed, String at) {
    for (String key : node.propertyNames()) {
      check(allowed.contains(key), at + " has an unknown property " + key);
    }
  }

  private static LocalDate date(JsonNode node, String key, String at) {
    String value = node.path(key).isString() ? node.path(key).asString() : "";
    check(ISO_DATE.matcher(value).matches(), at + "." + key + " must be YYYY-MM-DD");
    try {
      return LocalDate.parse(value, STRICT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(at + "." + key + " must be an existing date");
    }
  }

  /** A string with a non-space character. */
  private static Optional<String> text(JsonNode node, String key) {
    JsonNode value = node.path(key);
    return value.isString() && !value.asString().isBlank()
        ? Optional.of(value.asString())
        : Optional.empty();
  }

  /** Absent, {@code null}, or a string with a non-space character. */
  private static Optional<String> optionalText(JsonNode node, String key, String at) {
    Optional<String> value = text(node, key);
    check(
        !node.has(key) || node.get(key).isNull() || value.isPresent(),
        at + "." + key + " must be a non-empty string or null");
    return value;
  }

  private static <E extends Enum<E>> Optional<E> choice(
      JsonNode node, String key, E[] values, Function<E, String> name, String at) {
    if (!node.has(key)) {
      return Optional.empty();
    }
    String value = node.get(key).isString() ? node.get(key).asString() : "";
    Optional<E> found = Arrays.stream(values).filter(v -> name.apply(v).equals(value)).findFirst();
    check(found.isPresent(), at + "." + key + " has an unknown value");
    return found;
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
