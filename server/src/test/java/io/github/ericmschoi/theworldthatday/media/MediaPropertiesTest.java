package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Allowed embed providers start empty, and a malformed entry fails startup. */
class MediaPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MediaProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void bothListsDefaultToEmpty() {
    runner.run(
        context -> {
          MediaProperties properties = context.getBean(MediaProperties.class);
          assertThat(properties.allowedImageEmbedProviders()).isEmpty();
          assertThat(properties.allowedVideoEmbedProviders()).isEmpty();
        });
  }

  @Test
  void applicationYmlStartsWithBothListsEmpty() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              MediaProperties properties = context.getBean(MediaProperties.class);
              assertThat(properties.allowedImageEmbedProviders()).isEmpty();
              assertThat(properties.allowedVideoEmbedProviders()).isEmpty();
            });
  }

  @Test
  void anEmptyListValueStaysEmpty() {
    runner
        .withPropertyValues("media.allowed-video-embed-providers=")
        .run(
            context ->
                assertThat(context.getBean(MediaProperties.class).allowedVideoEmbedProviders())
                    .isEmpty());
  }

  @Test
  void providersCanBeSet() {
    runner
        .withPropertyValues(
            "media.allowed-video-embed-providers=youtube,example",
            "media.allowed-image-embed-providers[0]=example")
        .run(
            context -> {
              MediaProperties properties = context.getBean(MediaProperties.class);
              assertThat(properties.allowedVideoEmbedProviders())
                  .containsExactly("youtube", "example");
              assertThat(properties.allowedImageEmbedProviders()).containsExactly("example");
            });
  }

  @ParameterizedTest(name = "allowed-video-embed-providers[0]=[{0}] fails startup")
  @ValueSource(strings = {"", " ", "YouTube", "you tube", "-youtube", "youtube,"})
  void malformedVideoProviderFailsStartup(String entry) {
    runner
        .withPropertyValues("media.allowed-video-embed-providers[0]=" + entry)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("media.allowed-video-embed-providers"));
  }

  @Test
  void emptyImageProviderEntryFailsStartup() {
    runner
        .withPropertyValues(
            "media.allowed-image-embed-providers[0]=youtube",
            "media.allowed-image-embed-providers[1]=")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("media.allowed-image-embed-providers"));
  }

  @Test
  void constructorRejectsABlankEntryAndCopiesTheLists() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new MediaProperties(List.of(""), List.of()))
        .withMessageContaining("media.allowed-image-embed-providers");
    // Spring trims bound values; the constructor still refuses surrounding spaces.
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new MediaProperties(List.of(), List.of(" youtube")))
        .withMessageContaining("media.allowed-video-embed-providers");
    assertThat(new MediaProperties(null, null).allowedVideoEmbedProviders()).isEmpty();
  }
}
