package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Plan M3a5 T1 (1): the {@code youtube.*} settings, their defaults and checks. */
class YoutubePropertiesTest {

  static final String KTV_ARCHIVE = "UC8_LPVE4Yuc6KF0opF6uS_w";
  static final String FAKE_KEY = "test-key-not-real";
  private static final String RETROSPECTIVE = "\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*오늘";

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(YoutubeProperties.class)
  static class PropertiesOnly {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

  private static void assertPlanDefaults(YoutubeProperties properties) {
    assertThat(properties.apiUrl()).isEqualTo(URI.create("https://www.googleapis.com/youtube/v3"));
    assertThat(properties.allowedChannels()).containsExactly(KTV_ARCHIVE);
    assertThat(properties.dailySearchBudget()).isEqualTo(80);
    assertThat(properties.dailyUnitBudget()).isEqualTo(8000);
    assertThat(properties.maxResults()).isEqualTo(25);
    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.retrospectiveTitlePatterns()).containsExactly(RETROSPECTIVE);
    Pattern retrospective = Pattern.compile(RETROSPECTIVE);
    assertThat(retrospective.matcher("1986년 9월 20일 오늘 #제10회 #아시안게임").find()).isTrue();
    assertThat(retrospective.matcher("1986 서울아시안게임 개막식").find()).isFalse();
  }

  @Test
  void withoutSettingsTheDefaultsMatchThePlan() {
    runner.run(
        context -> {
          YoutubeProperties properties = context.getBean(YoutubeProperties.class);
          assertThat(properties.apiKey()).isEmpty();
          assertThat(properties.enabled()).isFalse();
          assertPlanDefaults(properties);
        });
  }

