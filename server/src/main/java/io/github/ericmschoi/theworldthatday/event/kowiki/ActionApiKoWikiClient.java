package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MediaWiki Action API ({@code action=parse}): one request for the section list and revision, then
 * one request per "사건"/"문화" section, pinned to that revision with {@code oldid}. Requests are
 * serial. Any failure makes the whole page unavailable so both sections come from one revision. An
 * API error {@code ratelimited} (sent with HTTP 200) is handled like a 429: no retry, and the host
 * is blocked through {@link WikimediaHttpClient#blockHost}.
 */
@Component
public class ActionApiKoWikiClient implements KoWikiClient {

  static final String SOURCE = "kowiki";
  static final String EVENTS = "사건";
  static final String CULTURE = "문화";
  static final String RATELIMITED = "ratelimited";

  private static final Logger log = LoggerFactory.getLogger(ActionApiKoWikiClient.class);

  private final WikimediaHttpClient http;
  private final WikimediaProperties properties;
  private final JsonMapper json;

  public ActionApiKoWikiClient(
      WikimediaHttpClient http, WikimediaProperties properties, JsonMapper json) {
    this.http = http;
    this.properties = properties;
    this.json = json;
  }

  @Override
  public KoWikiPage fetch(String title) {
    JsonNode toc =
        call(title, api().queryParam("page", title).queryParam("prop", "sections|revid"), true);
    if (toc == null) {
      return KoWikiPage.unavailable(title);
    }
    String error = toc.path("error").path("code").asString(null);
    if ("missingtitle".equals(error)) {
      return KoWikiPage.absent(title);
    }
    long revid = toc.path("parse").path("revid").asLong(0);
    if (error != null || revid <= 0) {
      return failed(title, error != null ? "api error " + error : "no revision");
    }
    String eventsIndex = null;
    String cultureIndex = null;
    for (JsonNode section : toc.path("parse").path("sections")) {
      if (!"2".equals(section.path("level").asString(null))) {
        continue;
      }
      String line = section.path("line").asString("");
      if (EVENTS.equals(line) && eventsIndex == null) {
        eventsIndex = section.path("index").asString(null);
      } else if (CULTURE.equals(line) && cultureIndex == null) {
        cultureIndex = section.path("index").asString(null);
      }
    }
    String events = eventsIndex == null ? null : section(title, revid, eventsIndex);
    if (eventsIndex != null && events == null) {
      return KoWikiPage.unavailable(title); // stop: the culture section alone would mislead
    }
    String culture = cultureIndex == null ? null : section(title, revid, cultureIndex);
    if (cultureIndex != null && culture == null) {
      return KoWikiPage.unavailable(title);
    }
    return new KoWikiPage(title, KoWikiPage.State.FOUND, revid, events, culture);
  }

  /** The section's wikitext, or {@code null} when the request fails. */
  private String section(String title, long revid, String index) {
    JsonNode body =
        call(
            title,
            api()
                .queryParam("oldid", revid)
                .queryParam("prop", "wikitext")
                .queryParam("section", index),
            false);
    JsonNode wikitext = body == null ? null : body.path("parse").path("wikitext");
    if (wikitext == null || !wikitext.isString()) {
      if (body != null) {
        failed(title, "no wikitext");
      }
      return null;
    }
    return wikitext.stringValue();
  }

  private UriComponentsBuilder api() {
    return UriComponentsBuilder.fromUri(properties.kowikiApiUrl()).queryParam("action", "parse");
  }

  private JsonNode call(String title, UriComponentsBuilder query, boolean followRedirects) {
    if (followRedirects) {
      query.queryParam("redirects", 1);
    }
    URI uri =
        query.queryParam("format", "json").queryParam("formatversion", 2).build().encode().toUri();
    WikimediaResponse response = http.get(SOURCE, title, uri);
    if (!(response instanceof WikimediaResponse.Ok(String body))) {
      return null;
    }
    try {
      JsonNode tree = json.readTree(body);
      if (RATELIMITED.equals(tree.path("error").path("code").asString(null))) {
        http.blockHost(uri);
        failed(title, "api error " + RATELIMITED);
        return null;
      }
      return tree;
    } catch (JacksonException e) {
      failed(title, "invalid JSON");
      return null;
    }
  }

  private static KoWikiPage failed(String title, String cause) {
    log.warn("Wikimedia call failed: source={} title={} cause={}", SOURCE, title, cause);
    return KoWikiPage.unavailable(title);
  }
}
