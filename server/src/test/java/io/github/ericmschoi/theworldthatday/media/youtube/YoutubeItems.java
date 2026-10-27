package io.github.ericmschoi.theworldthatday.media.youtube;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.config.AppProperties;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import io.github.ericmschoi.theworldthatday.media.MediaFilter;
import io.github.ericmschoi.theworldthatday.media.MediaNormalizer;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Synthetic {@code videos.list} items of {@code fixtures/youtube/videos-1986-09-20.json} (made-up
 * IDs, channel IDs and texts, official field structure) and a {@link YoutubeVideoMapper} built the
 * way the application builds it, without Spring (plan M3a5a T4).
 */
public final class YoutubeItems {

  public static final String CHANNEL = "UCsyntheticChannelId0001";
  public static final String SECOND_CHANNEL = "UCsyntheticChannelId0002";
  public static final String RETROSPECTIVE = "\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*오늘";

  /** The contract example's {@code retrieved_at} (2026-10-25T03:00:00Z) and a fraction. */
  public static final Instant NOW = Instant.parse("2026-10-25T03:00:00.400Z");

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final List<String> CREDITS = List.of("연합뉴스", "Yonhap", "AP", "Getty");

  private YoutubeItems() {}

  /** A fresh copy of the fixture item with this ID. */
  public static ObjectNode item(String id) {
    for (JsonNode item :
        JSON.readTree(YoutubeDataApiClientTest.fixture("videos-1986-09-20.json")).path("items")) {
      if (id.equals(item.path("id").asString())) {
        return (ObjectNode) item.deepCopy();
      }
    }
    throw new IllegalArgumentException("no fixture item " + id);
  }

  /** The fixture item with another title and description. */
  public static ObjectNode titled(String id, String title, String description) {
    ObjectNode item = item(id);
    ((ObjectNode) item.path("snippet")).put("title", title).put("description", description);
    return item;
  }

  public static String fixture(String name) {
    return YoutubeDataApiClientTest.fixture(name);
  }

  /** The plan defaults with {@link #CHANNEL} allowed, {@code youtube} allowed, no blocklist. */
  public static YoutubeVideoMapper mapper() {
    return mapper(List.of(CHANNEL), "http://localhost:5173", List.of(), List.of("youtube"));
  }

  public static YoutubeVideoMapper mapper(
      List<String> channels, String webOrigin, List<BlockEntry> blocklist, List<String> embeds) {
    YoutubeProperties youtube =
        new YoutubeProperties(
            "",
            URI.create("http://localhost:9/youtube/v3"),
            channels,
            80,
            8000,
            25,
            Duration.ofSeconds(2),
            List.of(RETROSPECTIVE));
    return new YoutubeVideoMapper(
        youtube,
        new AppProperties("../data", webOrigin),
        new MediaFilter(new MediaProperties(List.of(), embeds, blocklist, CREDITS)),
        new MediaNormalizer(new MediaProperties(List.of(), embeds)),
        new MutableClock(NOW, ClockConfig.SERVICE_ZONE));
  }
}
