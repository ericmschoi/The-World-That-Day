package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Ok;
import java.net.URI;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/** The WireMock test base wires the real client bean to WireMock (plan M3a1 T1 (2), (7)). */
class WikimediaHttpClientContextTest extends WikimediaWireMockTest {

  @Autowired private WikimediaHttpClient client;
  @Autowired private Environment environment;
  @Autowired private Clock injectedClock;

  @Test
  void everyWikimediaUrlPointsAtLocalhost() {
    assertThat(environment.getProperty("wikimedia.kowiki-api-url"))
        .startsWith("http://localhost:" + wikimedia.port());
    assertThat(properties.kowikiApiUrl().toString()).startsWith("http://localhost");
  }

  @Test
  void clientBeanSendsTheConfiguredUserAgent() {
    wikimedia.stubFor(get(urlPathEqualTo(KOWIKI_PATH)).willReturn(ok("{}")));

    WikimediaResponse response =
        client.get("kowiki", "1988년", URI.create(properties.kowikiApiUrl() + "?action=parse"));

    assertThat(response).isEqualTo(new Ok("{}"));
    assertThat(requestCount()).isEqualTo(1);
    wikimedia.verify(
        1,
        getRequestedFor(urlPathEqualTo(KOWIKI_PATH))
            .withHeader("User-Agent", equalTo(TEST_USER_AGENT)));
  }

  @Test
  void testClockReplacesTheSystemClock() {
    assertThat(injectedClock).isSameAs(clock);
  }
}
