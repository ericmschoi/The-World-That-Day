package io.github.ericmschoi.theworldthatday.media.youtube;

import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaText;
import io.github.ericmschoi.theworldthatday.media.video.Candidate;
import io.github.ericmschoi.theworldthatday.media.video.EventContext;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeClient.Response;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Task;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * The official channel video source of plan M3a5a T5 (key {@code youtube}): one {@code search.list}
 * per event with a title and allowed channel, then {@code videos.list} for the date's distinct IDs
 * in batches of {@link YoutubeClient#MAX_IDS}; each event gets the {@link YoutubeVideoMapper}
 * candidates of its own search order (an ID missing from {@code videos.list} is dropped). Each step
 * first reserves all it needs from {@link YoutubeQuota} and is skipped ({@code QUOTA_SKIPPED}, no
 * request) when that is refused; with nothing to reserve no reservation is made. Any failed call
 * makes the answer {@code FAILED} (the client logged it). A 403 {@code quotaExceeded} marks the
 * bucket spent, unless the Pacific day changed since the reservation, and gives {@code
 * QUOTA_SKIPPED}; mixed with a failure the answer is {@code FAILED}. The calls of a step run at
 * once on an inner deadline of {@code media.lookup-deadline}, closed when the search ends or is
 * interrupted. Without an API key the source is not registered at all, so the search neither reads
 * its cache nor asks it.
 */
@Component
@Conditional(YoutubeVideoSource.KeySet.class)
public class YoutubeVideoSource implements EventVideoSource {

  private final YoutubeProperties properties;
  private final YoutubeClient client;
  private final YoutubeQuota quota;
  private final YoutubeVideoMapper mapper;
  private final Duration readTimeout;

  public YoutubeVideoSource(
      YoutubeProperties properties,
      YoutubeClient client,
      YoutubeQuota quota,
      YoutubeVideoMapper mapper,
      MediaLookupProperties lookup) {
    this.properties = properties;
    this.client = client;
    this.quota = quota;
    this.mapper = mapper;
    this.readTimeout = lookup.lookupDeadline();
  }

  @Override
  public String key() {
    return YoutubeVideoMapper.KEY;
  }

  @Override
  public SourceResult find(LocalDate date, List<EventContext> events, boolean dayPathsOff) {
    List<EventContext> asked = events.stream().filter(e -> !MediaText.isBlank(e.title())).toList();
    List<String> channels = properties.allowedChannels();
    Map<String, List<Candidate>> byEvent = new HashMap<>();
    events.forEach(event -> byEvent.put(event.eventId(), List.of()));
    if (asked.isEmpty()) {
      return new SourceResult(byEvent, Map.of(), Status.OK);
    }
    LocalDate searchDay = quota.reserveOn(Bucket.SEARCH, asked.size() * channels.size());
    if (searchDay == null) {
      return result(Status.QUOTA_SKIPPED);
    }
    try (Deadline inner = Deadline.after(readTimeout)) {
      Map<String, List<Task<Response<List<String>>>>> searches = new LinkedHashMap<>();
      for (EventContext event : asked) {
        List<Task<Response<List<String>>>> tasks = new ArrayList<>();
        for (String channel : channels) {
          tasks.add(inner.start("youtube-search", () -> client.search(channel, event.title())));
        }
        searches.put(event.eventId(), tasks);
      }
      Map<String, Set<String>> found = new LinkedHashMap<>();
      Status searched = collect(searches, found, Bucket.SEARCH, searchDay);
      if (searched != Status.OK) {
        return result(searched);
      }
      List<String> ids = found.values().stream().flatMap(Set::stream).distinct().toList();
      if (ids.isEmpty()) {
        return new SourceResult(byEvent, Map.of(), Status.OK);
      }
      int batches = (ids.size() + YoutubeClient.MAX_IDS - 1) / YoutubeClient.MAX_IDS;
      LocalDate unitDay = quota.reserveOn(Bucket.UNITS, batches);
      if (unitDay == null) {
        return result(Status.QUOTA_SKIPPED);
      }
      List<Task<Response<List<JsonNode>>>> lists = new ArrayList<>();
      for (int i = 0; i < ids.size(); i += YoutubeClient.MAX_IDS) {
        List<String> batch = ids.subList(i, Math.min(ids.size(), i + YoutubeClient.MAX_IDS));
        lists.add(inner.start("youtube-videos", () -> client.videos(batch)));
      }
      Map<String, Set<JsonNode>> items = new LinkedHashMap<>();
      Status listed = collect(Map.of("", lists), items, Bucket.UNITS, unitDay);
      if (listed != Status.OK) {
        return result(listed);
      }
      Map<String, JsonNode> byId = new HashMap<>();
      items
          .getOrDefault("", Set.of())
          .forEach(item -> byId.put(item.path("id").asString(""), item));
      for (EventContext event : asked) {
        List<JsonNode> videos =
            found.get(event.eventId()).stream().map(byId::get).filter(Objects::nonNull).toList();
        byEvent.put(event.eventId(), mapper.candidates(videos, event));
      }
      return new SourceResult(byEvent, Map.of(), Status.OK);
    }
  }

  /**
   * Awaits every task, adding the values of each key's tasks in order: {@code FAILED} when one
   * failed, timed out or this search was interrupted, else {@code QUOTA_SKIPPED} (the bucket marked
   * spent for {@code reservedOn}) when one hit the quota, else {@code OK}.
   */
  private <T> Status collect(
      Map<String, ? extends List<Task<Response<List<T>>>>> tasks,
      Map<String, Set<T>> values,
      Bucket bucket,
      LocalDate reservedOn) {
    boolean failed = false;
    boolean spent = false;
    for (Map.Entry<String, ? extends List<Task<Response<List<T>>>>> entry : tasks.entrySet()) {
      Set<T> kept = values.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>());
      for (Task<Response<List<T>>> task : entry.getValue()) {
        switch (value(task)) {
          case Response.Ok<List<T>>(var value) -> kept.addAll(value);
          case Response.QuotaExceeded<List<T>>() -> spent = true;
          case Response.Failed<List<T>>() -> failed = true;
          case null -> failed = true;
        }
      }
    }
    if (spent) {
      quota.exhausted(bucket, reservedOn);
    }
    return failed ? Status.FAILED : spent ? Status.QUOTA_SKIPPED : Status.OK;
  }

  /**
   * The task's value, or {@code null} when it timed out or this search was interrupted. A task's
   * exception is thrown again, so the search logs it with its class.
   */
  private static <T> T value(Task<T> task) {
    return switch (task.await()) {
      case Done<T>(T value) -> value;
      case Failed<T>(Throwable e) when e instanceof RuntimeException runtime -> throw runtime;
      case Failed<T>(Throwable e) -> throw new IllegalStateException(e.getClass().getSimpleName());
      default -> null;
    };
  }

  private static SourceResult result(Status status) {
    return new SourceResult(Map.of(), Map.of(), status);
  }

  /** Whether {@code youtube.api-key} is set (as {@link YoutubeProperties#enabled()} reads it). */
  static final class KeySet implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return Binder.get(context.getEnvironment())
          .bind("youtube.api-key", String.class)
          .map(key -> !key.isBlank())
          .orElse(false);
    }
  }
}
