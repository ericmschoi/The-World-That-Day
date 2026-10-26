package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

/**
 * A plain test context cannot reach Google: the key is empty and the address is local, even when
 * the root {@code .env} gives {@code YOUTUBE_API_KEY} as a property value (plan M3a5 T1 (4)). Same
 * configuration as EffectiveSettingsTest (the app configuration), so the cached context is shared.
 *
 * <p>Limit: the Gradle test task removes {@code YOUTUBE_API_KEY} from the test JVM's environment,
 * so an exported key never reaches these tests. A run outside Gradle (for example from an IDE) can
 * still pass the variable on; it would bind to {@code youtube.api-key} through relaxed binding and
 * override the test settings. The API address stays local, so nothing reaches Google even then.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class YoutubeTestDefaultsTest {

  @Autowired private YoutubeProperties properties;

  @Test
  void theTestContextHasNoKeyAndALocalAddress() {
    assertThat(properties.enabled()).isFalse();
    assertThat(properties.apiKey()).isEmpty();
    assertThat(properties.apiUrl().toString()).startsWith("http://localhost:9/");
  }

  @Test
  void aKeyGivenAsAPropertyValueDoesNotReachTheTestSettings() {
    new ApplicationContextRunner()
        .withUserConfiguration(YoutubePropertiesTest.PropertiesOnly.class)
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
        .withUserConfiguration(YoutubePropertiesTest.PropertiesOnly.class)
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
