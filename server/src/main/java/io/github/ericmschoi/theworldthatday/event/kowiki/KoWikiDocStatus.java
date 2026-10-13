package io.github.ericmschoi.theworldthatday.event.kowiki;

import java.time.Instant;

/** One page of a day: its title, revision and read time (both null without content), and state. */
public record KoWikiDocStatus(
    String title, Long revision, Instant retrievedAt, KoWikiPage.State state) {}
