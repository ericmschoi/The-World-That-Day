package io.github.ericmschoi.theworldthatday;

import static io.github.ericmschoi.theworldthatday.RepositoryFiles.map;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** .github/workflows/server-ci.yml (plan M1a T4). */
class ServerCiWorkflowTest {

  private static final String WORKFLOW = ".github/workflows/server-ci.yml";
  private static final List<String> PATHS =
      List.of("server/**", "docker-compose.yml", ".github/workflows/server-ci.yml");

  private static Map<Object, Object> workflow() {
    return RepositoryFiles.yaml(WORKFLOW);
  }

  private static Map<Object, Object> triggers() {
    Map<Object, Object> workflow = workflow();
    // YAML 1.1 reads the bare key `on` as boolean true.
    Object on = workflow.containsKey("on") ? workflow.get("on") : workflow.get(Boolean.TRUE);
    return map(on);
  }

  private static Map<Object, Object> serverJob() {
    return map(map(workflow().get("jobs")).get("server"));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<Object, Object>> steps() {
    return (List<Map<Object, Object>>) serverJob().get("steps");
  }

  @Test
  void runsOnPullRequestsForServerPaths() {
    Map<Object, Object> pullRequest = map(triggers().get("pull_request"));

    assertThat(pullRequest.get("paths")).isEqualTo(PATHS);
  }

  @Test
  void runsOnPushToMainForServerPaths() {
    Map<Object, Object> push = map(triggers().get("push"));

    assertThat(push.get("branches")).isEqualTo(List.of("main"));
    assertThat(push.get("paths")).isEqualTo(PATHS);
  }

  @Test
  void hasReadOnlyContentsPermission() {
    assertThat(workflow().get("permissions")).isEqualTo(Map.of("contents", "read"));
  }

  @Test
  void hasTimeout() {
    assertThat(serverJob().get("timeout-minutes")).isInstanceOf(Integer.class);
    assertThat((Integer) serverJob().get("timeout-minutes")).isPositive();
  }

  @Test
  void setsUpTemurinJava21() {
    assertThat(steps())
        .anySatisfy(
            step -> {
              assertThat(step.get("uses")).asString().startsWith("actions/setup-java@");
              Map<Object, Object> with = map(step.get("with"));
              assertThat(with.get("distribution")).isEqualTo("temurin");
              assertThat(with.get("java-version")).asString().isEqualTo("21");
            });
  }

  @Test
  void cachesGradleWithSetupGradleAction() {
    assertThat(steps())
        .anySatisfy(
            step ->
                assertThat(step.get("uses")).asString().startsWith("gradle/actions/setup-gradle@"));
  }

  @Test
  void runsGradleCheckInServerDirectory() {
    Map<Object, Object> run = map(map(serverJob().get("defaults")).get("run"));

    assertThat(run.get("working-directory")).isEqualTo("server");
    assertThat(steps())
        .anySatisfy(step -> assertThat(step.get("run")).isEqualTo("./gradlew check"));
  }

  @Test
  void usesNoSecretsAndNoDeployOrUploadSteps() {
    assertThat(RepositoryFiles.read(WORKFLOW)).doesNotContain("secrets.");
    assertThat(map(workflow().get("jobs"))).containsOnlyKeys("server");
    assertThat(steps())
        .allSatisfy(
            step ->
                assertThat(String.valueOf(step.get("uses")))
                    .doesNotContainIgnoringCase("upload")
                    .doesNotContainIgnoringCase("deploy"));
  }
}
