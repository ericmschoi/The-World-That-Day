package io.github.ericmschoi.theworldthatday.media.youtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs one WARN at startup when {@code YOUTUBE_API_KEY} is empty (the value is never logged). */
@Component
class YoutubeKeyCheck {

  private static final Logger log = LoggerFactory.getLogger(YoutubeKeyCheck.class);

  YoutubeKeyCheck(YoutubeProperties properties) {
    if (!properties.enabled()) {
      log.warn("YOUTUBE_API_KEY is empty: YouTube videos are disabled");
    }
  }
}
