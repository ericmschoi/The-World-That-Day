package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Defaults in the root docker-compose.yml and application.yml must match, so the server reaches the
 * Compose database when .env leaves the optional names unset (chore-m1-followups T1 (6)).
 */
class ComposeApplicationDefaultsTest {

  private static final String COMPOSE = "docker-compose.yml";
  private static final String APPLICATION = "server/src/main/resources/application.yml";

  @ParameterizedTest
  @CsvSource({
    "POSTGRES_DB, the_world_that_day",
    "POSTGRES_USER, postgres",
    "POSTGRES_PORT, 5432",
  })
  void composeAndApplicationShareDefault(String name, String expected) {
    String compose = RepositoryFiles.read(COMPOSE);
    String application = RepositoryFiles.read(APPLICATION);

    assertThat(PlaceholderDefaults.sameDefault(compose, application, name)).isEqualTo(expected);
  }
}
