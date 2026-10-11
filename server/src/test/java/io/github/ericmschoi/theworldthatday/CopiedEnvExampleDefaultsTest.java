package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/**
 * A root .env copied unchanged from .env.example must not override the application.yml database
 * defaults with empty strings (chore-m1-followups goal 2). application.yml imports the root .env
 * with the {@code [.properties]} hint, so it is read here with the same properties loader and put
 * ahead of application.yml, as Spring Boot does for imported config data. Real environment
 * variables and system properties are left out so the result depends only on the two files.
 */
class CopiedEnvExampleDefaultsTest {

  private static ConfigurableEnvironment environmentWithRootEnv(String envFileContent)
      throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    MutablePropertySources sources = environment.getPropertySources();
    sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

    List<PropertySource<?>> env =
        new PropertiesPropertySourceLoader()
            .load(".env", new ByteArrayResource(envFileContent.getBytes(StandardCharsets.UTF_8)));
    List<PropertySource<?>> yml =
        new YamlPropertySourceLoader()
            .load("application.yml", new ClassPathResource("application.yml"));
    env.forEach(sources::addLast);
    yml.forEach(sources::addLast);
    return environment;
  }

  @Test
  void copiedEnvExampleKeepsDatabaseDefaults() throws IOException {
    ConfigurableEnvironment environment =
        environmentWithRootEnv(RepositoryFiles.read(".env.example"));

    assertThat(environment.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://localhost:5432/the_world_that_day");
    assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("postgres");
  }

  @Test
  void copiedEnvExampleDefinesNoOptionalDatabaseName() throws IOException {
    ConfigurableEnvironment environment =
        environmentWithRootEnv(RepositoryFiles.read(".env.example"));

    assertThat(environment.containsProperty("POSTGRES_DB")).isFalse();
    assertThat(environment.containsProperty("POSTGRES_USER")).isFalse();
    assertThat(environment.containsProperty("POSTGRES_PORT")).isFalse();
  }

  @Test
  void uncommentedEmptyOptionalNameWouldOverrideDefault() throws IOException {
    // Control case: shows the check above can fail. An empty value counts as set in Spring.
    ConfigurableEnvironment environment =
        environmentWithRootEnv("POSTGRES_DB=\nPOSTGRES_USER=\nPOSTGRES_PASSWORD=\n");

    assertThat(environment.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://localhost:5432/");
    assertThat(environment.getProperty("spring.datasource.username")).isEmpty();
  }
}
