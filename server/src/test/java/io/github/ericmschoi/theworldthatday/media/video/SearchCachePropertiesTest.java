package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a4c T1 (4): the {@code media.search-cache.ttl} settings. */
class SearchCachePropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({
    SearchCacheProperties.class,
    MediaProperties.class,
    MediaLookupProperties.class,
    VideoSearchProperties.class
  })
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void applicationYmlCachesCommonsForSevenDaysAndNothingElse() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              SearchCacheProperties properties = context.getBean(SearchCacheProperties.class);
              assertThat(properties.ttl()).containsOnlyKeys("commons");
              assertThat(properties.ttl("commons")).contains(Duration.ofDays(7));
              assertThat(properties.ttl("youtube")).isEmpty();
              assertThat(properties.ttl("x")).isEmpty();
            });
  }

  @Test
  void withoutSettingsNoSourceIsCached() {
    runner.run(
        context -> {
          SearchCacheProperties properties = context.getBean(SearchCacheProperties.class);
          assertThat(properties.ttl()).isEmpty();
          assertThat(properties.ttl("commons")).isEmpty();
        });
  }

  @ParameterizedTest(name = "ttl {0} turns the cache off")
  @ValueSource(strings = {"0", "0s", "0d"})
  void zeroTurnsTheSourceOffWithoutFailing(String value) {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("media.search-cache.ttl.commons=" + value)
        .run(
            context ->
                assertThat(context.getBean(SearchCacheProperties.class).ttl("commons")).isEmpty());
  }

  @Test
  void theSmallestPositiveTtlCaches() {
    SearchCacheProperties properties =
        new SearchCacheProperties(Map.of("commons", Duration.ofNanos(1)));

    assertThat(properties.ttl("commons")).contains(Duration.ofNanos(1));
  }

  @Test
  void anotherSourceIsReadFromItsOwnKey() {
    runner
        .withPropertyValues(
            "media.search-cache.ttl.commons=7d", "media.search-cache.ttl.example=12h")
        .run(
            context -> {
              SearchCacheProperties properties = context.getBean(SearchCacheProperties.class);
              assertThat(properties.ttl("commons")).contains(Duration.ofDays(7));
              assertThat(properties.ttl("example")).contains(Duration.ofHours(12));
            });
  }

  @ParameterizedTest(name = "ttl {0} fails startup")
  @ValueSource(strings = {"-1s", "-7d", "-1ns"})
  void negativeTtlFailsStartupWithItsName(String value) {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("media.search-cache.ttl.commons=" + value)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("media.search-cache.ttl.commons"));
  }

  @Test
  void negativeOrMissingValueIsRejectedByTheConstructorWithItsName() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SearchCacheProperties(Map.of("commons", Duration.ofSeconds(-1))))
        .withMessageContaining("media.search-cache.ttl.commons");
    Map<String, Duration> missing = new HashMap<>();
    missing.put("commons", null);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SearchCacheProperties(missing))
        .withMessageContaining("media.search-cache.ttl.commons");
  }

  @Test
  void nullMapIsEmptyAndTheMapIsACopy() {
    assertThat(new SearchCacheProperties(null).ttl()).isEmpty();

    Map<String, Duration> values = new HashMap<>(Map.of("commons", Duration.ofDays(7)));
    SearchCacheProperties properties = new SearchCacheProperties(values);
    values.put("commons", Duration.ZERO);

    assertThat(properties.ttl("commons")).contains(Duration.ofDays(7));
  }

  @Test
  void theCacheBlockLeavesTheOtherMediaSettingsAsTheyWere() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              MediaLookupProperties lookup = context.getBean(MediaLookupProperties.class);
              assertThat(lookup.dayMediaLimit()).isEqualTo(6);
              assertThat(lookup.lookupDeadline()).isEqualTo(Duration.ofSeconds(3));
              assertThat(lookup.maxYearsApart()).isEqualTo(5);
              MediaProperties media = context.getBean(MediaProperties.class);
              assertThat(media.allowedVideoEmbedProviders()).isEmpty();
              assertThat(media.blocklist()).isEmpty();
              assertThat(media.excludedCredits()).contains("Yonhap", "AP");
              VideoSearchProperties video = context.getBean(VideoSearchProperties.class);
              assertThat(video.minScore()).isEqualTo(60);
              assertThat(video.maxEvents()).isEqualTo(3);
            });
  }
}
