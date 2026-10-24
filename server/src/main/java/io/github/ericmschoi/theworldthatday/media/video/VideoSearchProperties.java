package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.MediaText;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Event video settings (plan M3a4): the accuracy threshold, how many events are searched, the
 * length cap, and the word lists of {@link VideoAccuracy}. The lists are in {@code
 * application.yml}. A negative count, a cap of 0 or less, or a blank word fails startup with the
 * setting's name in the message.
 */
@ConfigurationProperties("media.video")
public record VideoSearchProperties(
    @DefaultValue("60") int minScore,
    @DefaultValue("3") int maxEvents,
    @DefaultValue("600") int maxDurationSeconds,
    @DefaultValue List<String> commemorationWords,
    @DefaultValue List<String> otherStageWords,
    @DefaultValue List<String> stopwords) {

  @ConstructorBinding
  public VideoSearchProperties {
    if (minScore < 0) {
      throw new IllegalArgumentException(
          "media.video.min-score must not be negative, got: " + minScore);
    }
    if (maxEvents < 0) {
      throw new IllegalArgumentException(
          "media.video.max-events must not be negative, got: " + maxEvents);
    }
    if (maxDurationSeconds <= 0) {
      throw new IllegalArgumentException(
          "media.video.max-duration-seconds must be positive, got: " + maxDurationSeconds);
    }
    commemorationWords = words("media.video.commemoration-words", commemorationWords);
    otherStageWords = words("media.video.other-stage-words", otherStageWords);
    stopwords = words("media.video.stopwords", stopwords);
  }

  private static List<String> words(String key, List<String> values) {
    List<String> copy = values == null ? List.of() : values;
    if (copy.stream().anyMatch(MediaText::isBlank)) {
      throw new IllegalArgumentException(key + " entries must not be empty");
    }
    return copy.stream().map(MediaText::strip).toList();
  }
}
