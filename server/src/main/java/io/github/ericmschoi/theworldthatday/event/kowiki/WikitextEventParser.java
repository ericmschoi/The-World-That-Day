package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Picks the lines of one "사건"/"문화" section that fall exactly on a date and turns them into plain
 * text (plan M3a1, "파서 규칙"). The text is the original sentence with only link brackets, bold and
 * italic quotes, references, comments and file/category links removed.
 *
 * <p>{@code skippedLines} counts dropped lines that may describe the date: lines of that date (and
 * their sub-lines) with a template, an external link or an unknown format such as a date range, and
 * lines whose leading year or date cannot be read at all (BC years, unknown formats). Lines that
 * clearly belong to another date are not counted.
 */
@Component
public class WikitextEventParser {

  public record ParsedEvent(String text, List<String> linkTitles) {}

  public record Result(List<ParsedEvent> events, int skippedLines) {}

  private enum Key {
    TARGET,
    OTHER,
    UNKNOWN
  }

  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern REF =
      Pattern.compile(
          "<ref(?:\\s[^>]*)?(?:/>|>.*?</ref\\s*>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern TOP = Pattern.compile("\\*(?!\\*)\\s*(.*)");
  private static final Pattern SUB = Pattern.compile("\\*{2,}\\s*(.*)");
  private static final Pattern YEAR_KEY = Pattern.compile("\\[\\[(\\d{1,4})년(?:\\|[^\\[\\]]*)?]]");
  private static final Pattern DATE_KEY =
      Pattern.compile("\\[\\[(\\d{1,2})월 (\\d{1,2})일(?:\\|[^\\[\\]]*)?]]|(\\d{1,2})월 (\\d{1,2})일");
  private static final Pattern SEPARATOR = Pattern.compile("\\s+[-–]\\s+");
  private static final Pattern MEDIA_LINK =
      Pattern.compile("\\[\\[\\s*(?:파일|그림|분류|File|Image|Category)\\s*:", Pattern.CASE_INSENSITIVE);
  private static final Pattern LINK = Pattern.compile("\\[\\[([^\\[\\]|]*)(?:\\|([^\\[\\]]*))?]]");
  private static final Pattern QUOTES = Pattern.compile("'{2,}");

  public Result parse(String wikitext, KoWikiPageKind kind, LocalDate date) {
    List<ParsedEvent> events = new ArrayList<>();
    int skipped = 0;
    if (wikitext == null) {
      return new Result(events, skipped);
    }
    String text = REF.matcher(COMMENT.matcher(wikitext).replaceAll("")).replaceAll("");
    Key parent = Key.UNKNOWN;
    boolean parentUsable = false;
    for (String raw : text.split("\n")) {
      String line = raw.strip();
      Matcher sub = SUB.matcher(line);
      Matcher top = TOP.matcher(line);
      if (sub.matches()) {
        if (parent == Key.TARGET && parentUsable) {
          skipped += emit(sub.group(1), events);
        } else if (parent != Key.OTHER) {
          skipped++;
        }
        continue;
      }
      if (!top.matches()) {
        continue; // headings, blank lines, prose
      }
      String content = top.group(1);
      Matcher key = (kind == KoWikiPageKind.DATE ? YEAR_KEY : DATE_KEY).matcher(content);
      if (!key.lookingAt()) {
        parent = Key.UNKNOWN;
        parentUsable = false;
        skipped++;
        continue;
      }
      parent = matches(key, kind, date) ? Key.TARGET : Key.OTHER;
      String rest = content.substring(key.end());
      Matcher separator = SEPARATOR.matcher(rest);
      parentUsable = rest.isBlank() || separator.lookingAt();
      if (parent != Key.TARGET || rest.isBlank()) {
        continue; // another date, or a heading line whose sub-lines carry the events
      }
      skipped += parentUsable ? emit(rest.substring(separator.end()), events) : 1;
    }
    return new Result(List.copyOf(events), skipped);
  }

  private static boolean matches(Matcher key, KoWikiPageKind kind, LocalDate date) {
    if (kind == KoWikiPageKind.DATE) {
      return Integer.parseInt(key.group(1)) == date.getYear();
    }
    int group = key.group(1) != null ? 1 : 3;
    return Integer.parseInt(key.group(group)) == date.getMonthValue()
        && Integer.parseInt(key.group(group + 1)) == date.getDayOfMonth();
  }

  /** Adds the body as an event and returns 0, or returns 1 when the line has to be skipped. */
  private static int emit(String body, List<ParsedEvent> events) {
    if (body.contains("{{") || body.contains("[http")) {
      return 1; // never rewrite templates or external links
    }
    Set<String> links = new LinkedHashSet<>();
    StringBuilder plain = new StringBuilder();
    Matcher link = LINK.matcher(removeMediaLinks(body));
    while (link.find()) {
      String target = link.group(1);
      String alias = link.group(2);
      String label = alias == null || alias.isBlank() ? target.replace('_', ' ') : alias;
      link.appendReplacement(plain, Matcher.quoteReplacement(label));
      String title =
          target.replace('_', ' ').replaceFirst("#.*", "").replaceFirst("^:", "").strip();
      if (!title.isEmpty()) {
        links.add(title);
      }
    }
    link.appendTail(plain);
    String text =
        QUOTES.matcher(plain).replaceAll("").replace("&nbsp;", " ").replaceAll("\\s+", " ").strip();
    if (text.isEmpty()) {
      return 1;
    }
    events.add(new ParsedEvent(text, List.copyOf(links)));
    return 0;
  }

  /** Drops file and category links, including links nested in their captions. */
  private static String removeMediaLinks(String body) {
    StringBuilder out = new StringBuilder();
    Matcher media = MEDIA_LINK.matcher(body);
    int from = 0;
    while (from < body.length() && media.find(from)) {
      out.append(body, from, media.start());
      int depth = 0;
      int i = media.start();
      while (i < body.length()) {
        if (body.startsWith("[[", i)) {
          depth++;
          i += 2;
        } else if (body.startsWith("]]", i)) {
          depth--;
          i += 2;
          if (depth == 0) {
            break;
          }
        } else {
          i++;
        }
      }
      from = i;
    }
    return out.append(body.substring(Math.min(from, body.length()))).toString();
  }
}
