package io.github.ericmschoi.theworldthatday.event.kowiki;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Event deduplication settings (plan M3a1c, rule D3). */
@ConfigurationProperties("wikimedia.dedup")
public record KoWikiDedupProperties(@DefaultValue("0.6") double minLinkOverlap) {

  public KoWikiDedupProperties {
    if (!(minLinkOverlap > 0 && minLinkOverlap <= 1)) {
      throw new IllegalArgumentException(
          "wikimedia.dedup.min-link-overlap must be in (0, 1], got: " + minLinkOverlap);
    }
  }
}
