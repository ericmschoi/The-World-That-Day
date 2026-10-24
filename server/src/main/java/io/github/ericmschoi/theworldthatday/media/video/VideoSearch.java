package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup.EventTarget;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.Selection;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.EventJudge;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Judgement;
import io.github.ericmschoi.theworldthatday.media.video.VideoAccuracy.Reject;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Interrupted;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Task;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The event video search of plan M3a4 ("고르기·개수·tragic·실패"): one task per {@link EventVideoSource}
 * on the media lookup's own {@link Deadline}, for the first {@code media.video.max-events} targets
 * (never a {@code tragic} event: the service does not make them targets). Each candidate is judged
 * by {@link VideoAccuracy}; per event the same video from two sources counts once (higher score,
 * then the basis order), the order is free license, official, other, then score, basis and ID, and
 * the first video not already chosen for an earlier event is kept. A source that fails, throws,
 * misses the deadline or whose candidates cannot be judged is left empty and makes the answer
 * partial; a skipped quota does not.
 */
@Component
public class VideoSearch {

  private static final Logger log = LoggerFactory.getLogger(VideoSearch.class);

  /** The basis order for ties, strongest first. */
  private static final List<Basis> BASIS_ORDER =
      List.of(
          Basis.DAEHAN_NEWS,
          Basis.YOUTUBE_OFFICIAL,
          Basis.COMMONS_P10,
          Basis.COMMONS_CATEGORY,
          Basis.COMMONS_DATE_CATEGORY,
          Basis.COMMONS_SEARCH);

  private static final Comparator<Scored> STRONGER =
      Comparator.comparingInt((Scored s) -> -s.score())
          .thenComparingInt(s -> BASIS_ORDER.indexOf(s.candidate().basis()));
  private static final Comparator<Scored> ORDER =
      Comparator.comparingInt(Scored::tier).thenComparing(STRONGER).thenComparing(Scored::id);

  private final List<EventVideoSource> sources;
  private final VideoAccuracy accuracy;
  private final VideoSearchProperties properties;

  @Autowired
  public VideoSearch(
      ObjectProvider<EventVideoSource> sources,
      VideoAccuracy accuracy,
      VideoSearchProperties properties) {
    this(sources.orderedStream().toList(), accuracy, properties);
  }

  public VideoSearch(
      List<EventVideoSource> sources, VideoAccuracy accuracy, VideoSearchProperties properties) {
    this.sources = List.copyOf(sources);
    this.accuracy = accuracy;
    this.properties = properties;
  }

  /** Starts one task per source on {@code deadline}; nothing when there is no event to search. */
  public Running start(
      Deadline deadline, LocalDate date, List<EventTarget> targets, boolean dayPathsOff) {
    List<EventContext> events =
        targets.stream()
            .limit(properties.maxEvents())
            .map(target -> EventContext.of(target, date))
            .toList();
    List<Started> started = new ArrayList<>();
    for (EventVideoSource source : events.isEmpty() ? List.<EventVideoSource>of() : sources) {
      Task<SourceResult> task =
          deadline.start(
              "video-" + source.key(),
              () -> Objects.requireNonNull(source.find(date, events, dayPathsOff), "result"));
      started.add(new Started(source.key(), task));
    }
    return new Running(events, started);
  }

  /**
   * The chosen video by event ID (0 or 1 each, with its selection), and whether a source failed.
   */
  public record Videos(Map<String, MediaItem> byEvent, boolean failed) {

    public Videos {
      byEvent = Map.copyOf(byEvent);
    }
  }

  /** The searches of one lookup. */
  public final class Running {

    private final List<EventContext> events;
    private final List<Started> started;

    private Running(List<EventContext> events, List<Started> started) {
      this.events = events;
      this.started = started;
    }

