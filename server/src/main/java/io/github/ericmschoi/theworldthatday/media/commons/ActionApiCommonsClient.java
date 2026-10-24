package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.wikimedia.CommonsProperties;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Commons photo lookups through the Action API ({@code action=query}, {@code prop=imageinfo}), sent
 * once with the caller's read timeout; requests, errors and log lines as {@link CommonsActionApi}.
 */
@Component
public class ActionApiCommonsClient implements CommonsClient {

  static final String SOURCE = CommonsActionApi.SOURCE;
  static final String DAY_CATEGORY = "commons day category";
  static final String FILES = "commons files of ";

  private final CommonsActionApi api;

  public ActionApiCommonsClient(
      WikimediaHttpClient http, CommonsProperties properties, JsonMapper json) {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("prop", "imageinfo");
    info.put("iiprop", "url|mime|size|extmetadata");
    info.put("iiurlwidth", 640);
    this.api = new CommonsActionApi(http, properties, json, info);
  }

  @Override
  public Pages dayCategory(LocalDate date, Duration readTimeout) {
    UriComponentsBuilder query =
        api.query()
            .queryParam("generator", "categorymembers")
            .queryParam("gcmtitle", "Category:South Korea photographs taken on " + date)
            .queryParam("gcmtype", "file")
            .queryParam("gcmlimit", 20);
    return api.pages(DAY_CATEGORY, query, readTimeout);
  }

  @Override
  public Files files(List<String> fileNames, Duration readTimeout) {
    return api.files(FILES, fileNames, readTimeout);
  }
}
