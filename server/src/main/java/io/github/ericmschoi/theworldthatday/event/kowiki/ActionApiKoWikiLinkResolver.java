package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Action API {@code action=query&prop=pageprops&ppprop=wikibase_item&redirects=1}: titles joined
 * with {@code |}, up to {@value #MAX_TITLES} per request, requests in series. Title normalization
 * and redirects are followed; a missing page keeps its title and has no item. Any failure,
 * including the API error {@code ratelimited} (which blocks the host like a 429), fails the whole
 * lookup.
 */
@Component
public class ActionApiKoWikiLinkResolver implements KoWikiLinkResolver {

  static final int MAX_TITLES = 50;

  private static final Logger log = LoggerFactory.getLogger(ActionApiKoWikiLinkResolver.class);

  private final WikimediaHttpClient http;
  private final WikimediaProperties properties;
  private final JsonMapper json;

  public ActionApiKoWikiLinkResolver(
      WikimediaHttpClient http, WikimediaProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public Optional<Map<String, Link>> resolve(List<String> titles) {
    Map<String, Link> links = new HashMap<>();
    for (int from = 0; from < titles.size(); from += MAX_TITLES) {
      List<String> batch = titles.subList(from, Math.min(from + MAX_TITLES, titles.size()));
      JsonNode query = query(batch);
      if (query == null) {
        return Optional.empty();
      }
      Map<String, String> normalized = pairs(query.path("normalized"));
      Map<String, String> redirects = pairs(query.path("redirects"));
      Map<String, String> items = new HashMap<>();
      for (JsonNode page : query.path("pages")) {
        items.put(
            page.path("title").asString(""),
            page.path("pageprops").path("wikibase_item").asString(null));
      }
      for (String title : batch) {
        String name = normalized.getOrDefault(title, title);
        String target = redirects.getOrDefault(name, name);
        links.put(title, new Link(target, items.get(target)));
      }
    }
    return Optional.of(links);
  }

  private static Map<String, String> pairs(JsonNode list) {
    Map<String, String> pairs = new HashMap<>();
    for (JsonNode pair : list) {
      pairs.put(pair.path("from").asString(""), pair.path("to").asString(""));
    }
    return pairs;
  }

  /** The {@code query} node, or {@code null} when the request or the response failed. */
  private JsonNode query(List<String> batch) {
    URI uri =
        UriComponentsBuilder.fromUri(properties.kowikiApiUrl())
            .queryParam("action", "query")
            .queryParam("prop", "pageprops")
            .queryParam("ppprop", "wikibase_item")
            .queryParam("redirects", 1)
            .queryParam("titles", "{titles}")
            .queryParam("format", "json")
            .queryParam("formatversion", 2)
            .encode()
            .buildAndExpand(String.join("|", batch))
            .toUri();
    String label = "pageprops of " + batch.size() + " titles";
    if (!(http.get(ActionApiKoWikiClient.SOURCE, label, uri)
        instanceof WikimediaResponse.Ok(String body))) {
      return null;
    }
    try {
      JsonNode tree = json.readTree(body);
      String error = tree.path("error").path("code").asString(null);
      if (ActionApiKoWikiClient.RATELIMITED.equals(error)) {
        http.blockHost(uri);
      }
      if (error != null || !tree.path("query").isObject()) {
        return failed(label, error != null ? "api error " + error : "no query");
      }
      return tree.path("query");
    } catch (JacksonException e) {
      return failed(label, "invalid JSON");
    }
  }

  private static JsonNode failed(String label, String cause) {
    log.warn(
        "Wikimedia call failed: source={} title={} cause={}",
        ActionApiKoWikiClient.SOURCE,
        label,
        cause);
    return null;
  }
}
