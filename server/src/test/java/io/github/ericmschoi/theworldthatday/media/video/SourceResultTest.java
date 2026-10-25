package io.github.ericmschoi.theworldthatday.media.video;

import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.commons;
import static io.github.ericmschoi.theworldthatday.media.video.FakeVideoSource.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.SourceResult;
import io.github.ericmschoi.theworldthatday.media.video.EventVideoSource.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4b T0 (2): a {@link SourceResult} copies its candidate lists and refuses {@code null}
 * lists and candidates. Synthetic candidates on the leap day 2000-02-29.
 */
class SourceResultTest {

  private static final String E1 = "kowiki-2000-02-29-0000000a";

  private static Candidate video(long n) {
    return commons(n, "알파 행사 영상", day("2000-02-29"), EventLink.P10, Basis.COMMONS_P10);
  }

  @Test
  void theCandidateListsAreCopiedAndCannotBeChanged() {
    List<Candidate> candidates = new ArrayList<>(List.of(video(900001)));
    Map<String, List<Candidate>> byEvent = new HashMap<>(Map.of(E1, candidates));

    SourceResult result = new SourceResult(byEvent, null, Status.OK);
    candidates.add(video(900002));
    byEvent.clear();

    assertThat(result.byEvent()).containsOnlyKeys(E1);
    assertThat(result.byEvent().get(E1)).containsExactly(video(900001));
    assertThat(result.englishLabels()).isEmpty();
    assertThat(result.byEvent().get(E1)).isUnmodifiable();
  }

  @Test
  void noMapAndEmptyListsAreFine() {
    assertThat(new SourceResult(null, null, Status.OK).byEvent()).isEmpty();
    assertThat(new SourceResult(Map.of(E1, List.of()), null, Status.OK).byEvent().get(E1))
        .isEmpty();
  }

  @Test
  void aNullCandidateIsRefused() {
    Map<String, List<Candidate>> byEvent = Map.of(E1, Arrays.asList(video(900001), null));

    assertThatNullPointerException()
        .isThrownBy(() -> new SourceResult(byEvent, Map.of(), Status.OK));
  }

  @Test
  void aNullListIsRefused() {
    Map<String, List<Candidate>> byEvent = new HashMap<>();
    byEvent.put(E1, null);

    assertThatNullPointerException()
        .isThrownBy(() -> new SourceResult(byEvent, Map.of(), Status.OK));
  }

  @Test
  void aNullStatusIsRefused() {
    assertThatNullPointerException()
        .isThrownBy(() -> new SourceResult(Map.of(), Map.of(), null))
        .withMessage("status");
  }
}
