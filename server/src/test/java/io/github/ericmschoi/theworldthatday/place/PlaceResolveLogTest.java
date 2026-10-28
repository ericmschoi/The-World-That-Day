package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

/**
 * Plan M2a T4 (4): with the application's loggers at DEBUG, the demo, 400 and 503 paths leave no
 * query text, region name or region code in any log line ({@code CLAUDE.md} 7.6, decision M2-P4).
 * The only line is the off search's fixed {@code Place search off}.
 */
@WebMvcTest(PlaceResolveController.class)
@Import({
  PlaceResolveService.class,
  RegionMatcher.class,
  DisabledPlaceSearch.class,
  PlaceResolveControllerTest.SyntheticDictionary.class
})
@ExtendWith(OutputCaptureExtension.class)
class PlaceResolveLogTest {

  private static final List<String> FOUND = List.of("서울", "광주", "서구", "광주 서구", "수원시 장안구");
  private static final List<String> UNAVAILABLE = List.of("광주 서구 풍암동", "서울대학교병원");
  private static final List<String> TOO_LONG = List.of("서울특별시" + "가".repeat(46));

  @Autowired private MockMvc mvc;

  /** Every query, raw and URL-encoded, and every name, alias and code of the dictionary. */
  private static List<String> forbidden() {
    List<String> values = new ArrayList<>();
    for (List<String> queries : List.of(FOUND, UNAVAILABLE, TOO_LONG)) {
      for (String q : queries) {
        values.add(q);
        values.add(URLEncoder.encode(q, StandardCharsets.UTF_8));
        values.add(URLEncoder.encode(q, StandardCharsets.UTF_8).replace("+", "%20"));
      }
    }
    for (JsonNode region : RegionDictionaryTest.synthetic().path("regions")) {
      values.add(region.path("code").asString());
      values.add(region.path("name").asString());
      if (region.has("short_name")) {
        values.add(region.path("short_name").asString());
      }
      region.path("aliases").forEach(alias -> values.add(alias.asString()));
    }
    return values;
  }

  @Test
  void noQueryRegionNameOrCodeIsLoggedAtDebug(CapturedOutput output) throws Exception {
    ch.qos.logback.classic.Logger app =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.github.ericmschoi");
    Level before = app.getLevel();
    app.setLevel(Level.DEBUG);
    int start = output.getAll().length();
    try {
      for (String q : FOUND) {
        PlaceResolveControllerTest.resolve(mvc, q).andExpect(status().isOk());
      }
      for (String q : UNAVAILABLE) {
        PlaceResolveControllerTest.resolve(mvc, q).andExpect(status().isServiceUnavailable());
      }
      for (String q : TOO_LONG) {
        PlaceResolveControllerTest.resolve(mvc, q).andExpect(status().isBadRequest());
      }
      PlaceResolveControllerTest.resolve(mvc, " ").andExpect(status().isBadRequest());
      PlaceResolveControllerTest.resolve(mvc, null).andExpect(status().isBadRequest());
    } finally {
      app.setLevel(before);
    }

    String logs = output.getAll().substring(start);
    // The off search's DEBUG line proves DEBUG output is captured, so the absence below means
    // something: one line per 503 request and nothing else from the application.
    assertThat(logs.lines().filter(line -> line.contains("Place search off")))
        .hasSize(UNAVAILABLE.size());
    // Application logger names are abbreviated to i.g.e.t.* by the log pattern.
    assertThat(logs.lines().filter(line -> line.contains(" i.g.e.t.")))
        .hasSize(UNAVAILABLE.size())
        .allSatisfy(line -> assertThat(line).contains("Place search off"));
    for (String value : forbidden()) {
      assertThat(logs).as("no log line holds a query, name or code").doesNotContain(value);
    }
  }
}
