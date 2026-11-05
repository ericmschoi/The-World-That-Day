package io.github.ericmschoi.theworldthatday.event.tone;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiEvent;
import io.github.ericmschoi.theworldthatday.event.tone.EventCuration.Entry;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource;
import io.github.ericmschoi.theworldthatday.event.wikidata.WikidataSignalSource.Signals;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The tone table of plan M3a2 (decision #22 (c)), first match wins: curation, a tragic keyword in
 * the summary, then the Wikidata signals of the main link (class, deaths, none), and {@code
 * tragic}/{@code default} when nothing could be evaluated. Scope: curation, then P17 (Canada Q16 →
 * domestic, other countries only → world), else unknown. One Wikidata request per call, for every
 * QID of the events that are not hidden. A matching takedown entry hides the event even when
 * another entry gives it a tone.
 */
@Component
public class CuratedEventToneJudge implements EventToneJudge {

  static final String CANADA = "Q16";
  private static final String KEYWORDS = "tone-keywords-en.txt";

  private final EventCuration curation;
  private final WikidataSignalSource wikidata;
  private final Pattern keywords;

  public CuratedEventToneJudge(EventCuration curation, WikidataSignalSource wikidata) {
    this.curation = curation;
    this.wikidata = wikidata;
    this.keywords = loadKeywords();
  }

  @Override
  public Judgement judge(List<KoWikiEvent> events) {
    List<Optional<Entry>> entries = events.stream().map(curation::match).toList();
    List<Boolean> hidden = events.stream().map(curation::hidden).toList();
    List<String> qids = new ArrayList<>();
    for (int i = 0; i < events.size(); i++) {
      if (!hidden.get(i)) {
        qids.add(events.get(i).wikidataQid());
      }
    }
    Optional<Map<String, Signals>> signals =
        wikidata.signals(qids.stream().filter(Objects::nonNull).distinct().toList());
    List<Verdict> verdicts = new ArrayList<>();
    for (int i = 0; i < events.size(); i++) {
      verdicts.add(verdict(events.get(i), entries.get(i), hidden.get(i), signals));
    }
    return new Judgement(verdicts, signals.isEmpty());
  }

  private Verdict verdict(
      KoWikiEvent event,
      Optional<Entry> entry,
      boolean hidden,
      Optional<Map<String, Signals>> all) {
    // Only queried items are in the answer; an ID that was never sent (malformed) has no signals.
    Optional<Signals> signals =
        Optional.ofNullable(event.wikidataQid())
            .flatMap(qid -> all.flatMap(map -> Optional.ofNullable(map.get(qid))));
    Scope scope =
        entry
            .flatMap(Entry::scope)
            .orElseGet(
                () ->
                    signals
                        .filter(s -> !s.countries().isEmpty())
                        .map(s -> s.countries().contains(CANADA) ? Scope.DOMESTIC : Scope.WORLD)
                        .orElse(Scope.UNKNOWN));
    Optional<Tone> curated = entry.flatMap(Entry::tone);
    if (curated.isPresent()) {
      return new Verdict(curated.get(), Basis.CURATED, scope, hidden);
    }
    if (keywords.matcher(event.text()).find()) {
      return new Verdict(Tone.TRAGIC, Basis.RULE_KEYWORD, scope, hidden);
    }
    if (signals.isEmpty()) {
      return new Verdict(Tone.TRAGIC, Basis.DEFAULT, scope, hidden);
    }
    if (signals.get().tragicClass()) {
      return new Verdict(Tone.TRAGIC, Basis.RULE_CLASS, scope, hidden);
    }
    if (signals.get().deaths()) {
      return new Verdict(Tone.TRAGIC, Basis.RULE_DEATHS, scope, hidden);
    }
    return new Verdict(Tone.NORMAL, Basis.RULE_NO_SIGNAL, scope, hidden);
  }

  /**
   * One pattern for every keyword, matched as a whole word in any letter case (plan C1 S3a): {@code
   * Killed} matches {@code killed}, {@code skilled} does not match {@code kill}.
   */
  private static Pattern loadKeywords() {
    try (InputStream in =
        CuratedEventToneJudge.class.getClassLoader().getResourceAsStream(KEYWORDS)) {
      if (in == null) {
        throw new IllegalStateException("missing resource " + KEYWORDS);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .lines()
          .map(String::strip)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .map(Pattern::quote)
          .collect(
              Collectors.collectingAndThen(
                  Collectors.joining("|", "(?<![\\p{L}\\p{N}])(?:", ")(?![\\p{L}\\p{N}])"),
                  words ->
                      Pattern.compile(words, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
