package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.LocalDate;

/** The two Korean Wikipedia pages that list a day's events. */
public enum KoWikiPageKind {
  /** "9월 17일": one line per year. */
  DATE,
  /** "1988년": one line per day. */
  YEAR;

  /** Page title without leading zeros, e.g. {@code 2월 29일} or {@code 2000년}. */
  public String title(LocalDate date) {
    return this == DATE
        ? date.getMonthValue() + "월 " + date.getDayOfMonth() + "일"
        : date.getYear() + "년";
  }
}
