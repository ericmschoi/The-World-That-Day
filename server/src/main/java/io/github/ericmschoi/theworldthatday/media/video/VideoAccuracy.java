package io.github.ericmschoi.theworldthatday.media.video;

import io.github.ericmschoi.theworldthatday.media.CaptureTime;
import io.github.ericmschoi.theworldthatday.media.DateRelation;
import io.github.ericmschoi.theworldthatday.media.MediaRecord;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;
import io.github.ericmschoi.theworldthatday.media.MediaRecord.LicenseStatus;
import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import io.github.ericmschoi.theworldthatday.media.TextDates;
import io.github.ericmschoi.theworldthatday.media.video.Candidate.EventLink;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The accuracy rules of plan M3a4 (research rules R-T1 to R-T4): scores a candidate video for an
 * event and says why it is rejected, if it is. When unsure, a video is not attached. Pure: the same
 * input gives the same {@link Judgement}, nothing is logged, the candidate is not changed and no
 * {@code MediaItem} is built, so an event date never becomes a {@code captured_at}.
 */
@Component
public class VideoAccuracy {

  /** Why a candidate is not attached, in the order the required rules are checked. */
  public enum Reject {
    NO_MATCH,
    BASIS_RULE,
    DURATION,
    LOW_SCORE,
    LATE_CAPTURE
  }

  /** The score, and the reason it is rejected ({@code null} when it may be attached). */
  public record Judgement(int score, Reject reject) {
    public boolean accepted() {
      return reject == null;
    }
  }

  /** Punctuation splits words, except the apostrophe: "Queen's" stays one word (PM decision 3). */
  private static final Pattern PUNCTUATION = Pattern.compile("[\\p{P}&&[^'\\u2019]]+");

  private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");
  private static final Pattern YEAR_WORD = Pattern.compile("[12]\\d{3}년?");
  private static final Pattern TITLE_YEAR = Pattern.compile("(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)");
  private static final Pattern HANGUL = Pattern.compile("\\p{IsHangul}");

  private final VideoSearchProperties properties;
  private final List<Listed> commemoration;
  private final List<Listed> otherStage;
  private final Set<String> stopwords;

