package io.github.ericmschoi.theworldthatday;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class HealthEndpointTest {

  @Autowired private RestTestClient client;

  @Test
  void healthReturnsUpWithoutComponents() {
    client
        .get()
        .uri("/api/health")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components")
        .doesNotExist()
        .jsonPath("$.details")
        .doesNotExist()
        .json("{\"status\":\"UP\"}", JsonCompareMode.STRICT);
  }

  @Test
  void apiRootDoesNotExposeActuatorDiscovery() {
    client.get().uri("/api").exchange().expectStatus().isNotFound();
  }

  @Test
  void otherActuatorEndpointsAreNotExposed() {
    client.get().uri("/api/info").exchange().expectStatus().isNotFound();
    client.get().uri("/actuator/health").exchange().expectStatus().isNotFound();
  }
}
