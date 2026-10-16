package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a3b T2 (8): {@code media.day-media-limit} and {@code media.lookup-deadline}. */
class MediaLookupPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({MediaLookupProperties.class, MediaProperties.class})
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void defaultsAndApplicationYmlAreSixAndThreeSeconds() {
    runner.run(context -> assertDefaults(context.getBean(MediaLookupProperties.class)));
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              assertDefaults(context.getBean(MediaLookupProperties.class));
              assertThat(context.getBean(MediaProperties.class).blocklist()).isEmpty();
            });
  }

  private static void assertDefaults(MediaLookupProperties properties) {
    assertThat(properties.dayMediaLimit()).isEqualTo(6);
    assertThat(properties.lookupDeadline()).isEqualTo(Duration.ofSeconds(3));
  }

  @Test
  void zeroLimitTurnsTheLookupOffWithoutFailing() {
    runner
        .withPropertyValues("media.day-media-limit=0")
        .run(
            context ->
                assertThat(context.getBean(MediaLookupProperties.class).dayMediaLimit()).isZero());
  }

  @ParameterizedTest(name = "{0}={1} fails startup")
  @CsvSource({
    "media.day-media-limit, -1",
    "media.lookup-deadline, 0s",
    "media.lookup-deadline, -1s"
  })
  void badValueFailsStartupWithItsName(String key, String value) {
    runner
        .withPropertyValues(key + "=" + value)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(key));
  }
}
