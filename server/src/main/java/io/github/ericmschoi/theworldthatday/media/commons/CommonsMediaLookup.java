package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.DateRelation.Kind;
import io.github.ericmschoi.theworldthatday.media.MediaItem;
import io.github.ericmschoi.theworldthatday.media.MediaLookup;
import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Drop;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsFileMapper.Result;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Day photos from the Commons day category (plan M3a3b "고르는 법"): JPEG files, placed in Korea unless
 * they carry GPS, {@code same_day} first, then other dates, {@code unknown} last, each group by
 * file title, at most {@code media.day-media-limit}. The whole lookup ends within {@code
 * media.lookup-deadline}, which is also the read timeout of its one request (sent once, no retry);
 * then it is cancelled with up to 1s of grace ({@link Deadline}), and the deadline is logged only
 * when no other failure line was.
 */
@Component
public class CommonsMediaLookup implements MediaLookup {

  static final String PLACE = "country:KR";

  private static final Logger log = LoggerFactory.getLogger(CommonsMediaLookup.class);

  private final CommonsClient client;
  private final CommonsFileMapper mapper;
  private final MediaLookupProperties properties;

  public CommonsMediaLookup(
      CommonsClient client, CommonsFileMapper mapper, MediaLookupProperties properties) {
    this.client = client;
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  public DayMedia dayMedia(LocalDate date, boolean tragicDay) {
    if (tragicDay || properties.dayMediaLimit() == 0) {
      return DayMedia.NONE;
    }
    return switch (Deadline.after(properties.lookupDeadline())
        .start("commons", () -> collect(date))
        .await()) {
      case Done<DayMedia>(DayMedia media) -> media;
      case TimedOut<DayMedia>(boolean stopped, DayMedia late) -> {
        if (!stopped || late == null || !late.failed()) {
          warn("deadline");
        }
        yield DayMedia.FAILED;
      }
      case Failed<DayMedia>(InterruptedException e) -> DayMedia.FAILED;
      case Failed<DayMedia>(Throwable e) -> {
        warn(e.getClass().getSimpleName());
        yield DayMedia.FAILED;
      }
    };
  }

  private DayMedia collect(LocalDate date) {
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
    return new DayMedia(
        items.subList(0, Math.min(items.size(), properties.dayMediaLimit())), false);
  }

  private static int rank(MediaItem item) {
    Kind kind = item.dateRelation().kind();
    return kind == Kind.SAME_DAY ? 0 : kind == Kind.UNKNOWN ? 2 : 1;
  }

  private static void warn(String cause) {
    log.warn(
        "Wikimedia call failed: source={} title={} cause={}",
        ActionApiCommonsClient.SOURCE,
        ActionApiCommonsClient.DAY_CATEGORY,
        cause);
  }
}
