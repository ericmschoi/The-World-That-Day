package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Plan M3a5 T1 (3): the startup WARN for an empty key, and the key never reaching a log. */
@ExtendWith(OutputCaptureExtension.class)
class YoutubeKeyCheckTest {

  private static final String WARNING = "YOUTUBE_API_KEY is empty: YouTube videos are disabled";

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(YoutubeProperties.class)
  @Import(YoutubeKeyCheck.class)
  static class WithCheck {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(WithCheck.class);

  private final ch.qos.logback.classic.Logger checkLogger =
      (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(YoutubeKeyCheck.class);
  private Level levelBefore;

  /** The test yml lowers this logger to ERROR for other tests; this class needs the WARN. */
  @BeforeEach
  void showTheWarning() {
    levelBefore = checkLogger.getLevel();
    checkLogger.setLevel(Level.WARN);
  }

  @AfterEach
  void restoreTheLevel() {
    checkLogger.setLevel(levelBefore);
  }

  private static int count(String text, String part) {
    return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
  }

  @Test
  void anEmptyKeyLogsExactlyOneWarning(CapturedOutput output) {
    runner.run(context -> assertThat(context).hasNotFailed());

    assertThat(count(output.getAll(), WARNING)).isEqualTo(1);
    assertThat(output.getAll()).contains("WARN");
  }

  @Test
  void aBlankKeyLogsTheWarningToo(CapturedOutput output) {
    runner.withPropertyValues("youtube.api-key=   ").run(context -> {});

    assertThat(count(output.getAll(), WARNING)).isEqualTo(1);
  }

  @Test
  void aKeyLogsNothingAndTheValueNeverAppearsEvenAtDebug(CapturedOutput output) {
    ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Level before = root.getLevel();
    root.setLevel(Level.DEBUG);
    try {
      LoggerFactory.getLogger("youtube-key-test").debug("debug-control-line");
      runner
          .withPropertyValues("youtube.api-key=" + YoutubePropertiesTest.FAKE_KEY)
          .run(
              context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(YoutubeProperties.class).enabled()).isTrue();
              });
    } finally {
      root.setLevel(before);
    }

    // The control line proves DEBUG output is captured, so the absence below means something.
    assertThat(output.getAll()).contains("debug-control-line");
    assertThat(output.getAll()).doesNotContain(WARNING);
    assertThat(output.getAll()).doesNotContain(YoutubePropertiesTest.FAKE_KEY);
  }
}
