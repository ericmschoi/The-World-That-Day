package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a3b T1 (1): {@code wikimedia.commons-api-url} and its startup check. */
class CommonsPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(CommonsProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void defaultIsTheCommonsActionApi() {
    runner.run(
        context ->
            assertThat(context.getBean(CommonsProperties.class).commonsApiUrl())
                .isEqualTo(URI.create("https://commons.wikimedia.org/w/api.php")));
  }

  @Test
  void testConfigurationPointsAtLocalhost() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context ->
                assertThat(context.getBean(CommonsProperties.class).commonsApiUrl().toString())
                    .startsWith("http://localhost"));
  }

  @ParameterizedTest(name = "[{0}] fails startup")
  @ValueSource(
      strings = {"/w/api.php", "commons.wikimedia.org/w/api.php", "ftp://commons.wikimedia.org/w"})
  void relativeOrNonHttpAddressFailsStartup(String url) {
    runner
        .withPropertyValues("wikimedia.commons-api-url=" + url)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("wikimedia.commons-api-url"));
  }

  @Test
  void missingAddressIsRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CommonsProperties(null))
        .withMessageContaining("wikimedia.commons-api-url");
  }
}
