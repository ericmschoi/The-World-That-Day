package io.github.ericmschoi.theworldthatday;

import static io.github.ericmschoi.theworldthatday.PlaceholderDefaults.defaultOf;
import static io.github.ericmschoi.theworldthatday.PlaceholderDefaults.sameDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ericmschoi.theworldthatday.PlaceholderDefaults.Syntax;
import org.junit.jupiter.api.Test;

/** Unit tests for the placeholder default reader (chore-m1-followups T1 (6)). */
class PlaceholderDefaultsTest {

  @Test
  void matchingDefaultsReturnTheSharedValue() {
    String compose = "POSTGRES_DB: ${POSTGRES_DB:-app}";
    String spring = "url: jdbc:postgresql://localhost/${POSTGRES_DB:app}";

    assertThat(sameDefault(compose, spring, "POSTGRES_DB")).isEqualTo("app");
  }

  @Test
  void differentDefaultsFail() {
    String compose = "ports: [\"${POSTGRES_PORT:-5432}:5432\"]";
    String spring = "url: jdbc:postgresql://localhost:${POSTGRES_PORT:5433}/db";

    assertThatThrownBy(() -> sameDefault(compose, spring, "POSTGRES_PORT"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("'5432'")
        .hasMessageContaining("'5433'");
  }

  @Test
  void missingComposeDefaultFails() {
    String compose = "POSTGRES_USER: ${POSTGRES_USER}";
    String spring = "username: ${POSTGRES_USER:postgres}";

    assertThatThrownBy(() -> sameDefault(compose, spring, "POSTGRES_USER"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("has no default");
  }

  @Test
  void missingSpringDefaultFails() {
    String compose = "POSTGRES_USER: ${POSTGRES_USER:-postgres}";
    String spring = "username: ${POSTGRES_USER}";

    assertThatThrownBy(() -> sameDefault(compose, spring, "POSTGRES_USER"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("has no default");
  }

  @Test
  void composeRequiredFormIsNotADefault() {
    String compose = "POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?required}";

    assertThatThrownBy(() -> defaultOf(compose, "POSTGRES_PASSWORD", Syntax.COMPOSE))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("has no default");
  }

  @Test
  void absentNameFails() {
    assertThatThrownBy(() -> defaultOf("other: ${OTHER:-x}", "POSTGRES_DB", Syntax.COMPOSE))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("no ${POSTGRES_DB} placeholder");
  }

  @Test
  void longerNameWithSamePrefixIsIgnored() {
    String compose = "a: ${POSTGRES_DB_EXTRA:-other}\nb: ${POSTGRES_DB:-app}";

    assertThat(defaultOf(compose, "POSTGRES_DB", Syntax.COMPOSE)).isEqualTo("app");
  }

  @Test
  void composeEscapedReferenceIsIgnored() {
    String compose =
        "POSTGRES_USER: ${POSTGRES_USER:-postgres}\n"
            + "test: [\"CMD-SHELL\", \"pg_isready -U $${POSTGRES_USER}\"]";

    assertThat(defaultOf(compose, "POSTGRES_USER", Syntax.COMPOSE)).isEqualTo("postgres");
  }

  @Test
  void emptyDefaultIsReturnedAsEmpty() {
    assertThat(defaultOf("a: ${POSTGRES_DB:}", "POSTGRES_DB", Syntax.SPRING)).isEmpty();
  }

  @Test
  void conflictingDefaultsInOneFileFail() {
    String spring = "a: ${POSTGRES_PORT:5432}\nb: ${POSTGRES_PORT:5433}";

    assertThatThrownBy(() -> defaultOf(spring, "POSTGRES_PORT", Syntax.SPRING))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("different defaults in one file");
  }
}
