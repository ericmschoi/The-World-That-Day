package io.github.ericmschoi.theworldthatday.place;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Area;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Candidate;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Source;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Status;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Type;
import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Level;
import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Region;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Resolves a place query (plan M2a): the dictionary's regions when one or more match, without
 * asking place search ("서울" never becomes 서울역); otherwise place search, and 503 {@code
 * UPSTREAM_UNAVAILABLE} when it cannot be used. Nothing is logged here.
 */
@Service
public class PlaceResolveService {

  private final RegionDictionary dictionary;
  private final RegionMatcher matcher;
  private final PlaceSearch search;

  public PlaceResolveService(
      RegionDictionary dictionary, RegionMatcher matcher, PlaceSearch search) {
    this.dictionary = dictionary;
    this.matcher = matcher;
    this.search = search;
  }

  public PlaceResolveResponse resolve(PlaceQuery query) {
    List<Region> regions = matcher.match(query);
    if (regions.isEmpty()) {
      return search
          .search(query)
          .orElseThrow(
              () ->
                  new ApiProblem(
                      HttpStatus.SERVICE_UNAVAILABLE,
                      "UPSTREAM_UNAVAILABLE",
                      "place search is unavailable, try again later"));
    }
    RegionDictionary.Source source = dictionary.source().orElseThrow();
    return new PlaceResolveResponse(
        Status.FOUND,
        regions.stream().map(this::candidate).toList(),
        false,
        List.of(new Source(source.name(), source.url())));
  }

  /**
   * {@code name} is the short opening name ("서울", "광주 서구", "수원시 장안구"), {@code full_name} the
   * official names top-down ("광주광역시 서구").
   */
  private Candidate candidate(Region region) {
    List<Region> line = new ArrayList<>(dictionary.ancestors(region));
    line.add(region);
    Region sido = line.getFirst();
    String name =
        switch (region.level()) {
          case SIDO -> sido.shortName().orElseThrow();
          case SIGUNGU -> sido.shortName().orElseThrow() + " " + region.name();
          case GU -> line.get(1).name() + " " + region.name();
        };
    return new Candidate(
        "region-" + region.code(),
        Type.REGION,
        name,
        String.join(" ", line.stream().map(Region::name).toList()),
        null,
        null,
        null,
        null,
        new Area(sido.code(), sido.name()),
        region.level() == Level.SIDO ? null : new Area(region.code(), region.name()));
  }
}
