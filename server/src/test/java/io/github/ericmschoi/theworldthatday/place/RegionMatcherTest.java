package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Region;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M2a T3 (1)(2)(4): rules M1 to M6 against the synthetic dictionary {@code
 * src/test/resources/places/regions.json}. The demo table against the real file is {@link
 * RegionMatcherDemoTest}.
 */
@ExtendWith(OutputCaptureExtension.class)
class RegionMatcherTest {

  @TempDir Path dataDir;

  private RegionMatcher matcher(ObjectNode dictionary) {
    return new RegionMatcher(
        RegionDictionaryTest.load(RegionDictionaryTest.write(dataDir, dictionary.toString())));
  }

  private RegionMatcher synthetic() {
    return matcher(RegionDictionaryTest.synthetic());
  }

  private static List<String> codes(RegionMatcher matcher, String query) {
    return matcher.match(PlaceQuery.parse(query)).stream().map(Region::code).toList();
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "서울특별시 | 1100000000", // official name
        "서울 | 1100000000", // short_name
        "서울시 | 1100000000", // alias
        "강원도 | 5100000000", // alias (former official name)
        "종로구 | 1111000000",
        "수원시 | 4111000000",
        "수원 | 4111000000", // final 시 stripped
        "경기 광주 | 4161000000", // final 시 stripped, after its province
      })
  void eachNameOfARegionMatchesIt(String query, String code) {
    assertThat(codes(synthetic(), query)).containsExactly(code);
  }

  @Test
  void finalGunIsStrippedAndBothCountiesAreCandidates() {
    assertThat(codes(synthetic(), "고성")).containsExactly("4882000000", "5182000000");
    assertThat(codes(synthetic(), "고성군")).containsExactly("4882000000", "5182000000");
    assertThat(codes(synthetic(), "강원 고성")).containsExactly("5182000000");
  }

  @ParameterizedTest
  @ValueSource(strings = {"중", "서", "종로", "장안", "서울 종로", "수원시 장안"})
  void finalGuIsNeverStripped(String query) {
    assertThat(codes(synthetic(), query)).isEmpty();
  }

  @Test
  void stripsOnlyWhenTwoCharactersRemain() {
    ObjectNode dictionary = RegionDictionaryTest.synthetic();
    dictionary
        .withArray("regions")
        .addObject()
        .put("code", "4199000000")
        .put("level", "sigungu")
        .put("name", "단시")
        .put("parent", "4100000000")
        .putArray("aliases");
    RegionMatcher matcher = matcher(dictionary);

    assertThat(codes(matcher, "단시")).containsExactly("4199000000");
    assertThat(codes(matcher, "단")).isEmpty();
  }

  @Test
  void aliasesAreNotStripped() {
    // 서울시 is an alias of 서울특별시; its 시 is not taken off ("서울" matches by short_name only).
    ObjectNode dictionary = RegionDictionaryTest.synthetic();
    RegionDictionaryTest.region(dictionary, "1100000000").put("short_name", "한양");

    assertThat(codes(matcher(dictionary), "서울")).isEmpty();
    assertThat(codes(matcher(dictionary), "서울시")).containsExactly("1100000000");
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "광주 서구 | 2914000000", // 광주 is 광주광역시 here: every word must be used
        "광주광역시 서구 | 2914000000",
        "서울 종로구 | 1111000000",
        "서울  종로구 | 1111000000", // two spaces (PlaceQuery makes them one)
        "수원시 장안구 | 4111100000",
        "수원 장안구 | 4111100000",
        "경기 수원시 장안구 | 4111100000",
        "경기도 장안구 | 4111100000", // the city may be left out
        "인천 중구 | 2811000000",
      })
  void wordsBeforeTheLastNameItsAncestorsInOrder(String query, String code) {
    assertThat(codes(synthetic(), query)).containsExactly(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "수원시 경기 장안구", // ancestors out of order
        "장안구 수원시", // a descendant before its ancestor
        "종로구 서울",
        "서울 광주 서구", // a word that is no ancestor of the match
        "부산 종로구", // another province
        "서울 서울", // a province has no ancestors
        "서울 서울 종로구", // one ancestor used twice
        "경기 경기 수원시",
      })
  void wordsThatAreNotItsAncestorsInOrderMatchNothing(String query) {
    assertThat(codes(synthetic(), query)).isEmpty();
  }

  @Test
  void fourOrMoreWordsMatchNothing() {
    assertThat(codes(synthetic(), "경기 수원시 장안구")).containsExactly("4111100000");
    assertThat(codes(synthetic(), "경기도 경기 수원시 장안구")).isEmpty();
    assertThat(codes(synthetic(), "대한민국 경기 수원시 장안구")).isEmpty();
    assertThat(codes(synthetic(), "서울 서울 서울 서울 서울")).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "서울특",
        "서울특별시청",
        "서울종로구",
        "서울특별시종로구",
        "광주서구",
        "서 구",
        "구",
        "울",
        "광주 서구 풍암동",
        "서울대학교병원",
        "서울역"
      })
  void onlyWholeWordsMatchNoPartsAndNoSpacingRepairs(String query) {
    assertThat(codes(synthetic(), query)).isEmpty();
  }

  @Test
  void allMatchesAreCandidatesInCodeOrder() {
    assertThat(codes(synthetic(), "서구"))
        .containsExactly("2614000000", "2723000000", "2826000000", "2914000000", "3017000000");
    assertThat(codes(synthetic(), "광주")).containsExactly("2900000000", "4161000000");
    assertThat(codes(synthetic(), "중구")).containsExactly("1114000000", "2811000000");
  }

  @Test
  void dictionaryTextInAnotherUnicodeFormStillMatches() {
    ObjectNode dictionary = RegionDictionaryTest.synthetic();
    RegionDictionaryTest.region(dictionary, "1111000000")
        .put("name", Normalizer.normalize("종로구", Normalizer.Form.NFD));
    RegionMatcher matcher = matcher(dictionary);

    assertThat(codes(matcher, "종로구")).containsExactly("1111000000");
    assertThat(codes(matcher, Normalizer.normalize("서울 종로구", Normalizer.Form.NFD)))
        .containsExactly("1111000000");
  }

  @Test
  void emptyDictionaryMatchesNothing() {
    RegionMatcher matcher =
        new RegionMatcher(RegionDictionaryTest.load(dataDir.resolve("nowhere")));

    assertThat(codes(matcher, "서울")).isEmpty();
  }

  @Test
  void sameQueryGivesTheSameAnswerAndNothingIsLoggedEvenAtDebug(CapturedOutput output) {
    RegionMatcher matcher = synthetic();
    ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Level before = root.getLevel();
    root.setLevel(Level.DEBUG);
    List<String> first;
    List<String> second;
    try {
      LoggerFactory.getLogger("region-matcher-test").debug("debug-control-line");
      first = codes(matcher, "광주");
      second = codes(matcher, "광주");
      codes(matcher, "광주 서구 풍암동");
    } finally {
      root.setLevel(before);
    }

    assertThat(second).isEqualTo(first).hasSize(2);
    // The control line proves DEBUG output is captured, so the absence below means something.
    assertThat(output.getAll()).contains("debug-control-line");
    assertThat(output.getAll())
        .doesNotContain("광주")
        .doesNotContain("풍암동")
        .doesNotContain("2900000000")
        .doesNotContain("4161000000");
  }
}
