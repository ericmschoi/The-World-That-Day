package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * M3a5 review recommendation 7: the HTTP client of {@link YoutubeDataApiClient} connects within
 * {@code youtube.connect-timeout} (the default 2 s and another value). How the JDK times out a
 * connection is not tested here: that would need an address that never answers, outside the host.
 */
class YoutubeConnectTimeoutTest {

  @ParameterizedTest
  @ValueSource(strings = {"PT2S", "PT0.75S"})
  void theHttpClientUsesTheConnectTimeoutSetting(String timeout) {
    YoutubeProperties properties =
        new YoutubeProperties(
            YoutubePropertiesTest.FAKE_KEY,
            URI.create("http://localhost:9/youtube/v3"),
            List.of(YoutubeItems.CHANNEL),
            80,
            8000,
            25,
            Duration.parse(timeout),
            List.of());

    assertThat(YoutubeDataApiClient.httpClient(properties).connectTimeout())
        .contains(Duration.parse(timeout));
  }
}
