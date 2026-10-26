package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.VideoResult;
import io.github.ericmschoi.theworldthatday.media.commons.WikidataVideoLinks.Links;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Task;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * The Commons video source of plan M3a4b (research 7: (a-3) {@code P10}, (a-1) the event's own
 * category, (c-1) {@code Videos taken on} the date). Two waits: the Wikidata links with the date
 * category, then the {@code P10} file batch with one search per category. Each request is sent once
 * with the read timeout {@code media.lookup-deadline}; the inner tasks share a deadline that is
 * closed, interrupting them, when this search ends or is itself interrupted. Any failed request
 * makes the answer {@code FAILED} (the clients logged it); none, even with no video, is {@code OK}.
 */
@Component
public class CommonsVideoSource implements EventVideoSource {

  static final String KEY = "commons";

  private final WikidataVideoLinks links;
  private final CommonsVideoClient client;
  private final CommonsFileMapper mapper;
  private final Duration readTimeout;

  public CommonsVideoSource(
      WikidataVideoLinks links,
      CommonsVideoClient client,
      CommonsFileMapper mapper,
      MediaLookupProperties properties) {
    this.links = links;
    this.client = client;
    this.mapper = mapper;
    this.readTimeout = properties.lookupDeadline();
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff) {
    List<String> qids =
        events.stream().map(EventContext::wikidataQid).filter(Objects::nonNull).toList();
    try (Deadline inner = Deadline.after(readTimeout)) {
      Task<Optional<Map<String, Links>>> linkTask =
          qids.isEmpty()
              ? null
              : inner.start("commons-video-links", () -> links.links(qids, readTimeout));
      Task<Pages> dayTask =
          dayPathsOff
              ? null
              : inner.start("commons-video-day", () -> client.dateCategory(date, readTimeout));
      Optional<Map<String, Links>> linked =
          linkTask == null ? Optional.of(Map.of()) : value(linkTask);
      Pages day = dayTask == null ? new Pages(List.of(), false) : value(dayTask);
      if (linked == null || linked.isEmpty() || day == null || day.failed()) {
        return failed();
      }
      Map<String, Links> byQid = linked.get();
      List<String> files = new ArrayList<>();
      Map<String, Task<Pages>> categories = new LinkedHashMap<>();
      for (EventContext event : events) {
        Links link = links(byQid, event);
        link.videoFiles().stream().filter(f -> !files.contains(f)).forEach(files::add);
        if (link.category() != null && !categories.containsKey(link.category())) {
          String category = link.category();
          categories.put(
              category,
              inner.start("commons-video-category", () -> client.category(category, readTimeout)));
        }
      }
      Task<Files> fileTask =
          files.isEmpty()
              ? null
              : inner.start("commons-video-files", () -> client.files(files, readTimeout));
      Files found = fileTask == null ? new Files(Map.of(), false) : value(fileTask);
      Map<String, Pages> categoryPages = new HashMap<>();
      for (Map.Entry<String, Task<Pages>> category : categories.entrySet()) {
        Pages pages = value(category.getValue());
        if (pages == null || pages.failed()) {
          return failed();
        }
        categoryPages.put(category.getKey(), pages);
      }
      if (found == null || found.failed()) {
        return failed();
      }
      Map<String, List<Candidate>> byEvent = new HashMap<>();
      Map<String, String> labels = new HashMap<>();
      for (EventContext event : events) {
        Links link = links(byQid, event);
        List<Candidate> candidates = new ArrayList<>();
        for (String file : link.videoFiles()) {
          JsonNode page = found.byName().get(file);
          if (page != null) {
            add(candidates, page, event, date, EventLink.P10, Basis.COMMONS_P10);
          }
        }
        if (link.category() != null) {
          for (JsonNode page : categoryPages.get(link.category()).pages()) {
            add(candidates, page, event, date, EventLink.CATEGORY_DIRECT, Basis.COMMONS_CATEGORY);
          }
        }
        for (JsonNode page : day.pages()) {
          add(candidates, page, event, date, EventLink.NONE, Basis.COMMONS_DATE_CATEGORY);
        }
        byEvent.put(event.eventId(), candidates);
        if (link.englishLabel() != null) {
          labels.put(event.eventId(), link.englishLabel());
        }
      }
      return new SourceResult(byEvent, labels, Status.OK);
    }
  }

  private static Links links(Map<String, Links> byQid, EventContext event) {
    return event.wikidataQid() == null
        ? Links.NONE
        : byQid.getOrDefault(event.wikidataQid(), Links.NONE);
  }

  /** The video of {@code page} for {@code event}, unless the mapper drops it. */
  private void add(
      List<Candidate> candidates,
      JsonNode page,
      EventContext event,
      LocalDate date,
      EventLink link,
      Basis basis) {
    VideoResult video = mapper.mapVideo(page, event.placeRef(), date);
    if (video.item() != null) {
      candidates.add(
          new Candidate(
              video.item(),
              video.title(),
              video.description(),
              video.originalCapture(),
              List.of(),
              link,
              false,
              false,
              basis,
              video.origin()));
    }
  }

  /**
   * The task's value, or {@code null} when it timed out or this search was interrupted (the
   * interrupted requests log their own lines). A task's exception is thrown again, so the search
   * logs it with its class.
   */
  private static <T> T value(Task<T> task) {
    return switch (task.await()) {
      case Done<T>(T value) -> value;
      case Failed<T>(Throwable e) when e instanceof RuntimeException runtime -> throw runtime;
      case Failed<T>(Throwable e) -> throw new IllegalStateException(e.getClass().getSimpleName());
      default -> null;
    };
  }

  private static SourceResult failed() {
    return new SourceResult(Map.of(), Map.of(), Status.FAILED);
  }
}
