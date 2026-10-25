package io.github.ericmschoi.theworldthatday.media.video;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface EventMediaSearchCacheRepository extends JpaRepository<EventMediaSearchCache, Long> {

  /** The rows of one source and date for the given events, with day paths off or on. */
  @Query(
      """
      SELECT c FROM EventMediaSearchCache c
      WHERE c.source = :source AND c.eventDate = :date AND c.dayPathsOff = :dayPathsOff
        AND c.eventId IN :eventIds
      """)
  List<EventMediaSearchCache> find(
      @Param("source") String source,
      @Param("date") LocalDate date,
      @Param("dayPathsOff") boolean dayPathsOff,
      @Param("eventIds") Collection<String> eventIds);

  /** Inserts the result or replaces the row of the same key, even when two race. */
  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          INSERT INTO event_media_search_cache
              (source, event_date, event_id, day_paths_off, result, fetched_at)
          VALUES (:source, :date, :eventId, :dayPathsOff, :result, :fetchedAt)
          ON CONFLICT (source, event_date, event_id, day_paths_off) DO UPDATE SET
              result = EXCLUDED.result,
              fetched_at = EXCLUDED.fetched_at
          """)
  void upsert(
      @Param("source") String source,
      @Param("date") LocalDate date,
      @Param("eventId") String eventId,
      @Param("dayPathsOff") boolean dayPathsOff,
      @Param("result") String result,
      @Param("fetchedAt") Instant fetchedAt);
}
