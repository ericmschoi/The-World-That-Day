package io.github.ericmschoi.theworldthatday.media.pageimages;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;

/**
 * Plan M3a3c T1 (3) and (5) through Spring: the PageImages bean asks {@code
 * wikimedia.kowiki-api-url} (WireMock) with the test User-Agent, and the base class's default
 * PageImages stub answers it even when a test stubs pageprops on {@code action=query} alone.
 */
class KoWikiPageImagesContextTest extends WikimediaWireMockTest {

  @Autowired private KoWikiPageImages pageImages;

  @Test
  void defaultStubAnswersPageImagesBeforeAPagepropsStubOnActionQuery() {
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(
                okJson(ActionApiKoWikiPageImagesTest.fixture("kowiki/pageprops-1988-09-17.json"))));

    var names = pageImages.imageNames(List.of("1988년 하계 올림픽"), Duration.ofSeconds(2));

    assertThat(names).contains(Map.of());
    assertThat(requestCount()).isEqualTo(1);
    ServeEvent event = wikimedia.getAllServeEvents().getFirst();
    assertThat(event.getRequest().getUrl()).startsWith(KOWIKI_PATH + "?");
    assertThat(event.getRequest().queryParameter("prop").firstValue()).isEqualTo("pageimages");
    assertThat(event.getRequest().getHeader("User-Agent")).isEqualTo(TEST_USER_AGENT);
    assertThat(event.getResponse().getBodyAsString()).isEqualTo(NO_PAGE_IMAGES);
  }

  @Test
  void pagepropsRequestsStillReachTheTestsOwnStub() {
    String pageprops = ActionApiKoWikiPageImagesTest.fixture("kowiki/pageprops-1988-09-17.json");
    wikimedia.stubFor(
        WireMock.get(urlPathEqualTo(KOWIKI_PATH))
            .withQueryParam("action", equalTo("query"))
            .willReturn(okJson(pageprops)));

    var response =
        RestClient.create()
            .get()
            .uri(wikimedia.baseUrl() + KOWIKI_PATH + "?action=query&prop=pageprops&titles=x")
            .retrieve()
            .body(String.class);

    assertThat(response).isEqualTo(pageprops);
  }
}
