package io.github.ericmschoi.theworldthatday.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Candidate;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Status;
import io.github.ericmschoi.theworldthatday.place.PlaceResolveResponse.Type;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Plan M2a T4 (1)(2): the contract of {@code GET /api/places/resolve} with the synthetic dictionary
 * ({@code src/test/resources/places/regions.json}) and the off place search. The same demo queries
 * against the real {@code data/places/regions.json} are checked once plan M2a T1 lands (T6).
 */
@WebMvcTest(PlaceResolveController.class)
@Import({
  PlaceResolveService.class,
  RegionMatcher.class,
  DisabledPlaceSearch.class,
  PlaceResolveControllerTest.SyntheticDictionary.class
})
class PlaceResolveControllerTest {

  static final String PATH = "/api/places/resolve";
  private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

  @Autowired private MockMvc mvc;

  /** The synthetic dictionary, copied into a temporary data folder. */
  @TestConfiguration(proxyBeanMethods = false)
  static class SyntheticDictionary {
    @Bean
    RegionDictionary regionDictionary() {
      try {
        return RegionDictionaryTest.load(
            RegionDictionaryTest.write(
                Files.createTempDirectory("regions"), RegionDictionaryTest.synthetic().toString()));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  static ResultActions resolve(MockMvc mvc, String q) throws Exception {
    return mvc.perform(q == null ? get(PATH) : get(PATH).param("q", q));
  }

  private ResultActions ok(String q) throws Exception {
    return resolve(mvc, q)
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.status").value("FOUND"))
        .andExpect(jsonPath("$.more").value(false))
        .andExpect(jsonPath("$.sources.length()").value(1))
        .andExpect(jsonPath("$.sources[0].name").value("행정안전부 법정동코드 (공공데이터포털)"))
        .andExpect(
            jsonPath("$.sources[0].url").value("https://www.data.go.kr/data/15077871/openapi.do"));
  }

  private void expectProblem(String q, int status, String code) throws Exception {
    MvcResult result =
        resolve(mvc, q)
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith(PROBLEM))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.status").value(status))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.instance").value(PATH))
            .andExpect(jsonPath("$.title").isString())
            .andExpect(jsonPath("$.detail").isString())
            .andReturn();
    String body = result.getResponse().getContentAsString();
    if (q != null && !q.isBlank()) {
      assertThat(body).as("the body never echoes the input").doesNotContain(q.strip());
    }
  }

  @Test
  void seoulIsOneRegionWithoutCoordinates() throws Exception {
    ok("서울")
        .andExpect(jsonPath("$.candidates.length()").value(1))
        .andExpect(jsonPath("$.candidates[0].id").value("region-1100000000"))
        .andExpect(jsonPath("$.candidates[0].type").value("REGION"))
        .andExpect(jsonPath("$.candidates[0].name").value("서울"))
        .andExpect(jsonPath("$.candidates[0].full_name").value("서울특별시"))
        .andExpect(jsonPath("$.candidates[0].address").value(nullValue()))
        .andExpect(jsonPath("$.candidates[0].category").value(nullValue()))
        .andExpect(jsonPath("$.candidates[0].lat").value(nullValue()))
        .andExpect(jsonPath("$.candidates[0].lng").value(nullValue()))
        .andExpect(jsonPath("$.candidates[0].sido.code").value("1100000000"))
        .andExpect(jsonPath("$.candidates[0].sido.name").value("서울특별시"))
        .andExpect(jsonPath("$.candidates[0].sigungu").value(nullValue()));
  }

  @Test
  void bodyHasExactlyTheContractFields() throws Exception {
    String body = ok("서울").andReturn().getResponse().getContentAsString();

    assertThat(RegionDictionaryTest.JSON.readTree(body).propertyNames())
        .containsExactly("status", "candidates", "more", "sources");
    assertThat(RegionDictionaryTest.JSON.readTree(body).path("candidates").get(0).propertyNames())
        .containsExactly(
            "id",
            "type",
            "name",
            "full_name",
            "address",
            "category",
            "lat",
            "lng",
            "sido",
            "sigungu");
  }

