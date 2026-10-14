package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DisplayMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The blocklist (safeguard 5) and the ban on showing wire and news agency media directly (safeguard
 * 3) of plan M3a3a. Source adapters call it before {@link MediaNormalizer}. It never throws, and it
 * logs only the reason name: never a file name, author, credit or channel.
 */
@Component
public class MediaFilter {

  private static final Logger log = LoggerFactory.getLogger(MediaFilter.class);

  private static final Pattern HANGUL = Pattern.compile("\\p{IsHangul}");
  private static final String COMMONS = "commons";

  /** Why an item is left out. */
  public enum Reason {
    BLOCKED_ID,
    BLOCKED_AUTHOR,
    BLOCKED_CHANNEL,
    EXCLUDED_CREDIT;

    public String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private final List<BlockEntry> blocklist;
  private final List<Predicate<String>> excluded;

  public MediaFilter(MediaProperties properties) {
    this.blocklist = properties.blocklist();
    this.excluded = properties.excludedCredits().stream().map(MediaFilter::matcher).toList();
  }

  /**
   * Why {@code record} must be left out, or empty to keep it. A {@code null} record or origin
   * counts as one without fields.
   */
  public Optional<Reason> check(MediaRecord record, MediaOrigin origin) {
    MediaOrigin o = origin == null ? new MediaOrigin(null, null, null, null, null) : origin;
    Optional<Reason> reason = blocked(o).or(() -> excludedCredit(record, o));
    reason.ifPresent(r -> log.debug("Media filtered: reason={}", r.value()));
    return reason;
  }

  private Optional<Reason> blocked(MediaOrigin o) {
    if (o.source() == null) {
      return Optional.empty();
    }
    String source = o.source().strip().toLowerCase(Locale.ROOT);
    List<BlockEntry> entries = blocklist.stream().filter(e -> e.source().equals(source)).toList();
    if (entries.stream().anyMatch(e -> sameId(source, e.id(), o.itemId()))) {
      return Optional.of(Reason.BLOCKED_ID);
    }
    if (entries.stream()
        .anyMatch(e -> e.author() != null && e.author().equalsIgnoreCase(strip(o.author())))) {
      return Optional.of(Reason.BLOCKED_AUTHOR);
    }
    if (entries.stream()
        .anyMatch(e -> e.channel() != null && e.channel().equals(strip(o.channel())))) {
      return Optional.of(Reason.BLOCKED_CHANNEL);
    }
    return Optional.empty();
  }

  /**
   * IDs are equal as given; Commons file names also ignore a {@code File:} prefix and underscores.
   */
  private static boolean sameId(String source, String blocked, String itemId) {
    if (blocked == null || itemId == null) {
      return false;
    }
    return COMMONS.equals(source)
        ? commonsName(blocked).equals(commonsName(itemId))
        : blocked.equals(itemId.strip());
  }

  private static String commonsName(String name) {
    String plain = name.replace('_', ' ').strip().replaceAll(" +", " ");
    return plain.regionMatches(true, 0, "File:", 0, 5) ? plain.substring(5).strip() : plain;
  }

  /** Safeguard 3 applies only to media we show ourselves, never to a provider's own embed. */
  private Optional<Reason> excludedCredit(MediaRecord record, MediaOrigin o) {
    if (record == null
        || (record.displayMode() != DisplayMode.IMAGE
            && record.displayMode() != DisplayMode.VIDEO_FILE)) {
      return Optional.empty();
    }
    boolean hit =
        Stream.of(record.sourceName(), record.attribution(), o.author(), o.credit())
            .filter(Objects::nonNull)
            .anyMatch(text -> excluded.stream().anyMatch(p -> p.test(text)));
    return hit ? Optional.of(Reason.EXCLUDED_CREDIT) : Optional.empty();
  }

  /**
   * A Hangul entry matches anywhere ({@code ⓒ연합뉴스}). Any other entry matches as whole words,
   * ignoring case: no letter or digit right before or after it, so {@code AP} matches {@code AP
   * Photo} but not {@code Japan}. Hangul is a letter too, so {@code AP통신} is not matched (known
   * limitation, plan M3a3a D1 (b)).
   */
  private static Predicate<String> matcher(String entry) {
    String name = entry.strip();
    if (HANGUL.matcher(name).find()) {
      return text -> text.contains(name);
    }
    Pattern word =
        Pattern.compile(
            "(?<![\\p{L}\\p{N}])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    return text -> word.matcher(text).find();
  }

  private static String strip(String value) {
    return value == null ? null : value.strip();
  }
}
