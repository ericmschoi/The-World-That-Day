package io.github.ericmschoi.theworldthatday.event.kowiki;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The recorded English Wikipedia fixtures are what plan C1 S3a T1 says they are: the section list
 * and the {@code Events} section of the ten demo pages, and for each demo date the link lookup, the
 * lead image lookup, the Commons file batch and the Wikidata signals. Every file has a README row
 * with its recording time and original URL, the URLs are the ones the server builds (same
 * parameters, same encoding), page rows carry the revision of the file, and nothing holds an email
 * address. Reads files only; never calls the network.
 */
class EnWikiFixtureIntegrityTest {

  private static final Path ROOT = Path.of("src/test/resources/fixtures/wikimedia/enwiki");
  private static final Path WIKIDATA_README =
      Path.of("src/test/resources/fixtures/wikimedia/wikidata/README.md");
  private static final String WIKI_API = "https://en.wikipedia.org/w/api.php";
  private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
  private static final String SPARQL = "https://query.wikidata.org/sparql";
  private static final String USER_AGENT =
      "TheWorldThatDay/0.1 (https://github.com/ericmschoi/The-World-That-Day)";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** Demo pages by fixture prefix (plan decision 1 (b) and the video sample 1969-07-20). */
  private static final Map<String, String> PAGES = new LinkedHashMap<>();

  static {
    PAGES.put("02-13", "February 13");
    PAGES.put("07-17", "July 17");
    PAGES.put("02-12", "February 12");
    PAGES.put("04-12", "April 12");
    PAGES.put("07-20", "July 20");
    PAGES.put("1988", "1988");
    PAGES.put("1976", "1976");
    PAGES.put("2010", "2010");
    PAGES.put("1980", "1980");
    PAGES.put("1969", "1969");
  }

  private static final List<String> DATES =
      List.of("1988-02-13", "1976-07-17", "2010-02-12", "1980-04-12", "1969-07-20");

  /** Plan T1 (4), the same expression. */
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

  private static final Pattern ANY_ROW =
      Pattern.compile("^\\| `([^`]+\\.json)` \\|(.*)\\| `(https://[^`]+)` \\|$");
  private static final Pattern RECORDED =
      Pattern.compile("\\| (2026-10-30 \\d\\d:\\d\\d:\\d\\d) \\|");
  private static final Pattern PAGE_ROW =
      Pattern.compile(
          "^\\| `([^`]+\\.json)` \\| ([^|]+?) \\| (\\d+) \\| [-0-9: ]+ \\| `([^`]+)` \\|$");
  private static final Pattern LOOKUP_ROW =
      Pattern.compile(
          "^\\| `((?:pageprops|pageimages|commons-files)-\\d{4}-\\d\\d-\\d\\d\\.json)` \\|"
              + " (\\d{4}-\\d\\d-\\d\\d) \\| (.+?) \\| [-0-9: ]+ \\| `([^`]+)` \\|$");
  private static final Pattern SIGNALS_ROW =
      Pattern.compile(
          "^\\| `(wikidata-signals-[^`]+\\.json)` \\| (\\d{4}-\\d\\d-\\d\\d) \\| ([Q0-9 ]+) \\|"
              + " [-0-9: ]+ \\| [0-9.]+ \\| `([^`]+)` \\|$");
  private static final Pattern QUOTED = Pattern.compile("`([^`]+)`");
  private static final Pattern LEVEL2_HEADING =
      Pattern.compile("(?m)^==(?!=)\\s*(.*?)\\s*==(?!=)\\s*$");
  private static final Pattern OUT_OF_SCOPE_HEADING =
      Pattern.compile("(?m)^=+\\s*(Births|Deaths|Holidays and observances)\\s*=+\\s*$");
  private static final Pattern ITEM_VALUES = Pattern.compile("VALUES \\?item \\{ [^}]* \\}");

