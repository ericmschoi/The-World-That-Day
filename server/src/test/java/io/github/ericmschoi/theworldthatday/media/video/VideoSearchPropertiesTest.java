package io.github.ericmschoi.theworldthatday.media.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.ericmschoi.theworldthatday.media.MediaLookupProperties;
import io.github.ericmschoi.theworldthatday.media.MediaProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a4 T1 (3) to (5): the {@code media.video} settings. */
class VideoSearchPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({
    VideoSearchProperties.class,
    MediaProperties.class,
    MediaLookupProperties.class
  })
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  private static final List<String> COMMEMORATION =
      List.of(
          "재현",
          "기념",
          "기념식",
          "주년",
          "다시 보는",
          "추모",
          "anniversary",
          "reenactment",
          "montage",
          "highlights",
          "comparison",
          "memorial",
          "commemoration",
          "tribute");
  private static final List<String> OTHER_STAGE =
      List.of(
          "발사",
          "launch",
          "리허설",
          "rehearsal",
          "예행연습",
          "카운트다운",
          "countdown",
          "훈련",
          "training",
          "준비",
          "preparation");
  private static final List<String> STOPWORDS =
      List.of("the", "a", "an", "of", "in", "on", "at", "and", "for", "to", "및");

  @Test
  void defaultsAreSixtyThreeAndSixHundredWithEmptyLists() {
    runner.run(
        context -> {
          VideoSearchProperties properties = context.getBean(VideoSearchProperties.class);
          assertNumbers(properties);
          assertThat(properties.commemorationWords()).isEmpty();
          assertThat(properties.otherStageWords()).isEmpty();
          assertThat(properties.stopwords()).isEmpty();
        });
  }

  @Test
  void applicationYmlHoldsThePlanValuesAndLists() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              VideoSearchProperties properties = context.getBean(VideoSearchProperties.class);
              assertNumbers(properties);
              assertThat(properties.commemorationWords()).containsExactlyElementsOf(COMMEMORATION);
              assertThat(properties.otherStageWords()).containsExactlyElementsOf(OTHER_STAGE);
              assertThat(properties.stopwords()).containsExactlyElementsOf(STOPWORDS);
            });
  }

  private static void assertNumbers(VideoSearchProperties properties) {
    assertThat(properties.minScore()).isEqualTo(60);
    assertThat(properties.maxEvents()).isEqualTo(3);
    assertThat(properties.maxDurationSeconds()).isEqualTo(600);
  }

  @Test
  void theVideoBlockLeavesTheOtherMediaSettingsAsTheyWere() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              MediaLookupProperties lookup = context.getBean(MediaLookupProperties.class);
              assertThat(lookup.dayMediaLimit()).isEqualTo(6);
              assertThat(lookup.lookupDeadline()).isEqualTo(Duration.ofSeconds(3));
              assertThat(lookup.maxYearsApart()).isEqualTo(5);
              MediaProperties media = context.getBean(MediaProperties.class);
              assertThat(media.allowedVideoEmbedProviders()).isEmpty();
              assertThat(media.blocklist()).isEmpty();
              assertThat(media.excludedCredits()).contains("Yonhap", "AP");
            });
  }

  @Test
  void zeroMaxEventsStarts() {
    runner
        .withPropertyValues("media.video.max-events=0")
        .run(
            context ->
                assertThat(context.getBean(VideoSearchProperties.class).maxEvents()).isZero());
  }

  @Test
  void zeroMinScoreStarts() {
    runner
        .withPropertyValues("media.video.min-score=0")
        .run(
            context ->
                assertThat(context.getBean(VideoSearchProperties.class).minScore()).isZero());
  }

  @Test
  void wordsLoseSurroundingSpacesAndMissingListsAreEmpty() {
    VideoSearchProperties properties =
        new VideoSearchProperties(60, 3, 600, List.of("\u3000다시 보는 "), null, null);

    assertThat(properties.commemorationWords()).containsExactly("다시 보는");
    assertThat(properties.otherStageWords()).isEmpty();
    assertThat(properties.stopwords()).isEmpty();
  }

  @ParameterizedTest(name = "list {0} with a blank word fails")
  @CsvSource({
    "0, media.video.commemoration-words",
    "1, media.video.other-stage-words",
    "2, media.video.stopwords"
  })
  void blankWordInAnyListFailsWithItsName(int list, String key) {
    for (String blank : new String[] {"", " ", "\u00A0", "\u3000"}) {
      List<List<String>> lists = new ArrayList<>(List.of(List.of(), List.of(), List.of()));
      lists.set(list, List.of("launch", blank));
      assertThatIllegalArgumentException()
          .isThrownBy(
              () -> new VideoSearchProperties(60, 3, 600, lists.get(0), lists.get(1), lists.get(2)))
          .withMessageContaining(key);
    }
  }

  @ParameterizedTest(name = "{0}={1} fails startup")
  @CsvSource(
      delimiter = '|',
      value = {
        "media.video.min-score | -1",
        "media.video.max-events | -1",
        "media.video.max-duration-seconds | 0",
        "media.video.max-duration-seconds | -600"
      })
  void badNumberFailsStartupWithItsName(String key, String value) {
    assertFails(key, key + "=" + value);
  }

  @ParameterizedTest(name = "{0} with \"{1}\" fails startup")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '\'',
      value = {
        "media.video.commemoration-words | ''",
        "media.video.commemoration-words | ' '",
        "media.video.other-stage-words | ''",
        "media.video.other-stage-words | ' '",
        "media.video.stopwords | ''",
        "media.video.stopwords | ' '"
      })
  void blankWordFailsStartupWithItsName(String key, String word) {
    assertFails(key, key + "[0]=launch", key + "[1]=" + word);
  }

  private void assertFails(String key, String... properties) {
    runner
        .withPropertyValues(properties)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(key));
  }
}
