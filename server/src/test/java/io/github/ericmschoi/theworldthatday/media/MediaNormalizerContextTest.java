package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.imageEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoEmbed;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Plan M3a3 T2 (5) through Spring: the {@link MediaNormalizer} bean built from {@code
 * application.yml} drops every provider embed, and a test setting that allows {@code example} keeps
 * them. {@link MediaNormalizerTest} checks the same rules on hand-built normalizers.
 */
class MediaNormalizerContextTest {

  private static final LocalDate OLYMPICS = LocalDate.of(1988, 9, 17);

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MediaProperties.class)
  @Import(MediaNormalizer.class)
  static class NormalizerOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withUserConfiguration(NormalizerOnly.class);

  @Test
  void applicationYmlNormalizerDropsEveryProviderEmbed() {
    runner.run(
        context -> {
          MediaNormalizer normalizer = context.getBean(MediaNormalizer.class);
          assertThat(normalizer.normalize(record(imageEmbed()), OLYMPICS)).isEmpty();
          assertThat(normalizer.normalize(record(videoEmbed()), OLYMPICS)).isEmpty();
          assertThat(normalizer.normalize(record(image()), OLYMPICS)).isPresent();
        });
  }

  @Test
  void allowingExampleInTheSettingsKeepsItsEmbeds() {
    runner
        .withPropertyValues(
            "media.allowed-image-embed-providers=example",
            "media.allowed-video-embed-providers=example")
        .run(
            context -> {
              MediaNormalizer normalizer = context.getBean(MediaNormalizer.class);
              assertThat(normalizer.normalize(record(imageEmbed()), OLYMPICS)).isPresent();
              assertThat(normalizer.normalize(record(videoEmbed()), OLYMPICS)).isPresent();
            });
  }
}
