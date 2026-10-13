package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Merges lines that describe the same event across sections and pages (plan M3a1c D1~D6). Lines are
 * taken in input order; a line joins the one earlier group it matches, and the first line of a
 * group is the one kept, unchanged (D5). Lines of the same section never merge (D1), except lines
 * with the same event ID: identical text on one page is one line written twice, and IDs must stay
 * unique. A line that matches two or more groups stays on its own (D4): showing an event twice is a
 * smaller failure than losing one. Debug logs carry the rule and the hash part of the event IDs
 * only, never text or dates (D6).
 */
@Component
public class KoWikiEventDeduplicator {

  /** An event with its section in input order (0 date 사건, 1 문화, 2 year 사건, 3 문화). */
  public record Candidate(KoWikiEvent event, int section, List<Link> links) {}

  public record Result(List<KoWikiEvent> events, int mergedCount) {}

  static final String GENERIC_LINKS = "kowiki-generic-links.txt";

  private static final Logger log = LoggerFactory.getLogger(KoWikiEventDeduplicator.class);
  private static final Pattern PAGE_LINK = Pattern.compile("\\d+년|\\d+월 \\d+일");
  private static final Pattern NOISE = Pattern.compile("[\\s\\p{P}]");

  private final double minLinkOverlap;
  private final Set<String> generic;

  public KoWikiEventDeduplicator(KoWikiDedupProperties properties) {
    this.minLinkOverlap = properties.minLinkOverlap();
    this.generic = loadGenericLinks();
  }

  public Result dedup(List<Candidate> candidates) {
    List<List<Candidate>> groups = new ArrayList<>();
    for (Candidate next : candidates) {
      List<Candidate> sameId =
          groups.stream()
              .filter(g -> g.stream().anyMatch(m -> m.event().id().equals(next.event().id())))
              .findFirst()
              .orElse(null);
      if (sameId != null) {
        join(sameId, next, "D2");
        continue;
      }
      List<List<Candidate>> matches = new ArrayList<>();
      String rule = null;
      for (List<Candidate> group : groups) {
        if (group.stream().anyMatch(member -> member.section() == next.section())) {
          continue; // D1
        }
        for (Candidate member : group) {
          String matched = rule(member, next);
          if (matched != null) {
            matches.add(group);
            rule = matched;
            break;
          }
        }
      }
      if (matches.size() == 1) {
        join(matches.getFirst(), next, rule);
      } else {
        if (matches.size() > 1) {
          log.debug("Wiki event not merged: rule=D4 event={}", hash(next));
        }
        groups.add(new ArrayList<>(List.of(next)));
      }
    }
    List<KoWikiEvent> kept = groups.stream().map(group -> group.getFirst().event()).toList();
    return new Result(kept, candidates.size() - groups.size());
  }

  private static void join(List<Candidate> group, Candidate next, String rule) {
    log.debug(
        "Wiki event merged: rule={} kept={} removed={}", rule, hash(group.getFirst()), hash(next));
    group.add(next);
  }

  /** D2 or D3 when the two lines describe the same event, otherwise {@code null}. */
  private String rule(Candidate a, Candidate b) {
    if (textKey(a).equals(textKey(b))) {
      return "D2";
    }
    List<Link> left = comparable(a.links());
    List<Link> right = comparable(b.links());
    if (left.isEmpty() || right.isEmpty() || !(sharesMain(a, right) || sharesMain(b, left))) {
      return null;
    }
    List<Link> smaller = left.size() <= right.size() ? left : right;
    List<Link> larger = smaller == left ? right : left;
    long shared =
        smaller.stream().filter(l -> larger.stream().anyMatch(o -> sameLink(l, o))).count();
    return (double) shared / smaller.size() >= minLinkOverlap ? "D3" : null;
  }

  /**
   * The main link of {@code event} is specific (not generic, not a page link) and in {@code set}.
   */
  private boolean sharesMain(Candidate event, List<Link> set) {
    if (event.links().isEmpty()) {
      return false;
    }
    Link main = event.links().getFirst();
    return !isPageLink(main)
        && !generic.contains(main.title())
        && set.stream().anyMatch(link -> sameLink(main, link));
  }

  /** Same QID, same resolved title, or same title once a leading "2002년 " reads "2002 ". */
  static boolean sameLink(Link a, Link b) {
    return (a.wikidataQid() != null && a.wikidataQid().equals(b.wikidataQid()))
        || a.title().equals(b.title())
        || titleKey(a.title()).equals(titleKey(b.title()));
  }

  static String titleKey(String title) {
    return title.replaceFirst("^(\\d{4})년 ", "$1 ");
  }

  private static List<Link> comparable(List<Link> links) {
    return links.stream().filter(link -> !isPageLink(link)).toList();
  }

  private static boolean isPageLink(Link link) {
    return PAGE_LINK.matcher(link.title()).matches();
  }

  private static String textKey(Candidate candidate) {
    return NOISE.matcher(candidate.event().text()).replaceAll("");
  }

  /** The hash part of the event ID: the full ID holds the date, which logs must not show. */
  private static String hash(Candidate candidate) {
    String id = candidate.event().id();
    return id.substring(id.lastIndexOf('-') + 1);
  }

  private static Set<String> loadGenericLinks() {
    try (InputStream in =
        KoWikiEventDeduplicator.class.getClassLoader().getResourceAsStream(GENERIC_LINKS)) {
      if (in == null) {
        throw new IllegalStateException("missing resource " + GENERIC_LINKS);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .lines()
          .map(String::strip)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .collect(Collectors.toUnmodifiableSet());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The generic links, for tests and the T2 report. */
  Set<String> genericLinks() {
    return generic;
  }
}
