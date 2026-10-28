package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.place.RegionDictionary.Region;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M2a T3 (3): the demo queries against the real {@code data/places/regions.json}. Runs only
 * once that file is in the repository (plan M2a T1); until then every test here is skipped.
 * Expected regions are named by their official names and their province, as in the plan's demo
 * table.
 */
@EnabledIf("realDictionaryExists")
class RegionMatcherDemoTest {

  private static final Path DATA = Path.of("..", "data");

  static boolean realDictionaryExists() {
    return Files.isRegularFile(DATA.resolve(RegionDictionary.FILE));
  }

  private final RegionDictionary dictionary = RegionDictionaryTest.load(DATA);
  private final RegionMatcher matcher = new RegionMatcher(dictionary);

  /** {@code <province name>/<name>} of each match, or the province name alone, in result order. */
  private List<String> matches(String query) {
    return matcher.match(PlaceQuery.parse(query)).stream().map(this::label).toList();
  }

  private String label(Region region) {
    return region
        .parentCode()
        .map(parent -> dictionary.byCode(parent).orElseThrow().name() + "/" + region.name())
        .orElse(region.name());
  }

  @ParameterizedTest
  @ValueSource(strings = {"서울", "서울특별시", "서울시"})
  void seoulIsOneProvince(String query) {
    assertThat(matcher.match(PlaceQuery.parse(query)))
        .singleElement()
        .satisfies(region -> assertThat(region.code()).isEqualTo("1100000000"));
  }

  @Test
  void gwangjuIsTheMetropolitanCityAndTheGyeonggiCityInCodeOrder() {
    assertThat(matches("광주")).containsExactly("광주광역시", "경기도/광주시");
    assertThat(matcher.match(PlaceQuery.parse("광주"))).extracting(Region::code).isSorted();
  }

  @Test
  void seoguIsFiveDistrictsInCodeOrder() {
    assertThat(matches("서구"))
        .containsExactly("부산광역시/서구", "대구광역시/서구", "인천광역시/서구", "광주광역시/서구", "대전광역시/서구");
    assertThat(matcher.match(PlaceQuery.parse("서구"))).extracting(Region::code).isSorted();
  }

  @ParameterizedTest
  @ValueSource(strings = {"광주 서구", "광주광역시 서구"})
  void gwangjuSeoguIsOneDistrict(String query) {
    assertThat(matches(query)).containsExactly("광주광역시/서구");
  }

  @ParameterizedTest
  @ValueSource(strings = {"서울 종로구", "종로구", "서울  종로구"})
  void jongnoIsOneDistrict(String query) {
    assertThat(matches(query)).containsExactly("서울특별시/종로구");
  }

  @ParameterizedTest
  @ValueSource(strings = {"광주 서구 풍암동", "서울대학교병원"})
  void neighborhoodsAndSpotsAreNotInTheDictionary(String query) {
    assertThat(matches(query)).isEmpty();
  }
}
