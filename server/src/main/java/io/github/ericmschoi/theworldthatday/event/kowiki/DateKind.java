package io.github.ericmschoi.theworldthatday.event.kowiki;

/**
 * How an event line falls on its date (plan C1 S3a, decision D2 (a)): on the day itself, or as the
 * first day of a range such as {@code [[February 13]]–[[February 28|28]]}. Days inside a range are
 * never events of that day.
 */
public enum DateKind {
  DAY("day"),
  RANGE_START("range_start");

  private final String value;

  DateKind(String value) {
    this.value = value;
  }

  /** The {@code date_kind} value of the API. */
  public String value() {
    return value;
  }
}
