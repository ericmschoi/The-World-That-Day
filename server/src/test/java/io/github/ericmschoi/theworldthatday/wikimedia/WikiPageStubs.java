package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * English Wikipedia page stubs for WireMock (plan C1 S3a): the section list ({@code
 * prop=tocdata|revid}) by title, then the level-2 {@code Events} section by revision and index, the
 * two requests {@code ActionApiKoWikiClient} sends. Pages come from {@code
 * fixtures/wikimedia/enwiki/<prefix>-tocdata.json} and {@code <prefix>-events.json} (recorded demo
 * pages such as {@code 07-17}, synthetic ones such as {@code synthetic/synthetic-09-17}), or are
 * built from a wikitext string. Never touches the network.
 */
public final class WikiPageStubs {

  public static final String PATH = WikimediaWireMockTest.KOWIKI_PATH;
  private static final String ROOT = "fixtures/wikimedia/enwiki/";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private WikiPageStubs() {}

  /** A fixture under {@code fixtures/}, e.g. {@code wikimedia/enwiki/07-17-events.json}. */
  public static String read(String path) {
    try (InputStream in =
        WikiPageStubs.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** {@code fixtures/wikimedia/enwiki/<path>}. */
  public static String enwiki(String path) {
    return read("wikimedia/enwiki/" + path);
  }

  /** The revision of a section list fixture. */
  public static long revid(String prefix) {
    return JSON.readTree(enwiki(prefix + "-tocdata.json")).path("parse").path("revid").asLong();
  }

  /** The index of the level-2 {@code Events} section of a section list, or {@code null}. */
  public static String eventsIndex(String tocdata) {
    for (JsonNode section : JSON.readTree(tocdata).path("parse").path("tocdata").path("sections")) {
      if (section.path("hLevel").asInt(0) == 2
          && "Events".equals(section.path("line").asString())) {
        return section.path("index").asString();
      }
    }
    return null;
  }

  /** The page from {@code <prefix>-tocdata.json} and {@code <prefix>-events.json}. */
  public static void stubPage(Stubbing server, String title, String prefix) {
    stubPage(server, title, prefix, 0);
  }

  /** As {@link #stubPage(Stubbing, String, String)}, every answer {@code delayMs} late. */
  public static void stubPage(Stubbing server, String title, String prefix, int delayMs) {
    String tocdata = enwiki(prefix + "-tocdata.json");
    String events = enwiki(prefix + "-events.json");
    stub(server, title, tocdata, events, delayMs);
  }

  /** A synthetic page whose {@code Events} section (index 1) is {@code wikitext}. */
  public static void stubPage(Stubbing server, String title, long revid, String wikitext) {
    stub(server, title, tocdata(title, revid), events(title, revid, wikitext), 0);
  }

  /** The section list answer of a synthetic page with one level-2 {@code Events} section. */
  public static String tocdata(String title, long revid) {
    ObjectNode root = JSON.createObjectNode();
    ObjectNode parse =
        root.putObject("parse").put("title", title).put("pageid", revid).put("revid", revid);
    parse.putArray("redirects");
    ArrayNode sections = parse.putObject("tocdata").putArray("sections");
    sections
        .addObject()
        .put("tocLevel", 1)
        .put("hLevel", 2)
        .put("line", "Events")
        .put("number", "1")
        .put("index", "1")
        .put("anchor", "Events");
    sections
        .addObject()
        .put("tocLevel", 1)
        .put("hLevel", 2)
        .put("line", "Births")
        .put("number", "2")
        .put("index", "2")
        .put("anchor", "Births");
    return root.toString();
  }

  /** The {@code Events} section answer of a synthetic page. */
  public static String events(String title, long revid, String wikitext) {
    ObjectNode root = JSON.createObjectNode();
    root.putObject("parse")
        .put("title", title)
        .put("pageid", revid)
        .put("revid", revid)
        .put("wikitext", wikitext);
    return root.toString();
  }

  /** The section list of {@code title} answers HTTP 503. */
  public static void unavailablePage(Stubbing server, String title) {
    server.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(aResponse().withStatus(503)));
  }

  private static void stub(
      Stubbing server, String title, String tocdata, String events, int delayMs) {
    String revid = Long.toString(JSON.readTree(tocdata).path("parse").path("revid").asLong());
    server.stubFor(
        get(urlPathEqualTo(PATH))
            .withQueryParam("page", equalTo(title))
            .withQueryParam("prop", equalTo("tocdata|revid"))
            .willReturn(delayed(okJson(tocdata), delayMs)));
    String index = eventsIndex(tocdata);
    if (index != null) {
      server.stubFor(
          get(urlPathEqualTo(PATH))
              .withQueryParam("oldid", equalTo(revid))
              .withQueryParam("prop", equalTo("wikitext"))
              .withQueryParam("section", equalTo(index))
              .willReturn(delayed(okJson(events), delayMs)));
    }
  }

  private static ResponseDefinitionBuilder delayed(ResponseDefinitionBuilder response, int ms) {
    return ms > 0 ? response.withFixedDelay(ms) : response;
  }
}