    /** Awaits every source (until the shared end at most), then judges and chooses. */
    public Videos await() {
      Map<String, List<Scored>> byEvent = new HashMap<>();
      Map<Reject, Integer> rejected = new EnumMap<>(Reject.class);
      boolean failed = false;
      for (Started source : started) {
        SourceResult result = result(source);
        Judged judged = result == null ? null : judged(source.key(), result);
        if (judged == null) {
          failed = true;
          continue;
        }
        judged
            .byEvent()
            .forEach(
                (id, scored) -> byEvent.computeIfAbsent(id, k -> new ArrayList<>()).addAll(scored));
        judged.rejected().forEach((reason, n) -> rejected.merge(reason, n, Integer::sum));
      }
      if (!rejected.isEmpty()) {
        log.debug("Video candidates rejected: {}", rejected); // reason counts only
      }
      Set<String> chosenIds = new HashSet<>();
      Map<String, MediaItem> chosen = new HashMap<>();
      for (EventContext event : events) {
        for (Scored video : ordered(byEvent.getOrDefault(event.eventId(), List.of()))) {
          if (chosenIds.add(video.id())) {
            Selection selection = new Selection(video.candidate().basis(), video.score());
            chosen.put(event.eventId(), video.candidate().item().withSelection(selection));
            break;
          }
        }
      }
      return new Videos(chosen, failed);
    }

    /**
     * The source's result, or {@code null} when it failed: a timeout is logged unless the source
     * ended {@code FAILED} (it logged), an exception is logged with its class, an interrupted
     * caller is not logged.
     */
    private SourceResult result(Started source) {
      return switch (source.task().await()) {
        case Done<SourceResult>(SourceResult value) ->
            value.status() == Status.FAILED ? null : value;
        case TimedOut<SourceResult>(boolean stopped, SourceResult late) -> {
          if (!stopped || late == null || late.status() != Status.FAILED) {
            warn(source.key(), "deadline");
          }
          yield null;
        }
        case Interrupted<SourceResult>() -> null;
        case Failed<SourceResult>(Throwable e) -> {
          warn(source.key(), e.getClass().getSimpleName());
          yield null;
        }
      };
    }

    /**
     * One source's accepted candidates by event and its rejection counts, or {@code null} when
     * judging them threw: that is logged with its class and only this source is left empty.
     */
    private Judged judged(String key, SourceResult result) {
      Map<String, List<Scored>> accepted = new HashMap<>();
      Map<Reject, Integer> rejected = new EnumMap<>(Reject.class);
      try {
        for (EventContext event : events) {
          List<Candidate> candidates = result.byEvent().getOrDefault(event.eventId(), List.of());
          if (candidates.isEmpty()) {
            continue;
          }
          String label = result.englishLabels().get(event.eventId());
          EventJudge judge =
              accuracy.forEvent(label == null ? event : event.withEnglishLabel(label));
          for (Candidate candidate : candidates) {
            Judgement judgement = judge.judge(candidate);
            if (judgement.accepted()) {
              accepted
                  .computeIfAbsent(event.eventId(), id -> new ArrayList<>())
                  .add(new Scored(candidate, judgement.score()));
            } else {
              rejected.merge(judgement.reject(), 1, Integer::sum);
            }
          }
        }
      } catch (RuntimeException e) {
        warn(key, e.getClass().getSimpleName());
        return null;
      }
      return new Judged(accepted, rejected);
    }

    private void warn(String key, String cause) {
      log.warn("Video search failed: source={} events={} cause={}", key, events.size(), cause);
    }
  }

  /** One per video ID (the stronger), in the order of safeguard 4. */
  private static List<Scored> ordered(List<Scored> videos) {
    List<Scored> strongest = new ArrayList<>(videos);
    strongest.sort(STRONGER);
    Map<String, Scored> once = new LinkedHashMap<>();
    strongest.forEach(video -> once.putIfAbsent(video.id(), video));
    return once.values().stream().sorted(ORDER).toList();
  }

  private record Started(String key, Task<SourceResult> task) {}

  private record Judged(Map<String, List<Scored>> byEvent, Map<Reject, Integer> rejected) {}

  private record Scored(Candidate candidate, int score) {

    String id() {
      return candidate.item().record().id();
    }

    /** Safeguard 4: free licenses, then official channels, then the rest. */
    int tier() {
      return candidate.item().record().licenseStatus() == LicenseStatus.FREE
          ? 0
          : candidate.official() ? 1 : 2;
    }
  }
}
