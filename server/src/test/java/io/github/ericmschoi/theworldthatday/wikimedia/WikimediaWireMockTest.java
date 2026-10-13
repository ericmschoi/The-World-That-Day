package io.github.ericmschoi.theworldthatday.wikimedia;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.TestcontainersConfiguration;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for Spring context tests that talk to Wikimedia (plan M3a1 test policy 1). Every {@code
 * wikimedia.*-url} points at one WireMock server; M3a2/M3a3 add their URLs to {@link
 * #wikimediaUrls}. The server lives for the whole JVM so the cached Spring context keeps a valid
 * port across test classes.
 *
 * <p>Before each test WireMock is reset and the clock moves past {@code rate-limit-max-wait}, so a
 * 429 window from an earlier test never leaks. Time only moves forward: use {@link #clock} to
 * advance it.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, WikimediaWireMockTest.TestClock.class})
public abstract class WikimediaWireMockTest {

  public static final String TEST_USER_AGENT =
      "TheWorldThatDay-test/0 (https://github.com/ericmschoi/The-World-That-Day)";
  public static final String KOWIKI_PATH = "/w/api.php";
  public static final String SPARQL_PATH = "/sparql";

  protected static final WireMockServer wikimedia = start();

  @Autowired protected MutableClock clock;
  @Autowired protected WikimediaProperties properties;

  private static WireMockServer start() {
    WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    return server;
  }

  @DynamicPropertySource
  static void wikimediaUrls(DynamicPropertyRegistry registry) {
    registry.add("wikimedia.user-agent", () -> TEST_USER_AGENT);
    // 2 s, not 300 ms: context tests count requests, and a timed-out first attempt on a stalled
    // machine adds a retry. Timeout behaviour is tested with short timeouts in the unit tests.
    registry.add("wikimedia.read-timeout", () -> "2s");
    registry.add("wikimedia.kowiki-api-url", () -> wikimedia.baseUrl() + KOWIKI_PATH);
    registry.add("wikimedia.wikidata-sparql-url", () -> wikimedia.baseUrl() + SPARQL_PATH);
  }

  @BeforeEach
  void resetWikimedia() {
    wikimedia.resetAll();
    clock.advance(properties.rateLimitMaxWait().plusSeconds(1));
  }

  protected static int requestCount() {
    return wikimedia.getAllServeEvents().size();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestClock {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ClockConfig.SERVICE_ZONE);
    }
  }
}
