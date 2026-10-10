package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseSmokeTest {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private HealthEndpoint healthEndpoint;
  @Autowired private ApplicationContext context;

  @Test
  void flywayAppliesBaselineMigration() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL");

    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()).containsEntry("version", "1").containsEntry("success", true);
  }

  @Test
  void baselineCreatesNoApplicationTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String.class);

    assertThat(tables).containsExactly("flyway_schema_history");
  }

  @Test
  void databaseHealthIsUp() {
    HealthDescriptor db = healthEndpoint.healthForPath("db");

    assertThat(db).isNotNull();
    assertThat(db.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void runsAgainstPostgres17() {
    String version = jdbc.queryForObject("SHOW server_version", String.class);

    assertThat(version).startsWith("17.");
  }

  @Test
  void openInViewIsDisabled() {
    assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
  }
}
