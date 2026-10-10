package io.github.ericmschoi.theworldthatday;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Health contract with web/ when the database is unreachable: 503 and {"status":"DOWN"} with no
 * details. The extra property gives this class its own context and container, so stopping the
 * container does not affect other tests. DirtiesContext closes that context afterwards.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.hikari.connection-timeout=2000")
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class HealthEndpointDatabaseDownTest {

  @Autowired private RestTestClient client;
  @Autowired private PostgreSQLContainer postgres;

  @Test
  void healthReturns503DownWithoutDetailsWhenDatabaseStops() {
    client.get().uri("/api/health").exchange().expectStatus().isOk();

    postgres.stop();

    client
        .get()
        .uri("/api/health")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .json("{\"status\":\"DOWN\"}", JsonCompareMode.STRICT);
  }
}
