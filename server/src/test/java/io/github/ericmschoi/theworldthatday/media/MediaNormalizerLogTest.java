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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * CLAUDE.md 7.6 privacy: when the normalizer drops a record it logs the source prefix of the ID and
 * the rule only, never the item ID, the requested date or the place, even at DEBUG (plan
 * chore-m3-followups T2).
 */
@ExtendWith(OutputCaptureExtension.class)
class MediaNormalizerLogTest {

  @Test
  void droppedRecordLogNamesOnlyTheSourcePrefixAndTheRule(CapturedOutput output) {
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
        .contains("Media dropped: source=example rule=R1")
        .doesNotContain("example-image")
        .doesNotContain("2002-06-22")
        .doesNotContain("1988-09-17")
        .doesNotContain("country:KR");
  }

  @ParameterizedTest
  @CsvSource({
    "youtube-Abc123xyz_5,youtube",
    "commons-123,commons",
    "example-image,example",
    "a-,a",
    "0123456789abcdef-x,0123456789abcdef"
  })
  void sourcePrefixIsTheLeadingPartOfTheId(String id, String expected) {
    assertThat(MediaNormalizer.sourcePrefix(id)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        "   ",
        "nodash",
        "-leading",
        "YouTube-x",
        "you tube-x",
        "you\ntube-x",
        " youtube-x",
        "0123456789abcdefg-x",
        "you_tube-x"
      })
  void sourcePrefixIsUnknownWhenTheIdIsMissingOrTheRuleIsNotMet(String id) {
    assertThat(MediaNormalizer.sourcePrefix(id)).isEqualTo("unknown");
  }
}
