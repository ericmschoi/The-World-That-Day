package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.data.DataSchemas;
import io.github.ericmschoi.theworldthatday.event.wikidata.SparqlWikidataSignalSourceTest;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.Type;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3b T2 through the whole application against WireMock: the demo table with the recorded
 * Commons day categories (T1), the synthetic tragic day, failures, the deadline, and carried-over
 * review recommendation 4 (null keys in Spring's own JSON mapper and in the MockMvc response). No
 * network.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryDayMediaTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** The 26 stored keys of plan M3a3 plus {@code date_relation}. */
  static final Set<String> ITEM_KEYS =
      Set.of(
          "id",
          "type",
          "display_mode",
          "url",
          "thumbnail_url",
          "thumbnail_width",
          "thumbnail_height",
          "video_sources",
          "duration_seconds",
          "embed_url",
          "provider",
          "render",
          "link_only_reason",
          "source_name",
          "source_url",
          "license",
          "license_status",
          "license_url",
          "attribution",
          "description",
          "captured_at",
          "date_precision",
          "lat",
          "lng",
          "place_ref",
          "retrieved_at",
          "date_relation");

  private static final List<String> NULL_PHOTO_KEYS =
      List.of(
          "video_sources",
          "duration_seconds",
          "embed_url",
          "provider",
          "render",
          "link_only_reason");

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private JsonMapper springJson;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private static String read(String path) {
    try (InputStream in =
        WorldStoryDayMediaTest.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
      if (in == null) {
        throw new IllegalArgumentException("no fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Section list by title, then the level-2 사건 and 문화 sections by revid and index. */
  private static void stubPage(String title, String prefix) {
    String kowiki = "wikimedia/kowiki/";
    JsonNode parse = JSON.readTree(read(kowiki + prefix + "-sections.json")).path("parse");
    String revid = Long.toString(parse.path("revid").asLong());
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo(title))
            .willReturn(okJson(read(kowiki + prefix + "-sections.json"))));
    for (JsonNode section : parse.path("sections")) {
      String line = section.path("line").asString("");
      String kind = line.equals("사건") ? "events" : line.equals("문화") ? "culture" : null;
      if (kind != null && "2".equals(section.path("level").asString(""))) {
        wikimedia.stubFor(
            WireMock.get(urlPathEqualTo(KOWIKI_PATH))
                .withQueryParam("oldid", equalTo(revid))
                .withQueryParam("section", equalTo(section.path("index").asString()))
                .willReturn(okJson(read(kowiki + prefix + "-" + kind + ".json"))));
      }
    }
  }

  private static void stubPageprops(String body) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(body)));
  }

  private static void stubSparql(String file) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(okJson(SparqlWikidataSignalSourceTest.fixture(file))));
  }

  private static void stubCommons(String date) {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson(read("wikimedia/commons/day-category-" + date + ".json"))));
  }

  private static void stubOpeningDayEvents() {
    stubPage("9월 17일", "09-17");
    stubPage("1988년", "1988");
    stubPageprops(read("wikimedia/kowiki/pageprops-1988-09-17.json"));
    stubSparql("sparql-1988-09-17.json");
  }

  private static long requests(String path) {
    return wikimedia.getAllServeEvents().stream()
        .filter(event -> event.getRequest().getUrl().startsWith(path))
        .filter(event -> !isVideoSearch(event))
        .count();
  }

  private MockHttpServletResponse call(String date) throws Exception {
    return mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
  }

  private JsonNode ok(String date) throws Exception {
    MockHttpServletResponse response = call(date);
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("\"display_mode\":\"link_only\"");
    JsonNode root = JSON.readTree(body);
    for (JsonNode event : root.path("events")) {
      assertThat(event.path("media").isArray()).isTrue();
      assertThat(event.path("media").isEmpty()).as("event media is M3a3c").isTrue();
    }
    for (JsonNode item : root.path("day_media")) {
      assertThat(Set.copyOf(item.propertyNames())).isEqualTo(ITEM_KEYS);
      for (String key : NULL_PHOTO_KEYS) {
        assertThat(item.get(key)).as(key).isNotNull();
        assertThat(item.get(key).isNull()).as(key).isTrue();
      }
      ObjectNode stored = (ObjectNode) item.deepCopy();
      stored.remove("date_relation");
      assertThat(DataSchemas.errors(DataSchemas.schema("media.schema.json"), stored)).isEmpty();
    }
    return root;
  }

  private static List<String> column(JsonNode items, String field) {
    List<String> values = new ArrayList<>();
    items.forEach(item -> values.add(item.path(field).asString(null)));
    return values;
  }

  // ---- demo table ----

  @Test
  void openingDayShowsEveryCategoryPhotoWithTheTwoDateFileLast() throws Exception {
    stubOpeningDayEvents();
    stubCommons("1988-09-17");

    JsonNode root = ok("1988-09-17");
    JsonNode media = root.path("day_media");

    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(root.path("events").size()).isEqualTo(3);
    assertThat(column(media, "id"))
        .containsExactly(
            "commons-4930021", // 1988 Summer Olympics opening ceremony.JPEG
            "commons-4196553", // Seoul Olympic torch.jpg
            "commons-112453227", // Seul 1988 - Romania.jpg
            "commons-4929997", // US Olympics Team 1988.JPEG
            "commons-26546944"); // Jose 5.jpg: two dates, unknown
    assertThat(column(media, "display_mode")).containsOnly("image");
    assertThat(column(media, "license_status")).containsOnly("free");
    for (int i = 0; i < 4; i++) {
      assertThat(media.get(i).path("date_relation").path("kind").asString()).isEqualTo("same_day");
      assertThat(media.get(i).path("captured_at").asString()).isEqualTo("1988-09-17");
      assertThat(media.get(i).path("date_precision").asString()).isEqualTo("day");
    }
    JsonNode unknown = media.get(4);
    assertThat(unknown.path("date_relation").path("kind").asString()).isEqualTo("unknown");
    assertThat(unknown.get("captured_at").isNull()).isTrue();
    assertThat(unknown.get("date_precision").isNull()).isTrue();
    assertThat(unknown.path("license").asString()).isEqualTo("CC BY-SA 3.0");
    assertThat(requests(COMMONS_PATH)).isEqualTo(1);
  }

  @Test
  void quarterFinalDayShowsTheKoglPhoto() throws Exception {
    stubPage("6월 22일", "06-22");
    stubPage("2002년", "2002");
    stubPageprops(read("wikimedia/kowiki/pageprops-2002-06-22.json"));
    stubSparql("sparql-2002-06-22.json");
    stubCommons("2002-06-22");

    JsonNode root = ok("2002-06-22");
    JsonNode media = root.path("day_media");

    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(media.size()).isEqualTo(1);
    JsonNode photo = media.get(0);
    assertThat(photo.path("license").asString()).isEqualTo("KOGL Type 1");
    assertThat(photo.path("license_status").asString()).isEqualTo("free");
    assertThat(photo.path("date_relation").path("kind").asString()).isEqualTo("same_day");
    assertThat(photo.path("attribution").asString())
        .isEqualTo("문덕관 기증 · KOGL Type 1 · Wikimedia Commons");
  }

  @Test
  void dayWithoutEventsAsksCommonsOnceAndHasNoPhotos() throws Exception {
    stubPage("3월 15일", "03-15");
    stubPage("1995년", "1995");
    stubCommons("1995-03-15");

    JsonNode root = ok("1995-03-15");

    assertThat(root.path("notice").path("code").asString()).isEqualTo("NO_EVENTS_FOUND");
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(requests(COMMONS_PATH)).isEqualTo(1);
    assertThat(requests(SPARQL_PATH)).isZero();
  }

  /** An uncurated event with Wikidata down is {@code tragic} by {@code default}. */
  @Test
  void tragicDayHasNoPhotosAndNoCommonsRequest() throws Exception {
    stubPage("2월 29일", "synthetic/synthetic-02-29");
    stubPage("2000년", "synthetic/synthetic-2000");
    stubPageprops(
        """
        {"batchcomplete":true,"query":{"pages":[{"pageid":1,"ns":0,"title":"시험 문서 가",
         "pageprops":{"wikibase_item":"Q1"}}]}}""");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH)).willReturn(aResponse().withStatus(503)));

    JsonNode root = ok("2000-02-29");

    assertThat(root.path("events").findValuesAsString("tone")).contains("tragic");
    assertThat(root.path("events").findValuesAsString("tone_basis")).contains("default");
    assertThat(root.path("day_media").isEmpty()).isTrue();
    assertThat(requests(COMMONS_PATH)).isZero();
  }

  // ---- failures ----

  @Test
  void unavailableOrRateLimitedCommonsKeepsTheEventsAndIsPartial() throws Exception {
    stubOpeningDayEvents();
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH)).willReturn(aResponse().withStatus(503)));

    JsonNode down = ok("1988-09-17");
    assertThat(down.path("events").size()).isEqualTo(3);
    assertThat(down.path("day_media").isEmpty()).isTrue();
    assertThat(down.path("partial").asBoolean()).isTrue();

    jdbc.execute("TRUNCATE wiki_section_cache");
    clock.advance(properties.rateLimitMaxWait().plusSeconds(1));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson("{\"error\":{\"code\":\"ratelimited\"}}")));
    JsonNode limited = ok("1988-09-17");
    assertThat(limited.path("events").size()).isEqualTo(3);
    assertThat(limited.path("day_media").isEmpty()).isTrue();
    assertThat(limited.path("partial").asBoolean()).isTrue();
  }

  @Test
  void bothWikipediaPagesUnavailableIsStill503WithoutCommons() throws Exception {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH)).willReturn(aResponse().withStatus(503)));

    MockHttpServletResponse response = call("1988-09-17");

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(JSON.readTree(response.getContentAsString()).path("code").asString())
        .isEqualTo("UPSTREAM_UNAVAILABLE");
    assertThat(requests(COMMONS_PATH)).isZero();
  }

  private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  @Test
  void slowCommonsEndsAtTheDeadlineWithOneWarning(CapturedOutput output) throws Exception {
    stubOpeningDayEvents();
    stubNoVideos(); // the slow day category below is the photo path's
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .willReturn(okJson("{\"batchcomplete\":true}").withFixedDelay(8_000)));

    long start = System.nanoTime();
    JsonNode root = ok("1988-09-17");
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    // media.lookup-deadline (3s) + 1s grace + 1s slack
    assertThat(took).isLessThan(Duration.ofSeconds(5));
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").size()).isEqualTo(3);
    List<String> warnings =
        output
            .getAll()
            .lines()
            .filter(l -> l.contains("Wikimedia call failed") && l.contains("source=commons"))
            .toList();
    assertThat(warnings).hasSize(1);
    String message = warnings.getFirst().substring(warnings.getFirst().indexOf("Wikimedia"));
    assertThat(message).contains("title=commons day category");
    assertThat(ISO_DATE.matcher(message).find()).isFalse();
    assertThat(output.getAll()).doesNotContain("File:", "Category:", "kowiki-1988");
  }

  // ---- carried-over review recommendation 4: null keys ----

  @Test
  void springJsonMapperKeepsEveryNullKeyOfAPhoto() {
    MediaRecord record =
        new MediaRecord(
            "commons-1000001",
            Type.PHOTO,
            DisplayMode.IMAGE,
            "https://upload.wikimedia.org/wikipedia/commons/a/ab/Synthetic.jpg",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Synthetic.jpg/960px-Synthetic.jpg",
            640,
            480,
            null,
            null,
            null,
            null,
            null,
            null,
            "Wikimedia Commons",
            "https://commons.wikimedia.org/wiki/File:Synthetic.jpg",
            null,
            LicenseStatus.UNKNOWN,
            null,
            "Synthetic Author · 라이선스 확인 안 됨 · Wikimedia Commons",
            null,
            null,
            null,
            null,
            null,
            "country:KR",
            "2026-01-01T00:00:00Z");
    MediaItem item = new MediaItem(record, new DateRelation(DateRelation.Kind.UNKNOWN, 0));

    JsonNode written = springJson.valueToTree(item);

    assertThat(new LinkedHashSet<>(written.propertyNames())).isEqualTo(ITEM_KEYS);
    for (String key : NULL_PHOTO_KEYS) {
      assertThat(written.get(key).isNull()).as(key).isTrue();
    }
    assertThat(written.get("captured_at").isNull()).isTrue();
    assertThat(written.get("date_precision").isNull()).isTrue();
  }
}
