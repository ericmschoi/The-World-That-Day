package io.github.ericmschoi.theworldthatday.media.youtube;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * The YouTube Data API calls the official channel video source uses (plan M3a5 T2). Calling while
 * YouTube is disabled (empty key) throws {@link IllegalStateException}; callers check {@link
 * YoutubeProperties#enabled()} first.
 */
public interface YoutubeClient {

  /** The most IDs one {@link #videos} call takes; the caller splits longer lists. */
  int MAX_IDS = 50;

  /** Video IDs of one {@code search.list} page in {@code channelId} for {@code q}, in order. */
  Response<List<String>> search(String channelId, String q);

  /**
   * The {@code videos.list} items of {@code ids}. Throws {@link IllegalArgumentException} without a
   * request when there are more than {@link #MAX_IDS}.
   */
  Response<List<JsonNode>> videos(List<String> ids);

  /** Outcome of one call. Call failures are values, never exceptions. */
  sealed interface Response<T> {

    record Ok<T>(T value) implements Response<T> {}

    /** A 403 with the reason {@code quotaExceeded}: the bucket is spent until the next reset. */
    record QuotaExceeded<T>() implements Response<T> {}

    /** Any other failure: an error status, an unreadable body, a timeout or an interrupt. */
    record Failed<T>() implements Response<T> {}
  }
}
