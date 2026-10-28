package io.github.ericmschoi.theworldthatday.place;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/places/resolve?q=<place>} (plan M2a). Every answer, errors included, is {@code
 * Cache-Control: no-store}: it holds a user's place. Request values are never logged.
 */
@RestController
public class PlaceResolveController {

  private final PlaceResolveService service;

  public PlaceResolveController(PlaceResolveService service) {
    this.service = service;
  }

  /**
   * {@code q} is optional here so a missing value gets {@code PLACE_QUERY_REQUIRED}, not a 400 from
   * Spring.
   */
  @GetMapping("/api/places/resolve")
  public PlaceResolveResponse resolve(
      @RequestParam(name = "q", required = false) String q, HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    return service.resolve(PlaceQuery.parse(q));
  }
}
