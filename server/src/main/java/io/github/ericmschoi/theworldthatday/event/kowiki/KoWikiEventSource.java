package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.LocalDate;

/**
 * A day's events from the Korean Wikipedia date page ("9월 17일") and year page ("1988년"), merged
 * (plan M3a1c). Never throws; failures show up in the document states of the result.
 */
public interface KoWikiEventSource {

  KoWikiDayResult find(LocalDate date);
}