  public VideoAccuracy(VideoSearchProperties properties) {
    this.properties = properties;
    this.commemoration = properties.commemorationWords().stream().map(Listed::of).toList();
    this.otherStage = properties.otherStageWords().stream().map(Listed::of).toList();
    this.stopwords =
        properties.stopwords().stream()
            .map(w -> w.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
  }

  /** The judging of one candidate for one event. */
  @FunctionalInterface
  public interface EventJudge {
    Judgement judge(Candidate candidate);
  }

  public Judgement judge(EventContext event, Candidate candidate) {
    return forEvent(event).judge(candidate);
  }

  /**
   * The judging of {@code event}'s candidates: its keywords, their word patterns and the
   * other-stage words its own text does not hold are prepared once here, not for each candidate.
   */
  public EventJudge forEvent(EventContext event) {
    return new Prepared(event);
  }

  private final class Prepared implements EventJudge {

    private final LocalDate day;
    private final List<String> titleKeys;
    private final List<String> labelKeys;
    private final Map<String, Pattern> labelWords = new HashMap<>();
    private final List<Listed> otherStageOnly;

    Prepared(EventContext event) {
      day = event.date();
      titleKeys = keywords(event.title());
      labelKeys = keywords(event.englishLabel());
      labelKeys.forEach(key -> labelWords.computeIfAbsent(key, VideoAccuracy::wholeWord));
      Text eventText = Text.of(join(event.title(), event.summary()));
      otherStageOnly = otherStage.stream().filter(w -> !w.in(eventText)).toList();
    }

    @Override
    public Judgement judge(Candidate candidate) {
      MediaRecord record = candidate.item().record();
      Text text = Text.of(apostrophes(join(candidate.title(), candidate.description())));
      int date = dateScore(day, candidate);
      boolean words =
          half(titleKeys, key -> numberSafe(text.compact(), key))
              || half(labelKeys, key -> labelWords.get(key).matcher(text.plain()).find());

      int score = date;
      score += candidate.eventLink() != EventLink.NONE ? 50 : 0;
      score += words ? 30 : 0;
      score += candidate.official() ? 20 : 0;
      score += record.licenseStatus() == LicenseStatus.FREE ? 10 : 0;
      score -= otherYear(candidate.title(), day.getYear()) ? 40 : 0;
      score -= commemoration.stream().anyMatch(w -> w.in(text)) ? 30 : 0;
      score -= otherStageOnly.stream().anyMatch(w -> w.in(text)) ? 40 : 0;

      Double duration = record.durationSeconds();
      Reject reject = null;
      if (date == 0 && !words) {
        reject = Reject.NO_MATCH;
      } else if ((candidate.basis() == Basis.COMMONS_DATE_CATEGORY && !words)
          || (candidate.basis() == Basis.COMMONS_SEARCH && !(date == 40 && words))) {
        reject = Reject.BASIS_RULE;
      } else if (duration == null || duration > properties.maxDurationSeconds()) {
        reject = Reject.DURATION;
      } else if (score < properties.minScore()) {
        reject = Reject.LOW_SCORE;
      } else if (late(new CaptureTime(record.capturedAt(), record.datePrecision()), day)) {
        reject = Reject.LATE_CAPTURE;
      }
      return new Judgement(score, reject);
    }
  }

  /**
   * {@code capture}, or {@link CaptureTime#UNKNOWN} when it is later than {@code eventDate} at its
   * precision ({@code day}, {@code month} or {@code year}); the same day, month or year is kept, as
   * are {@code approx}, {@code current} and unknown times. For adapters (research rule R-V3).
   */
  public static CaptureTime notAfter(CaptureTime capture, LocalDate eventDate) {
    return late(capture, eventDate) ? CaptureTime.UNKNOWN : capture;
  }

  private static boolean late(CaptureTime capture, LocalDate day) {
    if (capture == null || capture.datePrecision() == null) {
      return false;
    }
    String at = capture.capturedAt();
    return switch (capture.datePrecision()) {
      case DAY -> LocalDate.parse(at).isAfter(day);
      case MONTH -> YearMonth.parse(at).isAfter(YearMonth.from(day));
      case YEAR -> Integer.parseInt(at) > day.getYear();
      case APPROX, CURRENT -> false;
    };
  }

  /**
   * The best of the dates written in the title and description (unless the source excludes them),
   * the source's capture date (unless later than the event) and its stated dates.
   */
  private static int dateScore(LocalDate day, Candidate candidate) {
    int best = late(candidate.originalCapture(), day) ? 0 : score(candidate.originalCapture(), day);
    for (LocalDate stated : candidate.explicitDates()) {
      best = Math.max(best, score(new CaptureTime(stated.toString(), DatePrecision.DAY), day));
    }
    if (!candidate.dateTextExcluded()) {
      List<CaptureTime> written =
          Stream.of(candidate.title(), candidate.description())
              .filter(Objects::nonNull)
              .flatMap(t -> TextDates.find(t).stream())
              .toList();
      for (CaptureTime time : written) {
        best = Math.max(best, score(time, day));
      }
    }
    return best;
  }

  private static int score(CaptureTime time, LocalDate day) {
    return switch (DateRelation.of(time.capturedAt(), time.datePrecision(), day).kind()) {
      case SAME_DAY -> 40;
      case SAME_MONTH -> 20;
      case SAME_YEAR -> 10;
      default -> 0;
    };
  }

  /**
   * Lowercase words of a title, split at spaces and punctuation other than the apostrophe, without
   * years and stopwords.
   */
  private List<String> keywords(String title) {
    String plain = PUNCTUATION.matcher(apostrophes(title == null ? "" : title)).replaceAll(" ");
    return Arrays.stream(SPACES.split(plain.toLowerCase(Locale.ROOT)))
        .filter(w -> !w.isEmpty() && !YEAR_WORD.matcher(w).matches() && !stopwords.contains(w))
        .toList();
  }

  /**
   * At least half of the keywords are found, and every one with a digit. Korean title keywords are
   * looked for in the text without spaces, English label keywords as whole words ignoring case.
   */
  private static boolean half(List<String> keys, Predicate<String> found) {
    int hits = 0;
    for (String key : keys) {
      if (found.test(key)) {
        hits++;
      } else if (key.chars().anyMatch(Character::isDigit)) {
        return false;
      }
    }
    return !keys.isEmpty() && hits * 2 >= keys.size();
  }

  /** {@code key} occurs with no other digit right before a leading or after a trailing digit. */
  private static boolean numberSafe(String text, String key) {
    for (int i = text.indexOf(key); i >= 0; i = text.indexOf(key, i + 1)) {
      int end = i + key.length();
      boolean before = !Character.isDigit(key.charAt(0)) || i == 0 || !isDigit(text, i - 1);
      boolean after =
          !Character.isDigit(key.charAt(key.length() - 1))
              || end == text.length()
              || !isDigit(text, end);
      if (before && after) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDigit(String text, int index) {
    return Character.isDigit(text.charAt(index));
  }

  /** A four-digit year from 1900 to 2099 in the title other than the event's year. */
  private static boolean otherYear(String title, int year) {
    Matcher m = TITLE_YEAR.matcher(title == null ? "" : title);
    while (m.find()) {
      if (Integer.parseInt(m.group(1)) != year) {
        return true;
      }
    }
    return false;
  }

  /** A text as written and without spaces in lowercase, made once for each text. */
  private record Text(String plain, String compact) {
    static Text of(String plain) {
      return new Text(plain, VideoAccuracy.compact(plain));
    }
  }

  /** A listed word: with Hangul, anywhere ignoring every space; otherwise as a whole word. */
  private record Listed(String needle, Pattern pattern) {
    static Listed of(String word) {
      return HANGUL.matcher(word).find()
          ? new Listed(compact(word), null)
          : new Listed(null, wholeWord(word));
    }

    boolean in(Text text) {
      return pattern == null
          ? text.compact().contains(needle)
          : pattern.matcher(text.plain()).find();
    }
  }

  private static Pattern wholeWord(String word) {
    return Pattern.compile(
        "(?<![\\p{L}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private static String compact(String text) {
    return SPACES.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
  }

  /** The typographic apostrophe U+2019 read as {@code '}: both write the same apostrophe. */
  private static String apostrophes(String text) {
    return text.replace('\u2019', '\'');
  }

  private static String join(String first, String second) {
    return (first == null ? "" : first) + " " + (second == null ? "" : second);
  }
}
