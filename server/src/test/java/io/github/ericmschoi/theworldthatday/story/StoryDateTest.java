package io.github.ericmschoi.theworldthatday.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

/** The date rules of plans M3a2 and C1 S1, with "today" fixed at 2026-10-10 in St. John's. */
class StoryDateTest {

  /** 2026-10-10 00:30 in St. John's. */
  private static final Clock TODAY =
      Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneOffset.UTC);

  private static String code(String raw, Clock clock) {
    ApiProblem problem = catchThrowableOfType(ApiProblem.class, () -> StoryDate.parse(raw, clock));
    assertThat(problem).as("rejected: %s", raw).isNotNull();
    assertThat(problem.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    return problem.code();
  }

  @ParameterizedTest
  @CsvSource({
    "1988-09-17", // demo
    "2000-02-29", // leap day of a century divisible by 400
    "2004-02-29", // ordinary leap year
    "1988-12-31", // last day of a year
    "1989-01-01", // first day of the next year
    "2026-10-10", // today is allowed
    "2026-09-10", // same day, last month
    "2025-10-10" // same day and month, last year
  })
  void acceptsExistingDatesUpToToday(String raw) {
    assertThat(StoryDate.parse(raw, TODAY)).isEqualTo(LocalDate.parse(raw));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void missingOrBlankIsRequired(String raw) {
    assertThat(code(raw, TODAY)).isEqualTo("DATE_REQUIRED");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1988-9-17",
        "88-09-17",
        "1988/09/17",
        "19880917",
        "1988-09-17T00:00",
        " 1988-09-17",
        "1988-09-17,2002-06-22",
        "１９８８-09-17", // full-width digits
        "+1988-09-17"
      })
  void otherShapesAreFormatErrors(String raw) {
    assertThat(code(raw, TODAY)).isEqualTo("DATE_FORMAT");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2001-02-29",
        "1900-02-29",
        "1988-02-30",
        "1988-04-31",
        "1988-13-01",
        "1988-00-10"
      })
  void wellFormedButNonexistentDates(String raw) {
    assertThat(code(raw, TODAY)).isEqualTo("DATE_NOT_EXIST");
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-10-11", "2026-11-10", "2027-10-10", "2028-02-29"})
  void datesAfterTodayAreInTheFuture(String raw) {
    assertThat(code(raw, TODAY)).isEqualTo("DATE_IN_FUTURE");
  }

  @ParameterizedTest
  @CsvSource({
    // St. John's is UTC-3:30 in standard time and UTC-2:30 in daylight time.
    "2026-01-15T03:30:00Z, 2026-01-15, true",
    "2026-01-15T03:29:59Z, 2026-01-15, false",
    "2026-07-15T02:30:00Z, 2026-07-15, true",
    "2026-07-15T02:29:59Z, 2026-07-15, false"
  })
  void todayIsTheStJohnsCalendarDay(Instant now, LocalDate day, boolean dayAllowed) {
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    if (dayAllowed) {
      assertThat(StoryDate.parse(day.toString(), clock)).isEqualTo(day);
    } else {
      assertThat(code(day.toString(), clock)).isEqualTo("DATE_IN_FUTURE");
    }
  }

  @Test
  void theStartDateIsAllowed() {
    assertThat(StoryDate.parse("1950-01-01", TODAY)).isEqualTo(StoryDate.START_DATE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1949-12-31", "1949-01-01", "1948-02-29", "1900-01-01", "0000-01-01"})
  void existingDatesBeforeTheStartAreTooEarly(String raw) {
    ApiProblem problem = catchThrowableOfType(ApiProblem.class, () -> StoryDate.parse(raw, TODAY));

    assertThat(problem.code()).isEqualTo("DATE_TOO_EARLY");
    assertThat(problem.getMessage()).isEqualTo("date must not be earlier than 1950-01-01");
  }

  @Test
  void aNonexistentDateBeforeTheStartIsStillNotExist() {
    assertThat(code("1900-02-29", TODAY)).isEqualTo("DATE_NOT_EXIST");
  }

  @Test
  void theFutureIsCheckedBeforeTheStart() {
    Clock in1940 = Clock.fixed(Instant.parse("1940-06-01T12:00:00Z"), ZoneOffset.UTC);

    ApiProblem problem =
        catchThrowableOfType(ApiProblem.class, () -> StoryDate.parse("1945-01-01", in1940));

    assertThat(problem.code()).isEqualTo("DATE_IN_FUTURE");
    assertThat(problem.getMessage())
        .isEqualTo("date must not be later than today in America/St_Johns");
  }
}
