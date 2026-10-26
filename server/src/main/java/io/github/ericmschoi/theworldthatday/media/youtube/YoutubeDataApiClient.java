package io.github.ericmschoi.theworldthatday.media.youtube;

import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link YoutubeClient} over the YouTube Data API v3. The key goes only in the {@code
 * x-goog-api-key} header, never in the URL. Each call is sent once (no retry, plan M3a3c decision 1
 * (a)) with the connect timeout {@code youtube.connect-timeout} and the read timeout {@code
 * media.lookup-deadline}; an interrupt cancels the request and keeps the thread's interrupt flag.
 * There is no request limiter (plan M3a5 "시간과 요청 제한"): the daily quota is the limit. Each failure
 * logs one WARN with the call name and a fixed cause only, never the URL, the query, an ID, the key
 * or the body.
 */
@Component
public class YoutubeDataApiClient implements YoutubeClient {

  private static final Logger log = LoggerFactory.getLogger(YoutubeDataApiClient.class);
  private static final String SEARCH = "search";
  private static final String VIDEOS = "videos";
  private static final String VIDEO_PARTS = "snippet,status,contentDetails,recordingDetails";

  private record Reply(int status, String body) {}

  private final YoutubeProperties properties;
  private final JsonMapper json;
  private final RestClient restClient;

  public YoutubeDataApiClient(
      YoutubeProperties properties, MediaLookupProperties lookup, JsonMapper json) {
    this.properties = properties;
    this.json = json;
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient(properties));
    factory.setReadTimeout(lookup.lookupDeadline());
    this.restClient = RestClient.builder().requestFactory(factory).build();
  }

  /** The HTTP client of every call, connecting within {@code youtube.connect-timeout}. */
  static HttpClient httpClient(YoutubeProperties properties) {
    return HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
  }

  @Override
  public Response<List<String>> search(String channelId, String q) {
    if (q == null || q.isBlank()) {
      return new Response.Ok<>(List.of());
    }
    Map<String, String> query = new LinkedHashMap<>();
    query.put("part", "snippet");
    query.put("type", "video");
    query.put("videoEmbeddable", "true");
    query.put("channelId", encode(channelId));
    query.put("q", encode(q));
    query.put("maxResults", Integer.toString(properties.maxResults()));
    query.put("regionCode", "KR");
    query.put("relevanceLanguage", "ko");
    return call(
        SEARCH,
        query,
        items ->
            items
                .valueStream()
                .map(item -> item.path("id").path("videoId").asString(""))
                .filter(id -> !id.isBlank())
                .toList());
  }

  @Override
  public Response<List<JsonNode>> videos(List<String> ids) {
    if (ids.size() > MAX_IDS) {
      throw new IllegalArgumentException(
          "videos.list takes at most " + MAX_IDS + " IDs, got " + ids.size());
    }
    if (ids.isEmpty()) {
      return new Response.Ok<>(List.of());
    }
    Map<String, String> query = new LinkedHashMap<>();
    query.put("part", VIDEO_PARTS);
    // each ID encoded on its own, so the separators stay plain commas as in the API reference
    query.put(
        "id", ids.stream().map(YoutubeDataApiClient::encode).collect(Collectors.joining(",")));
    return call(VIDEOS, query, items -> items.valueStream().toList());
  }

  /** Query values in {@code query} are already encoded. */
  private <T> Response<T> call(
      String call, Map<String, String> query, Function<JsonNode, T> readItems) {
    if (!properties.enabled()) {
      throw new IllegalStateException("YouTube is disabled: youtube.api-key is empty");
    }
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUri(properties.apiUrl()).pathSegment(call);
    query.forEach(builder::queryParam);
    URI uri = builder.build(true).toUri();
    Reply reply;
    try {
      reply =
          restClient
              .get()
              .uri(uri)
              .header("x-goog-api-key", properties.apiKey())
              .exchangeForRequiredValue(this::read);
    } catch (RestClientException e) {
      return fail(call, NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName());
    }
    if (reply.status() == 403 && quotaExceeded(reply.body())) {
      log.warn("YouTube call failed: call={} cause=quotaExceeded", call);
      return new Response.QuotaExceeded<>();
    }
    if (reply.status() < 200 || reply.status() >= 300) {
      return fail(call, "status " + reply.status());
    }
    JsonNode items = tree(reply.body()).path("items");
    if (!items.isArray()) {
      return fail(call, "unreadable response");
    }
    return new Response.Ok<>(readItems.apply(items));
  }

  /**
   * Whether a 403 body names the reason {@code quotaExceeded}. The error reference [91] lists that
   * reason as a 403 but shows no body; the {@code error.errors[].reason} shape is assumed from the
   * Google API error format, not documented there (research T0c, 2026-10-25). A body that cannot be
   * read or names no such reason is an ordinary failure.
   */
  private boolean quotaExceeded(String body) {
    for (JsonNode error : tree(body).path("error").path("errors")) {
      if ("quotaExceeded".equals(error.path("reason").asString(""))) {
        return true;
      }
    }
    return false;
  }

  private JsonNode tree(String body) {
    try {
      return body == null ? json.missingNode() : json.readTree(body);
    } catch (JacksonException e) {
      return json.missingNode();
    }
  }

  /** The body of a 2xx or 403 answer (the only ones read); others are not read. */
  private Reply read(HttpRequest request, ConvertibleClientHttpResponse response)
      throws IOException {
    int status = response.getStatusCode().value();
    boolean wanted = status == 403 || (status >= 200 && status < 300);
    return new Reply(
        status,
        wanted ? StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8) : null);
  }

  private static String encode(String value) {
    return UriUtils.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private static <T> Response<T> fail(String call, String cause) {
    log.warn("YouTube call failed: call={} cause={}", call, cause);
    return new Response.Failed<>();
  }
}
