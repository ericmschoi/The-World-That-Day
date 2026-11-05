package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearchProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan C1 S3a T4 (7), decision D7 (a): with the {@code application.yml} values ({@code
 * media.day-media-limit: 0}, {@code media.video.max-events: 0}; the base class turns both on for
 * the other tests, so this class drops its properties) the world API sends no Commons date category
 * request and no video request, while the event photo (PageImages, then the Commons file) is still
 * shown inline with every §6 field. A leap day with one normal event in Canada and no tragic line,
 * so only the settings keep the day photos off. Synthetic pages and WireMock only.
 */
@TestPropertySource(inheritProperties = false, properties = "media.search-cache.ttl.commons=0")
class WorldStoryDayMediaOffTest extends WikimediaWireMockTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String LEAP_DAY = "2000-02-29";

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MediaLookupProperties mediaLookup;
  @Autowired private VideoSearchProperties videoSearch;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE wiki_section_cache");
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  /** The leap day: one line, Test page A (Q1, in Canada, no tragic signal); no 2000 page. */
  private static void stubLeapDay(String taken) {
    WikiPageStubs.stubPage(
        wikimedia,
        "February 29",
        100L,
        "==Events==\n*[[2000]] &ndash; [[Test page A|Test event A]] opens.\n");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("page", equalTo("2000"))
            .willReturn(okJson(WikiPageStubs.enwiki("synthetic/synthetic-missingtitle.json"))));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(
                okJson(
                    """
                    {"batchcomplete":true,"query":{"pages":[{"pageid":1,"ns":0,
                     "title":"Test page A","pageprops":{"wikibase_item":"Q1"}}]}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .willReturn(
                okJson(
                    """
                    {"head":{"vars":["item","tragicClass","deaths","country"]},"results":{"bindings":[
                     {"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q1"},
                      "country":{"type":"uri","value":"http://www.wikidata.org/entity/Q16"}}]}}""")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("prop", equalTo("pageimages"))
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,"
                        + "\"title\":\"Test page A\",\"pageimage\":\"Synthetic_test_photo.jpg\"}]}}")));
    String date = taken == null ? "" : ",\"DateTimeOriginal\":{\"value\":\"" + taken + "\"}";
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("imageinfo"))
            .withQueryParam("titles", matching(".+"))
            .willReturn(
                okJson(
                    """
                    {"batchcomplete":true,"query":{
                     "normalized":[{"fromencoded":false,"from":"File:Synthetic_test_photo.jpg",
                      "to":"File:Synthetic test photo.jpg"}],
                     "pages":[{"pageid":900001,"ns":6,
                     "title":"File:Synthetic test photo.jpg","imageinfo":[{
                      "url":"https://upload.wikimedia.org/wikipedia/commons/0/00/Synthetic_test_photo.jpg",
                      "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_test_photo.jpg",
                      "thumburl":"https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Synthetic_test_photo.jpg/640px-Synthetic_test_photo.jpg",
                      "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
                      "extmetadata":{"Artist":{"value":"Synthetic Author"},
                       "LicenseShortName":{"value":"CC BY-SA 4.0"}%s}}]}]}}"""
                        .formatted(date))));
    // what the day photos and the video search would get if they asked
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("generator", equalTo("categorymembers"))
            .atPriority(2)
            .willReturn(okJson("{\"batchcomplete\":true}")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(SPARQL_PATH))
            .withQueryParam("query", containing("?video"))
            .atPriority(1)
            .willReturn(okJson(WorldStoryCommonsVideoTest.LINKS)));
  }

  private JsonNode photoOfTheOnlyEvent(String date) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get("/api/story/world").param("date", date)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("\"link_only\"");
    JsonNode root = JSON.readTree(body);
    assertThat(root.path("events").size()).isEqualTo(1);
    JsonNode event = root.path("events").get(0);
    assertThat(event.path("title").asString()).isEqualTo("Test page A");
    assertThat(event.path("tone").asString()).isEqualTo("normal");
    assertThat(event.path("scope").asString()).isEqualTo("domestic");
    assertThat(root.path("day_media").isArray()).isTrue();
    assertThat(root.path("day_media").isEmpty()).as("day photos are off").isTrue();
    assertThat(root.path("partial").asBoolean()).isFalse();
    assertThat(root.findValuesAsString("type")).doesNotContain("video");
    assertThat(dateCategoryRequests()).as("no Commons date category request").isZero();
    assertThat(videoRequests()).as("no video request").isEmpty();
    assertThat(event.path("media").size()).isEqualTo(1);
    JsonNode photo = event.path("media").get(0);
    assertThat(photo.path("type").asString()).isEqualTo("photo");
    assertThat(photo.path("display_mode").asString()).isEqualTo("image");
    assertThat(photo.path("place_ref").asString()).isEqualTo("country:CA");
    assertThat(photo.path("source_name").asString()).isNotBlank();
    assertThat(photo.path("source_url").asString())
        .isEqualTo("https://commons.wikimedia.org/wiki/File:Synthetic_test_photo.jpg");
    assertThat(photo.path("license").asString()).isEqualTo("CC BY-SA 4.0");
    assertThat(photo.path("attribution").asString()).contains("Synthetic Author");
    assertThat(photo.path("url").asString())
        .as("shown from the original URL, never copied")
        .startsWith("https://upload.wikimedia.org/");
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
    return photo;
  }

  @Test
  void applicationYmlTurnsDayPhotosAndVideosOff() {
    assertThat(mediaLookup.dayMediaLimit()).isZero();
    assertThat(videoSearch.maxEvents()).isZero();
  }

  @Test
  void eventPhotoTakenAnotherYearKeepsItsDateAndEverySourceField() throws Exception {
    stubLeapDay("1998");

    JsonNode photo = photoOfTheOnlyEvent(LEAP_DAY);

    assertThat(photo.path("captured_at").asString()).isEqualTo("1998");
    assertThat(photo.path("date_precision").asString()).isEqualTo("year");
    assertThat(photo.path("date_relation").path("kind").asString())
        .as("never shown as that day")
        .isEqualTo("years_apart");
    assertThat(photo.path("date_relation").path("years").asInt()).isEqualTo(-2);
  }

  @Test
  void eventPhotoWithoutCaptureTimeHasNoPrecision() throws Exception {
    stubLeapDay(null);

    JsonNode photo = photoOfTheOnlyEvent(LEAP_DAY);

    assertThat(photo.path("captured_at").isNull()).isTrue();
    assertThat(photo.path("date_precision").isNull()).as("capture time unknown").isTrue();
    assertThat(photo.path("date_relation").path("kind").asString()).isEqualTo("unknown");
  }

  private static long dateCategoryRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(e -> e.getRequest().getUrl().startsWith(COMMONS_PATH))
        .filter(e -> e.getRequest().queryParameter("generator").isPresent())
        .count();
  }

  private static List<ServeEvent> videoRequests() {
    return wikimedia.getAllServeEvents().stream()
        .filter(WikimediaWireMockTest::isVideoSearch)
        .toList();
  }
}
