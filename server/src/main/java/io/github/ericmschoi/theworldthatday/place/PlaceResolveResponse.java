package io.github.ericmschoi.theworldthatday.place;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Body of {@code GET /api/places/resolve} (plan M2a contract, shared with M2a2 and M2b2). JSON
 * names are snake_case and absent values are {@code null}. {@code NOT_FOUND} and {@code
 * OUTSIDE_KOREA} come with no candidates. Nothing of it is stored or logged.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record PlaceResolveResponse(
    Status status, List<Candidate> candidates, boolean more, List<Source> sources) {

  public enum Status {
    FOUND,
    NOT_FOUND,
    OUTSIDE_KOREA
  }

  public enum Type {
    REGION,
    NEIGHBORHOOD,
    SPOT
  }

  public PlaceResolveResponse {
    candidates = List.copyOf(candidates);
    sources = List.copyOf(sources);
  }

  /**
   * One place to choose. {@code address} and {@code category} are for {@code SPOT} only; {@code
   * lat}/{@code lng} (WGS84) are {@code null} for {@code REGION}.
   */
  @JsonNaming(SnakeCaseStrategy.class)
  public record Candidate(
      String id,
      Type type,
      String name,
      String fullName,
      String address,
      String category,
      Double lat,
      Double lng,
      Area sido,
      Area sigungu) {}

  /** A province or district of the region dictionary. */
  public record Area(String code, String name) {}

  /** A source used for this answer. */
  public record Source(String name, String url) {}
}
