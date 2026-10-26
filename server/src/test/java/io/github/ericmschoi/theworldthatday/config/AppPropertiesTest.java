package io.github.ericmschoi.theworldthatday.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a5 T1 (1): {@code app.web-origin}. */
class AppPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AppProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void theDefaultsAreTheLocalWebAppAndTheDataFolder() {
    runner.run(
        context -> {
          AppProperties properties = context.getBean(AppProperties.class);
          assertThat(properties.webOrigin()).isEqualTo("http://localhost:5173");
          assertThat(properties.dataDir()).isEqualTo("../data");
        });
  }

  @Test
  void applicationYmlGivesTheSameValues() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              AppProperties properties = context.getBean(AppProperties.class);
              assertThat(properties.webOrigin()).isEqualTo("http://localhost:5173");
              assertThat(properties.dataDir()).isEqualTo("../data");
            });
  }

  @ParameterizedTest(name = "web-origin {0} starts")
  @ValueSource(
      strings = {
        "http://localhost:5173",
        "https://example.test",
        "https://example.test:8443",
        "http://127.0.0.1:3000"
      })
  void anOriginStarts(String origin) {
    runner
        .withPropertyValues("app.web-origin=" + origin)
        .run(
            context ->
                assertThat(context.getBean(AppProperties.class).webOrigin()).isEqualTo(origin));
  }

  @ParameterizedTest(name = "web-origin [{0}] fails startup")
  @ValueSource(
      strings = {
        "http://localhost:5173/",
        "http://localhost:5173/app",
        "http://localhost:5173?x=1",
        "http://localhost:5173#top",
        "http://user@localhost:5173",
        "ftp://x",
        "localhost:5173",
        "//localhost:5173",
        "http://",
        "http://local host",
        "",
        "   "
      })
  void anythingElseFailsStartupWithItsName(String origin) {
    runner
        .withPropertyValues("app.web-origin=" + origin)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("app.web-origin"));
  }

  @Test
  void theDataFolderConstructorKeepsTheDefaultOrigin() {
    assertThat(new AppProperties("somewhere").webOrigin()).isEqualTo("http://localhost:5173");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AppProperties("somewhere", null))
        .withMessageContaining("app.web-origin");
  }
}
