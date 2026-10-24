package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.VideoCandidates.youtubeVideo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaWireMockTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plan M3a4 T3 (1), (6): the running application's {@link VideoAccuracy} gets the plan's {@code
 * media.video} values from {@code application.yml} (T1 (3)), the quoted stopword "on" included, and
 * judging asks nothing of the network. Reuses the shared WireMock context; the leap day and
 * invented event text only.
 */
class VideoAccuracyContextTest extends WikimediaWireMockTest {

  private static final LocalDate LEAP_DAY = LocalDate.of(2000, 2, 29);

  @Autowired private VideoSearchProperties settings;
  @Autowired private VideoAccuracy accuracy;

  @Test
  void theApplicationContextHoldsThePlanSettings() {
    assertThat(settings.minScore()).isEqualTo(60);
    assertThat(settings.maxEvents()).isEqualTo(3);
    assertThat(settings.maxDurationSeconds()).isEqualTo(600);
    assertThat(settings.commemorationWords())
        .containsExactlyElementsOf(VideoAccuracyTest.COMMEMORATION);
    assertThat(settings.otherStageWords()).containsExactlyElementsOf(VideoAccuracyTest.OTHER_STAGE);
    assertThat(settings.stopwords()).containsExactlyElementsOf(VideoAccuracyTest.STOPWORDS);
  }

  @Test
  void theApplicationBeanDropsTheStopwordOnAndMakesNoRequest() {
    // Label words {synthetic, moon} once "on" is left out: one of two is half, so +30. Were "on"
    // read as a boolean, the words would be {synthetic, on, moon} and one of three would not match.
    EventContext event =
        new EventContext(
            "kowiki-2000-02-29-00000001",
            LEAP_DAY,
            "시험 사건",
            "Synthetic on Moon",
            "지어낸 문장이다.",
            null,
            "country:KR");
    Candidate candidate =
        new Candidate(
            youtubeVideo("CCCCCCCCCCC", 120.0, null, LEAP_DAY),
            "Moon footage",
            null,
            null,
            List.of(),
            EventLink.NONE,
            false,
            false,
            Basis.YOUTUBE_OFFICIAL);

    assertThat(accuracy.judge(event, candidate).score()).isEqualTo(30);
    assertThat(requestCount()).isZero();
    assertThat(wikimedia.findAllUnmatchedRequests()).isEmpty();
  }
}
