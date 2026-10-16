package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikidataProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan M3a3b T3 through the whole application against WireMock: the demo {@code day_media} table
 * row by row (T3 (4)), proof that the context sends nothing past WireMock (T3 (6)), logs of whole
 * demo requests at DEBUG that hold no date, category, file name or author (T2 (11), CLAUDE.md 7.6),
 * and the world section's worst time when Wikipedia, Wikidata and Commons are all slow (T3 (7)).
 * Recorded Commons fixtures and synthetic pages only; no network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryDayMediaAcceptanceTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String UPLOAD = "upload.wikimedia.org";
  private static final String THUMB = "thumb.wikimedia.org";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ConfigurableEnvironment environment;
  @Autowired private WikidataProperties wikidata;
  @Autowired private CommonsProperties commons;
  @Autowired private MediaLookupProperties mediaLookup;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryDayMediaAcceptanceTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/" + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Section list by title, then the level-2 사건 and 문화 sections by revid and index, each answer
   * after {@code delayMs}.
   */
  private static void stubPage(String title, String prefix, int delayMs) {
    String kowiki = "wikimedia/kowiki/";
    JsonNode parse = JSON.readTree(read(kowiki + prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(kowiki + prefix + "-sections.json")).withFixedDelay(delayMs)));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(
                    okJson(read(kowiki + prefix + "-" + kind + ".json")).withFixedDelay(delayMs)));
      }
    }
  }

  private static void stubPageprops(String file) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(read("wikimedia/kowiki/" + file))));
  }

  private static void stubSparql(String file, int delayMs) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(
                okJson(SparqlWikidataSignalSourceTest.fixture(file)).withFixedDelay(delayMs)));
  }

  private static void stubCommons(String body, int delayMs) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(body).withFixedDelay(delayMs)));
  }

  private static void stubOpeningDay() {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 0);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 0);
    stubCommons(read("wikimedia/commons/day-category-1988-09-17.json"), 0);
  }

  private static void stubQuarterFinalDay() {
    stubPage("6월 22일", "06-22", 0);
    stubPage("2002년", "2002", 0);
    stubPageprops("pageprops-2002-06-22.json");
    stubSparql("sparql-2002-06-22.json", 0);
    stubCommons(read("wikimedia/commons/day-category-2002-06-22.json"), 0);
  }

  private static long requests(String path) {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(path))
        .count();
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JSON.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  /** One row of the T3 (4) table: the fields the plan names, with the two address hosts. */
  private static String row(JsonNode item) {
    JsonNode relation = item.path("date_relation");
    return String.join(
        " | ",
        item.path("id").asString(),
        item.path("display_mode").asString(),
        item.path("license").asString("null"),
        item.path("license_status").asString(),
        item.path("captured_at").asString("null"),
        item.path("date_precision").asString("null"),
        relation.path("kind").asString() + "/" + relation.path("years").asInt(),
        URI.create(item.path("url").asString()).getHost(),
        URI.create(item.path("thumbnail_url").asString()).getHost());
  }

  private static List<String> rows(JsonNode items) {
    List<String> rows = new ArrayList<>();
    items.forEach(item -> rows.add(row(item)));
    return rows;
  }

  // ---- T3 (4): the demo day_media table ----

  @Test
  void demoDayMediaTableRowByRow() throws Exception {
    stubOpeningDay();
    JsonNode opening = ok("1988-09-17").path("day_media");

    String pd = "image | Public domain | free | 1988-09-17 | day | same_day/0 | ";
    assertThat(rows(opening))
        .containsExactly(
            "commons-4930021 | " + pd + UPLOAD + " | " + THUMB,
            "commons-4196553 | " + pd + UPLOAD + " | " + THUMB,
            "commons-112453227 | " + pd + UPLOAD + " | " + THUMB,
            "commons-4929997 | " + pd + UPLOAD + " | " + THUMB,
            "commons-26546944 | image | CC BY-SA 3.0 | free | null | null | unknown/0 | "
                + UPLOAD
                + " | "
                + THUMB);
    // the unknown file's keys are present with null values, never the category's date
    JsonNode unknown = opening.get(4);
    assertThat(unknown.has("captured_at") && unknown.get("captured_at").isNull()).isTrue();
    assertThat(unknown.has("date_precision") && unknown.get("date_precision").isNull()).isTrue();
    // every recorded thumbnail is the 960px step of a 640px request
    opening.forEach(
        item -> {
          assertThat(item.path("thumbnail_url").asString()).contains("/960px-");
          assertThat(item.path("thumbnail_width").asInt()).isEqualTo(640);
          assertThat(item.path("source_url").asString())
              .startsWith("https://commons.wikimedia.org/wiki/File:");
          assertThat(item.path("place_ref").asString()).isEqualTo("country:KR");
        });

    wikimedia.resetAll();
    stubQuarterFinalDay();
    JsonNode quarter = ok("2002-06-22").path("day_media");

    assertThat(rows(quarter))
        .containsExactly(
            "commons-129150318 | image | KOGL Type 1 | free | 2002-06-22 | day | same_day/0 | "
                + UPLOAD
                + " | "
                + THUMB);
    assertThat(quarter.get(0).path("license_url").asString())
        .isEqualTo("http://www.kogl.or.kr/info/licenseType1.do");

    wikimedia.resetAll();
    stubPage("3월 15일", "03-15", 0);
    stubPage("1995년", "1995", 0);
    stubCommons(read("wikimedia/commons/day-category-1995-03-15.json"), 0);
    JsonNode noEvents = ok("1995-03-15");

    assertThat(noEvents.path("day_media").isArray()).isTrue();
    assertThat(noEvents.path("day_media").isEmpty()).isTrue();
    assertThat(noEvents.path("partial").asBoolean()).isFalse();
  }

  // ---- T3 (6): nothing leaves for the real network ----

  /**
   * Every address setting of the context ({@code *-url}, {@code *.url}, {@code *-uri}, {@code
   * *.uri}, outside the JVM's own system properties and the process environment) is on localhost;
   * the three Wikimedia addresses are exactly WireMock's. Whole demo requests then reach only
   * WireMock's three paths, and every request matched a stub.
   */
  @Test
  void everyAddressSettingIsLocalAndDemoRequestsStayOnWireMock() throws Exception {
    Map<String, String> addresses = new TreeMap<>();
    environment
        .getPropertySources()
        .forEach(
            source -> {
              if (source instanceof EnumerablePropertySource<?> enumerable
                  && !StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME.equals(
                      source.getName())
                  && !StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(
                      source.getName())) {
                for (String name : enumerable.getPropertyNames()) {
                  String key = name.toLowerCase(Locale.ROOT);
                  if (key.contains(".")
                      && (key.endsWith("-url")
                          || key.endsWith(".url")
                          || key.endsWith("-uri")
                          || key.endsWith(".uri"))) {
                    addresses.put(name, environment.getProperty(name));
                  }
                }
              }
            });

    assertThat(addresses)
        .containsKeys(
            "wikimedia.kowiki-api-url",
            "wikimedia.wikidata-sparql-url",
            "wikimedia.commons-api-url");
    addresses.forEach(
        (name, value) -> {
          String plain = value.startsWith("jdbc:") ? value.substring(5) : value;
          assertThat(URI.create(plain).getHost()).as(name).isIn("localhost", "127.0.0.1");
        });
    String base = wikimedia.baseUrl();
    assertThat(properties.kowikiApiUrl().toString()).isEqualTo(base + KOWIKI_PATH);
    assertThat(wikidata.wikidataSparqlUrl().toString()).isEqualTo(base + SPARQL_PATH);
    assertThat(commons.commonsApiUrl().toString()).isEqualTo(base + COMMONS_PATH);

    stubOpeningDay();
    ok("1988-09-17");
    stubQuarterFinalDay();
    jdbc.execute("TRUNCATE wiki_section_cache");
    ok("2002-06-22");

    List<ServeEvent> served = wikimedia.getAllServeEvents();
    assertThat(served).isNotEmpty();
    assertThat(served)
        .allSatisfy(
            event ->
                assertThat(event.getRequest().getUrl())
                    .matches(
                        "^(" + KOWIKI_PATH + "|" + SPARQL_PATH + "|" + COMMONS_PATH + ")\\?.*$"));
    assertThat(requests(COMMONS_PATH)).isEqualTo(2);
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }

  // ---- T2 (11), CLAUDE.md 7.6: logs of whole demo requests at DEBUG ----

  private static final String SYNTHETIC_CATEGORY =
      """
      {"batchcomplete":true,"query":{"pages":[
       {"ns":6,"title":"File:Synthetic 1995-03-15 local.jpg","missing":true},
       {"pageid":2000001,"ns":6,"title":"File:Synthetic 1995-03-15 map.png","imageinfo":[{
         "url":"https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_map.png",
         "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_1995-03-15_map.png",
         "thumburl":"https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_map.png/960px-Synthetic_map.png",
         "thumbwidth":640,"thumbheight":480,"mime":"image/png",
         "extmetadata":{"Artist":{"value":"Synthetic Mapper"},
          "LicenseShortName":{"value":"CC0"},"DateTimeOriginal":{"value":"1995-03-15"}}}]},
       {"pageid":2000002,"ns":6,"title":"File:Synthetic 1995-03-15 wire.jpg","imageinfo":[{
         "url":"https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_wire.jpg",
         "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_1995-03-15_wire.jpg",
         "thumburl":"https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_wire.jpg/960px-Synthetic_wire.jpg",
         "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
         "extmetadata":{"Credit":{"value":"AP Photo/Synthetic Shooter"},
          "LicenseShortName":{"value":"CC0"},"DateTimeOriginal":{"value":"1995-03-15"}}}]},
       {"pageid":2000003,"ns":6,"title":"File:Synthetic 1995-03-15 kept.jpg","imageinfo":[{
         "url":"https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic_kept.jpg",
         "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_1995-03-15_kept.jpg",
         "thumburl":"https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic_kept.jpg/960px-Synthetic_kept.jpg",
         "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
         "extmetadata":{"Artist":{"value":"Synthetic Keeper"},
          "LicenseShortName":{"value":"CC0"},"DateTimeOriginal":{"value":"1995-03-15"}}}]}
      ]}}""";

  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  @Test
  void wholeDemoRequestsAtDebugLogNoDateCategoryFileNameOrAuthor(CapturedOutput output)
      throws Exception {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    try {
      stubOpeningDay();
      assertThat(ok("1988-09-17").path("day_media").size()).isEqualTo(5);

      wikimedia.resetAll();
      stubQuarterFinalDay();
      assertThat(ok("2002-06-22").path("day_media").size()).isEqualTo(1);

      wikimedia.resetAll();
      stubPage("3월 15일", "03-15", 0);
      stubPage("1995년", "1995", 0);
      stubCommons(SYNTHETIC_CATEGORY, 0);
      JsonNode noEvents = ok("1995-03-15");
      assertThat(noEvents.path("day_media").findValuesAsString("id"))
          .containsExactly("commons-2000003");
      assertThat(noEvents.path("partial").asBoolean()).isFalse();
    } finally {
      app.setLevel(before);
    }

    String logs = output.getAll();
    assertThat(logs).as("the run logged at DEBUG").contains("Day photos dropped:");
    assertThat(logs)
        .doesNotContain("1988-09-17", "2002-06-22", "1995-03-15")
        .doesNotContain("File:", "Category:", "photographs taken")
        .doesNotContain("Seoul Olympic torch", "Jose 5", "Seul 1988", "US Olympics Team")
        .doesNotContain("Olympics opening ceremony", "월드컵-대한민국")
        .doesNotContain("Ken Hackman", "Jose Quinaliza", "문덕관", "Comitetul")
        .doesNotContain("Synthetic Mapper", "Synthetic Shooter", "Synthetic Keeper", "Synthetic 1")
        .doesNotContain("kowiki-19", "kowiki-20");
    List<String> appMessages =
        logs.lines()
            .filter(line -> line.contains("i.g.e.") || line.contains("io.github.ericmschoi"))
            .map(line -> line.contains(" : ") ? line.substring(line.indexOf(" : ")) : line)
            .toList();
    assertThat(appMessages).isNotEmpty();
    assertThat(appMessages)
        .allSatisfy(message -> assertThat(ISO_DATE.matcher(message).find()).as(message).isFalse());
  }

  // ---- T3 (7): the world section's worst time with every source slow ----

  /**
   * The date page answers at once, every year page request just under the read timeout (so the page
   * still arrives near the Wikipedia day deadline), Wikidata and Commons never in time. The answer
   * is still 200 with the events, partial, and ends within kowiki-day-deadline +
   * wikidata-read-timeout + 1s grace + lookup-deadline + 1s grace (+1s slack). The measured time is
   * printed for the T3 report.
   */
  @Test
  void worldSectionWorstTimeWithEverySourceSlow() throws Exception {
    stubPage("9월 17일", "09-17", 0);
    stubPage("1988년", "1988", 1_200);
    stubPageprops("pageprops-1988-09-17.json");
    stubSparql("sparql-1988-09-17.json", 12_000);
    stubCommons(read("wikimedia/commons/day-category-1988-09-17.json"), 12_000);

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println("T3 (7) world section worst time (stubs): " + took.toMillis() + " ms");

    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(requests(SPARQL_PATH)).isPositive();
    // One day category call. Its read timeout equals media.lookup-deadline, so on a loaded
    // machine the one timeout retry (wikimedia.max-retries) can leave just before the deadline
    // cancels it (plan M3a3b "실패와 시간": retry after a timeout is "사실상 없음", not never).
    assertThat(requests(COMMONS_PATH)).isBetween(1L, 1L + properties.maxRetries());
    Duration bound =
        properties
            .kowikiDayDeadline()
            .plus(wikidata.wikidataReadTimeout())
            .plusSeconds(1)
            .plus(mediaLookup.lookupDeadline())
            .plusSeconds(1)
            .plusSeconds(1);
    assertThat(took).isLessThan(bound);
    // Wikidata and Commons each waited their whole budget, one after the other
    assertThat(took)
        .isGreaterThanOrEqualTo(wikidata.wikidataReadTimeout().plus(mediaLookup.lookupDeadline()));
  }
}
