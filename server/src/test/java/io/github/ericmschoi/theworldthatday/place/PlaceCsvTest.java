package io.github.ericmschoi.theworldthatday.place;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ericmschoi.theworldthatday.place.PlaceCsv.Row;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Plan C2 T1: reading and writing the dictionary CSV files (synthetic rows). */
class PlaceCsvTest {

  @TempDir Path temp;

  List<Row> read(String text, List<String> required) throws IOException {
    Path file = Files.write(temp.resolve("in.csv"), text.getBytes(UTF_8));
    List<Row> rows = new ArrayList<>();
    PlaceCsv.read(file, UTF_8, required, rows::add);
    return rows;
  }

  @Test
  void readsQuotedCommasByColumnNameAndSkipsEmptyLines() throws IOException {
    List<Row> rows =
        read(
            "﻿code,name\r\n4,\"Division No. 1, Subd. V\"\r\n\r\n5, Victoria \r\n", List.of("name"));

    assertThat(rows)
        .extracting(row -> row.get("name"))
        .containsExactly("Division No. 1, Subd. V", "Victoria");
    assertThat(rows).extracting(Row::line).containsExactly(2L, 4L);
  }

  @Test
  void rowWithAnotherColumnCountFailsWithItsLine() {
    assertThatThrownBy(() -> read("code,name\n4,London\n5\n", List.of("name")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageEndingWith(":3: expected 2 columns");
  }

  @Test
  void missingColumnOrHeaderFails() {
    assertThatThrownBy(() -> read("code\n4\n", List.of("name"))).hasMessageContaining(":1:");
    assertThatThrownBy(() -> read("", List.of("name"))).hasMessageContaining(":1: no header");
  }

  @Test
  void writesUtf8WithLineFeedsAndQuotesOnlyWhereNeeded() throws IOException {
    Path file = temp.resolve("out.csv");

    PlaceCsv.write(
        file,
        List.of("csd_code", "name"),
        List.of(List.of("1001519", "St. John's"), List.of("1", "a, \"b\"")));

    assertThat(Files.readString(file, UTF_8))
        .isEqualTo("csd_code,name\n1001519,St. John's\n1,\"a, \"\"b\"\"\"\n");
    assertThat(read(Files.readString(file), List.of("name")))
        .extracting(row -> row.get("name"))
        .containsExactly("St. John's", "a, \"b\"");
  }
}
