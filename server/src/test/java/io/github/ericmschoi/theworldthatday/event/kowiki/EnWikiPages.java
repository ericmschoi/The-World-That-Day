package io.github.ericmschoi.theworldthatday.event.kowiki;

import io.github.ericmschoi.theworldthatday.wikimedia.WikiPageStubs;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The English Wikipedia pages the event tests stub (plan C1 S3a): the ten recorded demo pages of
 * {@code fixtures/wikimedia/enwiki/README.md} and the synthetic pages of {@code enwiki/synthetic/},
 * by title. The synthetic pages keep the dates of the former Korean demo days (public event dates,
 * M3-P13) with neutral English test sentences.
 */
final class EnWikiPages {

  /** Recorded pages by title (plan decision 1 (b) dates and the video sample 1969-07-20). */
  static final Map<String, String> RECORDED = new LinkedHashMap<>();

  /** Synthetic pages by title. */
  static final Map<String, String> SYNTHETIC = new LinkedHashMap<>();

  static {
    RECORDED.put("February 13", "02-13");
    RECORDED.put("July 17", "07-17");
    RECORDED.put("February 12", "02-12");
    RECORDED.put("April 12", "04-12");
    RECORDED.put("July 20", "07-20");
    RECORDED.put("1988", "1988");
    RECORDED.put("1976", "1976");
    RECORDED.put("2010", "2010");
    RECORDED.put("1980", "1980");
    RECORDED.put("1969", "1969");
    SYNTHETIC.put("September 17", "synthetic/synthetic-09-17");
    SYNTHETIC.put("June 22", "synthetic/synthetic-06-22");
    SYNTHETIC.put("March 15", "synthetic/synthetic-03-15");
    SYNTHETIC.put("February 29", "synthetic/synthetic-02-29");
    SYNTHETIC.put("2000", "synthetic/synthetic-2000");
  }

  private EnWikiPages() {}

  /** The revision of a page's section list fixture. */
  static long revid(String prefix) {
    return WikiPageStubs.revid(prefix);
  }

  /** The index of a page's level-2 {@code Events} section. */
  static String eventsIndex(String prefix) {
    return WikiPageStubs.eventsIndex(KoWikiFixtures.read(prefix + "-tocdata.json"));
  }
}
