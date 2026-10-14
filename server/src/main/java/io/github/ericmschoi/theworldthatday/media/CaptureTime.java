package io.github.ericmschoi.theworldthatday.media;

import io.github.ericmschoi.theworldthatday.media.MediaRecord.DatePrecision;

/**
 * A capture time read by {@link CaptureTimeParser}: the {@code captured_at} and {@code
 * date_precision} values of a {@link MediaRecord}. Both are {@code null} when the time is unknown
 * ("촬영 시점 미상"). The shape of {@code capturedAt} always follows {@code datePrecision} (rule R8).
 */
public record CaptureTime(String capturedAt, DatePrecision datePrecision) {

  /** The capture time is unknown: both values are {@code null}. */
  public static final CaptureTime UNKNOWN = new CaptureTime(null, null);

  public boolean isKnown() {
    return datePrecision != null;
  }
}
