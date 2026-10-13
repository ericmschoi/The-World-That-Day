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
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
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
 * GET once on timeouts and 502/503/504, and honours Retry-After per host on a 429 and on a 503 (an
 * overload signal per the Wikimedia rate-limit guidance; such a 503 is not retried). At most {@code
 * wikimedia.max-concurrent-requests} requests are in flight across all Wikimedia hosts; each
 * attempt waits up to the read timeout for a slot. Never throws for remote failures; logs one WARN
 * line with the source, the page title and the status or exception type (never the URL or the
 * body).
 */
@Component
public class WikimediaHttpClient {

  private static final Logger log = LoggerFactory.getLogger(WikimediaHttpClient.class);
  private static final Set<Integer> RETRYABLE = Set.of(502, 503, 504);

  private record Reply(int status, String retryAfter, String body) {
    boolean hasRetryAfter() {
      return retryAfter != null && !retryAfter.isBlank();
    }
  }

  private final WikimediaProperties properties;
  private final HostRateLimiter rateLimiter;
  private final HttpClient http;

  /** One client per read timeout ({@code read-timeout}, or a source's own such as Wikidata's). */
  private final Map<Duration, RestClient> restClients = new ConcurrentHashMap<>();

  private final Semaphore permits;
  private final Map<String, LongAdder> timeouts = new ConcurrentHashMap<>();

  public WikimediaHttpClient(WikimediaProperties properties, Clock clock) {
    this.properties = properties;
    this.permits = new Semaphore(properties.maxConcurrentRequests(), true);
    this.rateLimiter = new HostRateLimiter(clock, properties.rateLimitMaxWait());
    this.http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    if (!properties.enabled()) {
      log.warn("WIKIMEDIA_USER_AGENT is empty: Wikimedia sources are disabled");
    }
  }

  /** GETs {@code uri}. {@code source} and {@code title} are only used in log lines. */
  public WikimediaResponse get(String source, String title, URI uri) {
    return get(source, title, uri, properties.readTimeout());
  }

  /** As {@link #get(String, String, URI)}, with {@code readTimeout} for each attempt. */
  public WikimediaResponse get(String source, String title, URI uri, Duration readTimeout) {
    if (!properties.enabled()) {
      return new Unavailable(Reason.DISABLED);
    }
    RestClient restClient = restClients.computeIfAbsent(readTimeout, this::restClient);
    String host = host(uri);
    if (host == null) {
      return fail(source, title, "no host in URI", Reason.INVALID_URI);
    }
    for (int attempt = 0; ; attempt++) {
      boolean canRetry = attempt < properties.maxRetries();
      if (rateLimiter.isBlocked(host)) {
        return fail(source, title, "skipped, rate limited", Reason.RATE_LIMITED);
      }
      if (!acquirePermit()) {
        return Thread.currentThread().isInterrupted()
            ? fail(
                source, title, "interrupted while waiting for a request slot", Reason.INTERRUPTED)
            : fail(source, title, "no free request slot", Reason.BUSY);
      }
      try {
        if (rateLimiter.isBlocked(host)) { // blocked by another request while this one waited
          return fail(source, title, "skipped, rate limited", Reason.RATE_LIMITED);
        }
        Reply reply = restClient.get().uri(uri).exchangeForRequiredValue(this::read);
        if (reply.status() >= 200 && reply.status() < 300) {
          return new Ok(reply.body());
        }
        if (reply.status() == 429 || (reply.status() == 503 && reply.hasRetryAfter())) {
          rateLimiter.block(host, reply.retryAfter());
          return fail(source, title, "status " + reply.status(), Reason.RATE_LIMITED);
        }
        if (!canRetry || !RETRYABLE.contains(reply.status())) {
          return fail(source, title, "status " + reply.status(), Reason.HTTP_ERROR);
        }
      } catch (RestClientException e) {
        boolean timeout = isTimeout(e);
        if (timeout) {
          timeouts.computeIfAbsent(host, key -> new LongAdder()).increment();
        }
        if (!canRetry || !timeout) {
          String cause = NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName();
          return fail(source, title, cause, timeout ? Reason.TIMEOUT : Reason.IO_ERROR);
        }
      } finally {
        permits.release();
      }
    }
  }

  /**
   * Blocks the host of {@code uri} as a 429 without Retry-After does (30 seconds). For rate limits
   * that arrive inside a 2xx body, such as the Action API error {@code ratelimited}.
   */
  public void blockHost(URI uri) {
    String host = host(uri);
    if (host != null) {
      rateLimiter.block(host, null);
    }
  }

  /**
   * Attempts to the host of {@code uri} that hit the connect or read timeout so far. A caller
   * compares a reading before and after its own requests to tell whether the host stalled; other
   * callers' timeouts to the same host count too, which also means the host is stalling.
   */
  public long timeouts(URI uri) {
    String host = host(uri);
    LongAdder count = host == null ? null : timeouts.get(host);
    return count == null ? 0 : count.sum();
  }

  /** The shared request slots; tests use it to occupy every slot. */
  Semaphore permits() {
    return permits;
  }

  private RestClient restClient(Duration readTimeout) {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(readTimeout);
    return RestClient.builder()
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
        .build();
  }

  /** False on timeout, or on interrupt with the interrupt flag set again. */
  private boolean acquirePermit() {
    try {
      return permits.tryAcquire(properties.readTimeout().toNanos(), TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String host(URI uri) {
    return uri == null || uri.getHost() == null ? null : uri.getAuthority();
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
