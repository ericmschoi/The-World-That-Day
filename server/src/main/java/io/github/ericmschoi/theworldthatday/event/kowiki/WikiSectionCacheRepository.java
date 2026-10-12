package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface WikiSectionCacheRepository extends JpaRepository<WikiSectionCache, Long> {

  Optional<WikiSectionCache> findBySiteAndPageTitle(String site, String pageTitle);

  @Transactional
  @Modifying
  @Query("DELETE FROM WikiSectionCache c WHERE c.site = :site AND c.pageTitle = :title")
  void deleteBySiteAndPageTitle(@Param("site") String site, @Param("title") String title);

  /** Inserts the page or replaces the row of the same site and title, even when two race. */
  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          INSERT INTO wiki_section_cache
              (site, page_title, revid, events_wikitext, culture_wikitext, fetched_at)
          VALUES (:site, :title, :revid, :events, :culture, :fetchedAt)
          ON CONFLICT (site, page_title) DO UPDATE SET
              revid = EXCLUDED.revid,
              events_wikitext = EXCLUDED.events_wikitext,
              culture_wikitext = EXCLUDED.culture_wikitext,
              fetched_at = EXCLUDED.fetched_at
          """)
  void upsert(
      @Param("site") String site,
      @Param("title") String title,
      @Param("revid") long revid,
      @Param("events") String events,
      @Param("culture") String culture,
      @Param("fetchedAt") Instant fetchedAt);
}
