package io.github.ericmschoi.theworldthatday.place;

import java.util.Optional;

/**
 * Place search for queries the region dictionary does not know (neighborhoods and spots, plan
 * M2a2). The answer is {@code FOUND} with candidates and {@code more}, {@code NOT_FOUND} or {@code
 * OUTSIDE_KOREA}; empty when search cannot be used (off, no key, failure, timeout, rate limit).
 */
public interface PlaceSearch {

  Optional<PlaceResolveResponse> search(PlaceQuery query);
}
