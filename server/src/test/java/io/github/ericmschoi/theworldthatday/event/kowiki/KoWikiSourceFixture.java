package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the real event source parts (HTTP client, Action API page client, link resolver, parser)
 * against a WireMock server, and stubs English pages from fixtures (plan M3a1c tests, plan C1 S3a,
 * no network).
 */
final class KoWikiSourceFixture {

  static final String PATH = "/w/api.php";
  static final String USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";
  static final String EMPTY_PAGEPROPS = "{\"batchcomplete\":true,\"query\":{\"pages\":[]}}";

  final WikimediaProperties properties;
  final MutableClock clock =
      new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
  final WikimediaHttpClient http;
  final ActionApiKoWikiClient pages;
  final ActionApiKoWikiLinkResolver resolver;
  final WikitextEventParser parser = new WikitextEventParser();
  final KoWikiEventDeduplicator deduplicator =
      new KoWikiEventDeduplicator(new KoWikiDedupProperties(0.6));

  KoWikiSourceFixture(WireMockExtension server, Duration readTimeout, Duration dayDeadline) {
    properties =
        new WikimediaProperties(
            USER_AGENT,
            Duration.ofSeconds(2),
            readTimeout,
            1,
            Duration.ofSeconds(120),
            3,
            Duration.ofDays(7),
            dayDeadline,
            URI.create(server.baseUrl() + PATH));
    http = new WikimediaHttpClient(properties, clock);
    pages = new ActionApiKoWikiClient(http, properties, KoWikiFixtures.JSON, clock);
    resolver = new ActionApiKoWikiLinkResolver(http, properties, KoWikiFixtures.JSON);
  }

  TwoPageKoWikiEventSource source() {
    return source(pages);
  }

  TwoPageKoWikiEventSource source(KoWikiClient client) {
    return new TwoPageKoWikiEventSource(client, resolver, parser, http, properties, deduplicator);
  }

  /** Section list by title, then the {@code Events} section, from {@code <prefix>-*.json}. */
  static void stubPage(Stubbing server, String title, String prefix) {
    WikiPageStubs.stubPage(server, title, prefix);
  }

  /** A recorded or synthetic page of {@link EnWikiPages} by title. */
  static void stubRecorded(Stubbing server, String title) {
    String prefix = EnWikiPages.RECORDED.getOrDefault(title, EnWikiPages.SYNTHETIC.get(title));
    stubPage(server, title, prefix);
  }

  static void stubPageprops(WireMockExtension server, String body) {
    server.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  /** Requests in the order they were received. */
  static List<ServeEvent> requests(WireMockExtension server) {
    return server.getAllServeEvents().reversed();
  }

  static String param(ServeEvent event, String name) {
    var value = event.getRequest().queryParameter(name);
    return value.isPresent() ? value.firstValue() : null;
  }

  static List<String> params(WireMockExtension server, String name) {
    Function<ServeEvent, String> value = event -> param(event, name);
    return requests(server).stream().map(value).filter(v -> v != null).toList();
  }

  static List<ServeEvent> pagepropsRequests(WireMockExtension server) {
    return requests(server).stream().filter(e -> "query".equals(param(e, "action"))).toList();
  }
}
