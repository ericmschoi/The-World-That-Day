package io.github.ericmschoi.theworldthatday.story;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * The {@code date} query parameter of the story sections (plan M3a2, C1 S1 T5). Checks the format,
 * that the date exists, that it is not after today in {@link #TODAY_ZONE} and not before {@link
 * #START_DATE}. The web's {@code dateRules} has the same two values; change both together.
 */
public final class StoryDate {

  /** "Today" is the calendar day in Canada's earliest zone (Canada research, decision 2 (a)). */
  public static final ZoneId TODAY_ZONE = ZoneId.of("America/St_Johns");

  /** The earliest supported date, for now (Canada research, decision 3 (a)). */
  public static final LocalDate START_DATE = LocalDate.of(1950, 1, 1);

  private static final Pattern FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final DateTimeFormatter STRICT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

  private StoryDate() {}

  public static LocalDate parse(String raw, Clock clock) {
    if (raw == null || raw.isBlank()) {
      throw invalid("DATE_REQUIRED", "date is required");
    }
    if (!FORMAT.matcher(raw).matches()) {
      throw invalid("DATE_FORMAT", "date must be in YYYY-MM-DD format");
    }
    LocalDate date;
    try {
      date = LocalDate.parse(raw, STRICT);
    } catch (DateTimeParseException e) {
      throw invalid(
          "DATE_NOT_EXIST", "date must be an existing calendar date in YYYY-MM-DD format");
    }
    if (date.isAfter(LocalDate.now(clock.withZone(TODAY_ZONE)))) {
      throw invalid("DATE_IN_FUTURE", "date must not be later than today in " + TODAY_ZONE);
    }
    if (date.isBefore(START_DATE)) {
      throw invalid("DATE_TOO_EARLY", "date must not be earlier than " + START_DATE);
    }
    return date;
  }

  private static ApiProblem invalid(String code, String detail) {
    return new ApiProblem(HttpStatus.BAD_REQUEST, code, detail);
  }
}
