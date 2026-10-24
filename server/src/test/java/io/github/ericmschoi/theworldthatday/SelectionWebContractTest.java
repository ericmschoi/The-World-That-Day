package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.media.Selection.Basis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a4 T1 (2): the server's {@code selection.basis} values are the six strings of the web's
 * {@code MediaSelection.basis} in {@code web/src/media/types.ts}, read from the repository.
 */
class SelectionWebContractTest {

  private static final Pattern SELECTION_TYPE =
      Pattern.compile("export type MediaSelection = \\{(.*?)\\n}", Pattern.DOTALL);
  private static final Pattern BASIS_UNION =
      Pattern.compile("basis:((?:\\s*\\|\\s*'[a-z0-9_]+')+)");
  private static final Pattern QUOTED = Pattern.compile("'([a-z0-9_]+)'");

  @Test
  void basisValuesMatchTheWebType() {
    String types = RepositoryFiles.read("web/src/media/types.ts");
    Matcher type = SELECTION_TYPE.matcher(types);
    assertThat(type.find()).as("MediaSelection type in types.ts").isTrue();
    Matcher union = BASIS_UNION.matcher(type.group(1));
    assertThat(union.find()).as("basis union in MediaSelection").isTrue();
    List<String> web = new ArrayList<>();
    Matcher quoted = QUOTED.matcher(union.group(1));
    while (quoted.find()) {
      web.add(quoted.group(1));
    }

    assertThat(web).hasSize(6);
    assertThat(Arrays.stream(Basis.values()).map(Basis::value))
        .containsExactlyInAnyOrderElementsOf(web);
  }
}
