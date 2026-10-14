package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Commons through the Action API ({@code action=query}, {@code prop=imageinfo}), only via {@link
 * WikimediaHttpClient}. An API error {@code ratelimited} (sent with HTTP 200) blocks the host as a
 * 429 does. Log lines carry a fixed label, never a date, category or file name.
 */
@Component
public class ActionApiCommonsClient implements CommonsClient {

  static final String SOURCE = "commons";
  static final String DAY_CATEGORY = "commons day category";
  static final String RATELIMITED = "ratelimited";

  private static final Logger log = LoggerFactory.getLogger(ActionApiCommonsClient.class);

  private final WikimediaHttpClient http;
  private final CommonsProperties properties;
  private final JsonMapper json;

  public ActionApiCommonsClient(
      WikimediaHttpClient http, CommonsProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public Pages dayCategory(LocalDate date, Duration readTimeout) {
    UriComponentsBuilder query =
        UriComponentsBuilder.fromUri(properties.commonsApiUrl())
            .queryParam("action", "query")
            .queryParam("generator", "categorymembers")
            .queryParam("gcmtitle", "Category:South Korea photographs taken on " + date)
            .queryParam("gcmtype", "file")
            .queryParam("gcmlimit", 20);
    return pages(DAY_CATEGORY, query, readTimeout);
  }

  /** Asks for the file info of {@code query} and reads {@code query.pages} of the answer. */
  Pages pages(String label, UriComponentsBuilder query, Duration readTimeout) {
    URI uri =
        query
            .queryParam("prop", "imageinfo")
            .queryParam("iiprop", "url|mime|size|extmetadata")
            .queryParam("iiurlwidth", 640)
            .queryParam("iiextmetadatalanguage", "ko")
            .queryParam("format", "json")
            .queryParam("formatversion", 2)
            .build()
            .encode()
            .toUri();
    if (!(http.get(SOURCE, label, uri, readTimeout) instanceof WikimediaResponse.Ok(String body))) {
      return Pages.failure();
    }
    JsonNode tree;
    try {
      tree = json.readTree(body);
    } catch (JacksonException e) {
      return failed(label, "invalid JSON");
    }
    String error = tree.path("error").path("code").asString(null);
    if (error != null) {
      if (RATELIMITED.equals(error)) {
        http.blockHost(uri);
      }
      return failed(label, "api error " + error);
    }
    JsonNode pages = tree.path("query").path("pages");
    if (!tree.isObject() || !(pages.isArray() || pages.isMissingNode())) {
      return failed(label, "unexpected body");
    }
    List<JsonNode> found = new ArrayList<>();
    pages.forEach(found::add);
    return new Pages(found, false);
  }

  private static Pages failed(String label, String cause) {
    log.warn("Wikimedia call failed: source={} title={} cause={}", SOURCE, label, cause);
    return Pages.failure();
  }
}
