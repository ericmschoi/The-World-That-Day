package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.PATH;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.param;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiSourceFixture.requests;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiLinkResolver.Link;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Reason;
import io.github.ericmschoi.theworldthatday.wikimedia.WikimediaResponse.Unavailable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plan M3a1c T1 (5)(6): the pageprops lookup and link title normalization. */
class ActionApiKoWikiLinkResolverTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));

  @ParameterizedTest(name = "[{0}] -> [{1}]")
  @CsvSource(
      delimiter = '|',
      value = {
        "2002_FIFA_월드컵|2002 FIFA 월드컵",
        "대한민국#역사|대한민국",
        "'  서울   특별시 '|서울 특별시",
        "'  KBS  1TV  '|KBS 1TV",
        "iPhone|IPhone",
        "élan|Élan",
        "대한민국|대한민국",
        "1988년 하계 올림픽|1988년 하계 올림픽",
        "'#절만'|''"
      })
  void normalizeFollowsTheMediaWikiTitleRules(String raw, String expected) {
    assertThat(KoWikiLinkResolver.normalize(raw)).isEqualTo(expected);
  }

  @Test
  void normalizeKeepsInnerSpacesSingleAfterUnderscores() {
    assertThat(KoWikiLinkResolver.normalize("서울__특별시")).isEqualTo("서울 특별시");
  }

  @Test
  void recordedLookupFollowsRedirectsAndReturnsItemsInOneRequest() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(read("pageprops-1988-09-17.json"))));
    List<String> titles = List.of("1988년 하계 올림픽", "대한민국", "서울", "서울특별시", "KBS 1TV", "KBS 2TV");

    Map<String, Link> links = parts.resolver.resolve(titles).orElseThrow();

    assertThat(links)
        .containsEntry("1988년 하계 올림픽", new Link("1988년 하계 올림픽", "Q8470"))
        .containsEntry("서울", new Link("서울특별시", "Q8684"))
        .containsEntry("서울특별시", new Link("서울특별시", "Q8684"))
        .containsEntry("대한민국", new Link("대한민국", "Q884"))
        .hasSize(6);
    List<ServeEvent> requests = requests(wikimedia);
    assertThat(requests).hasSize(1);
    ServeEvent request = requests.getFirst();
    assertThat(param(request, "titles")).isEqualTo(String.join("|", titles));
    assertThat(param(request, "action")).isEqualTo("query");
    assertThat(param(request, "prop")).isEqualTo("pageprops");
    assertThat(param(request, "ppprop")).isEqualTo("wikibase_item");
    assertThat(param(request, "redirects")).isEqualTo("1");
    assertThat(param(request, "formatversion")).isEqualTo("2");
    assertThat(request.getRequest().getHeader("User-Agent"))
        .isEqualTo(KoWikiSourceFixture.USER_AGENT);
  }

  @Test
  void bothSpellingsOfTheSameCupResolveToOneTitleAndItem() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(read("pageprops-2002-06-22.json"))));

    Map<String, Link> links =
        parts.resolver.resolve(List.of("2002 FIFA 월드컵", "2002년 FIFA 월드컵")).orElseThrow();

    assertThat(links.values()).containsOnly(new Link("2002년 FIFA 월드컵", "Q47735"));
  }

  @Test
  void fiftyOneTitlesAreSentAsFiftyThenOne() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(KoWikiSourceFixture.EMPTY_PAGEPROPS)));
    List<String> titles = IntStream.rangeClosed(1, 51).mapToObj(i -> "시험 문서 " + i).toList();

    Map<String, Link> links = parts.resolver.resolve(titles).orElseThrow();

    assertThat(links).hasSize(51);
    assertThat(links.get("시험 문서 51")).isEqualTo(new Link("시험 문서 51", null));
    List<ServeEvent> requests = requests(wikimedia);
    assertThat(requests).hasSize(2);
    assertThat(param(requests.get(0), "titles").split("\\|")).hasSize(50);
    assertThat(param(requests.get(1), "titles")).isEqualTo("시험 문서 51");
  }

  @Test
  void normalizedAndMissingTitlesAreMapped() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                okJson(
                    """
                    {"batchcomplete":true,"query":{
                    "normalized":[{"fromencoded":false,"from":"시험_문서","to":"시험 문서"}],
                    "redirects":[{"from":"시험 문서","to":"시험 문서 가"}],
                    "pages":[{"pageid":1,"ns":0,"title":"시험 문서 가",
                              "pageprops":{"wikibase_item":"Q1"}},
                             {"ns":0,"title":"없는 시험 문서","missing":true}]}}
                    """)));

    Map<String, Link> links = parts.resolver.resolve(List.of("시험_문서", "없는 시험 문서")).orElseThrow();

    assertThat(links)
        .containsEntry("시험_문서", new Link("시험 문서 가", "Q1"))
        .containsEntry("없는 시험 문서", new Link("없는 시험 문서", null));
  }

  @Test
  void reservedCharactersInTitlesAreEncodedSoEachTitleArrivesWhole() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(KoWikiSourceFixture.EMPTY_PAGEPROPS)));

    parts.resolver.resolve(List.of("A&B", "C+D", "E=F", "가 나")).orElseThrow();

    assertThat(param(requests(wikimedia).getFirst(), "titles")).isEqualTo("A&B|C+D|E=F|가 나");
    assertThat(requests(wikimedia).getFirst().getRequest().getUrl())
        .contains("titles=A%26B%7CC%2BD");
  }

  @Test
  void serverErrorFailsTheLookup() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThat(parts.resolver.resolve(List.of("대한민국"))).isEmpty();
    assertThat(requests(wikimedia)).as("first request and one retry").hasSize(2);
  }

  @Test
  void failureOfTheSecondBatchFailsTheWholeLookup() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .atPriority(1)
            .withQueryParam("titles", equalTo("시험 문서 51"))
            .willReturn(aResponse().withStatus(404)));
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH))
            .atPriority(5)
            .willReturn(okJson(KoWikiSourceFixture.EMPTY_PAGEPROPS)));
    List<String> titles = IntStream.rangeClosed(1, 51).mapToObj(i -> "시험 문서 " + i).toList();

    assertThat(parts.resolver.resolve(titles)).isEmpty();
  }

  @Test
  void ratelimitedErrorFailsTheLookupAndBlocksTheHost() {
    wikimedia.stubFor(
        get(urlPathEqualTo(PATH)).willReturn(okJson(read("synthetic/synthetic-ratelimited.json"))));

    assertThat(parts.resolver.resolve(List.of("대한민국"))).isEmpty();
    assertThat(parts.http.get("kowiki", "대한민국", parts.properties.kowikiApiUrl()))
        .isEqualTo(new Unavailable(Reason.RATE_LIMITED));
    assertThat(requests(wikimedia)).hasSize(1);
  }

  @Test
  void invalidJsonOrNoQueryFailsTheLookup() {
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{not json")));
    assertThat(parts.resolver.resolve(List.of("대한민국"))).isEmpty();

    wikimedia.resetAll();
    wikimedia.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{\"batchcomplete\":true}")));
    assertThat(parts.resolver.resolve(List.of("대한민국"))).isEmpty();
  }
}
