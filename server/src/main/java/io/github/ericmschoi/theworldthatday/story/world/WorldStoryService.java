package io.github.ericmschoi.theworldthatday.story.world;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventSource;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Scope;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.WorldMedia;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Event;
import io.github.ericmschoi.theworldthatday.story.world.WorldStoryResponse.Notice;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * The world section of a day (plan M3a2): every event {@link KoWikiEventSource} returns, as written
 * (M3-P16, nothing is filtered for quality), {@code normal} events first, then {@code tragic}, each
 * group in the source order. Only curated takedowns ({@code hide}) are left out; they do not make
 * the answer partial. 503 only when both pages are unavailable. Media is looked up after the tones:
 * day photos (plan M3a3b) never on a day with a shown {@code tragic} event or an unavailable page
 * (plan M3a3d decision 2 (b)), event photos (plan M3a3d) for shown {@code normal} events with a
 * title only; a failed lookup only makes the answer partial.
 */
@Service
public class WorldStoryService {

  private static final Logger log = LoggerFactory.getLogger(WorldStoryService.class);

  private final KoWikiEventSource source;
  private final EventToneJudge judge;
  private final MediaLookup media;

  public WorldStoryService(KoWikiEventSource source, EventToneJudge judge, MediaLookup media) {
    this.source = source;
    this.judge = judge;
    this.media = media;
  }

  public WorldStoryResponse find(LocalDate date) {
    KoWikiDayResult day = source.find(date);
    if (day.allUnavailable()) {
      log.warn("World section unavailable: source=kowiki states={}", states(day.docs()));
      throw new ApiProblem(
          HttpStatus.SERVICE_UNAVAILABLE,
          "UPSTREAM_UNAVAILABLE",
          "the event source is unavailable, try again later");
    }
    List<KoWikiEvent> found = day.events();
    Judgement judgement = judge.judge(found);
    List<Verdict> verdicts = judgement.verdicts();
    if (verdicts.size() != found.size()) {
      throw new IllegalStateException("one verdict per event is required");
    }
    List<Integer> normal = new ArrayList<>();
    List<Integer> tragic = new ArrayList<>();
    List<String> hidden = new ArrayList<>();
    List<EventTarget> targets = new ArrayList<>();
    for (int i = 0; i < found.size(); i++) {
      Verdict verdict = verdicts.get(i);
      KoWikiEvent event = found.get(i);
      if (verdict.hidden()) {
        hidden.add(hash(event));
      } else if (verdict.tone() != Tone.NORMAL) {
        tragic.add(i);
      } else {
        normal.add(i);
        if (!event.linkTitles().isEmpty()) {
          targets.add(
              new EventTarget(event.id(), event.linkTitles().getFirst(), place(verdict.scope())));
        }
      }
    }
    boolean noDayMedia =
        !tragic.isEmpty() || day.docs().stream().anyMatch(doc -> doc.state() == State.UNAVAILABLE);
    WorldMedia worldMedia = media.lookup(date, targets, noDayMedia);
    normal.addAll(tragic);
    List<Event> events = new ArrayList<>();
    for (int i : normal) {
      List<MediaItem> photos = worldMedia.eventMedia().getOrDefault(found.get(i).id(), List.of());
      events.add(Event.of(found.get(i), verdicts.get(i), photos));
    }
    if (!hidden.isEmpty()) {
      log.debug("World events hidden by curation: count={} ids={}", hidden.size(), hidden);
    }
    return new WorldStoryResponse(
        date.toString(),
        events,
        worldMedia.dayMedia(),
        events.isEmpty() ? Notice.NO_EVENTS_FOUND : null,
        day.degraded() || judgement.partial() || worldMedia.failed());
  }

  /** The photo place of an event: its scope as a {@code place_ref}. */
  private static String place(Scope scope) {
    return switch (scope) {
      case DOMESTIC -> "country:KR";
      case WORLD -> "world";
      case UNKNOWN -> "unknown";
    };
  }

  /** The hash part of the event ID: the full ID holds the date, which logs must not show. */
  private static String hash(KoWikiEvent event) {
    return event.id().substring(event.id().lastIndexOf('-') + 1);
  }

  private static String states(List<KoWikiDocStatus> docs) {
    return docs.stream().map(doc -> doc.state().name()).collect(Collectors.joining(","));
  }
}
