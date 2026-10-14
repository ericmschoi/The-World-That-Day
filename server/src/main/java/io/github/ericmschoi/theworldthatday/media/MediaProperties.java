package io.github.ericmschoi.theworldthatday.media;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Media settings (plans M3a3 and M3a3a). The allowed embed providers are the only providers whose
 * {@code image_embed} (R3) and {@code video_embed} (R5) items are returned. Both lists start empty:
 * provider image embeds are out of M3, and M3a5 adds {@code youtube} for videos. {@code blocklist}
 * takes single items down at a rights holder's request (safeguard 5) and {@code excludedCredits}
 * names wire and news agencies that are never shown directly (safeguard 3); {@link MediaFilter}
 * applies both. A malformed entry fails startup with the setting's name in the message.
 */
@ConfigurationProperties("media")
public record MediaProperties(
    @DefaultValue List<String> allowedImageEmbedProviders,
    @DefaultValue List<String> allowedVideoEmbedProviders,
    @DefaultValue List<BlockEntry> blocklist,
    @DefaultValue List<String> excludedCredits) {

  private static final Pattern PROVIDER = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

  @ConstructorBinding
  public MediaProperties {
    allowedImageEmbedProviders =
        providers("media.allowed-image-embed-providers", allowedImageEmbedProviders);
    allowedVideoEmbedProviders =
        providers("media.allowed-video-embed-providers", allowedVideoEmbedProviders);
    blocklist = blocklist == null ? List.of() : List.copyOf(blocklist);
    for (int i = 0; i < blocklist.size(); i++) {
      check(i, blocklist.get(i));
    }
    excludedCredits = excludedCredits == null ? List.of() : List.copyOf(excludedCredits);
    if (excludedCredits.stream().anyMatch(MediaText::isBlank)) {
      throw new IllegalArgumentException("media.excluded-credits entries must not be empty");
    }
  }

  /** Only the embed providers, with an empty blocklist and no excluded credits. */
  public MediaProperties(
      List<String> allowedImageEmbedProviders, List<String> allowedVideoEmbedProviders) {
    this(allowedImageEmbedProviders, allowedVideoEmbedProviders, List.of(), List.of());
  }

  /**
   * One blocklist entry: a lowercase source key ({@code commons}, {@code youtube}, ...) and at
   * least one of {@code id} (Commons file name without {@code File:}, video ID or asset ID), {@code
   * author} (plain Commons {@code Artist}) or {@code channel} (channel ID). Blank values ({@link
   * MediaText#isBlank}) count as absent.
   */
  public record BlockEntry(String source, String id, String author, String channel) {

    public BlockEntry {
      id = present(id);
      author = present(author);
      channel = present(channel);
    }

    private static String present(String value) {
      return MediaText.isBlank(value) ? null : value.strip();
    }
  }

  private static void check(int index, BlockEntry entry) {
    String key = "media.blocklist[" + index + "]";
    if (entry == null || entry.source() == null || !PROVIDER.matcher(entry.source()).matches()) {
      throw new IllegalArgumentException(
          key + ".source must be a lowercase source key such as commons");
    }
    if (entry.id() == null && entry.author() == null && entry.channel() == null) {
      throw new IllegalArgumentException(key + " needs an id, an author or a channel");
    }
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
