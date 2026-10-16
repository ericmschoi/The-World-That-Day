package io.github.ericmschoi.theworldthatday.media.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plan M3a3b T1 (3) and (7) through Spring: the Commons client is a bean on the shared Wikimedia
 * client, and its address is WireMock's Commons path, apart from the Wikipedia path.
 */
class CommonsClientContextTest extends WikimediaWireMockTest {

  @Autowired private CommonsClient client;
  @Autowired private CommonsProperties commons;

  @Test
  void commonsRequestsGoToTheirOwnWireMockPathWithTheTestUserAgent() {
    wikimedia.stubFor(
        get(urlPathEqualTo(COMMONS_PATH)).willReturn(okJson("{\"batchcomplete\":true}")));

    var pages = client.dayCategory(LocalDate.of(1995, 3, 15), Duration.ofSeconds(2));

    assertThat(pages.failed()).isFalse();
    assertThat(commons.commonsApiUrl().toString()).isEqualTo(wikimedia.baseUrl() + COMMONS_PATH);
    assertThat(COMMONS_PATH).isNotEqualTo(KOWIKI_PATH);
    assertThat(requestCount()).isEqualTo(1);
    var request = wikimedia.getAllServeEvents().getFirst().getRequest();
    assertThat(request.getUrl()).startsWith(COMMONS_PATH + "?");
    assertThat(request.getHeader("User-Agent")).isEqualTo(TEST_USER_AGENT);
  }
}
