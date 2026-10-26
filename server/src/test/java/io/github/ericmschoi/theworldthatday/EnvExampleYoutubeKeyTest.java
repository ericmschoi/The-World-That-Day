package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Plan M3a5 T1 (4), T7 (6): the YouTube key is a server-only, optional, valueless name. */
class EnvExampleYoutubeKeyTest {

  @Test
  void theKeyIsListedCommentedOutWithNoValueAndNeverAsABrowserName() {
    List<String> lines = RepositoryFiles.read(".env.example").lines().toList();

    // Optional, so commented out: an empty value would override the application.yml default.
    assertThat(lines).contains("# YOUTUBE_API_KEY=");
    assertThat(lines).doesNotContain("YOUTUBE_API_KEY=");
    assertThat(lines).noneMatch(line -> line.matches("^\\s*YOUTUBE_API_KEY\\s*=.*"));
    assertThat(lines).noneMatch(line -> line.matches("^\\s*#?\\s*VITE_\\w*YOUTUBE\\w*\\s*=.*"));
    assertThat(lines).noneMatch(line -> line.startsWith("VITE_"));
    assertThat(lines).anyMatch(line -> line.startsWith("#") && line.contains("YouTube"));
  }
}
