package io.github.ericmschoi.theworldthatday.story.world;

import io.github.ericmschoi.theworldthatday.story.StoryDate;
import java.time.Clock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/story/world?date=YYYY-MM-DD} (plan M3a2). Request values are never logged. */
@RestController
public class WorldStoryController {

  private final WorldStoryService service;
  private final Clock clock;

  public WorldStoryController(WorldStoryService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /** {@code date} is optional here so a missing value gets {@code DATE_REQUIRED}, not a 500. */
  @GetMapping("/api/story/world")
  public WorldStoryResponse world(@RequestParam(name = "date", required = false) String date) {
    return service.find(StoryDate.parse(date, clock));
  }
}
