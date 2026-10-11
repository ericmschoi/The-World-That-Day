package io.github.ericmschoi.theworldthatday;

import static io.github.ericmschoi.theworldthatday.RepositoryFiles.map;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Root docker-compose.yml and .env.example (plan M1a T3, T5 (3); chore-m1-followups T1). */
class LocalDatabaseComposeTest {

  private static Map<Object, Object> dbService() {
    Map<Object, Object> compose = RepositoryFiles.yaml("docker-compose.yml");
    return map(map(compose.get("services")).get("db"));
  }

  @Test
  void composeAndTestcontainersUseTheSamePostgresImage() {
    assertThat(dbService().get("image")).isEqualTo(TestcontainersConfiguration.POSTGRES_IMAGE);
    assertThat(TestcontainersConfiguration.POSTGRES_IMAGE).isEqualTo("postgres:17-alpine");
  }

  @Test
  void dbServiceHasPgIsReadyHealthcheck() {
    Object test = map(dbService().get("healthcheck")).get("test");

    assertThat(test).asString().contains("pg_isready");
  }

  @Test
  void passwordIsRequiredWithoutDefault() {
    Object password = map(dbService().get("environment")).get("POSTGRES_PASSWORD");

    assertThat(password).asString().startsWith("${POSTGRES_PASSWORD:?");
  }

  @Test
  void hostPortComesFromPostgresPort() {
    assertThat(dbService().get("ports")).isEqualTo(List.of("${POSTGRES_PORT:-5432}:5432"));
  }

  @Test
  void dataLivesInNamedVolume() {
    assertThat(dbService().get("volumes")).asString().contains("db-data:/var/lib/postgresql/data");
    assertThat(map(RepositoryFiles.yaml("docker-compose.yml").get("volumes")))
        .containsKey("db-data");
  }

  @Test
  void envExampleListsRequiredNamesUncommented() {
    List<String> lines = RepositoryFiles.read(".env.example").lines().toList();

    assertThat(lines).contains("POSTGRES_PASSWORD=", "WIKIMEDIA_USER_AGENT=");
  }

  @Test
  void envExampleListsOptionalNamesCommentedOut() {
    // An empty optional value would override the application.yml default (chore-m1-followups T1).
    List<String> lines = RepositoryFiles.read(".env.example").lines().toList();

    assertThat(lines).contains("# POSTGRES_DB=", "# POSTGRES_USER=", "# POSTGRES_PORT=");
    assertThat(lines).doesNotContain("POSTGRES_DB=", "POSTGRES_USER=", "POSTGRES_PORT=");
  }

  @Test
  void envExampleHasNoValues() {
    // Check C1: only comments, blank lines and "NAME=" lines.
    List<String> lines = RepositoryFiles.read(".env.example").lines().toList();

    assertThat(lines).allSatisfy(line -> assertThat(line).matches("^(#.*)?$|^[A-Z0-9_]+=$"));
  }

  @Test
  void envExampleHasNoBrowserKeys() {
    List<String> lines = RepositoryFiles.read(".env.example").lines().toList();

    assertThat(lines).noneMatch(line -> line.startsWith("VITE_"));
  }
}
