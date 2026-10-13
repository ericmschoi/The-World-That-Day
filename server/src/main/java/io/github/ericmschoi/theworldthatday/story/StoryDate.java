package io.github.ericmschoi.theworldthatday.story;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import io.github.ericmschoi.theworldthatday.config.ClockConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * The {@code date} query parameter of the story sections (plan M3a2). Checks the format, that the
 * date exists and that it is not after today in Asia/Seoul. The supported start year is M2b.
 */
public final class StoryDate {

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
    if (date.isAfter(LocalDate.now(clock.withZone(ClockConfig.SERVICE_ZONE)))) {
      throw invalid("DATE_IN_FUTURE", "date must not be later than today in Asia/Seoul");
    }
    return date;
  }

  private static ApiProblem invalid(String code, String detail) {
    return new ApiProblem(HttpStatus.BAD_REQUEST, code, detail);
  }
}
