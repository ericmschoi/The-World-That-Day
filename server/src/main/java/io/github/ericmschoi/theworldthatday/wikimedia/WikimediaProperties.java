package io.github.ericmschoi.theworldthatday.wikimedia;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Shared settings for every Wikimedia call (plan M3a1, "외부 호출 공통 규칙"). */
@ConfigurationProperties("wikimedia")
public record WikimediaProperties(
    String userAgent,
    Duration connectTimeout,
    Duration readTimeout,
    int maxRetries,
    Duration rateLimitMaxWait,
    URI kowikiApiUrl) {

  public WikimediaProperties {
    userAgent = userAgent == null ? "" : userAgent.strip();
  }

  /** Without a User-Agent that names a contact, Wikimedia sources stay off. */
  public boolean enabled() {
    return !userAgent.isEmpty();
  }
}
