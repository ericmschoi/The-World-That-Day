package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient.Lane;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Commons video lookups through the Action API with {@code prop=videoinfo} (derivatives and the 640
 * poster width in the same request, so no extra request for them); requests, errors and log lines
 * as {@link CommonsActionApi}, with fixed labels only.
 */
@Component
public class ActionApiCommonsVideoClient implements CommonsVideoClient {

  static final String DATE_CATEGORY = "commons video date category";
  static final String CATEGORY = "commons video category search";
  static final String FILES = "commons video files of ";

  private final CommonsActionApi api;

  public ActionApiCommonsVideoClient(
      WikimediaHttpClient http, CommonsProperties properties, JsonMapper json) {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("prop", "videoinfo");
    info.put("viprop", "url|size|mime|mediatype|extmetadata|derivatives");
    info.put("viurlwidth", 640);
    this.api = new CommonsActionApi(http, properties, json, info, Lane.VIDEO);
  }

  @Override
  public CommonsClient.Pages dateCategory(LocalDate date, Duration readTimeout) {
    return api.pages(
        DATE_CATEGORY,
        api.query()
            .queryParam("generator", "categorymembers")
            .queryParam("gcmtitle", "Category:Videos taken on " + date)
            .queryParam("gcmtype", "file")
            .queryParam("gcmlimit", 20),
        readTimeout);
  }

  @Override
  public CommonsClient.Pages category(String category, Duration readTimeout) {
    String name = category == null ? "" : category.replace("\"", "").strip();
    if (name.isEmpty()) {
      return new CommonsClient.Pages(List.of(), false);
    }
    return api.pages(
        CATEGORY,
        api.query()
            .queryParam("generator", "search")
            .queryParam("gsrnamespace", 6)
            .queryParam("gsrlimit", 10)
            .queryParam("gsrsearch", "{search}"),
        readTimeout,
        "incategory:\"" + name + "\" filetype:video");
  }

  @Override
  public CommonsClient.Files files(List<String> fileNames, Duration readTimeout) {
    return api.files(FILES, fileNames, readTimeout);
  }
}
