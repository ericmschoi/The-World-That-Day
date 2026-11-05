package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.wikimedia.ActionApiErrors;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MediaWiki Action API ({@code action=parse}) on English Wikipedia (plan C1 S3a): one request for
 * the section list ({@code prop=tocdata|revid}) and revision, then one request for the level-2
 * {@code Events} section, pinned to that revision with {@code oldid}. A page without that section
 * is read with the first request only and has no events. Requests are serial. Any failure makes the
 * page unavailable. An API error {@code ratelimited} (sent with HTTP 200) is handled like a 429: no
 * retry, and the host is blocked through {@link WikimediaHttpClient#blockHost}. Failure logs name
 * the source and the page title, never wikitext.
 */
@Component
public class ActionApiKoWikiClient implements KoWikiClient {

  static final String SOURCE = "enwiki";
  static final String EVENTS = "Events";

  private static final Logger log = LoggerFactory.getLogger(ActionApiKoWikiClient.class);

  private final WikimediaHttpClient http;
  private final WikimediaProperties properties;
  private final JsonMapper json;
  private final Clock clock;

  public ActionApiKoWikiClient(
      WikimediaHttpClient http, WikimediaProperties properties, JsonMapper json, Clock clock) {
    this.http = http;
    this.properties = properties;
    this.json = json;
    this.clock = clock;
  }

  @Override
  public KoWikiPage fetch(String title) {
    JsonNode toc =
        call(title, api().queryParam("page", title).queryParam("prop", "tocdata|revid"), true);
    if (toc == null) {
      return KoWikiPage.unavailable(title);
    }
    if (ActionApiErrors.hasCode(toc, "missingtitle")) {
      return KoWikiPage.absent(title);
    }
    Optional<String> error = ActionApiErrors.cause(toc);
    long revid = toc.path("parse").path("revid").asLong(0);
    if (error.isPresent() || revid <= 0) {
      return failed(title, error.orElse("no revision"));
    }
    String eventsIndex = null;
    for (JsonNode section : toc.path("parse").path("tocdata").path("sections")) {
      if (section.path("hLevel").asInt(0) == 2
          && EVENTS.equals(section.path("line").asString(""))) {
        eventsIndex = section.path("index").asString(null);
        break;
      }
    }
    String events = eventsIndex == null ? null : section(title, revid, eventsIndex);
    if (eventsIndex != null && events == null) {
      return KoWikiPage.unavailable(title);
    }
    return new KoWikiPage(title, KoWikiPage.State.FRESH, revid, events, null, clock.instant());
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
      if (ActionApiErrors.isRateLimited(tree)) {
        http.blockHost(uri);
        failed(title, ActionApiErrors.cause(tree).orElse("api error"));
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
