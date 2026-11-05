package io.github.ericmschoi.theworldthatday.place;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;

/**
 * CSV files of the place dictionary: a header row, columns read by name, and errors that name only
 * the file and the line, never a value. Reading drops a UTF-8 byte order mark; writing gives UTF-8
 * without one, {@code \n} line ends, and quotes only where CSV needs them.
 */
final class PlaceCsv {

  private static final ObjectReader ROWS =
      CsvMapper.shared()
          .readerForArrayOf(String.class)
          .with(CsvReadFeature.WRAP_AS_ARRAY)
          .with(CsvReadFeature.SKIP_EMPTY_LINES);
  private static final int BOM = 0xFEFF;
  private static final Pattern NEEDS_QUOTES = Pattern.compile("[,\"\r\n]");

  private PlaceCsv() {}

  /** One data row; {@code line} is the file line where the row starts (the header is line 1). */
  record Row(Path file, long line, Map<String, Integer> columns, String[] values) {

    String get(String column) {
      return values[columns.get(column)].strip();
    }

    IllegalArgumentException fail(String message) {
      return new IllegalArgumentException(file + ":" + line + ": " + message);
    }
  }

  /** Calls {@code each} for every data row; the header must hold every {@code required} column. */
  static void read(Path file, Charset charset, List<String> required, Consumer<Row> each)
      throws IOException {
    try (BufferedReader in = Files.newBufferedReader(file, charset)) {
      in.mark(1);
      if (in.read() != BOM) {
        in.reset();
      }
      MappingIterator<String[]> rows = ROWS.readValues(in);
      if (!rows.hasNextValue()) {
        throw new IllegalArgumentException(file + ":1: no header row");
      }
      String[] header = rows.nextValue();
      Map<String, Integer> columns = new HashMap<>();
      for (int i = 0; i < header.length; i++) {
        columns.putIfAbsent(header[i].strip(), i);
      }
      if (!columns.keySet().containsAll(required)) {
        throw new IllegalArgumentException(file + ":1: missing one of the columns " + required);
      }
      while (rows.hasNextValue()) {
        String[] values = rows.nextValue();
        Row row = new Row(file, rows.parser().currentTokenLocation().getLineNr(), columns, values);
        if (row.values().length != header.length) {
          throw row.fail("expected " + header.length + " columns");
        }
        each.accept(row);
      }
    } catch (JacksonException e) {
      throw new IllegalArgumentException(file + ": not readable as CSV", e);
    }
  }

  static void write(Path file, List<String> header, List<List<String>> rows) throws IOException {
    StringBuilder text = new StringBuilder();
    for (List<String> row : Stream.concat(Stream.of(header), rows.stream()).toList()) {
      text.append(String.join(",", row.stream().map(PlaceCsv::quote).toList())).append('\n');
    }
    Files.writeString(file, text, StandardCharsets.UTF_8);
  }

  private static String quote(String value) {
    return NEEDS_QUOTES.matcher(value).find() ? '"' + value.replace("\"", "\"\"") + '"' : value;
  }
}
