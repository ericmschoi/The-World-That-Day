package io.github.ericmschoi.theworldthatday.event.tone;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import java.util.List;

/**
 * Decides {@code tone} and {@code scope} of a day's events (plan M3a2, decision #22 (c)). The
 * verdict only separates events on screen; it does not interpret them.
 */
public interface EventToneJudge {

  /** One verdict per event, in the same order, and whether a supporting source failed. */
  Judgement judge(List<KoWikiEvent> events);

  /** {@code hidden}: a curated takedown; the event is left out of the response. */
  record Verdict(Tone tone, Basis basis, Scope scope, boolean hidden) {

    public Verdict(Tone tone, Basis basis, Scope scope) {
      this(tone, basis, scope, false);
    }
  }

  record Judgement(List<Verdict> verdicts, boolean partial) {

    public Judgement {
      verdicts = List.copyOf(verdicts);
    }
  }

  enum Tone {
    NORMAL("normal"),
    TRAGIC("tragic");

    private final String value;

    Tone(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum Basis {
    CURATED("curated"),
    RULE_KEYWORD("rule:keyword"),
    RULE_CLASS("rule:class"),
    RULE_DEATHS("rule:deaths"),
    RULE_NO_SIGNAL("rule:no-signal"),
    DEFAULT("default");

    private final String value;

    Basis(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum Scope {
    DOMESTIC("domestic"),
    WORLD("world"),
    UNKNOWN("unknown");

    private final String value;

    Scope(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}
