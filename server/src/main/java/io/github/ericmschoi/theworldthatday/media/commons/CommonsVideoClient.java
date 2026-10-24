package io.github.ericmschoi.theworldthatday.media.commons;

import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Files;
import io.github.ericmschoi.theworldthatday.media.commons.CommonsClient.Pages;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Wikimedia Commons video lookups (plan M3a4b "경로와 요청"), apart from the photo {@link
 * CommonsClient}. Every page comes with its {@code videoinfo}, derivatives included. Each request
 * is sent once with the given read timeout (no retry). Never throws.
 */
public interface CommonsVideoClient {

  /**
   * The file pages of {@code Category:Videos taken on <date>} (at most 20, one request). A category
   * that does not exist gives no pages and is not a failure.
   */
  Pages dateCategory(LocalDate date, Duration readTimeout);

  /**
   * The video files directly in {@code category} (its name without {@code Category:}, at most 10,
   * one search request). A category without videos or that does not exist gives no pages.
   */
  Pages category(String category, Duration readTimeout);

  /** As {@link CommonsClient#files}, with {@code videoinfo}: the {@code P10} files of events. */
  Files files(List<String> fileNames, Duration readTimeout);
}
