package io.github.ericmschoi.theworldthatday.wikimedia;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Wikidata Query Service settings (plan M3a2), next to the shared {@link WikimediaProperties}.
 * {@code wikidataReadTimeout} bounds each attempt and the whole lookup of one request. {@code
 * wikidataTragicClasses} are the classes whose subclasses count as tragic (rule {@code
 * rule:class}).
 */
@ConfigurationProperties("wikimedia")
public record WikidataProperties(
    @DefaultValue("https://query.wikidata.org/sparql") URI wikidataSparqlUrl,
    @DefaultValue("5s") Duration wikidataReadTimeout,
    @DefaultValue({"Q3839081", "Q171558", "Q2223653", "Q350604"})
        List<String> wikidataTragicClasses) {

  /** A Wikidata item ID; only these are ever written into a query. */
  public static final Pattern QID = Pattern.compile("Q[1-9][0-9]*");

  public WikidataProperties {
    boolean validUrl =
        wikidataSparqlUrl != null
            && wikidataSparqlUrl.isAbsolute()
            && wikidataSparqlUrl.getHost() != null
            && ("http".equalsIgnoreCase(wikidataSparqlUrl.getScheme())
                || "https".equalsIgnoreCase(wikidataSparqlUrl.getScheme()));
    if (!validUrl) {
      throw new IllegalArgumentException(
          "wikimedia.wikidata-sparql-url must be an absolute http(s) URL, got: "
              + wikidataSparqlUrl);
    }
    if (wikidataReadTimeout == null
        || wikidataReadTimeout.isNegative()
        || wikidataReadTimeout.isZero()) {
      throw new IllegalArgumentException(
          "wikimedia.wikidata-read-timeout must be positive, got: " + wikidataReadTimeout);
    }
    wikidataTragicClasses = List.copyOf(wikidataTragicClasses);
    if (wikidataTragicClasses.isEmpty()
        || !wikidataTragicClasses.stream().allMatch(q -> QID.matcher(q).matches())) {
      throw new IllegalArgumentException(
          "wikimedia.wikidata-tragic-classes must be Wikidata item IDs, got: "
              + wikidataTragicClasses);
    }
  }
}
