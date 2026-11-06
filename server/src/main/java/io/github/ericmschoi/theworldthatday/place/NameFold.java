package io.github.ericmschoi.theworldthatday.place;

import java.text.Normalizer;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * The comparison key for Canadian place names (plan C2 {@code fold}). The dictionary builder and
 * the matcher both use it, so a name and an input are equal exactly when their keys are equal.
 *
 * <p>Accents are dropped, case is lowered with {@link Locale#ROOT}, curly apostrophes become
 * straight ones, hyphens and dashes become spaces, the words {@code st}/{@code st.}/{@code saint}
 * become {@code saint} and {@code ste}/{@code ste.}/{@code sainte} become {@code sainte}, other
 * full stops are removed, and runs of spaces become one. "St. John's" keeps its {@code 's} and so
 * stays apart from "Saint John".
 */
public final class NameFold {

  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern DASHES = Pattern.compile("[-\\x{2010}\\x{2011}\\x{2013}\\x{2014}]");
  private static final Pattern QUOTES = Pattern.compile("[\\x{2018}\\x{2019}]");
  private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");

  private NameFold() {}

  public static String fold(String name) {
    String text = MARKS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
    text = QUOTES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("'");
    StringJoiner words = new StringJoiner(" ");
    for (String word : SPACES.split(DASHES.matcher(text).replaceAll(" "))) {
      String folded =
          switch (word) {
            case "st", "st.", "saint" -> "saint";
            case "ste", "ste.", "sainte" -> "sainte";
            default -> word.replace(".", "");
          };
      if (!folded.isEmpty()) {
        words.add(folded);
      }
    }
    return words.toString();
  }
}