  private static String read(Path file) throws IOException {
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static JsonNode json(String file) throws IOException {
    return JSON.readTree(read(ROOT.resolve(file)));
  }

  private static String readme() throws IOException {
    return read(ROOT.resolve("README.md"));
  }

  private static List<String> jsonFiles() throws IOException {
    try (Stream<Path> list = Files.list(ROOT)) {
      return list.map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .sorted()
          .toList();
    }
  }

  /** README table rows matching {@code row}, by file name. */
  private static Map<String, Matcher> rows(Pattern row) throws IOException {
    Map<String, Matcher> rows = new LinkedHashMap<>();
    for (String line : readme().split("\n")) {
      Matcher matcher = row.matcher(line.strip());
      if (matcher.matches()) {
        assertThat(rows.put(matcher.group(1), matcher)).as("one row for " + line).isNull();
      }
    }
    return rows;
  }

  private static UriComponentsBuilder wikiQuery() {
    return UriComponentsBuilder.fromUriString(WIKI_API).queryParam("action", "query");
  }

  /** The URI string of a builder whose one template variable is {@code value}, as the code does. */
  private static String expand(UriComponentsBuilder builder, String value) {
    return builder
        .queryParam("format", "json")
        .queryParam("formatversion", 2)
        .encode()
        .buildAndExpand(value)
        .toUri()
        .toString();
  }

  private static List<String> quoted(String cell) {
    return QUOTED.matcher(cell).results().map(m -> m.group(1)).toList();
  }

  @Test
  void twentyPageFilesCoverTheTenDemoPages() throws IOException {
    List<String> expected =
        PAGES.keySet().stream()
            .flatMap(prefix -> Stream.of(prefix + "-tocdata.json", prefix + "-events.json"))
            .sorted()
            .toList();

    assertThat(
            jsonFiles().stream()
                .filter(n -> n.endsWith("-tocdata.json") || n.endsWith("-events.json")))
        .containsExactlyElementsOf(expected);
    assertThat(jsonFiles()).hasSize(38);
  }

  @Test
  void everyRecordedFileHasOneReadmeRowWithItsRecordingTimeAndUrl() throws IOException {
    Map<String, Matcher> rows = rows(ANY_ROW);

    assertThat(rows.keySet()).containsExactlyInAnyOrderElementsOf(jsonFiles());
    rows.forEach(
        (file, row) -> {
          assertThat(RECORDED.matcher("|" + row.group(2) + "|").find()).as(file).isTrue();
          assertThat(row.group(3)).as(file).doesNotContain("prop=sections");
        });
  }

  @Test
  void pageRowsAreTheRequestsTheServerSendsPinnedToTheRecordedRevision() throws IOException {
    Map<String, Matcher> rows = rows(PAGE_ROW);

    for (Map.Entry<String, String> page : PAGES.entrySet()) {
      String prefix = page.getKey();
      String title = page.getValue();
      JsonNode toc = json(prefix + "-tocdata.json").path("parse");
      long revid = toc.path("revid").asLong(0);
      String index = null;
      for (JsonNode section : toc.path("tocdata").path("sections")) {
        if (section.path("hLevel").asInt(0) == 2
            && "Events".equals(section.path("line").asString(""))) {
          index = section.path("index").asString(null);
          break;
        }
      }
      assertThat(toc.path("title").asString()).isEqualTo(title);
      assertThat(revid).as(prefix).isPositive();
      assertThat(index).as(prefix).isEqualTo(prefix.equals("2010") ? "2" : "1");

      Matcher tocRow = rows.get(prefix + "-tocdata.json");
      assertThat(tocRow.group(2)).isEqualTo(title);
      assertThat(Long.parseLong(tocRow.group(3))).isEqualTo(revid);
      assertThat(tocRow.group(4))
          .isEqualTo(
              UriComponentsBuilder.fromUriString(WIKI_API)
                  .queryParam("action", "parse")
                  .queryParam("page", title)
                  .queryParam("prop", "tocdata|revid")
                  .queryParam("redirects", 1)
                  .queryParam("format", "json")
                  .queryParam("formatversion", 2)
                  .build()
                  .encode()
                  .toUri()
                  .toString());

      JsonNode events = json(prefix + "-events.json").path("parse");
      Matcher eventsRow = rows.get(prefix + "-events.json");
      assertThat(eventsRow.group(2)).isEqualTo(title + " (Events)");
      assertThat(Long.parseLong(eventsRow.group(3))).isEqualTo(revid);
      assertThat(events.path("revid").asLong(0)).isEqualTo(revid);
      assertThat(events.path("title").asString()).isEqualTo(title);
      assertThat(eventsRow.group(4))
          .isEqualTo(
              WIKI_API
                  + "?action=parse&oldid="
                  + revid
                  + "&prop=wikitext&section="
                  + index
                  + "&format=json&formatversion=2");
    }
  }

  @Test
  void eventsFilesHoldOnlyTheLevel2EventsSection() throws IOException {
    for (String prefix : PAGES.keySet()) {
      String wikitext = json(prefix + "-events.json").path("parse").path("wikitext").asString();

      assertThat(wikitext).as(prefix).startsWith("==Events==\n");
      assertThat(LEVEL2_HEADING.matcher(wikitext).results().map(m -> m.group(1)))
          .as(prefix)
          .containsExactly("Events");
      assertThat(OUT_OF_SCOPE_HEADING.matcher(wikitext).find()).as(prefix).isFalse();
    }
  }

  @Test
  void lookupRowsAreTheRequestsTheServerSendsForTheirTitles() throws IOException {
    Map<String, Matcher> rows = rows(LOOKUP_ROW);

    assertThat(rows.keySet())
        .containsExactlyInAnyOrderElementsOf(
            jsonFiles().stream()
                .filter(
                    n ->
                        n.startsWith("pageprops-")
                            || n.startsWith("pageimages-")
                            || n.startsWith("commons-files-"))
                .toList());
    for (String date : DATES) {
      assertThat(rows).containsKeys("pageprops-" + date + ".json", "pageimages-" + date + ".json");
    }
    rows.forEach(
        (file, row) -> {
          assertThat(file).endsWith(row.group(2) + ".json");
          String titles = String.join("|", quoted(row.group(3)));
          String expected =
              file.startsWith("pageprops-")
                  ? expand(
                      wikiQuery()
                          .queryParam("prop", "pageprops")
                          .queryParam("ppprop", "wikibase_item")
                          .queryParam("redirects", 1)
                          .queryParam("titles", "{titles}"),
                      titles)
                  : file.startsWith("pageimages-")
                      ? expand(
                          wikiQuery()
                              .queryParam("prop", "pageimages")
                              .queryParam("piprop", "name")
                              .queryParam("pilicense", "free")
                              .queryParam("pilimit", 50)
                              .queryParam("redirects", 1)
                              .queryParam("titles", "{titles}"),
                          titles)
                      : expand(
                          UriComponentsBuilder.fromUriString(COMMONS_API)
                              .queryParam("action", "query")
                              .queryParam("titles", "{titles}")
                              .queryParam("prop", "imageinfo")
                              .queryParam("iiprop", "url|mime|size|extmetadata")
                              .queryParam("iiurlwidth", 640),
                          titles);
          assertThat(row.group(4)).as(file).isEqualTo(expected);
        });
  }

  /**
   * Each date's Commons batch asks for exactly the files its lead image answer named, in order;
   * dates without a lead image have no batch, and every asked file came back.
   */
  @Test
  void commonsBatchesAskForTheLeadImagesAndHoldThem() throws IOException {
    for (String date : DATES) {
      List<String> images = new ArrayList<>();
      Map<String, String> byTitle = new HashMap<>();
      for (JsonNode page : json("pageimages-" + date + ".json").path("query").path("pages")) {
        String image = page.path("pageimage").asString("");
        if (!image.isEmpty()) {
          byTitle.put(page.path("title").asString(), image);
        }
      }
      for (String title : quoted(rows(LOOKUP_ROW).get("pageimages-" + date + ".json").group(3))) {
        if (byTitle.containsKey(title)) {
          images.add("File:" + byTitle.get(title));
        }
      }
      String batch = "commons-files-" + date + ".json";
      if (images.isEmpty()) {
        assertThat(jsonFiles()).as(date).doesNotContain(batch);
        continue;
      }
      assertThat(quoted(rows(LOOKUP_ROW).get(batch).group(3))).as(date).isEqualTo(images);
      JsonNode pages = json(batch).path("query").path("pages");
      assertThat(pages.size()).as(date).isEqualTo(images.size());
      pages.forEach(
          page -> {
            assertThat(page.has("missing")).as(date).isFalse();
            assertThat(page.path("imageinfo").path(0).path("extmetadata").isObject())
                .as(date)
                .isTrue();
          });
    }
  }

  /**
   * The signal queries ask for the QIDs of the lead image titles (the link lookups give them), and
   * are the query {@code SparqlWikidataSignalSource} sends: the recorded query of {@code
   * wikidata/README.md} (checked against the code by {@code SparqlWikidataSignalSourceTest}) with
   * only the item list changed.
   */
  @Test
  void signalRowsQueryTheMainLinkItemsWithTheServersQuery() throws IOException {
    Map<String, Matcher> rows = rows(SIGNALS_ROW);
    Matcher block = Pattern.compile("```sparql\n(.+)\n```").matcher(read(WIKIDATA_README));
    assertThat(block.find()).isTrue();
    String recordedQuery = block.group(1);

    assertThat(rows.keySet())
        .containsExactlyInAnyOrderElementsOf(
            jsonFiles().stream().filter(n -> n.startsWith("wikidata-signals-")).toList());
    for (String date : DATES) {
      Matcher row = rows.get("wikidata-signals-" + date + ".json");
      Map<String, String> qids = new HashMap<>();
      for (JsonNode page : json("pageprops-" + date + ".json").path("query").path("pages")) {
        qids.put(
            page.path("title").asString(),
            page.path("pageprops").path("wikibase_item").asString(null));
      }
      List<String> mainLinks =
          quoted(rows(LOOKUP_ROW).get("pageimages-" + date + ".json").group(3)).stream()
              .map(qids::get)
              .toList();
      assertThat(List.of(row.group(3).split(" "))).as(date).isEqualTo(mainLinks);

      String items = String.join(" ", mainLinks.stream().map(qid -> "wd:" + qid).toList());
      String query =
          ITEM_VALUES.matcher(recordedQuery).replaceFirst("VALUES ?item { " + items + " }");
      assertThat(row.group(4))
          .as(date)
          .isEqualTo(
              UriComponentsBuilder.fromUriString(SPARQL)
                  .queryParam("format", "json")
                  .queryParam("query", "{query}")
                  .encode()
                  .buildAndExpand(query)
                  .toUri()
                  .toString());
      assertThat(
              json("wikidata-signals-" + date + ".json").path("results").path("bindings").isArray())
          .as(date)
          .isTrue();
    }
  }

  @Test
  void readmeNamesTheRepositoryUserAgentTheLicencesAndNoMediaDownload() throws IOException {
    String readme = readme();

    assertThat(readme)
        .contains(USER_AGENT)
        .contains("CC BY-SA 4.0")
        .contains("CC0")
        .contains("No media file was downloaded");
    Matcher agent = Pattern.compile("TheWorldThatDay/[^ ]+ \\(([^)]*)\\)").matcher(readme);
    while (agent.find()) {
      assertThat(agent.group(1)).isEqualTo("https://github.com/ericmschoi/The-World-That-Day");
    }
  }

  /** Plan T1 (4): no recorded file and no README line holds an email address. */
  @Test
  void noFileHoldsAnEmailAddress() throws IOException {
    try (Stream<Path> walk = Files.walk(ROOT)) {
      for (Path file : walk.filter(Files::isRegularFile).toList()) {
        assertThat(EMAIL.matcher(read(file)).find()).as(file.toString()).isFalse();
      }
    }
  }
}
