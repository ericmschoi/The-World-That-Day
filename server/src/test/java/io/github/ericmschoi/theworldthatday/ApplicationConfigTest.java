package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Raw values in application.yml (plan M1a "설정" table, T2 (4)). Reads the file as written, before
 * placeholders are resolved, so a default added to a secret placeholder is caught.
 */
class ApplicationConfigTest {

  private static PropertySource<?> yml;

  @BeforeAll
  static void load() throws IOException {
    List<PropertySource<?>> sources =
        new YamlPropertySourceLoader()
            .load("application.yml", new ClassPathResource("application.yml"));
    assertThat(sources).hasSize(1);
    yml = sources.getFirst();
  }

  private static String value(String key) {
    Object raw = yml.getProperty(key);
    return raw == null ? null : raw.toString();
  }

  @Test
  void schemaIsManagedByFlywayOnly() {
    assertThat(value("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(value("spring.jpa.open-in-view")).isEqualTo("false");
  }

  @Test
  void databasePasswordHasNoDefault() {
    assertThat(value("spring.datasource.password")).isEqualTo("${POSTGRES_PASSWORD}");
  }

  @Test
  void datasourceUrlUsesComposeVariables() {
    assertThat(value("spring.datasource.url"))
        .isEqualTo(
            "jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:the_world_that_day}");
  }

  /** Plan M3a1c T1b: a cache read on a dead database fails well inside the day deadline. */
  @Test
  void connectionWaitIsShortAndValidationShorterStill() {
    assertThat(value("spring.datasource.hikari.connection-timeout")).isEqualTo("1000");
    assertThat(value("spring.datasource.hikari.validation-timeout")).isEqualTo("500");
  }

  @Test
  void rootEnvFileImportIsOptional() {
    assertThat(value("spring.config.import")).isEqualTo("optional:file:../.env[.properties]");
  }

  @Test
  void serverPortMatchesWebProxyContract() {
    assertThat(value("server.port")).isEqualTo("8080");
  }

  @Test
  void actuatorExposesOnlyHealthUnderApiWithoutDetails() {
    assertThat(value("management.endpoints.web.base-path")).isEqualTo("/api");
    assertThat(value("management.endpoints.web.exposure.include")).isEqualTo("health");
    assertThat(value("management.endpoint.health.show-details")).isEqualTo("never");
    assertThat(value("management.endpoints.web.discovery.enabled")).isEqualTo("false");
  }

  @Test
  void wikimediaSettingsMatchPlan() {
    // Empty default: no User-Agent means Wikimedia is off (plan M3a1, PM decision #25).
    assertThat(value("wikimedia.user-agent")).isEqualTo("${WIKIMEDIA_USER_AGENT:}");
    assertThat(value("wikimedia.connect-timeout")).isEqualTo("2s");
    assertThat(value("wikimedia.read-timeout")).isEqualTo("4s");
    assertThat(value("wikimedia.max-retries")).isEqualTo("1");
    assertThat(value("wikimedia.rate-limit-max-wait")).isEqualTo("120s");
    assertThat(value("wikimedia.kowiki-api-url")).isEqualTo("https://ko.wikipedia.org/w/api.php");
    assertThat(value("wikimedia.kowiki-day-deadline")).isEqualTo("5s");
    assertThat(value("wikimedia.dedup.min-link-overlap")).isEqualTo("0.6");
  }
}
