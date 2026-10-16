package io.github.ericmschoi.theworldthatday.story.world;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Body of {@code GET /api/story/world} (plan M3a2 response fields). JSON names are snake_case.
 * {@code dayMedia} holds the day's photos (M3a3b); every event {@code media} stays empty until
 * M3a3c (photos) and M3a4 (videos) fill it.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record WorldStoryResponse(
    String date, List<Event> events, List<MediaItem> dayMedia, Notice notice, boolean partial) {

  static final String WIKI_BASE = "https://ko.wikipedia.org/wiki/";

  public WorldStoryResponse {
    events = List.copyOf(events);
    dayMedia = List.copyOf(dayMedia);
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record Notice(String code) {

    static final Notice NO_EVENTS_FOUND = new Notice("NO_EVENTS_FOUND");
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record Link(String title, String url) {}

  /** One event; the text is {@code KoWikiEvent.text} unchanged (decision #23 (a), M3-P16). */
  @JsonNaming(SnakeCaseStrategy.class)
  public record Event(
      String id,
      String date,
      String scope,
      String title,
      String summary,
      String summaryLang,
      boolean summaryModified,
      String textLicense,
      String textLicenseUrl,
      String sourceName,
      String sourceTitle,
      String sourceUrl,
      String sourceSection,
      long sourceRevision,
      String wikidataQid,
      String tone,
      String toneBasis,
      List<Link> relatedLinks,
      List<MediaItem> media,
      String retrievedAt) {

    static Event of(KoWikiEvent event, Verdict verdict) {
      return new Event(
          event.id(),
          event.date().toString(),
          verdict.scope().value(),
          event.linkTitles().isEmpty() ? null : event.linkTitles().getFirst(),
          event.text(),
          "ko",
          false,
          "CC BY-SA 4.0",
          "https://creativecommons.org/licenses/by-sa/4.0/deed.ko",
          "한국어 위키백과",
          event.sourceTitle(),
          pageUrl(event.sourceTitle()),
          event.sourceSection(),
          event.revision(),
          event.wikidataQid(),
          verdict.tone().value(),
          verdict.basis().value(),
          event.linkTitles().stream().map(title -> new Link(title, pageUrl(title))).toList(),
          List.of(),
          seconds(event.retrievedAt()));
    }
  }

  /** Page address shown to users: spaces as {@code _}, UTF-8 percent-encoded. */
  static String pageUrl(String title) {
    return WIKI_BASE + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
  }

  /** ISO-8601 UTC, fractions of a second cut off. */
  static String seconds(Instant instant) {
    return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
  }
}
