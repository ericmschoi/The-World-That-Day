package io.github.ericmschoi.theworldthatday.wikimedia;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Wikimedia Commons Action API address (plan M3a3b), next to the shared {@link
 * WikimediaProperties}. A relative or non-http(s) address fails startup.
 */
@ConfigurationProperties("wikimedia")
public record CommonsProperties(
    @DefaultValue("https://commons.wikimedia.org/w/api.php") URI commonsApiUrl) {

  public CommonsProperties {
    boolean validUrl =
        commonsApiUrl != null
            && commonsApiUrl.isAbsolute()
            && commonsApiUrl.getHost() != null
            && ("http".equalsIgnoreCase(commonsApiUrl.getScheme())
                || "https".equalsIgnoreCase(commonsApiUrl.getScheme()));
    if (!validUrl) {
      throw new IllegalArgumentException(
          "wikimedia.commons-api-url must be an absolute http(s) URL, got: " + commonsApiUrl);
    }
  }
}
