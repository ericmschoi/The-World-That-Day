package io.github.ericmschoi.theworldthatday.place;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Place search is off until M2a2 adds the Kakao Local implementation: always unavailable. */
@Component
class DisabledPlaceSearch implements PlaceSearch {

  private static final Logger log = LoggerFactory.getLogger(DisabledPlaceSearch.class);

  @Override
  public Optional<PlaceResolveResponse> search(PlaceQuery query) {
    log.debug("Place search off");
    return Optional.empty();
  }
}
