package io.github.ericmschoi.theworldthatday.wikimedia;

import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Ok;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Reason;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Unavailable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

/**
 * The only way out to Wikimedia. Sends the configured User-Agent, applies the timeouts, retries a
 * GET once on timeouts and 502/503/504, and honours 429 Retry-After per host. Never throws for
 * remote failures; logs one WARN line with the source, the page title and the status or exception
 * type (never the URL or the body).
 */
@Component
public class WikimediaHttpClient {

  private static final Logger log = LoggerFactory.getLogger(WikimediaHttpClient.class);
  private static final Set<Integer> RETRYABLE = Set.of(502, 503, 504);

  private record Reply(int status, String retryAfter, String body) {}

  private final WikimediaProperties properties;
  private final HostRateLimiter rateLimiter;
  private final RestClient restClient;

  public WikimediaHttpClient(WikimediaProperties properties, Clock clock) {
    this.properties = properties;
    this.rateLimiter = new HostRateLimiter(clock, properties.rateLimitMaxWait());
    HttpClient http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(properties.readTimeout());
    this.restClient =
        RestClient.builder()
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
            .build();
    if (!properties.enabled()) {
      log.warn("WIKIMEDIA_USER_AGENT is empty: Wikimedia sources are disabled");
    }
  }

  /** GETs {@code uri}. {@code source} and {@code title} are only used in log lines. */
  public WikimediaResponse get(String source, String title, URI uri) {
    if (!properties.enabled()) {
      return new Unavailable(Reason.DISABLED);
    }
    String host = uri.getAuthority();
    if (rateLimiter.isBlocked(host)) {
      return fail(source, title, "skipped, rate limited", Reason.RATE_LIMITED);
    }
    for (int attempt = 0; ; attempt++) {
      boolean canRetry = attempt < properties.maxRetries();
      try {
        Reply reply = restClient.get().uri(uri).exchangeForRequiredValue(this::read);
        if (reply.status() >= 200 && reply.status() < 300) {
          return new Ok(reply.body());
        }
        if (reply.status() == 429) {
          rateLimiter.block(host, reply.retryAfter());
          return fail(source, title, "status 429", Reason.RATE_LIMITED);
        }
        if (!canRetry || !RETRYABLE.contains(reply.status())) {
          return fail(source, title, "status " + reply.status(), Reason.HTTP_ERROR);
        }
      } catch (RestClientException e) {
        boolean timeout = isTimeout(e);
        if (!canRetry || !timeout) {
          String cause = NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName();
          return fail(source, title, cause, timeout ? Reason.TIMEOUT : Reason.IO_ERROR);
        }
      }
    }
  }

  private Reply read(HttpRequest request, ConvertibleClientHttpResponse response)
      throws IOException {
    int status = response.getStatusCode().value();
    String body =
        status >= 200 && status < 300
            ? StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)
            : null;
    return new Reply(status, response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER), body);
  }

  private static boolean isTimeout(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }

  private static Unavailable fail(String source, String title, String cause, Reason reason) {
    log.warn("Wikimedia call failed: source={} title={} cause={}", source, title, cause);
    return new Unavailable(reason);
  }
}
