package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Plan M3a4 T1 (2): the JSON of a {@link Selection}. */
class SelectionJsonTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void writesTheBasisAsLowercaseSnakeAndTheScoreAsAnInteger() {
    JsonNode node = JSON.valueToTree(new Selection(Basis.COMMONS_P10, 130));

    assertThat(JSON.writeValueAsString(node))
        .isEqualTo("{\"basis\":\"commons_p10\",\"score\":130}");
    assertThat(node.get("score").isInt()).isTrue();
  }

  @Test
  void everyBasisHasItsSnakeName() {
    assertThat(Arrays.stream(Basis.values()).map(Basis::value))
        .containsExactly(
            "commons_p10",
            "commons_category",
            "commons_date_category",
            "commons_search",
            "youtube_official",
            "daehan_news");
  }

  @Test
  void basisIsRequired() {
    assertThatNullPointerException().isThrownBy(() -> new Selection(null, 60)).withMessage("basis");
  }
}
