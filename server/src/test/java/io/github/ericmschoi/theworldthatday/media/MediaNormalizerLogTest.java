package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.DEFAULT_NORMALIZER;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.with;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * CLAUDE.md 7.6 privacy: when the normalizer drops a record it logs the item ID and the rule only,
 * never the requested date or the place, even at DEBUG.
 */
@ExtendWith(OutputCaptureExtension.class)
class MediaNormalizerLogTest {

  @Test
  void droppedRecordLogNamesOnlyTheIdAndTheRule(CapturedOutput output) {
    Logger logger = (Logger) LoggerFactory.getLogger(MediaNormalizer.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      assertThat(
              DEFAULT_NORMALIZER.normalize(
                  record(with(image(), "source_url", "")), LocalDate.of(2002, 6, 22)))
          .isEmpty();
    } finally {
      logger.setLevel(before);
    }

    assertThat(output.getAll())
        .contains("Media dropped", "example-image", "R1")
        .doesNotContain("2002-06-22")
        .doesNotContain("1988-09-17")
        .doesNotContain("country:KR");
  }
}
