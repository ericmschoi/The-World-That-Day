package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link WikidataVideoLinks} through the Wikidata Query Service (CC0): one SPARQL query for all
 * items, sent once with the caller's read timeout ({@link WikimediaHttpClient#getOnce}). Several
 * values of one property are read in name order, so the answer does not depend on row order. Log
 * lines carry a fixed label with the number of items, never a QID, category or file name.
 */
@Component
public class SparqlWikidataVideoLinks implements WikidataVideoLinks {

  static final String SOURCE = "wikidata";
  static final String LABEL = "wikidata video links of ";

  private static final Logger log = LoggerFactory.getLogger(SparqlWikidataVideoLinks.class);
  private static final String ENTITY = "http://www.wikidata.org/entity/";
  private static final String FILE_PATH = "/wiki/Special:FilePath/";
  private static final String CATEGORY = "Category:";

  private final WikimediaHttpClient http;
  private final WikidataProperties properties;
  private final JsonMapper json;

  public SparqlWikidataVideoLinks(
      WikimediaHttpClient http, WikidataProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public Optional<Map<String, Links>> links(Collection<String> qids, Duration readTimeout) {
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
            .buildAndExpand(query(items))
            .toUri();
    String label = LABEL + items.size() + " items";
    if (!(http.getOnce(SOURCE, label, uri, readTimeout)
        instanceof WikimediaResponse.Ok(String body))) {
      return Optional.empty(); // the HTTP client logged the failure
    }
    JsonNode bindings = bindings(body);
    if (!bindings.isArray()) {
      log.warn(
          "Wikimedia call failed: source={} title={} cause=unreadable response", SOURCE, label);
      return Optional.empty();
    }
    Map<String, Found> found = new LinkedHashMap<>();
    items.forEach(item -> found.put(item, new Found()));
    for (JsonNode row : bindings) {
      String entity = value(row, "item");
      Found item = entity.startsWith(ENTITY) ? found.get(entity.substring(ENTITY.length())) : null;
      if (item != null) {
        item.add(row);
      }
    }
    Map<String, Links> links = new HashMap<>();
    found.forEach((item, values) -> links.put(item, values.links()));
    return Optional.of(links);
  }

  /** The query of plan M3a4b "경로와 요청" step 1, on one line. */
  static String query(List<String> items) {
    return "SELECT ?item ?video ?p373 ?cs ?label WHERE { VALUES ?item { "
        + items.stream().map(qid -> "wd:" + qid).collect(Collectors.joining(" "))
        + " } OPTIONAL { ?item wdt:P10 ?video } OPTIONAL { ?item wdt:P373 ?p373 }"
        + " OPTIONAL { ?item wdt:P910 ?cat . ?page schema:about ?cat ;"
        + " schema:isPartOf <https://commons.wikimedia.org/> ; schema:name ?cs }"
        + " OPTIONAL { ?item rdfs:label ?label . FILTER(LANG(?label) = \"en\") } }";
  }

  /** {@code results.bindings}, or a missing node when the body is not such JSON. */
  private JsonNode bindings(String body) {
    try {
      return json.readTree(body).path("results").path("bindings");
    } catch (JacksonException e) {
      return json.missingNode();
    }
  }

  private static String value(JsonNode row, String name) {
    return row.path(name).path("value").asString("").strip();
  }

  /** The values one item's rows hold. */
  private static final class Found {

    private final TreeSet<String> videos = new TreeSet<>();
    private final TreeSet<String> p373 = new TreeSet<>();
    private final TreeSet<String> sitelinks = new TreeSet<>();
    private final TreeSet<String> labels = new TreeSet<>();

    void add(JsonNode row) {
      addIfAny(videos, fileName(value(row, "video")));
      addIfAny(p373, value(row, "p373"));
      String sitelink = value(row, "cs");
      addIfAny(
          sitelinks, sitelink.startsWith(CATEGORY) ? sitelink.substring(CATEGORY.length()) : "");
      addIfAny(labels, value(row, "label"));
    }

    Links links() {
      String category =
          !p373.isEmpty() ? p373.first() : sitelinks.isEmpty() ? null : sitelinks.first();
      return videos.isEmpty() && category == null && labels.isEmpty()
          ? Links.NONE
          : new Links(List.copyOf(videos), category, labels.isEmpty() ? null : labels.first());
    }

    /** The file name of a {@code Special:FilePath} address, or "" when it is not one. */
    private static String fileName(String address) {
      int at = address.indexOf(FILE_PATH);
      try {
        return at < 0
            ? ""
            : UriUtils.decode(address.substring(at + FILE_PATH.length()), StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) { // a malformed escape: not a file name
        return "";
      }
    }

    private static void addIfAny(TreeSet<String> values, String value) {
      String stripped = value.strip();
      if (!stripped.isEmpty()) {
        values.add(stripped);
      }
    }
  }
}
