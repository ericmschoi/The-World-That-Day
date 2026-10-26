package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a5 T1 (2): YouTube API data is kept at most 30 days, so a longer ttl fails startup. */
class SearchCacheYoutubeTtlTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SearchCacheProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(PropertiesOnly.class)
          .withInitializer(new ConfigDataApplicationContextInitializer());

  @ParameterizedTest(name = "ttl.youtube {0} fails startup")
  @ValueSource(strings = {"31d", "721h", "2592001s"})
  void moreThanThirtyDaysFailsStartupWithItsName(String value) {
    runner
        .withPropertyValues("media.search-cache.ttl.youtube=" + value)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("media.search-cache.ttl.youtube"));
  }

  @ParameterizedTest(name = "ttl.youtube {0} starts")
  @ValueSource(strings = {"30d", "720h", "2592000s", "7d", "1s", "0"})
  void thirtyDaysOrLessStarts(String value) {
    runner
        .withPropertyValues("media.search-cache.ttl.youtube=" + value)
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void applicationYmlKeepsYoutubeForSevenDays() {
    runner.run(
        context ->
            assertThat(context.getBean(SearchCacheProperties.class).ttl("youtube"))
                .contains(Duration.ofDays(7)));
  }

  @Test
  void theCapIsExactlyThirtyDaysAndOnlyForYoutube() {
    assertThat(new SearchCacheProperties(Map.of("youtube", Duration.ofDays(30))).ttl("youtube"))
        .contains(Duration.ofDays(30));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new SearchCacheProperties(Map.of("youtube", Duration.ofDays(30).plusNanos(1))))
        .withMessageContaining("media.search-cache.ttl.youtube");
    assertThat(new SearchCacheProperties(Map.of("commons", Duration.ofDays(90))).ttl("commons"))
        .contains(Duration.ofDays(90));
  }
}
