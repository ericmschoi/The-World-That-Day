package io.github.ericmschoi.theworldthatday.event.kowiki;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One cached page (table {@code wiki_section_cache}, V2). Read-only here; written by upsert. */
@Entity
@Table(name = "wiki_section_cache")
class WikiSectionCache {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String site;

  @Column(name = "page_title", nullable = false)
  private String pageTitle;

  @Column(nullable = false)
  private long revid;

  @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
  @Column(name = "events_wikitext")
  private String eventsWikitext;

  @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
  @Column(name = "culture_wikitext")
  private String cultureWikitext;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  protected WikiSectionCache() {}

  long revid() {
    return revid;
  }

  String eventsWikitext() {
    return eventsWikitext;
  }

  String cultureWikitext() {
    return cultureWikitext;
  }

  Instant fetchedAt() {
    return fetchedAt;
  }
}
