package io.github.ericmschoi.theworldthatday.event.tone;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import java.util.Collections;
import java.util.List;

/**
 * Every event gets the conservative verdict (rule 6 of the tone table), {@code tragic}/{@code
 * default} with scope {@code unknown}, and no source is asked. A test double, not a bean: the app
 * uses {@link CuratedEventToneJudge}; this one serves tests that only check the response mapping.
 */
public class DefaultEventToneJudge implements EventToneJudge {

  static final Verdict CONSERVATIVE = new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN);

  @Override
  public Judgement judge(List<KoWikiEvent> events) {
    return new Judgement(Collections.nCopies(events.size(), CONSERVATIVE), false);
  }
}
