package io.github.ericmschoi.theworldthatday.story.world;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeItems;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a5a T5 (7): a failed or late YouTube search makes the answer partial, stores no row and is
 * asked again next time. A YouTube search slower than {@code media.lookup-deadline} runs inside the
 * one media window ({@code lookup-deadline} + {@link Deadline#CANCEL_GRACE} + 0.5 s, the bound of
 * {@code WorldStoryEventPhotoPartsRegressionTest}) and does not hold up the two photo paths: the
 * event photo is still shown and no Wikimedia call fails. Synthetic 1986-09-20 sample.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorldStoryYoutubeFailureTest extends YoutubeWorldTest {

  private static final int SILENT_MS = 12_000;
  private static final Duration MEDIA_SLACK = Duration.ofMillis(500);

  /** A free lead image of the Asian Games article, taken that day. */
  private static final String PHOTO =
      """
      {"batchcomplete":true,"query":{
       "normalized":[{"fromencoded":false,"from":"File:Synthetic_games_photo.jpg",
        "to":"File:Synthetic games photo.jpg"}],
       "pages":[{"pageid":900011,"ns":6,
       "title":"File:Synthetic games photo.jpg","imageinfo":[{
        "url":"https://upload.wikimedia.org/wikipedia/commons/0/01/Synthetic_games_photo.jpg",
        "descriptionurl":"https://commons.wikimedia.org/wiki/File:Synthetic_games_photo.jpg",
        "thumburl":"https://upload.wikimedia.org/wikipedia/commons/thumb/0/01/Synthetic_games_photo.jpg/640px-Synthetic_games_photo.jpg",
        "thumbwidth":640,"thumbheight":480,"mime":"image/jpeg",
        "extmetadata":{"Artist":{"value":"Synthetic Author"},
         "LicenseShortName":{"value":"CC BY-SA 4.0"},"DateTimeOriginal":{"value":"1986-09-20"}}}]}]}}""";

  @Autowired private MediaLookupProperties mediaLookup;

  @Test
  void aFailedSearchIsPartialStoresNothingAndIsAskedAgain() throws Exception {
    stubGamesDay();
    stubSearch(aResponse().withStatus(500));

    JsonNode failed = world("1986-09-20");
    assertThat(failed.path("partial").asBoolean()).isTrue();
    assertThat(youtubeItems(failed)).isEmpty();
    assertThat(youtubeRows()).isEmpty();

    stubGamesVideos();
    wikimedia.resetRequests();
    JsonNode next = world("1986-09-20");

    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(next.path("partial").asBoolean()).isFalse();
    assertThat(youtubeItems(next)).hasSize(1);
    assertThat(youtubeRows()).hasSize(1);
  }

  @Test
  void aFailedVideosCallIsPartialAndStoresNothing() throws Exception {
    stubGamesDay();
    stubSearch(okJson(YoutubeItems.fixture("search-1986-09-20.json")));
    stubVideos(aResponse().withStatus(503));

    JsonNode failed = world("1986-09-20");

    assertThat(failed.path("partial").asBoolean()).isTrue();
    assertThat(youtubeItems(failed)).isEmpty();
    assertThat(youtubeRows()).isEmpty();
  }

  @Test
  void aSilentYoutubeStaysInTheOneMediaWindowAndThePhotoPathsAnswer(CapturedOutput output)
      throws Exception {
    stubGamesDay();
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("prop", equalTo("pageimages"))
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":9000,\"ns\":0,"
                        + "\"title\":\""
                        + GAMES
                        + "\",\"pageimage\":\"Synthetic_games_photo.jpg\"}]}}")));
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(COMMONS_PATH))
            .withQueryParam("prop", equalTo("imageinfo"))
            .withQueryParam("titles", matching(".+"))
            .willReturn(okJson(PHOTO)));
    stubSearch(okJson(YoutubeItems.fixture("search-1986-09-20.json")).withFixedDelay(SILENT_MS));

    long start = System.nanoTime();
    JsonNode root = world("1986-09-20");
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    System.out.println(
        "M3a5a T5 (7) world section, YouTube search silent (stubs): " + took.toMillis() + " ms");

    assertThat(took)
        .as("one media window")
        .isLessThanOrEqualTo(
            mediaLookup.lookupDeadline().plus(Deadline.CANCEL_GRACE).plus(MEDIA_SLACK));
    assertThat(root.path("partial").asBoolean()).isTrue();
    assertThat(root.path("events").get(0).path("media").findValuesAsString("type"))
        .containsExactly("photo");
    assertThat(requests(SEARCH)).hasSize(1);
    assertThat(requests(VIDEOS)).isEmpty();
    assertThat(youtubeRows()).isEmpty();
    assertThat(output.getAll()).doesNotContain("Wikimedia call failed");
  }
}