  @Test
  void applicationYmlGivesTheSameValuesAndTestsKeepTheKeyEmpty() {
    runner
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              YoutubeProperties properties = context.getBean(YoutubeProperties.class);
              assertThat(properties.enabled()).isFalse();
              assertThat(properties.apiKey()).isEmpty();
              // The test config/application.yml points the address at a closed local port.
              assertThat(properties.apiUrl())
                  .isEqualTo(URI.create("http://localhost:9/youtube/v3"));
              assertThat(properties.allowedChannels()).containsExactly(KTV_ARCHIVE);
              assertThat(properties.dailySearchBudget()).isEqualTo(80);
              assertThat(properties.dailyUnitBudget()).isEqualTo(8000);
              assertThat(properties.maxResults()).isEqualTo(25);
              assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
              assertThat(properties.retrospectiveTitlePatterns()).containsExactly(RETROSPECTIVE);
            });
  }

  @Test
  void aKeyIsStrippedAndMakesTheSourceEnabled() {
    runner
        .withPropertyValues("youtube.api-key=  " + FAKE_KEY + " ")
        .run(
            context -> {
              YoutubeProperties properties = context.getBean(YoutubeProperties.class);
              assertThat(properties.enabled()).isTrue();
              assertThat(properties.apiKey()).isEqualTo(FAKE_KEY);
            });
  }

  @Test
  void aBlankKeyIsEmpty() {
    assertThat(
            new YoutubeProperties(
                    "   ",
                    properties().apiUrl(),
                    List.of(KTV_ARCHIVE),
                    80,
                    8000,
                    25,
                    Duration.ofSeconds(2),
                    List.of())
                .enabled())
        .isFalse();
  }

  @ParameterizedTest(name = "{0}={1} fails startup")
  @CsvSource({
    "youtube.allowed-channels,UC123",
    "youtube.allowed-channels,ucAAAAAAAAAAAAAAAAAAAAAA",
    "youtube.allowed-channels,UCAAAAAAAAAAAAAAAAAAAAAAA",
    "youtube.allowed-channels,UCAAAAAAAAAAAAAAAAAAAA!!",
    "youtube.daily-search-budget,-1",
    "youtube.daily-search-budget,101",
    "youtube.daily-unit-budget,-1",
    "youtube.daily-unit-budget,10001",
    "youtube.max-results,0",
    "youtube.max-results,51",
    "youtube.connect-timeout,0s",
    "youtube.connect-timeout,-1s",
    "youtube.api-url,ftp://x",
    "youtube.api-url,/youtube/v3",
  })
  void aValueThatBreaksACheckFailsStartupWithItsName(String key, String value) {
    runner
        .withPropertyValues(key + "=" + value)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(key));
  }

  @Test
  void anInvalidRegularExpressionFailsStartupWithItsName() {
    runner
        .withPropertyValues("youtube.retrospective-title-patterns[0]=(")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("youtube.retrospective-title-patterns"));
  }

  @Test
  void anEmptyChannelListFailsWithItsName() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new YoutubeProperties(
                    "",
                    properties().apiUrl(),
                    List.of(),
                    80,
                    8000,
                    25,
                    Duration.ofSeconds(2),
                    List.of()))
        .withMessageContaining("youtube.allowed-channels");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new YoutubeProperties(
                    "",
                    properties().apiUrl(),
                    null,
                    80,
                    8000,
                    25,
                    Duration.ofSeconds(2),
                    List.of()))
        .withMessageContaining("youtube.allowed-channels");
    runner
        .withPropertyValues("youtube.allowed-channels=")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("youtube.allowed-channels"));
  }

  @Test
  void theBoundariesOfEveryRangeStart() {
    runner
        .withPropertyValues(
            "youtube.daily-search-budget=0",
            "youtube.daily-unit-budget=0",
            "youtube.max-results=1",
            "youtube.allowed-channels=UCAAAAAAAAAAAAAAAAAAAAAA,UC_-0123456789abcdefghij")
        .run(
            context -> {
              YoutubeProperties properties = context.getBean(YoutubeProperties.class);
              assertThat(properties.dailySearchBudget()).isZero();
              assertThat(properties.dailyUnitBudget()).isZero();
              assertThat(properties.maxResults()).isEqualTo(1);
              assertThat(properties.allowedChannels()).hasSize(2);
            });
    runner
        .withPropertyValues(
            "youtube.daily-search-budget=100",
            "youtube.daily-unit-budget=10000",
            "youtube.max-results=50")
        .run(
            context -> {
              YoutubeProperties properties = context.getBean(YoutubeProperties.class);
              assertThat(properties.dailySearchBudget()).isEqualTo(100);
              assertThat(properties.dailyUnitBudget()).isEqualTo(10000);
              assertThat(properties.maxResults()).isEqualTo(50);
            });
  }

  @Test
  void toStringNeverShowsTheKey() {
    YoutubeProperties withKey =
        new YoutubeProperties(
            FAKE_KEY,
            URI.create("https://example.test/youtube/v3"),
            List.of(KTV_ARCHIVE),
            80,
            8000,
            25,
            Duration.ofSeconds(2),
            List.of());

    assertThat(withKey.toString()).doesNotContain(FAKE_KEY).contains("apiKey=<set>");
    assertThat(properties().toString()).contains("apiKey=<empty>");
  }

  @Test
  void aBindingErrorNeverShowsTheKey() {
    runner
        .withPropertyValues("youtube.api-key=" + FAKE_KEY, "youtube.max-results=0")
        .run(
            context -> {
              assertThat(context).hasFailed();
              StringBuilder text = new StringBuilder();
              for (Throwable t = context.getStartupFailure(); t != null; t = t.getCause()) {
                text.append(t).append('\n');
              }
              assertThat(text.toString()).contains("youtube.max-results").doesNotContain(FAKE_KEY);
            });
  }

  private YoutubeProperties properties() {
    return new YoutubeProperties(
        "",
        URI.create("https://www.googleapis.com/youtube/v3"),
        List.of(KTV_ARCHIVE),
        80,
        8000,
        25,
        Duration.ofSeconds(2),
        List.of());
  }
}
