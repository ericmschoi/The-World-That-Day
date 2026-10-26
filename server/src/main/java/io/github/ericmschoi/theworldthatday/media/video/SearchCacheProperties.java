package io.github.ericmschoi.theworldthatday.media.video;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Video search result cache settings (plan M3a4c): how long a source's stored result is used, by
 * source key ({@code media.search-cache.ttl.commons: 7d}). A row is fresh while {@code fetched_at +
 * ttl} is after now. A source with {@code 0} or no entry is not cached; a negative or empty value
 * fails startup with the setting's name in the message. YouTube's terms allow API data to be kept
 * at most 30 days (plan M3a5), so {@code youtube} above 30 days fails startup too.
 */
@ConfigurationProperties("media.search-cache")
public record SearchCacheProperties(@DefaultValue Map<String, Duration> ttl) {

  private static final Duration YOUTUBE_MAX_TTL = Duration.ofDays(30);

  @ConstructorBinding
  public SearchCacheProperties {
    ttl = ttl == null ? Map.of() : Map.copyOf(checked(ttl));
  }

  /** How long the source's results are kept, or empty when the source is not cached. */
  public Optional<Duration> ttl(String source) {
    return Optional.ofNullable(ttl.get(source)).filter(value -> !value.isZero());
  }

  private static Map<String, Duration> checked(Map<String, Duration> values) {
    values.forEach(
        (source, value) -> {
          if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(
                "media.search-cache.ttl." + source + " must be zero or positive, got: " + value);
          }
          if ("youtube".equals(source) && value.compareTo(YOUTUBE_MAX_TTL) > 0) {
            throw new IllegalArgumentException(
                "media.search-cache.ttl.youtube must be at most 30 days, got: " + value);
          }
        });
    return values;
  }
}
