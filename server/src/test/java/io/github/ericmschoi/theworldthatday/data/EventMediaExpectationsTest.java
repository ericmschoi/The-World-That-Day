package io.github.ericmschoi.theworldthatday.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Plan M3a4b T4 (8) on {@code data/verification/event-media-expectations.json} (T1): exactly the
 * eight demo and sample dates, every event cell of every source has an expectation and research
 * evidence numbers that resolve in the top-level {@code evidence} object (a cell that was not
 * researched says so and gives the gap), the {@code tragic} rows and the day without events ask for
 * no request, and the file holds no email address. Reads {@code ../data}; no network.
 */
class EventMediaExpectationsTest {

  private static final String FILE = "verification/event-media-expectations.json";
  private static final Set<String> DATES =
      Set.of(
          "1988-09-17",
          "2002-06-22",
          "1995-03-15",
          "1950-06-25",
          "1969-07-20",
          "1986-09-20",
          "2000-02-29",
          "2014-04-16");
  private static final Set<String> EXPECT =
      Set.of("present", "none", "no_request", "not_researched");
  private static final List<String> SOURCES = List.of("commons", "youtube", "daehan_news");
  private static final JsonNode DOC = DataSchemas.read(FILE);

  private static List<JsonNode> list(JsonNode array) {
    List<JsonNode> out = new ArrayList<>();
    array.forEach(out::add);
    return out;
  }

  /** Every cell, with a readable name. */
  private static List<Cell> cells() {
    List<Cell> cells = new ArrayList<>();
    for (JsonNode date : list(DOC.path("dates"))) {
      String day = date.path("date").asString();
      for (JsonNode event : list(date.path("events"))) {
        for (String source : SOURCES) {
          cells.add(
              new Cell(
                  day + " " + event.path("id").asString() + " " + source,
                  event.path("tone").asString(),
                  event.path("sources").path(source)));
        }
      }
      if (date.has("sources")) {
        for (String source : SOURCES) {
          cells.add(new Cell(day + " (day) " + source, "none", date.path("sources").path(source)));
        }
      }
    }
    return cells;
  }

  private record Cell(String name, String tone, JsonNode node) {}

  private static void collectEvidence(JsonNode node, List<String> out) {
    if (node.isObject()) {
      node.properties()
          .forEach(
              entry -> {
                if (entry.getKey().equals("evidence") && entry.getValue().isArray()) {
                  list(entry.getValue()).forEach(n -> out.add(n.asString()));
                } else if (!entry.getKey().equals("evidence")) {
                  collectEvidence(entry.getValue(), out);
                }
              });
    } else if (node.isArray()) {
      node.forEach(n -> collectEvidence(n, out));
    }
  }

  @Test
  void itCoversExactlyTheEightDemoAndSampleDates() {
    List<String> dates =
        list(DOC.path("dates")).stream().map(d -> d.path("date").asString()).toList();

    assertThat(dates).hasSize(8).containsExactlyInAnyOrderElementsOf(DATES);
    assertThat(list(DOC.path("dates")))
        .filteredOn(d -> d.path("role").asString().equals("demo"))
        .extracting(d -> d.path("date").asString())
        .containsExactlyInAnyOrder("1988-09-17", "2002-06-22", "1995-03-15");
  }

  @Test
  void everyEventHasACellForEachSourceWithAnExpectation() {
    List<Cell> cells = cells();

    assertThat(cells).isNotEmpty();
    for (Cell cell : cells) {
      assertThat(cell.node().isObject()).as(cell.name()).isTrue();
      assertThat(cell.node().path("expect").asString()).as(cell.name()).isIn(EXPECT);
    }
    for (JsonNode date : list(DOC.path("dates"))) {
      for (JsonNode event : list(date.path("events"))) {
        String name = date.path("date").asString() + " " + event.path("id").asString();
        assertThat(event.path("id").asString()).isNotBlank();
        assertThat(event.path("tone").asString()).as(name).isIn("normal", "tragic");
        boolean noneResearched =
            SOURCES.stream()
                .allMatch(
                    s ->
                        event
                            .path("sources")
                            .path(s)
                            .path("expect")
                            .asString()
                            .equals("not_researched"));
        // a row names its research evidence, unless no source was researched for it (then every
        // cell says so with its gap; see everyResearchedCellHasEvidenceNumbersAndAGapIsSaidOut)
        assertThat(!event.path("event_evidence").isEmpty() || noneResearched)
            .as("event evidence of " + name)
            .isTrue();
      }
    }
  }

  @Test
  void everyResearchedCellHasEvidenceNumbersAndAGapIsSaidOut() {
    for (Cell cell : cells()) {
      if (cell.node().path("expect").asString().equals("not_researched")) {
        assertThat(cell.node().path("evidence_gap").asString()).as(cell.name()).isNotBlank();
      } else {
        assertThat(
                cell.node().path("evidence").isArray() && !cell.node().path("evidence").isEmpty())
            .as("evidence numbers of " + cell.name())
            .isTrue();
      }
    }
  }

  @Test
  void everyEvidenceNumberResolvesToADescribedEntry() {
    JsonNode evidence = DOC.path("evidence");
    List<String> used = new ArrayList<>();
    collectEvidence(DOC.path("dates"), used);

    assertThat(used).isNotEmpty();
    assertThat(used).allSatisfy(id -> assertThat(evidence.has(id)).as("evidence " + id).isTrue());
    evidence
        .properties()
        .forEach(
            entry -> {
              assertThat(entry.getValue().path("description").asString())
                  .as("description of " + entry.getKey())
                  .isNotBlank();
              JsonNode url = entry.getValue().path("url");
              assertThat(url.isNull() || url.asString().startsWith("https://"))
                  .as("url of " + entry.getKey())
                  .isTrue();
            });
  }

  @Test
  void tragicEventsAndTheDayWithoutEventsAskForNoRequest() {
    for (Cell cell : cells()) {
      if (cell.tone().equals("tragic")) {
        assertThat(cell.node().path("expect").asString()).as(cell.name()).isEqualTo("no_request");
        assertThat(cell.node().path("reason").asString()).as(cell.name()).isEqualTo("tragic");
      }
    }
    JsonNode empty =
        list(DOC.path("dates")).stream()
            .filter(d -> d.path("date").asString().equals("1995-03-15"))
            .findFirst()
            .orElseThrow();
    assertThat(empty.path("events").isEmpty()).isTrue();
    for (String source : SOURCES) {
      assertThat(empty.path("sources").path(source).path("expect").asString())
          .isEqualTo("no_request");
    }
    // the two tragic sample dates each hold a tragic event
    for (String day : List.of("1950-06-25", "2014-04-16")) {
      JsonNode date =
          list(DOC.path("dates")).stream()
              .filter(d -> d.path("date").asString().equals(day))
              .findFirst()
              .orElseThrow();
      assertThat(list(date.path("events")))
          .extracting(e -> e.path("tone").asString())
          .contains("tragic");
    }
  }

  @Test
  void theFileHoldsNoEmailAddress() throws IOException {
    String text = Files.readString(DataSchemas.DATA.resolve(FILE), StandardCharsets.UTF_8);

    assertThat(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+").matcher(text).find()).isFalse();
  }
}
