package io.github.ericmschoi.theworldthatday.media.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One cached video search result of a source for one event of a date (table {@code
 * event_media_search_cache}, V3, plan M3a4c). Read-only here; written by upsert.
 */
@Entity
@Table(name = "event_media_search_cache")
class EventMediaSearchCache {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 32)
  private String source;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  @Column(name = "day_paths_off", nullable = false)
  private boolean dayPathsOff;

  @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
  @Column(nullable = false)
  private String result;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  protected EventMediaSearchCache() {}

  String source() {
    return source;
  }

  LocalDate eventDate() {
    return eventDate;
  }

  String eventId() {
    return eventId;
  }

  boolean dayPathsOff() {
    return dayPathsOff;
  }

  String result() {
    return result;
  }

  Instant fetchedAt() {
    return fetchedAt;
  }
}