  @Test
  void seoguIsFiveDistrictsInCodeOrder() throws Exception {
    ok("서구")
        .andExpect(
            jsonPath("$.candidates[*].id")
                .value(
                    contains(
                        "region-2614000000",
                        "region-2723000000",
                        "region-2826000000",
                        "region-2914000000",
                        "region-3017000000")))
        .andExpect(
            jsonPath("$.candidates[*].name")
                .value(contains("부산 서구", "대구 서구", "인천 서구", "광주 서구", "대전 서구")))
        .andExpect(jsonPath("$.candidates[3].full_name").value("광주광역시 서구"))
        .andExpect(jsonPath("$.candidates[3].sido.code").value("2900000000"))
        .andExpect(jsonPath("$.candidates[3].sigungu.code").value("2914000000"))
        .andExpect(jsonPath("$.candidates[3].sigungu.name").value("서구"));
  }

  @Test
  void gwangjuIsTwoRegionsAndGwangjuSeoguIsOne() throws Exception {
    ok("광주")
        .andExpect(jsonPath("$.candidates[*].name").value(contains("광주", "경기 광주시")))
        .andExpect(jsonPath("$.candidates[*].full_name").value(contains("광주광역시", "경기도 광주시")));
    ok("광주 서구")
        .andExpect(jsonPath("$.candidates.length()").value(1))
        .andExpect(jsonPath("$.candidates[0].id").value("region-2914000000"));
  }

  @Test
  void cityDistrictIsNamedByItsCityAndIsItsOwnSigungu() throws Exception {
    ok("장안구")
        .andExpect(jsonPath("$.candidates[0].name").value("수원시 장안구"))
        .andExpect(jsonPath("$.candidates[0].full_name").value("경기도 수원시 장안구"))
        .andExpect(jsonPath("$.candidates[0].sido.code").value("4100000000"))
        .andExpect(jsonPath("$.candidates[0].sigungu.code").value("4111100000"))
        .andExpect(jsonPath("$.candidates[0].sigungu.name").value("장안구"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"광주 서구 풍암동", "서울대학교병원", "경기도 경기 수원시 장안구"})
  void queriesOutsideTheDictionaryAre503WhilePlaceSearchIsOff(String q) throws Exception {
    expectProblem(q, 503, "UPSTREAM_UNAVAILABLE");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "　　", "\u0000"})
  void blankQueryIsRequired(String q) throws Exception {
    expectProblem(q, 400, "PLACE_QUERY_REQUIRED");
  }

  @Test
  void missingQueryIsRequired() throws Exception {
    expectProblem(null, 400, "PLACE_QUERY_REQUIRED");
  }

  @Test
  void fiftyCharactersAreAskedAndFiftyOneAreTooLong() throws Exception {
    expectProblem("가".repeat(50), 503, "UPSTREAM_UNAVAILABLE");
    expectProblem("가".repeat(51), 400, "PLACE_QUERY_TOO_LONG");
  }

  /** Plan M2a T4 (3): what a search implementation answers is passed on unchanged. */
  @Test
  void searchAnswerIsPassedOnAndTheDictionaryWinsWithoutAskingSearch() {
    RegionDictionary dictionary = new SyntheticDictionary().regionDictionary();
    AtomicReference<String> asked = new AtomicReference<>();
    PlaceResolveResponse notFound =
        new PlaceResolveResponse(Status.NOT_FOUND, List.of(), false, List.of());
    PlaceSearch stub =
        query -> {
          asked.set(query.value());
          return Optional.of(notFound);
        };
    PlaceResolveService service =
        new PlaceResolveService(dictionary, new RegionMatcher(dictionary), stub);

    assertThat(service.resolve(PlaceQuery.parse("광주 서구 풍암동"))).isSameAs(notFound);
    assertThat(asked.get()).isEqualTo("광주 서구 풍암동");
    asked.set(null);
    assertThat(service.resolve(PlaceQuery.parse("서울")).candidates())
        .extracting(Candidate::type)
        .containsExactly(Type.REGION);
    assertThat(asked.get()).as("the dictionary matched, search not asked").isNull();
  }
}
