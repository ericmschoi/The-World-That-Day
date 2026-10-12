package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * A misconfigured Wikimedia base URL or request limit fails at startup, not on the first request.
 */
class WikimediaPropertiesTest {

  private static WikimediaProperties withUrl(URI url) {
    return new WikimediaProperties(
        "",
        Duration.ofSeconds(2),
        Duration.ofSeconds(4),
        1,
        Duration.ofSeconds(120),
        3,
        Duration.ofDays(7),
        url);
  }

  @ParameterizedTest(name = "[{0}] is accepted")
  @ValueSource(
      strings = {
        "https://ko.wikipedia.org/w/api.php",
        "http://localhost:8089/w/api.php",
        "HTTPS://ko.wikipedia.org/w/api.php"
      })
  void absoluteHttpUrlsAreAccepted(String url) {
    assertThat(withUrl(URI.create(url)).kowikiApiUrl()).isEqualTo(URI.create(url));
  }

  @ParameterizedTest(name = "[{0}] is rejected")
  @ValueSource(
      strings = {
        "/w/api.php", // relative
        "ko.wikipedia.org/w/api.php", // no scheme
        "ftp://ko.wikipedia.org/w/api.php", // not http(s)
        "http:///w/api.php", // no host
        "mailto:someone@example.test" // opaque
      })
  void nonAbsoluteOrNonHttpUrlsAreRejected(String url) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> withUrl(URI.create(url)))
        .withMessageContaining("wikimedia.kowiki-api-url");
  }

  @Test
  void missingUrlIsRejected() {
    assertThatIllegalArgumentException().isThrownBy(() -> withUrl(null));
  }

  @ParameterizedTest(name = "max-concurrent-requests {0} is rejected")
  @ValueSource(ints = {0, -1})
  void lessThanOneConcurrentRequestIsRejected(int maxConcurrent) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new WikimediaProperties(
                    "",
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(4),
                    1,
                    Duration.ofSeconds(120),
                    maxConcurrent,
                    Duration.ofDays(7),
                    URI.create("https://ko.wikipedia.org/w/api.php")))
        .withMessageContaining("wikimedia.max-concurrent-requests");
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(WikimediaProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  @Test
  void relativeUrlInConfigurationFailsStartup() {
    runner
        .withPropertyValues("wikimedia.kowiki-api-url=/w/api.php")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("wikimedia.kowiki-api-url"));
  }

  @Test
  void absoluteUrlInConfigurationStarts() {
    runner
        .withPropertyValues("wikimedia.kowiki-api-url=https://ko.wikipedia.org/w/api.php")
        .run(
            context ->
                assertThat(context.getBean(WikimediaProperties.class).kowikiApiUrl())
                    .isEqualTo(URI.create("https://ko.wikipedia.org/w/api.php")));
  }

  @Test
  void concurrentRequestsDefaultToThreeAndCanBeSet() {
    String url = "wikimedia.kowiki-api-url=https://ko.wikipedia.org/w/api.php";
    runner
        .withPropertyValues(url)
        .run(
            context ->
                assertThat(context.getBean(WikimediaProperties.class).maxConcurrentRequests())
                    .isEqualTo(3));
    runner
        .withPropertyValues(url, "wikimedia.max-concurrent-requests=1")
        .run(
            context ->
                assertThat(context.getBean(WikimediaProperties.class).maxConcurrentRequests())
                    .isEqualTo(1));
  }

  @Test
  void zeroConcurrentRequestsInConfigurationFailsStartup() {
    runner
        .withPropertyValues(
            "wikimedia.kowiki-api-url=https://ko.wikipedia.org/w/api.php",
            "wikimedia.max-concurrent-requests=0")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("wikimedia.max-concurrent-requests"));
  }

  @ParameterizedTest(name = "cache-ttl {0} is rejected")
  @ValueSource(strings = {"PT0S", "-PT1S"})
  void nonPositiveCacheTtlIsRejected(String ttl) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new WikimediaProperties(
                    "",
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(4),
                    1,
                    Duration.ofSeconds(120),
                    3,
                    Duration.parse(ttl),
                    URI.create("https://ko.wikipedia.org/w/api.php")))
        .withMessageContaining("wikimedia.cache-ttl");
  }

  @Test
  void cacheTtlDefaultsToSevenDaysAndReadsDayUnits() {
    String url = "wikimedia.kowiki-api-url=https://ko.wikipedia.org/w/api.php";
    runner
        .withPropertyValues(url)
        .run(
            context ->
                assertThat(context.getBean(WikimediaProperties.class).cacheTtl())
                    .isEqualTo(Duration.ofDays(7)));
    runner
        .withPropertyValues(url, "wikimedia.cache-ttl=1d")
        .run(
            context ->
                assertThat(context.getBean(WikimediaProperties.class).cacheTtl())
                    .isEqualTo(Duration.ofDays(1)));
  }
}
