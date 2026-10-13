package io.github.ericmschoi.theworldthatday.story.world;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDayResult;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiDocStatus;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEventSource;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Judgement;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Tone;
import io.github.ericmschoi.theworldthatday.event.tone.EventToneJudge.Verdict;
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
 * the answer partial. 503 only when both pages are unavailable.
 */
@Service
public class WorldStoryService {

  private static final Logger log = LoggerFactory.getLogger(WorldStoryService.class);

  private final KoWikiEventSource source;
  private final EventToneJudge judge;

  public WorldStoryService(KoWikiEventSource source, EventToneJudge judge) {
    this.source = source;
    this.judge = judge;
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
    List<Event> normal = new ArrayList<>();
    List<Event> tragic = new ArrayList<>();
    List<String> hidden = new ArrayList<>();
    for (int i = 0; i < found.size(); i++) {
      Verdict verdict = verdicts.get(i);
      if (verdict.hidden()) {
        hidden.add(hash(found.get(i)));
        continue;
      }
      (verdict.tone() == Tone.NORMAL ? normal : tragic).add(Event.of(found.get(i), verdict));
    }
    normal.addAll(tragic);
    if (!hidden.isEmpty()) {
      log.debug("World events hidden by curation: count={} ids={}", hidden.size(), hidden);
    }
    return new WorldStoryResponse(
        date.toString(),
        normal,
        List.of(),
        normal.isEmpty() ? Notice.NO_EVENTS_FOUND : null,
        day.degraded() || judgement.partial());
  }

  /** The hash part of the event ID: the full ID holds the date, which logs must not show. */
  private static String hash(KoWikiEvent event) {
    return event.id().substring(event.id().lastIndexOf('-') + 1);
  }

  private static String states(List<KoWikiDocStatus> docs) {
    return docs.stream().map(doc -> doc.state().name()).collect(Collectors.joining(","));
  }
}
