package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * A plain test context can never reach Google: the key is empty and the address is local, even when
 * the root {@code .env} (or the environment) holds {@code YOUTUBE_API_KEY} (plan M3a5 T1 (4)). Same
 * configuration as EffectiveSettingsTest, so the cached context is shared.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class YoutubeTestDefaultsTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(YoutubeProperties.class)
  static class PropertiesOnly {}

  @Autowired private YoutubeProperties properties;

  @Test
  void theTestContextHasNoKeyAndALocalAddress() {
    assertThat(properties.enabled()).isFalse();
    assertThat(properties.apiKey()).isEmpty();
    assertThat(properties.apiUrl().toString()).startsWith("http://localhost:9/");
  }

  @Test
  void anEnvironmentKeyDoesNotReachTheTestSettings() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesOnly.class)
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("YOUTUBE_API_KEY=" + YoutubePropertiesTest.FAKE_KEY)
        .run(
            context -> {
              YoutubeProperties bound = context.getBean(YoutubeProperties.class);
              assertThat(bound.apiKey()).isEmpty();
              assertThat(bound.enabled()).isFalse();
              assertThat(bound.apiUrl().getHost()).isEqualTo("localhost");
            });
  }

  /** Control case: without the test file the same variable does reach the setting. */
  @Test
  void theMainApplicationYmlAloneReadsTheEnvironmentKey() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesOnly.class)
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues(
            "spring.config.location=classpath:/application.yml",
            "YOUTUBE_API_KEY=" + YoutubePropertiesTest.FAKE_KEY)
        .run(
            context -> {
              YoutubeProperties bound = context.getBean(YoutubeProperties.class);
              assertThat(bound.apiKey()).isEqualTo(YoutubePropertiesTest.FAKE_KEY);
              assertThat(bound.apiUrl().getHost()).isEqualTo("www.googleapis.com");
            });
  }
}
