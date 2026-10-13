package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.TestcontainersConfiguration;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Reason;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Unavailable;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * A plain test context (no WireMock base) can never reach the real Wikimedia: the URL is localhost
 * and the source is off (plan M3a1 T1 (7), test policy 1). Same configuration as
 * EffectiveSettingsTest, so the cached context is shared.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WikimediaTestDefaultsTest {

  @Autowired private WikimediaProperties properties;
  @Autowired private WikidataProperties wikidata;
  @Autowired private WikimediaHttpClient client;
  @Autowired private Clock clock;

  @Test
  void kowikiUrlStartsWithLocalhost() {
    assertThat(properties.kowikiApiUrl().toString()).startsWith("http://localhost");
  }

  @Test
  void wikimediaIsOffByDefaultInTests() {
    assertThat(properties.enabled()).isFalse();
    assertThat(client.get("kowiki", "9월 17일", properties.kowikiApiUrl()))
        .isEqualTo(new Unavailable(Reason.DISABLED));
  }

  @Test
  void productionValuesComeFromApplicationYml() {
    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(4));
    assertThat(properties.maxRetries()).isEqualTo(1);
    assertThat(properties.rateLimitMaxWait()).isEqualTo(Duration.ofSeconds(120));
    assertThat(properties.maxConcurrentRequests()).isEqualTo(3);
    assertThat(properties.cacheTtl()).isEqualTo(Duration.ofDays(7));
    assertThat(properties.kowikiDayDeadline()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void wikidataUrlStartsWithLocalhostAndKeepsTheProductionLimits() {
    assertThat(wikidata.wikidataSparqlUrl().toString()).startsWith("http://localhost");
    assertThat(wikidata.wikidataReadTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(wikidata.wikidataTragicClasses())
        .containsExactly("Q3839081", "Q171558", "Q2223653", "Q350604");
  }

  @Test
  void serviceClockUsesSeoulTime() {
    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
  }
}
