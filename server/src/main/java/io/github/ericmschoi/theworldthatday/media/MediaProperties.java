package io.github.ericmschoi.theworldthatday.media;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Media settings (plan M3a3). The allowed embed providers are the only providers whose {@code
 * image_embed} (R3) and {@code video_embed} (R5) items are returned. Both lists start empty:
 * provider image embeds are out of M3, and M3a5 adds {@code youtube} for videos. An entry that is
 * not a lowercase provider ID fails startup.
 */
@ConfigurationProperties("media")
public record MediaProperties(
    @DefaultValue List<String> allowedImageEmbedProviders,
    @DefaultValue List<String> allowedVideoEmbedProviders) {

  private static final Pattern PROVIDER = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

  public MediaProperties {
    allowedImageEmbedProviders =
        providers("media.allowed-image-embed-providers", allowedImageEmbedProviders);
    allowedVideoEmbedProviders =
        providers("media.allowed-video-embed-providers", allowedVideoEmbedProviders);
  }

  private static List<String> providers(String key, List<String> values) {
    List<String> copy = values == null ? List.of() : List.copyOf(values);
    if (!copy.stream().allMatch(value -> PROVIDER.matcher(value).matches())) {
      throw new IllegalArgumentException(
          key + " entries must be lowercase provider IDs such as youtube, got: " + copy);
    }
    return copy;
  }
}
