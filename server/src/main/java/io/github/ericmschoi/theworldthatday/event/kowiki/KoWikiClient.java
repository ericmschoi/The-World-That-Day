package io.github.ericmschoi.theworldthatday.event.kowiki;

/** Reads the "사건" and "문화" sections of a Korean Wikipedia page. Never throws. */
public interface KoWikiClient {

  KoWikiPage fetch(String title);
}
