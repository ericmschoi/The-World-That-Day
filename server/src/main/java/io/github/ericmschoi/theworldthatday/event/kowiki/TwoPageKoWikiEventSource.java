package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import io.github.ericmschoi.theworldthatday.event.kowiki.WikitextEventParser.ParsedEvent;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaHttpClient;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads the two pages of a date, always chosen with {@link KoWikiPageKind#title} and parsed for the
 * requested date itself (the parser only checks the year on a date page and the day on a year page,
 * plan M3a1c invariant). Pages are read in series, then the link lookup.
 *
 * <p>Everything for one date fits in {@code wikimedia.kowiki-day-deadline}: each step runs on a
 * virtual thread and is awaited for the time left only, so neither a stalled Wikipedia host nor a
 * stalled database can hold the caller longer. If the date page failed with a timeout (or the
 * deadline), the year page is not requested and only its cached copy is used. Lines describing the
 * same event are then merged by {@link KoWikiEventDeduplicator}.
 */
@Component
public class TwoPageKoWikiEventSource implements KoWikiEventSource {

  private static final Logger log = LoggerFactory.getLogger(TwoPageKoWikiEventSource.class);

  private record Line(KoWikiPage page, String section, int order, ParsedEvent event) {}

  private final KoWikiClient client;
  private final KoWikiLinkResolver resolver;
  private final WikitextEventParser parser;
  private final WikimediaHttpClient http;
  private final WikimediaProperties properties;
  private final KoWikiEventDeduplicator deduplicator;

  public TwoPageKoWikiEventSource(
      KoWikiClient client,
      KoWikiLinkResolver resolver,
      WikitextEventParser parser,
      WikimediaHttpClient http,
      WikimediaProperties properties,
      KoWikiEventDeduplicator deduplicator) {
    this.client = client;
    this.resolver = resolver;
    this.parser = parser;
    this.http = http;
    this.properties = properties;
    this.deduplicator = deduplicator;
  }

  @Override
  public KoWikiDayResult find(LocalDate date) {
    long deadline = System.nanoTime() + properties.kowikiDayDeadline().toNanos();
    String dateTitle = KoWikiPageKind.DATE.title(date);
    String yearTitle = KoWikiPageKind.YEAR.title(date);
    long timeouts = http.timeouts(properties.kowikiApiUrl());
    KoWikiPage datePage = within(deadline, dateTitle, () -> client.fetch(dateTitle));
    boolean stalled =
        datePage == null
            || (failed(datePage) && http.timeouts(properties.kowikiApiUrl()) > timeouts);
    KoWikiPage yearPage =
        within(
            deadline,
            yearTitle,
            () -> stalled ? client.cached(yearTitle) : client.fetch(yearTitle));
    List<KoWikiPage> pages =
        List.of(
            datePage != null ? datePage : KoWikiPage.unavailable(dateTitle),
            yearPage != null ? yearPage : KoWikiPage.unavailable(yearTitle));

    List<Line> lines = new ArrayList<>();
    int skipped = 0;
    for (KoWikiPageKind kind : KoWikiPageKind.values()) {
      KoWikiPage page = pages.get(kind.ordinal());
      for (String section : List.of(ActionApiKoWikiClient.EVENTS, ActionApiKoWikiClient.CULTURE)) {
        String wikitext =
            section.equals(ActionApiKoWikiClient.EVENTS)
                ? page.eventsWikitext()
                : page.cultureWikitext();
        WikitextEventParser.Result parsed = parser.parse(wikitext, kind, date);
        skipped += parsed.skippedLines();
        int order = kind.ordinal() * 2 + (section.equals(ActionApiKoWikiClient.EVENTS) ? 0 : 1);
        parsed.events().forEach(event -> lines.add(new Line(page, section, order, event)));
      }
    }

    List<String> titles =
        lines.stream().flatMap(line -> normalized(line.event()).stream()).distinct().toList();
    Optional<Map<String, Link>> links =
        titles.isEmpty()
            ? Optional.of(Map.of())
            : Optional.ofNullable(within(deadline, "pageprops", () -> resolver.resolve(titles)))
                .flatMap(resolved -> resolved);
    Map<String, Link> resolved = links.orElse(Map.of());
    KoWikiEventDeduplicator.Result deduplicated =
        deduplicator.dedup(lines.stream().map(line -> candidate(date, line, resolved)).toList());
    List<KoWikiDocStatus> docs =
        pages.stream()
            .map(p -> new KoWikiDocStatus(p.title(), p.revision(), p.fetchedAt(), p.state()))
            .toList();
    return new KoWikiDayResult(
        date, deduplicated.events(), docs, skipped, deduplicated.mergedCount(), links.isEmpty());
  }

  /** The event with canonical links (unresolved links keep their normalized title, no QID). */
  private static KoWikiEventDeduplicator.Candidate candidate(
      LocalDate date, Line line, Map<String, Link> links) {
    Map<String, Link> canonical = new LinkedHashMap<>();
    for (String title : normalized(line.event())) {
      Link link = links.getOrDefault(title, new Link(title, null));
      canonical.putIfAbsent(link.title(), link);
    }
    List<Link> resolved = List.copyOf(canonical.values());
    KoWikiPage page = line.page();
    KoWikiEvent event =
        new KoWikiEvent(
            id(date, page.title(), line.event().text()),
            date,
            page.title(),
            line.section(),
            page.revision(),
            line.event().text(),
            resolved.stream().map(Link::title).toList(),
            resolved.isEmpty() ? null : resolved.getFirst().wikidataQid(),
            page.fetchedAt());
    return new KoWikiEventDeduplicator.Candidate(event, line.order(), resolved);
  }

  private static List<String> normalized(ParsedEvent event) {
    return event.linkTitles().stream()
        .map(KoWikiLinkResolver::normalize)
        .filter(title -> !title.isEmpty())
        .distinct()
        .toList();
  }

  /** {@code kowiki-<date>-<first 8 hex digits of SHA-256(page title + "\n" + text)>}. */
  static String id(LocalDate date, String pageTitle, String text) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest((pageTitle + "\n" + text).getBytes(StandardCharsets.UTF_8));
      return "kowiki-" + date + "-" + HexFormat.of().formatHex(hash, 0, 4);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every Java platform", e);
    }
  }

  private static boolean failed(KoWikiPage page) {
    return page.state() == State.UNAVAILABLE || page.state() == State.STALE;
  }

  /** The task's result, or {@code null} when the deadline passes first (the task is cancelled). */
  private static <T> T within(long deadline, String label, Callable<T> task) {
    FutureTask<T> future = new FutureTask<>(task);
    Thread.ofVirtual().name("kowiki-day").start(future);
    try {
      return future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
    } catch (TimeoutException e) {
      log.warn("Wikimedia call failed: source=kowiki title={} cause=day deadline", label);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      log.warn(
          "Wikimedia call failed: source=kowiki title={} cause={}",
          label,
          e.getCause().getClass().getSimpleName());
    } finally {
      future.cancel(true);
    }
    return null;
  }
}
