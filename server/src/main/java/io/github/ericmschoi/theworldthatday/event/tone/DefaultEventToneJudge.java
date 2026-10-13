package io.github.ericmschoi.theworldthatday.event.tone;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Placeholder until plan M3a2 T3: every event gets the conservative verdict (rule 6 of the tone
 * table), {@code tragic}/{@code default} with scope {@code unknown}. No source is asked.
 */
@Component
public class DefaultEventToneJudge implements EventToneJudge {

  static final Verdict CONSERVATIVE = new Verdict(Tone.TRAGIC, Basis.DEFAULT, Scope.UNKNOWN);

  @Override
  public Judgement judge(List<KoWikiEvent> events) {
    return new Judgement(Collections.nCopies(events.size(), CONSERVATIVE), false);
  }
}
