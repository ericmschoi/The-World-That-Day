package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.ericmschoi.theworldthatday.media.MediaFilter.Reason;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Plan M3a3a T2 (3), (4) and (6) through Spring: the {@code media.blocklist} and {@code
 * media.excluded-credits} settings, their startup checks, and the filter's logging.
 */
@ExtendWith(OutputCaptureExtension.class)
class MediaFilterContextTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MediaProperties.class)
  @Import(MediaFilter.class)
  static class FilterOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(FilterOnly.class);

  private final ApplicationContextRunner withYml =
      runner.withInitializer(new ConfigDataApplicationContextInitializer());

  @Test
  void applicationYmlHasAnEmptyBlocklistAndTheInitialCredits() {
    withYml.run(
        context -> {
          assertThat(context).hasNotFailed();
          MediaProperties properties = context.getBean(MediaProperties.class);
          assertThat(properties.blocklist()).isEmpty();
          assertThat(properties.excludedCredits())
              .containsExactlyElementsOf(MediaFilterTest.INITIAL_CREDITS);
          MediaFilter filter = context.getBean(MediaFilter.class);
          assertThat(
                  filter.check(
                      record(image()), new MediaOrigin("commons", "A.jpg", null, "AP Photo", null)))
              .contains(Reason.EXCLUDED_CREDIT);
        });
  }

  @Test
  void withoutSettingsBothListsAreEmpty() {
    runner.run(
        context -> {
          MediaProperties properties = context.getBean(MediaProperties.class);
          assertThat(properties.blocklist()).isEmpty();
          assertThat(properties.excludedCredits()).isEmpty();
        });
  }

  @Test
  void blocklistEntriesBind() {
    withYml
        .withPropertyValues(
            "media.blocklist[0].source=commons",
            "media.blocklist[0].id=Example photo.jpg",
            "media.blocklist[1].source=youtube",
            "media.blocklist[1].channel=UCexampleexampleexample0",
            "media.blocklist[1].author=")
        .run(
            context -> {
              assertThat(context.getBean(MediaProperties.class).blocklist())
                  .containsExactly(
                      new BlockEntry("commons", "Example photo.jpg", null, null),
                      new BlockEntry("youtube", null, null, "UCexampleexampleexample0"));
              assertThat(
                      context
                          .getBean(MediaFilter.class)
                          .check(
                              record(image()),
                              new MediaOrigin("commons", "Example_photo.jpg", null, null, null)))
                  .contains(Reason.BLOCKED_ID);
            });
  }

  @Test
  void entryWithoutIdAuthorOrChannelFailsStartup() {
    withYml
        .withPropertyValues("media.blocklist[0].source=commons")
        .run(context -> assertFailsWith(context, "media.blocklist[0]"));
  }

  @ParameterizedTest(name = "source [{0}] fails startup")
  @ValueSource(strings = {"", " ", "Commons", "you tube"})
  void malformedSourceFailsStartup(String source) {
    withYml
        .withPropertyValues(
            "media.blocklist[0].source=" + source, "media.blocklist[0].id=Example photo.jpg")
        .run(context -> assertFailsWith(context, "media.blocklist[0].source"));
  }

  @Test
  void entryWithoutSourceFailsStartup() {
    withYml
        .withPropertyValues("media.blocklist[0].author=Example Author")
        .run(context -> assertFailsWith(context, "media.blocklist[0].source"));
  }

  @Test
  void emptyExcludedCreditFailsStartup() {
    withYml
        .withPropertyValues("media.excluded-credits[0]=AP", "media.excluded-credits[1]=")
        .run(context -> assertFailsWith(context, "media.excluded-credits"));
  }

  @Test
  void constructorChecksTheSameRules() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new MediaProperties(
                    List.of(),
                    List.of(),
                    List.of(new BlockEntry("commons", " ", null, null)),
                    null))
        .withMessageContaining("media.blocklist[0]");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new MediaProperties(List.of(), List.of(), null, List.of("AP", " ")))
        .withMessageContaining("media.excluded-credits");
    MediaProperties empty = new MediaProperties(null, null, null, null);
    assertThat(empty.blocklist()).isEmpty();
    assertThat(empty.excludedCredits()).isEmpty();
  }

  @ParameterizedTest(name = "excluded credit of Unicode spaces [{0}] fails startup")
  @ValueSource(strings = {"\u00A0", "\u2007", "\u3000"})
  void unicodeSpaceExcludedCreditFailsStartup(String blank) {
    withYml
        .withPropertyValues("media.excluded-credits[0]=AP", "media.excluded-credits[1]=" + blank)
        .run(context -> assertFailsWith(context, "media.excluded-credits"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new MediaProperties(List.of(), List.of(), null, List.of("AP", blank)))
        .withMessageContaining("media.excluded-credits");
  }

  @Test
  void unicodeSpaceBlockEntryFieldsBecomeNull() {
    assertThat(new BlockEntry("commons", "\u00A0", "Example Author", "\u2007\u3000"))
        .isEqualTo(new BlockEntry("commons", null, "Example Author", null));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new MediaProperties(
                    List.of(),
                    List.of(),
                    List.of(new BlockEntry("commons", "\u00A0", "\u202F", "\u3000")),
                    null))
        .withMessageContaining("media.blocklist[0]");
    withYml
        .withPropertyValues("media.blocklist[0].source=commons", "media.blocklist[0].id=\u00A0")
        .run(context -> assertFailsWith(context, "media.blocklist[0]"));
  }

  private static void assertFailsWith(AssertableApplicationContext context, String key) {
    assertThat(context)
        .hasFailed()
        .getFailure()
        .rootCause()
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(key);
  }

  /** T2 (6): only the reason name is logged, never the file name, author, credit or channel. */
  @Test
  void debugLogNamesOnlyTheReason(CapturedOutput output) {
    MediaFilter filter =
        new MediaFilter(
            new MediaProperties(
                List.of(),
                List.of(),
                List.of(
                    new BlockEntry("commons", "Example photo.jpg", null, null),
                    new BlockEntry("commons", null, "Example Author", null),
                    new BlockEntry("youtube", null, null, "UCexampleexampleexample0")),
                List.of("AP")));
    Logger logger = (Logger) LoggerFactory.getLogger(MediaFilter.class);
    Level before = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      filter.check(
          record(image()), new MediaOrigin("commons", "Example_photo.jpg", null, null, null));
      filter.check(
          record(image()), new MediaOrigin("commons", "B.jpg", "Example Author", null, null));
      filter.check(
          record(image()),
          new MediaOrigin("youtube", "AAAAAAAAAAA", null, null, "UCexampleexampleexample0"));
      filter.check(
          record(image()),
          new MediaOrigin("commons", "C.jpg", null, "AP Photo/Example Shooter", null));
    } finally {
      logger.setLevel(before);
    }

    assertThat(output.getAll())
        .contains(
            "reason=blocked_id",
            "reason=blocked_author",
            "reason=blocked_channel",
            "reason=excluded_credit")
        .doesNotContain("Example_photo", "Example photo", "B.jpg", "C.jpg")
        .doesNotContain("Example Author", "UCexample", "AAAAAAAAAAA")
        .doesNotContain("AP Photo", "Example Shooter", "example-image");
  }
}
