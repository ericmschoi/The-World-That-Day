package io.github.ericmschoi.theworldthatday.wikimedia;

import java.util.Optional;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * The {@code error} check of the media adapters' Action API answers (plan M3a3d T0, PR #20 review
 * recommendation 5). Any {@code error} key is a failure, with or without a code. The log cause
 * names the code only when it is {@code [a-z0-9-]{1,64}}, so a remote value can never shape a log
 * line.
 */
public final class ActionApiErrors {

  private static final Pattern LOGGED_CODE = Pattern.compile("[a-z0-9-]{1,64}");

  private ActionApiErrors() {}

  /** {@code "api error"} or {@code "api error <code>"} when the answer has an error, else empty. */
  public static Optional<String> cause(JsonNode tree) {
    if (!tree.has("error")) {
      return Optional.empty();
    }
    String code = code(tree);
    return Optional.of(LOGGED_CODE.matcher(code).matches() ? "api error " + code : "api error");
  }

  /** The error {@code ratelimited} (sent with HTTP 200), which blocks the host as a 429 does. */
  public static boolean isRateLimited(JsonNode tree) {
    return "ratelimited".equals(code(tree));
  }

  private static String code(JsonNode tree) {
    JsonNode code = tree.path("error").path("code");
    return code.isValueNode() ? code.asString("").strip() : "";
  }
}
