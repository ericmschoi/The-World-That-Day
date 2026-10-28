package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plan M3a3d T0 (4), PR #20 review recommendation 5: one {@code error} check for the media
 * adapters, which names a code in the log only when it is {@code [a-z0-9-]{1,64}}. Synthetic
 * answers.
 */
public class ActionApiErrorsTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static JsonNode read(String body) {
    return JSON.readTree(body);
  }

  /** {@code {"error":{"code":<code>,"info":"x"}}}, built as a tree so any string can be a code. */
  public static JsonNode errorWithCode(String code) {
    ObjectNode tree = JSON.createObjectNode();
    tree.putObject("error").put("code", code).put("info", "x");
    return tree;
  }

  /** Codes that must never reach a log line. */
  public static Stream<String> unsafeCodes() {
    return Stream.of("Bad Code", "x\ny", "internal_api_error_DBQueryError", "a".repeat(65), "");
  }

  @ParameterizedTest(name = "{0} is not an error")
  @ValueSource(
      strings = {
        "{\"batchcomplete\":true}",
        "{\"batchcomplete\":true,\"query\":{\"pages\":[]}}",
        "{\"warnings\":{\"main\":{\"warnings\":\"x\"}}}",
        "[]"
      })
  void answerWithoutAnErrorKeyHasNoCause(String body) {
    assertThat(ActionApiErrors.cause(read(body))).isEmpty();
    assertThat(ActionApiErrors.isRateLimited(read(body))).isFalse();
  }

  @Test
  void safeCodeIsNamed() {
    assertThat(ActionApiErrors.cause(errorWithCode("badvalue"))).contains("api error badvalue");
    assertThat(ActionApiErrors.cause(errorWithCode("a".repeat(64))))
        .contains("api error " + "a".repeat(64));
    assertThat(ActionApiErrors.cause(errorWithCode("maxlag-2"))).contains("api error maxlag-2");
  }

  @ParameterizedTest(name = "{0} is only an api error")
  @ValueSource(
      strings = {
        "{\"error\":{\"info\":\"x\"}}",
        "{\"error\":\"x\"}",
        "{\"error\":null}",
        "{\"error\":{\"code\":{\"info\":\"x\"}}}",
        "{\"error\":{\"code\":[\"badvalue\"]}}"
      })
  void errorWithoutACodeIsOnlyAnApiError(String body) {
    assertThat(ActionApiErrors.cause(read(body))).contains("api error");
  }

  @ParameterizedTest(name = "code [{0}] is not logged")
  @MethodSource("unsafeCodes")
  void unsafeCodeIsNeverNamed(String code) {
    assertThat(ActionApiErrors.cause(errorWithCode(code))).contains("api error");
  }

  @Test
  void ratelimitedIsToldFromItsOriginalCode() {
    assertThat(ActionApiErrors.isRateLimited(errorWithCode("ratelimited"))).isTrue();
    assertThat(ActionApiErrors.isRateLimited(errorWithCode(" ratelimited "))).isTrue();
    assertThat(ActionApiErrors.isRateLimited(errorWithCode("Ratelimited"))).isFalse();
    assertThat(ActionApiErrors.isRateLimited(errorWithCode("badvalue"))).isFalse();
    assertThat(ActionApiErrors.isRateLimited(read("{\"error\":{\"info\":\"x\"}}"))).isFalse();
  }

  @Test
  void hasCodeComparesTheTrimmedOriginalCode() {
    assertThat(ActionApiErrors.hasCode(errorWithCode("missingtitle"), "missingtitle")).isTrue();
    assertThat(ActionApiErrors.hasCode(errorWithCode(" missingtitle "), "missingtitle")).isTrue();
    assertThat(ActionApiErrors.hasCode(errorWithCode("Missingtitle"), "missingtitle")).isFalse();
    assertThat(ActionApiErrors.hasCode(errorWithCode("badvalue"), "missingtitle")).isFalse();
    assertThat(ActionApiErrors.hasCode(read("{\"error\":{\"info\":\"x\"}}"), "missingtitle"))
        .isFalse();
    assertThat(ActionApiErrors.hasCode(read("{\"batchcomplete\":true}"), "missingtitle")).isFalse();
  }
}
