package io.github.ericmschoi.theworldthatday.media.pageimages;

import io.github.ericmschoi.theworldthatday.wikimedia.ActionApiErrors;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient.Lane;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Action API {@code action=query&prop=pageimages&piprop=name&pilicense=free} on {@code
 * wikimedia.kowiki-api-url}, only via {@link WikimediaHttpClient} and sent once. Titles go in one
 * URI template variable, so {@code &} and {@code +} stay part of a title. Any {@code error} fails
 * the lookup ({@link ActionApiErrors}); {@code ratelimited} also blocks the host, and an answer
 * without {@code query.pages} fails too. Log lines carry a fixed label with the title count, never
 * a title or file name.
 */
@Component
public class ActionApiKoWikiPageImages implements KoWikiPageImages {

  static final String SOURCE = "kowiki";

  private static final Logger log = LoggerFactory.getLogger(ActionApiKoWikiPageImages.class);

  private final WikimediaHttpClient http;
  private final WikimediaProperties properties;
  private final JsonMapper json;

  public ActionApiKoWikiPageImages(
      WikimediaHttpClient http, WikimediaProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public Optional<Map<String, String>> imageNames(List<String> titles, Duration readTimeout) {
    List<String> batch =
        titles.stream()
            .filter(title -> title != null && !title.isBlank())
            .distinct()
            .limit(MAX_TITLES)
            .toList();
    if (batch.isEmpty()) {
      return Optional.of(Map.of());
    }
    URI uri =
        UriComponentsBuilder.fromUri(properties.kowikiApiUrl())
            .queryParam("action", "query")
            .queryParam("prop", "pageimages")
            .queryParam("piprop", "name")
            .queryParam("pilicense", "free")
            .queryParam("pilimit", MAX_TITLES)
            .queryParam("redirects", 1)
            .queryParam("titles", "{titles}")
            .queryParam("format", "json")
            .queryParam("formatversion", 2)
            .encode()
            .buildAndExpand(String.join("|", batch))
            .toUri();
    String label = "kowiki pageimages of " + batch.size();
    if (!(http.getOnce(SOURCE, label, uri, readTimeout, Lane.PHOTO)
        instanceof WikimediaResponse.Ok(String body))) {
      return Optional.empty();
    }
    JsonNode tree;
    try {
      tree = json.readTree(body);
    } catch (JacksonException e) {
      return failed(label, "invalid JSON");
    }
    Optional<String> error = ActionApiErrors.cause(tree);
    if (error.isPresent()) {
      if (ActionApiErrors.isRateLimited(tree)) {
        http.blockHost(uri);
      }
      return failed(label, error.get());
    }
    JsonNode query = tree.path("query");
    if (!query.path("pages").isArray()) {
      return failed(label, "no query");
    }
    Map<String, String> normalized = pairs(query.path("normalized"));
    Map<String, String> redirects = pairs(query.path("redirects"));
    Map<String, String> images = new HashMap<>();
    for (JsonNode page : query.path("pages")) {
      String image = page.path("pageimage").asString("").strip();
      if (!image.isEmpty()) {
        images.put(page.path("title").asString(""), image);
      }
    }
    Map<String, String> names = new LinkedHashMap<>();
    for (String title : batch) {
      String name = normalized.getOrDefault(title, title);
      String image = images.get(redirects.getOrDefault(name, name));
      if (image != null) {
        names.put(title, image.startsWith("File:") ? image.substring(5) : image);
      }
    }
    return Optional.of(names);
  }

  private static Map<String, String> pairs(JsonNode list) {
    Map<String, String> pairs = new HashMap<>();
    for (JsonNode pair : list) {
      pairs.put(pair.path("from").asString(""), pair.path("to").asString(""));
    }
    return pairs;
  }

  private static Optional<Map<String, String>> failed(String label, String cause) {
    log.warn("Wikimedia call failed: source={} title={} cause={}", SOURCE, label, cause);
    return Optional.empty();
  }
}
