package io.github.ericmschoi.theworldthatday.event.wikidata;

import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Interrupted;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the Wikidata Query Service once for all items of a request (CC0, User-Agent through {@link
 * WikimediaHttpClient}). The caller waits {@code wikimedia.wikidata-read-timeout} for the whole
 * lookup, retries included; when that runs out the request is cancelled and the caller waits at
 * most {@link Deadline#CANCEL_GRACE} (1s) more for it to stop, so one call takes up to the read
 * timeout plus 1s.
 */
@Component
public class SparqlWikidataSignalSource implements WikidataSignalSource {

  static final String SOURCE = "wikidata";
  private static final Logger log = LoggerFactory.getLogger(SparqlWikidataSignalSource.class);
  private static final String ENTITY = "http://www.wikidata.org/entity/";

  private final WikimediaHttpClient http;
  private final WikidataProperties properties;
  private final JsonMapper json;

  public SparqlWikidataSignalSource(
      WikimediaHttpClient http, WikidataProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public Optional<Map<String, Signals>> signals(Collection<String> qids) {
    List<String> items =
        qids.stream()
            .filter(qid -> qid != null && WikidataProperties.QID.matcher(qid).matches())
            .distinct()
            .toList();
    if (items.isEmpty()) {
      return Optional.of(Map.of());
    }
    URI uri =
        UriComponentsBuilder.fromUri(properties.wikidataSparqlUrl())
            .queryParam("format", "json")
            .queryParam("query", "{query}")
            .encode()
            .buildAndExpand(query(items, properties.wikidataTragicClasses()))
            .toUri();
    String label = "sparql of " + items.size() + " items";
    return within(label, () -> http.get(SOURCE, label, uri, properties.wikidataReadTimeout()))
        .flatMap(response -> parse(label, items, response));
  }

  /** The query of plan M3a2 "Wikidata 조회", on one line. */
  static String query(List<String> items, List<String> tragicClasses) {
    return "SELECT ?item ?tragicClass ?deaths ?country WHERE { VALUES ?item { "
        + values(items)
        + " } OPTIONAL { ?item wdt:P31/wdt:P279* ?tragicClass . VALUES ?tragicClass { "
        + values(tragicClasses)
        + " } } OPTIONAL { ?item wdt:P1120 ?deaths } OPTIONAL { ?item wdt:P17 ?country } }";
  }

  private static String values(List<String> qids) {
    return qids.stream().map(qid -> "wd:" + qid).collect(Collectors.joining(" "));
  }

  private Optional<Map<String, Signals>> parse(
      String label, List<String> items, WikimediaResponse response) {
    if (!(response instanceof WikimediaResponse.Ok(String body))) {
      return Optional.empty(); // the HTTP client logged the failure
    }
    JsonNode bindings = readBindings(body);
    if (!bindings.isArray()) {
      log.warn(
          "Wikimedia call failed: source={} title={} cause=unreadable response", SOURCE, label);
      return Optional.empty();
    }
    Map<String, Set<String>> countries = new LinkedHashMap<>();
    Set<String> tragic = new HashSet<>();
    Set<String> deaths = new HashSet<>();
    items.forEach(item -> countries.put(item, new HashSet<>()));
    for (JsonNode row : bindings) {
      String item = entity(row.path("item"));
      if (!countries.containsKey(item)) {
        continue;
      }
      if (row.has("tragicClass")) {
        tragic.add(item);
      }
      if (positive(row.path("deaths").path("value").asString(""))) {
        deaths.add(item);
      }
      String country = entity(row.path("country"));
      if (!country.isEmpty()) {
        countries.get(item).add(country);
      }
    }
    Map<String, Signals> signals = new LinkedHashMap<>();
    countries.forEach(
        (item, places) ->
            signals.put(item, new Signals(tragic.contains(item), deaths.contains(item), places)));
    return Optional.of(signals);
  }

  /** {@code results.bindings}, or a missing node when the body is not such JSON. */
  private JsonNode readBindings(String body) {
    try {
      return json.readTree(body).path("results").path("bindings");
    } catch (JacksonException e) {
      return json.missingNode();
    }
  }

  private static String entity(JsonNode node) {
    String value = node.path("value").asString("");
    return value.startsWith(ENTITY) ? value.substring(ENTITY.length()) : "";
  }

  private static boolean positive(String value) {
    try {
      return !value.isEmpty() && new BigDecimal(value).signum() > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * The response, or empty when the read timeout passes first. Then the request is interrupted and
   * normally ends at once with its own WARN from {@link WikimediaHttpClient}; the deadline is only
   * logged here when it did not, so one failure is one WARN line.
   */
  // Package-private for tests: no HTTP stub makes Failed (own InterruptedException) or Interrupted.
  Optional<WikimediaResponse> within(String label, Callable<WikimediaResponse> call) {
    return switch (Deadline.after(properties.wikidataReadTimeout()).start(SOURCE, call).await()) {
      case Done<WikimediaResponse>(WikimediaResponse response) -> Optional.of(response);
      case TimedOut<WikimediaResponse>(boolean stopped, WikimediaResponse late) -> {
        if (!stopped || !(late instanceof WikimediaResponse.Unavailable)) {
          log.warn("Wikimedia call failed: source={} title={} cause=deadline", SOURCE, label);
        }
        yield Optional.empty();
      }
      case Interrupted<WikimediaResponse>() -> Optional.empty();
      case Failed<WikimediaResponse>(Throwable e) -> {
        log.warn(
            "Wikimedia call failed: source={} title={} cause={}",
            SOURCE,
            label,
            e.getClass().getSimpleName());
        yield Optional.empty();
      }
    };
  }
}
