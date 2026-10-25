package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Result;
import io.github.ericmschoi.theworldthatday.media.pageimages.KoWikiPageImages;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Running;
import io.github.ericmschoi.theworldthatday.media.video.VideoSearch.Videos;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Interrupted;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Outcome;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Task;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient.PhotoPath;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Day photos from the Commons day category (plan M3a3b "고르는 법"): JPEG files, placed in Korea unless
 * they carry GPS, {@code same_day} first, then other dates, {@code unknown} last, each group by
 * file title, at most {@code media.day-media-limit}. Event photos (plan M3a3d): the free lead image
 * of each target's article (PageImages, then one Commons file batch), JPEG only, placed at the
 * event's scope unless it carries GPS, at most {@code media.max-years-apart} years from the date,
 * each file on the first event only, and left out of the day photos before they are cut. Both paths
 * run at once on one {@link Deadline} of {@code media.lookup-deadline}, which is also the read
 * timeout of each request (sent once, no retry); a path still running then is cancelled with up to
 * 1s of grace and left empty. A deadline is logged only when no other failure line was. Event
 * videos (plan M3a4): each video source of {@link VideoSearch} runs as one more task on the same
 * deadline; a chosen video follows the event's photo. Each photo path keeps a request slot from the
 * video requests while it runs ({@link WikimediaHttpClient#openPhotoPath}, plan M3a4b T3 (9)).
 */
@Component
public class CommonsMediaLookup implements MediaLookup {

  static final String PLACE = "country:KR";
  static final String MEDIA = "media";

  private static final Logger log = LoggerFactory.getLogger(CommonsMediaLookup.class);

  private final CommonsClient client;
  private final CommonsFileMapper mapper;
  private final KoWikiPageImages pageImages;
  private final MediaLookupProperties properties;
  private final VideoSearch videos;
  private final WikimediaHttpClient slots;

  /** Photos only, without a video search. */
  public CommonsMediaLookup(
      CommonsClient client,
      CommonsFileMapper mapper,
      KoWikiPageImages pageImages,
      MediaLookupProperties properties) {
    this(client, mapper, pageImages, properties, null, null);
  }

  /** With a video search, without request slots to keep (fake clients). */
  public CommonsMediaLookup(
      CommonsClient client,
      CommonsFileMapper mapper,
      KoWikiPageImages pageImages,
      MediaLookupProperties properties,
      VideoSearch videos) {
    this(client, mapper, pageImages, properties, videos, null);
  }

  /** {@code slots}: the HTTP client whose request slots the photo paths keep from videos. */
  @Autowired
  public CommonsMediaLookup(
      CommonsClient client,
      CommonsFileMapper mapper,
      KoWikiPageImages pageImages,
      MediaLookupProperties properties,
      VideoSearch videos,
      WikimediaHttpClient slots) {
    this.client = client;
    this.mapper = mapper;
    this.pageImages = pageImages;
    this.properties = properties;
    this.videos = videos;
    this.slots = slots;
  }

  @Override
  public DayMedia dayMedia(LocalDate date, boolean tragicDay) {
    WorldMedia media = lookup(date, List.of(), tragicDay);
    return new DayMedia(media.dayMedia(), media.failed());
  }

  @Override
  public WorldMedia lookup(LocalDate date, List<EventTarget> targets, boolean tragicDay) {
    boolean askDay = !tragicDay && properties.dayMediaLimit() > 0;
    List<EventTarget> asked = targets.stream().limit(KoWikiPageImages.MAX_TITLES).toList();
    String eventLabel = "event photos of " + asked.size();
    // opened before any task starts, so no video request can take a slot first
    PhotoPath dayPath = askDay ? photoPath() : null;
    PhotoPath eventPath = asked.isEmpty() ? null : photoPath();
    try (Deadline deadline = Deadline.after(properties.lookupDeadline())) {
      Task<DayMedia> day =
          askDay
              ? deadline.start("commons", () -> closing(dayPath, () -> dayCandidates(date)))
              : null;
      Task<EventPhotos> events =
          asked.isEmpty()
              ? null
              : deadline.start(MEDIA, () -> closing(eventPath, () -> eventPhotos(date, asked)));
      Running search = videos == null ? null : videos.start(deadline, date, targets, tragicDay);
      EventPhotos photos =
          events == null
              ? EventPhotos.NONE
              : outcome(events.await(), EventPhotos.FAILED, EventPhotos::failed, eventLabel);
      DayMedia candidates =
          day == null
              ? DayMedia.NONE
              : outcome(day.await(), DayMedia.FAILED, DayMedia::failed, null);
      Videos found = search == null ? new Videos(Map.of(), false) : search.await();
      Map<String, List<MediaItem>> eventMedia = new HashMap<>(photos.byEvent());
      found
          .byEvent()
          .forEach(
              (id, video) ->
                  eventMedia.merge(
                      id,
                      List.of(video),
                      (a, b) -> Stream.concat(a.stream(), b.stream()).toList()));
      List<MediaItem> dayItems = new ArrayList<>();
      for (MediaItem item : candidates.items()) {
        if (!photos.ids().contains(item.record().id())) {
          dayItems.add(item);
        }
      }
      return new WorldMedia(
          eventMedia,
          dayItems.subList(0, Math.min(dayItems.size(), properties.dayMediaLimit())),
          candidates.failed() || photos.failed() || found.failed());
    } finally {
      close(dayPath);
      close(eventPath);
    }
  }

  private PhotoPath photoPath() {
    return slots == null ? null : slots.openPhotoPath();
  }

  /** The value of {@code photos}, closing {@code path} when it ends, however it ends. */
  private static <T> T closing(PhotoPath path, Supplier<T> photos) {
    try {
      return photos.get();
    } finally {
      close(path);
    }
  }

  private static void close(PhotoPath path) {
    if (path != null) {
      path.close();
    }
  }

  /**
   * The value of a path, or {@code failed}: a timeout is logged under {@code label} (the day
   * category when null) unless the path ended failed, so its adapter logged; an exception of the
   * path is logged with its class; an interrupted caller is not logged.
   */
  private static <T> T outcome(Outcome<T> outcome, T failed, Predicate<T> isFailed, String label) {
    return switch (outcome) {
      case Done<T>(T value) -> value;
      case TimedOut<T>(boolean stopped, T late) -> {
        if (!stopped || late == null || !isFailed.test(late)) {
          warn(label, "deadline");
        }
        yield failed;
      }
      case Interrupted<T>() -> failed;
      case Failed<T>(Throwable e) -> {
        warn(label, e.getClass().getSimpleName());
        yield failed;
      }
    };
  }

  /** The day category's photos in their order, not cut yet. */
  private DayMedia dayCandidates(LocalDate date) {
    Pages pages = client.dayCategory(date, properties.lookupDeadline());
    if (pages.failed()) {
      return DayMedia.FAILED;
    }
    List<JsonNode> byTitle = new ArrayList<>(pages.pages());
    byTitle.sort(Comparator.comparing(page -> page.path("title").asString("")));
    Map<Drop, Integer> dropped = new EnumMap<>(Drop.class);
    Set<String> ids = new HashSet<>();
    List<MediaItem> items = new ArrayList<>();
    for (JsonNode page : byTitle) {
      Result result = mapper.map(page, "image/jpeg"::equals, PLACE, date);
      if (result.drop() != null) {
        dropped.merge(result.drop(), 1, Integer::sum);
      } else if (ids.add(result.item().record().id())) {
        items.add(result.item());
      }
    }
    if (!dropped.isEmpty()) {
      log.debug("Day photos dropped: {}", dropped); // reason counts only
    }
    items.sort(Comparator.comparingInt(CommonsMediaLookup::rank)); // stable: titles stay in order
    return new DayMedia(items, false);
  }

  /** The event photos of {@code targets}: PageImages, one file batch, the mapper, the rules. */
  private EventPhotos eventPhotos(LocalDate date, List<EventTarget> targets) {
    Optional<Map<String, String>> names =
        pageImages.imageNames(
            targets.stream().map(EventTarget::title).toList(), properties.lookupDeadline());
    if (names.isEmpty()) {
      return EventPhotos.FAILED; // the adapter logged the failure
    }
    if (names.get().isEmpty()) {
      return EventPhotos.NONE;
    }
    List<String> fileNames =
        targets.stream()
            .map(target -> names.get().get(target.title()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Files files = client.files(fileNames, properties.lookupDeadline());
    if (files.failed()) {
      return EventPhotos.FAILED;
    }
    Map<String, Integer> dropped = new LinkedHashMap<>();
    Map<String, List<MediaItem>> byEvent = new HashMap<>();
    Set<String> ids = new HashSet<>();
    for (EventTarget target : targets) {
      String name = names.get().get(target.title());
      if (name == null) {
        continue; // no free lead image: not a drop
      }
      JsonNode page = files.byName().get(name);
      Result result =
          page == null
              ? new Result(null, Drop.MISSING)
              : mapper.map(page, "image/jpeg"::equals, target.placeRef(), date);
      String reason =
          result.drop() != null
              ? result.drop().name()
              : Math.abs(result.item().dateRelation().years()) > properties.maxYearsApart()
                  ? "YEARS"
                  : !ids.add(result.item().record().id()) ? "DUPLICATE" : null;
      if (reason == null) {
        byEvent.put(target.eventId(), List.of(result.item()));
      } else {
        dropped.merge(reason, 1, Integer::sum);
      }
    }
    if (!dropped.isEmpty()) {
      log.debug("Event photos dropped: {}", dropped); // reason counts only
    }
    return new EventPhotos(byEvent, ids, false);
  }

  private static int rank(MediaItem item) {
    Kind kind = item.dateRelation().kind();
    return kind == Kind.SAME_DAY ? 0 : kind == Kind.UNKNOWN ? 2 : 1;
  }

  private static void warn(String label, String cause) {
    log.warn(
        "Wikimedia call failed: source={} title={} cause={}",
        label == null ? ActionApiCommonsClient.SOURCE : MEDIA,
        label == null ? ActionApiCommonsClient.DAY_CATEGORY : label,
        cause);
  }

  /** Event photos by event ID, the IDs of the kept files, and whether the path failed. */
  private record EventPhotos(
      Map<String, List<MediaItem>> byEvent, Set<String> ids, boolean failed) {

    static final EventPhotos NONE = new EventPhotos(Map.of(), Set.of(), false);
    static final EventPhotos FAILED = new EventPhotos(Map.of(), Set.of(), true);
  }
}
