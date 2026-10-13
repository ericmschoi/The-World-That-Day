package io.github.ericmschoi.theworldthatday.wikimedia;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Shared settings for every Wikimedia call (plan M3a1, "외부 호출 공통 규칙"). */
@ConfigurationProperties("wikimedia")
public record WikimediaProperties(
    String userAgent,
    Duration connectTimeout,
    Duration readTimeout,
    int maxRetries,
    Duration rateLimitMaxWait,
    @DefaultValue("3") int maxConcurrentRequests,
    @DefaultValue("7d") Duration cacheTtl,
    @DefaultValue("5s") Duration kowikiDayDeadline,
    URI kowikiApiUrl) {

  public WikimediaProperties {
    userAgent = userAgent == null ? "" : userAgent.strip();
    if (maxConcurrentRequests < 1) {
      throw new IllegalArgumentException("wikimedia.max-concurrent-requests must be at least 1");
    }
    if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
      throw new IllegalArgumentException("wikimedia.cache-ttl must be positive, got: " + cacheTtl);
    }
    if (kowikiDayDeadline == null || kowikiDayDeadline.isNegative() || kowikiDayDeadline.isZero()) {
      throw new IllegalArgumentException(
          "wikimedia.kowiki-day-deadline must be positive, got: " + kowikiDayDeadline);
    }
    requireAbsoluteHttpUrl("wikimedia.kowiki-api-url", kowikiApiUrl);
  }

  /** Fails binding, and so startup, when a base URL is missing, relative or not http(s). */
  private static void requireAbsoluteHttpUrl(String key, URI url) {
    boolean valid =
        url != null
            && url.isAbsolute()
            && url.getHost() != null
            && ("http".equalsIgnoreCase(url.getScheme())
                || "https".equalsIgnoreCase(url.getScheme()));
    if (!valid) {
      throw new IllegalArgumentException(key + " must be an absolute http(s) URL, got: " + url);
    }
  }

  /** Without a User-Agent that names a contact, Wikimedia sources stay off. */
  public boolean enabled() {
    return !userAgent.isEmpty();
  }
}
