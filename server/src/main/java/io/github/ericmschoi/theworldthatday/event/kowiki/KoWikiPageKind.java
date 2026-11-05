package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * The two English Wikipedia pages that list a day's events (plan C1 S3a; the Korean names stay
 * until S3c T4).
 */
public enum KoWikiPageKind {
  /** "February 13": one line per year. */
  DATE,
  /** "1988": one line per day. */
  YEAR;

  /** Page title without leading zeros, e.g. {@code February 29} or {@code 2000}. */
  public String title(LocalDate date) {
    return this == DATE
        ? date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " "
            + date.getDayOfMonth()
        : Integer.toString(date.getYear());
  }
}
