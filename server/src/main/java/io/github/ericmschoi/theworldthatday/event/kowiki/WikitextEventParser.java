package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Picks the lines of one English Wikipedia {@code Events} section that fall exactly on a date and
 * turns them into plain text (plan M3a1 "파서 규칙", plan C1 S3a "파서 규칙"). The text is the original
 * sentence with only link brackets, bold and italic quotes, references, comments and file/category
 * links removed, and HTML entities written as the character they stand for (plan M3a1b).
 *
 * <p>A date page ("February 13") keys its lines by year: {@code *[[1913]] &ndash; …}, or {@code *
 * 1945 &ndash; …} without a link for a second line of the same year. A year page ("1988") keys them
 * by date: {@code * [[February 12]] – …}, or a bare {@code * [[July 17]]} whose {@code **}
 * sub-lines carry the events. A range line {@code * [[February 13]]–[[February 28|28]] – …} (also
 * {@code [[July 30]] – [[August 2]]} and {@code February 13–28}) is an event of its first day only,
 * as {@link DateKind#RANGE_START} with its last day; a last day before the first is in the next
 * year. Days inside a range, month-only lines and lines without a date are never events of the day.
 * Separators are {@code -}, {@code –}, {@code —}, {@code &ndash;} and {@code &mdash;}.
 *
 * <p>{@code skippedLines} counts dropped lines that may describe the date: lines of that date (and
 * their sub-lines) with a template, an external link, an HTML tag, an entity outside the known list
 * or an unknown format, ranges of that date whose last day cannot be read, and lines whose leading
 * year or date cannot be read at all (BC years, month-only lines, unknown formats). Lines that
 * clearly belong to another date are not counted.
 */
@Component
public class WikitextEventParser {

  /** One event line; {@code rangeEnd} is the last day of a {@link DateKind#RANGE_START} line. */
  public record ParsedEvent(
      String text, List<String> linkTitles, DateKind dateKind, LocalDate rangeEnd) {

    /** An event of the day itself. */
    public ParsedEvent(String text, List<String> linkTitles) {
      this(text, linkTitles, DateKind.DAY, null);
    }
  }

  public record Result(List<ParsedEvent> events, int skippedLines) {}

  private enum Key {
    TARGET,
    OTHER,
    UNKNOWN
  }

  /** The key of the current top line: whose date it is, and how its sub-lines are dated. */
  private record Parent(Key key, boolean usable, DateKind kind, LocalDate rangeEnd) {
    static final Parent NONE = new Parent(Key.UNKNOWN, false, DateKind.DAY, null);
  }

  private static final String MONTH =
      "(January|February|March|April|May|June|July|August|September|October|November|December)";
  private static final String DASH = "(?:-|–|—|&ndash;|&mdash;)";
  private static final List<String> MONTHS =
      List.of(
          "January",
          "February",
          "March",
          "April",
          "May",
          "June",
          "July",
          "August",
          "September",
          "October",
          "November",
          "December");

  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern REF =
      Pattern.compile(
          "<ref(?:\\s[^>]*)?(?:/>|>.*?</ref\\s*>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern TOP = Pattern.compile("\\*(?!\\*)\\s*(.*)");
  private static final Pattern SUB = Pattern.compile("\\*{2,}\\s*(.*)");
  private static final Pattern YEAR_KEY =
      Pattern.compile("\\[\\[(\\d{1,4})(?:\\|[^\\[\\]]*)?]]|(\\d{1,4})(?!\\d)");
  private static final Pattern DATE_KEY =
      Pattern.compile(
          "\\[\\[" + MONTH + " (\\d{1,2})(?:\\|[^\\[\\]]*)?]]|" + MONTH + " (\\d{1,2})(?!\\d)");
  private static final Pattern RANGE_END =
      Pattern.compile(
          "\\s*"
              + DASH
              + "\\s*(?:\\[\\["
              + MONTH
              + " (\\d{1,2})(?:\\|[^\\[\\]]*)?]]|"
              + MONTH
              + " (\\d{1,2})(?!\\d)|(\\d{1,2})(?![\\d\\]]))");
  private static final Pattern SEPARATOR = Pattern.compile("\\s+" + DASH + "\\s+");
  private static final Pattern MEDIA_LINK =
      Pattern.compile("\\[\\[\\s*(?:파일|그림|분류|File|Image|Category)\\s*:", Pattern.CASE_INSENSITIVE);
  private static final Pattern LINK = Pattern.compile("\\[\\[([^\\[\\]|]*)(?:\\|([^\\[\\]]*))?]]");
  private static final Pattern QUOTES = Pattern.compile("'{2,}");
  private static final Pattern HTML_TAG =
      Pattern.compile("</?[A-Za-z][A-Za-z0-9]*(?:\\s[^<>]*)?/?>");
  private static final Pattern ENTITY =
      Pattern.compile("&(?:#(\\d{1,7})|#[xX](\\p{XDigit}{1,6})|([A-Za-z][A-Za-z0-9]*));");

  /** Named entities that are decoded. {@code &nbsp;} becomes a plain space, as in M3a1. */
  private static final Map<String, String> NAMED_ENTITIES =
      Map.of(
          "amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ", "ndash", "–",
          "mdash", "—", "hellip", "…", "middot", "·");

  public Result parse(String wikitext, KoWikiPageKind kind, LocalDate date) {
    List<ParsedEvent> events = new ArrayList<>();
    int skipped = 0;
    if (wikitext == null) {
      return new Result(events, skipped);
    }
    String text = REF.matcher(COMMENT.matcher(wikitext).replaceAll("")).replaceAll("");
    Parent parent = Parent.NONE;
    for (String raw : text.split("\n")) {
      String line = raw.strip();
      Matcher sub = SUB.matcher(line);
      Matcher top = TOP.matcher(line);
      if (sub.matches()) {
        if (parent.key() == Key.TARGET && parent.usable()) {
          skipped += emit(sub.group(1), parent, events);
        } else if (parent.key() != Key.OTHER) {
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
        parent = Parent.NONE;
        skipped++;
        continue;
      }
      boolean target = matches(key, kind, date);
      String rest = content.substring(key.end());
      parent = new Parent(target ? Key.TARGET : Key.OTHER, false, DateKind.DAY, null);
      if (kind == KoWikiPageKind.YEAR) {
        Matcher range = RANGE_END.matcher(rest);
        if (range.lookingAt() && headingOrSeparated(rest.substring(range.end()))) {
          rest = rest.substring(range.end());
          LocalDate end = rangeEnd(range, date);
          parent =
              new Parent(
                  parent.key(), end != null && headingOrSeparated(rest), DateKind.RANGE_START, end);
          if (parent.key() == Key.TARGET && end == null) {
            skipped++; // the last day cannot be read: never guess the length of the range
            continue;
          }
        } else {
          parent = new Parent(parent.key(), headingOrSeparated(rest), DateKind.DAY, null);
        }
      } else {
        parent = new Parent(parent.key(), headingOrSeparated(rest), DateKind.DAY, null);
      }
      if (parent.key() != Key.TARGET || rest.isBlank()) {
        continue; // another date, or a heading line whose sub-lines carry the events
      }
      Matcher separator = SEPARATOR.matcher(rest);
      skipped +=
          parent.usable() && separator.lookingAt()
              ? emit(rest.substring(separator.end()), parent, events)
              : 1;
    }
    return new Result(List.copyOf(events), skipped);
  }

  private static boolean headingOrSeparated(String rest) {
    return rest.isBlank() || SEPARATOR.matcher(rest).lookingAt();
  }

  private static boolean matches(Matcher key, KoWikiPageKind kind, LocalDate date) {
    if (kind == KoWikiPageKind.DATE) {
      String year = key.group(1) != null ? key.group(1) : key.group(2);
      return Integer.parseInt(year) == date.getYear();
    }
    int group = key.group(1) != null ? 1 : 3;
    return MONTHS.indexOf(key.group(group)) + 1 == date.getMonthValue()
        && Integer.parseInt(key.group(group + 1)) == date.getDayOfMonth();
  }

  /**
   * The last day of a range starting on {@code start}'s month and day, or {@code null} when it is
   * not a real date or is the first day itself.
   */
  private static LocalDate rangeEnd(Matcher range, LocalDate start) {
    int month;
    int day;
    if (range.group(1) != null) {
      month = MONTHS.indexOf(range.group(1)) + 1;
      day = Integer.parseInt(range.group(2));
    } else if (range.group(3) != null) {
      month = MONTHS.indexOf(range.group(3)) + 1;
      day = Integer.parseInt(range.group(4));
    } else {
      month = start.getMonthValue();
      day = Integer.parseInt(range.group(5));
    }
    LocalDate end = on(start.getYear(), month, day);
    if (end != null && end.isBefore(start)) {
      end = on(start.getYear() + 1, month, day); // December 30 – January 2
    }
    return end == null || end.equals(start) ? null : end;
  }

  private static LocalDate on(int year, int month, int day) {
    try {
      return LocalDate.of(year, month, day);
    } catch (DateTimeException e) {
      return null;
    }
  }

  /** Adds the body as an event and returns 0, or returns 1 when the line has to be skipped. */
  private static int emit(String body, Parent parent, List<ParsedEvent> events) {
    if (body.contains("{{")
        || body.contains("[http")
        || HTML_TAG.matcher(body).find()
        || ENTITY.matcher(body).results().anyMatch(entity -> character(entity) == null)) {
      return 1; // never rewrite templates, external links or tags, nor guess an entity
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
          decode(target).replace('_', ' ').replaceFirst("#.*", "").replaceFirst("^:", "").strip();
      if (!title.isEmpty()) {
        links.add(title);
      }
    }
    link.appendTail(plain);
    String text = decode(QUOTES.matcher(plain).replaceAll("")).replaceAll("\\s+", " ").strip();
    if (text.isEmpty()) {
      return 1;
    }
    events.add(new ParsedEvent(text, List.copyOf(links), parent.kind(), parent.rangeEnd()));
    return 0;
  }

  /** Decodes every entity; {@link #emit} has already skipped lines with an unknown one. */
  private static String decode(String text) {
    return ENTITY.matcher(text).replaceAll(entity -> Matcher.quoteReplacement(character(entity)));
  }

  /** The character an entity stands for, or {@code null} when it is unknown or not a character. */
  private static String character(MatchResult entity) {
    if (entity.group(3) != null) {
      return NAMED_ENTITIES.get(entity.group(3));
    }
    int code =
        entity.group(1) != null
            ? Integer.parseInt(entity.group(1))
            : Integer.parseInt(entity.group(2), 16);
    boolean character =
        code > 0
            && Character.isValidCodePoint(code)
            && Character.getType(code) != Character.SURROGATE;
    return character ? Character.toString(code) : null;
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
