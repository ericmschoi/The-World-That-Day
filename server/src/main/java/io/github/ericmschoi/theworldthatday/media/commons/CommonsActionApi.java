package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.wikimedia.ActionApiErrors;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Commons Action API part the photo and video clients share: {@code action=query} with the
 * client's file info parameters, sent once through {@link WikimediaHttpClient#getOnce} (no retry,
 * plan M3a3c decision 1 (a)). Any {@code error} in the answer is a failure ({@link
 * ActionApiErrors}); the error {@code ratelimited} (sent with HTTP 200) blocks the host as a 429
 * does. A file batch answer without {@code query.pages} is a failure too. Log lines carry the
 * caller's fixed label, never a date, category or file name.
 */
final class CommonsActionApi {

  static final String SOURCE = "commons";

  private static final Logger log = LoggerFactory.getLogger(CommonsActionApi.class);

  private final WikimediaHttpClient http;
  private final CommonsProperties properties;
  private final JsonMapper json;
  private final Map<String, Object> info;

  /** {@code info}: the file info parameters in request order, e.g. {@code prop=imageinfo}. */
  CommonsActionApi(
      WikimediaHttpClient http,
      CommonsProperties properties,
      JsonMapper json,
      Map<String, Object> info) {
    this.http = http;
    this.properties = properties;
    this.json = json;
    this.info = info;
  }

  /** A new {@code action=query} request on the configured address. */
  UriComponentsBuilder query() {
    return UriComponentsBuilder.fromUri(properties.commonsApiUrl()).queryParam("action", "query");
  }

  /**
   * The {@code query.pages} of the answer to {@code query} (none when the answer has none, such as
   * for a category that does not exist), or a failure.
   */
  Pages pages(String label, UriComponentsBuilder query, Duration readTimeout, Object... variables) {
    JsonNode tree = answer(label, query, readTimeout, variables);
    if (tree == null) {
      return Pages.failure();
    }
    List<JsonNode> found = new ArrayList<>();
    tree.path("query").path("pages").forEach(found::add);
    return new Pages(found, false);
  }

  /**
   * The pages of {@code fileNames} (without {@code File:}, blank and repeated names left out, at
   * most {@link CommonsClient#MAX_FILES}) by requested name, following title normalization. No
   * request when no name is left. The label is {@code labelPrefix} and the number of names.
   */
  Files files(String labelPrefix, List<String> fileNames, Duration readTimeout) {
    List<String> names =
        fileNames.stream()
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .limit(CommonsClient.MAX_FILES)
            .toList();
    if (names.isEmpty()) {
      return new Files(Map.of(), false);
    }
    String label = labelPrefix + names.size();
    String titles = names.stream().map(name -> "File:" + name).collect(Collectors.joining("|"));
    JsonNode tree = answer(label, query().queryParam("titles", "{titles}"), readTimeout, titles);
    if (tree == null) {
      return Files.failure();
    }
    if (!tree.path("query").path("pages").isArray()) { // titles were asked, so pages must come back
      failed(label, "no query");
      return Files.failure();
    }
    Map<String, String> normalized = new HashMap<>();
    for (JsonNode pair : tree.path("query").path("normalized")) {
      normalized.put(pair.path("from").asString(""), pair.path("to").asString(""));
    }
    Map<String, JsonNode> byTitle = new HashMap<>();
    tree.path("query")
        .path("pages")
        .forEach(page -> byTitle.put(page.path("title").asString(""), page));
    Map<String, JsonNode> byName = new HashMap<>();
    for (String name : names) {
      JsonNode page = byTitle.get(normalized.getOrDefault("File:" + name, "File:" + name));
      if (page != null) {
        byName.put(name, page);
      }
    }
    return new Files(byName, false);
  }

  /**
   * The answer to {@code query} with the file info parameters, the URI template {@code variables}
   * encoded strictly, or null after a logged failure.
   */
  private JsonNode answer(
      String label, UriComponentsBuilder query, Duration readTimeout, Object... variables) {
    info.forEach(query::queryParam);
    URI uri =
        query
            // default language: a Korean DateTimeOriginal ("촬영 날짜: …") would not parse (PM, M3a3b)
            .queryParam("format", "json")
            .queryParam("formatversion", 2)
            .encode()
            .buildAndExpand(variables)
            .toUri();
    if (!(http.getOnce(SOURCE, label, uri, readTimeout)
        instanceof WikimediaResponse.Ok(String body))) {
      return null;
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
    JsonNode pages = tree.path("query").path("pages");
    if (!tree.isObject() || !(pages.isArray() || pages.isMissingNode())) {
      return failed(label, "unexpected body");
    }
    return tree;
  }

  private static JsonNode failed(String label, String cause) {
    log.warn("Wikimedia call failed: source={} title={} cause={}", SOURCE, label, cause);
    return null;
  }
}
