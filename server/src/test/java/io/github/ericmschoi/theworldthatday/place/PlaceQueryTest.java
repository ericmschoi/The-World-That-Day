package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ericmschoi.theworldthatday.api.ApiProblem;
import java.text.Normalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

/** Plan M2a T2 (3): normalization and length of the {@code q} parameter. */
class PlaceQueryTest {

  private static ApiProblem rejected(String raw) {
    return catchThrowableOfType(ApiProblem.class, () -> PlaceQuery.parse(raw));
  }

  private static void assertProblem(String raw, String code) {
    ApiProblem problem = rejected(raw);
    assertThat(problem).as("rejected").isNotNull();
    assertThat(problem.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(problem.code()).isEqualTo(code);
    if (raw != null && !raw.isBlank()) {
      assertThat(problem.getMessage()).doesNotContain(raw.strip());
    }
  }

  @Test
  void plainQueryIsKept() {
    assertThat(PlaceQuery.parse("광주 서구").value()).isEqualTo("광주 서구");
  }

  @Test
  void fiftyCodePointsPassAndFiftyOneAreTooLong() {
    String fifty = "가".repeat(50);

    assertThat(PlaceQuery.parse(fifty).value()).isEqualTo(fifty);
    assertProblem("가".repeat(51), "PLACE_QUERY_TOO_LONG");
  }

  @Test
  void lengthIsCountedInCodePointsAfterNormalization() {
    // 50 characters outside the BMP are 100 UTF-16 units but 50 code points.
    String fiftyEmoji = "😀".repeat(50);
    assertThat(PlaceQuery.parse(fiftyEmoji).value()).isEqualTo(fiftyEmoji);
    assertProblem("😀".repeat(51), "PLACE_QUERY_TOO_LONG");
    // Spaces around and inside do not count once trimmed or collapsed.
    assertThat(PlaceQuery.parse("  " + "가".repeat(25) + "   " + "가".repeat(24) + "  ").value())
        .hasSize(50);
    // Decomposed Hangul (NFD) is two or three code points per syllable before NFC.
    String nfdFifty = Normalizer.normalize("한".repeat(50), Normalizer.Form.NFD);
    assertThat(nfdFifty.codePointCount(0, nfdFifty.length())).isEqualTo(150);
    assertThat(PlaceQuery.parse(nfdFifty).value()).isEqualTo("한".repeat(50));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        " ",
        "     ",
        "　",
        "　 　",
        " ",
        "\t",
        "\n\r",
        "\u0000\u0007\u001b\u007f",
        "\u0085\u009f",
        " \t　\u0000 "
      })
  void onlySpacesOrControlCharactersAreRequired(String raw) {
    assertProblem(raw, "PLACE_QUERY_REQUIRED");
  }

  @Test
  void runsOfSpacesCollapseToOneAndEndsAreTrimmed() {
    assertThat(PlaceQuery.parse("서울  종로구").value()).isEqualTo("서울 종로구");
    assertThat(PlaceQuery.parse("서울　종로구").value()).isEqualTo("서울 종로구");
    assertThat(PlaceQuery.parse("서울 　  종로구").value()).isEqualTo("서울 종로구");
    assertThat(PlaceQuery.parse("　 광주   서구 풍암동 　").value()).isEqualTo("광주 서구 풍암동");
  }

  @Test
  void controlCharactersAreRemovedBeforeSpacesAreCollapsed() {
    assertThat(PlaceQuery.parse("서울\u0000역").value()).isEqualTo("서울역");
    assertThat(PlaceQuery.parse("\u0007서울 \u001b 종로구\u007f").value()).isEqualTo("서울 종로구");
  }

  @Test
  void nfdInputEqualsNfcInput() {
    String nfc = "광주 서구";
    String nfd = Normalizer.normalize(nfc, Normalizer.Form.NFD);
    assertThat(nfd).isNotEqualTo(nfc);

    assertThat(PlaceQuery.parse(nfd)).isEqualTo(PlaceQuery.parse(nfc));
    assertThat(PlaceQuery.parse(nfd).value()).isEqualTo(nfc);
  }

  @Test
  void textIsNeverInTheStringFormOrTheErrorDetail() {
    assertThat(PlaceQuery.parse("서울대학교병원")).hasToString("PlaceQuery[length=7]");
    assertThat(rejected("가".repeat(51)).getMessage()).isEqualTo("q must be at most 50 characters");
    assertThat(rejected(" ").getMessage()).isEqualTo("q is required");
  }
}
