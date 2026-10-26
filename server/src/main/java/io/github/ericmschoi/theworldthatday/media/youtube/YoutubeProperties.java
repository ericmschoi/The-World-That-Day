package io.github.ericmschoi.theworldthatday.media.youtube;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * YouTube Data API settings (plan M3a5). {@code apiKey} is a secret: an empty key turns the YouTube
 * source off, and the value is never part of {@link #toString()} or of an error message. {@code
 * allowedChannels} are the only channels searched and shown (compared by channel ID). The two
 * budgets are the daily limits the app stays within: search calls (the API allows 100 a day) and
 * units for the other calls (10,000 a day), both about 80% of the API's limits. A value that breaks
 * a check fails startup with the setting's name in the message.
 */
@ConfigurationProperties("youtube")
public record YoutubeProperties(
    @DefaultValue("") String apiKey,
    @DefaultValue("https://www.googleapis.com/youtube/v3") URI apiUrl,
    @DefaultValue("UC8_LPVE4Yuc6KF0opF6uS_w") List<String> allowedChannels,
    @DefaultValue("80") int dailySearchBudget,
    @DefaultValue("8000") int dailyUnitBudget,
    @DefaultValue("25") int maxResults,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*오늘")
        List<String> retrospectiveTitlePatterns) {

  private static final Pattern CHANNEL_ID = Pattern.compile("UC[A-Za-z0-9_-]{22}");

  @ConstructorBinding
  public YoutubeProperties {
    apiKey = apiKey == null ? "" : apiKey.strip();
    if (apiUrl == null
        || !apiUrl.isAbsolute()
        || apiUrl.getHost() == null
        || !("http".equalsIgnoreCase(apiUrl.getScheme())
            || "https".equalsIgnoreCase(apiUrl.getScheme()))) {
      throw new IllegalArgumentException(
          "youtube.api-url must be an absolute http(s) URL, got: " + apiUrl);
    }
    allowedChannels = allowedChannels == null ? List.of() : List.copyOf(allowedChannels);
    if (allowedChannels.isEmpty()
        || !allowedChannels.stream().allMatch(id -> CHANNEL_ID.matcher(id).matches())) {
      throw new IllegalArgumentException(
          "youtube.allowed-channels needs at least one channel ID (UC and 22 more characters), got: "
              + allowedChannels);
    }
    requireRange("youtube.daily-search-budget", dailySearchBudget, 0, 100);
    requireRange("youtube.daily-unit-budget", dailyUnitBudget, 0, 10_000);
    requireRange("youtube.max-results", maxResults, 1, 50);
    if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
      throw new IllegalArgumentException(
          "youtube.connect-timeout must be positive, got: " + connectTimeout);
    }
    retrospectiveTitlePatterns =
        retrospectiveTitlePatterns == null ? List.of() : List.copyOf(retrospectiveTitlePatterns);
    for (String pattern : retrospectiveTitlePatterns) {
      try {
        Pattern.compile(pattern);
      } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException(
            "youtube.retrospective-title-patterns has an invalid regular expression: "
                + e.getDescription());
      }
    }
  }

  /** Without an API key the YouTube source stays off. */
  public boolean enabled() {
    return !apiKey.isEmpty();
  }

  private static void requireRange(String key, int value, int min, int max) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
          key + " must be between " + min + " and " + max + ", got: " + value);
    }
  }

  /** Everything but the key, which is only reported as set or empty. */
  @Override
  public String toString() {
    return "YoutubeProperties[apiKey="
        + (enabled() ? "<set>" : "<empty>")
        + ", apiUrl="
        + apiUrl
        + ", allowedChannels="
        + allowedChannels
        + ", dailySearchBudget="
        + dailySearchBudget
        + ", dailyUnitBudget="
        + dailyUnitBudget
        + ", maxResults="
        + maxResults
        + ", connectTimeout="
        + connectTimeout
        + ", retrospectiveTitlePatterns="
        + retrospectiveTitlePatterns
        + "]";
  }
}
