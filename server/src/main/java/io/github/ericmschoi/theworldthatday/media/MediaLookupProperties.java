package io.github.ericmschoi.theworldthatday.media;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Media lookup settings (plan M3a3b), next to {@link MediaProperties} under the same prefix: at
 * most {@code dayMediaLimit} day photos (0 turns the lookup off), and the whole lookup ends within
 * {@code lookupDeadline}. It is also the fixed read timeout of each media request, which is sent
 * once without a retry (plan M3a3c decision 1 (a)).
 */
@ConfigurationProperties("media")
public record MediaLookupProperties(
    @DefaultValue("6") int dayMediaLimit, @DefaultValue("3s") Duration lookupDeadline) {

  public MediaLookupProperties {
    if (dayMediaLimit < 0) {
      throw new IllegalArgumentException(
          "media.day-media-limit must not be negative, got: " + dayMediaLimit);
    }
    if (lookupDeadline == null || lookupDeadline.isNegative() || lookupDeadline.isZero()) {
      throw new IllegalArgumentException(
          "media.lookup-deadline must be positive, got: " + lookupDeadline);
    }
  }
}
