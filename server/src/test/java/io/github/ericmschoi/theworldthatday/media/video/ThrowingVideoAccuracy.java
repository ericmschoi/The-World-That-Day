package io.github.ericmschoi.theworldthatday.media.video;

import java.util.List;

/**
 * The plan's accuracy rules, except that judging throws (plan M3a4b T0 (1)): for a candidate titled
 * {@link #BOOM}, and already when preparing an event whose English label is {@link #BOOM}.
 */
public final class ThrowingVideoAccuracy extends VideoAccuracy {

  public static final String BOOM = "boom";

  public ThrowingVideoAccuracy(int maxEvents) {
    super(FakeVideoSource.properties(maxEvents));
  }

  @Override
  public EventJudge forEvent(EventContext event) {
    if (BOOM.equals(event.englishLabel())) {
      throw new IllegalStateException("synthetic");
    }
    EventJudge judge = super.forEvent(event);
    return candidate -> {
      if (BOOM.equals(candidate.title())) {
        throw new IllegalStateException("synthetic");
      }
      return judge.judge(candidate);
    };
  }

  /** A search over {@code sources} with the plan's settings and this throwing judge. */
  public static VideoSearch search(int maxEvents, EventVideoSource... sources) {
    return new VideoSearch(
        List.of(sources),
        new ThrowingVideoAccuracy(maxEvents),
        FakeVideoSource.properties(maxEvents));
  }
}
