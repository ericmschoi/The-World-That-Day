package io.github.ericmschoi.theworldthatday.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a3b T0 (5): {@link MediaText} is public, and {@link MediaText#strip} removes the same
 * space set {@link MediaText#isBlank} uses. Space characters are written as escapes.
 */
class MediaTextTest {

  @Test
  void classAndMethodsArePublic() throws Exception {
    assertThat(Modifier.isPublic(MediaText.class.getModifiers())).isTrue();
    assertThat(Modifier.isPublic(MediaText.class.getMethod("isBlank", String.class).getModifiers()))
        .isTrue();
    assertThat(Modifier.isPublic(MediaText.class.getMethod("strip", String.class).getModifiers()))
        .isTrue();
  }

  @Test
  void stripRemovesUnicodeSpacesAtBothEndsOnly() {
    assertThat(MediaText.strip("Example Author ")).isEqualTo("Example Author");
    assertThat(MediaText.strip("　Example Author")).isEqualTo("Example Author");
    assertThat(MediaText.strip("  Example Author\t \u001F")).isEqualTo("Example Author");
    assertThat(MediaText.strip("x")).isEqualTo("x");
  }

  @Test
  void stripOfBlankIsEmptyAndOfNullIsNull() {
    assertThat(MediaText.strip(" 　 \n")).isEmpty();
    assertThat(MediaText.strip("")).isEmpty();
    assertThat(MediaText.strip(null)).isNull();
  }

  @Test
  void stripKeepsCharactersOutsideTheSet() {
    // zero-width and format characters are not spaces (plan M3a3a risk 7 (b))
    assertThat(MediaText.strip("​Example﻿")).isEqualTo("​Example﻿");
    // a character outside the Basic Multilingual Plane next to a space
    assertThat(MediaText.strip(" 😀 ")).isEqualTo("😀");
  }

  @Test
  void stripAgreesWithIsBlank() {
    for (String value : MediaBlankParityTest.PLAN_SPACES) {
      assertThat(MediaText.strip(value + "a" + value)).isEqualTo("a");
      assertThat(MediaText.isBlank(value)).isTrue();
    }
  }
}
