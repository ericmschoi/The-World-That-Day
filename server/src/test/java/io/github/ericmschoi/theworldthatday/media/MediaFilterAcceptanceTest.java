package io.github.ericmschoi.theworldthatday.media;

import static io.github.ericmschoi.theworldthatday.media.MediaRecords.image;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.imageEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.record;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoEmbed;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.videoFile;
import static io.github.ericmschoi.theworldthatday.media.MediaRecords.with;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.ericmschoi.theworldthatday.media.MediaFilter.Reason;
import io.github.ericmschoi.theworldthatday.media.MediaProperties.BlockEntry;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Plan M3a3a T3 (4) and (8), T2 (5) and (6): every blocklist and exclusion case run at DEBUG (root
 * and filter loggers) leaves no file name, author, credit or channel in the output; the filter does
 * not use {@link MediaNormalizer}; and the public parts M3a3b and M3a4 call keep their shape. All
 * names and IDs are synthetic.
 */
@ExtendWith(OutputCaptureExtension.class)
class MediaFilterAcceptanceTest {

  private static final List<BlockEntry> BLOCKLIST =
      List.of(
          new BlockEntry("commons", "Synthetic blocked photo.jpg", null, null),
          new BlockEntry("commons", null, "Synthetic Blocked Author", null),
          new BlockEntry("youtube", "ZZZZZZZZZZZ", null, null),
          new BlockEntry("youtube", null, null, "UCsyntheticsyntheticsyn0"));

  private final MediaFilter filter =
      new MediaFilter(
          new MediaProperties(List.of(), List.of(), BLOCKLIST, MediaFilterTest.INITIAL_CREDITS));

  /** One case per rule of the plan's "차단과 제외" table, plus kept items. */
  private record Case(Map<String, Object> fields, MediaOrigin origin, Reason expected) {}

  private static List<Case> cases() {
    List<Case> cases = new ArrayList<>();
    cases.add(
        new Case(
            image(),
            new MediaOrigin("commons", "Synthetic_blocked_photo.jpg", null, null, null),
            Reason.BLOCKED_ID));
    cases.add(
        new Case(
            image(),
            new MediaOrigin("commons", "Kept one.jpg", "synthetic blocked author", null, null),
            Reason.BLOCKED_AUTHOR));
    cases.add(
        new Case(
            videoEmbed(),
            new MediaOrigin("youtube", "ZZZZZZZZZZZ", null, null, null),
            Reason.BLOCKED_ID));
    cases.add(
        new Case(
            videoEmbed(),
            new MediaOrigin("youtube", "YYYYYYYYYYY", null, null, "UCsyntheticsyntheticsyn0"),
            Reason.BLOCKED_CHANNEL));
    cases.add(
        new Case(
            image(),
            new MediaOrigin("commons", "Credit one.jpg", null, "AP Photo/Synthetic Shooter", null),
            Reason.EXCLUDED_CREDIT));
    cases.add(
        new Case(
            image(),
            new MediaOrigin("commons", "Credit two.jpg", null, "ⓒ연합뉴스 합성기자", null),
            Reason.EXCLUDED_CREDIT));
    cases.add(
        new Case(
            with(image(), "source_name", "Reuters Synthetic Desk"),
            new MediaOrigin("commons", "Credit three.jpg", null, null, null),
            Reason.EXCLUDED_CREDIT));
    cases.add(
        new Case(
            with(image(), "attribution", "Synthetic Writer · AFP · Wikimedia Commons"),
            new MediaOrigin("commons", "Credit four.jpg", null, null, null),
            Reason.EXCLUDED_CREDIT));
    cases.add(
        new Case(
            videoFile(),
            new MediaOrigin("commons", "Credit five.webm", "Getty Synthetic Staff", null, null),
            Reason.EXCLUDED_CREDIT));
    cases.add(
        new Case(
            image(),
            new MediaOrigin("commons", "Kept two.jpg", null, "Japan Synthetic Society", null),
            null));
    cases.add(
        new Case(
            with(imageEmbed(), "attribution", "뉴시스 Synthetic Embed"),
            new MediaOrigin("getty", "123456789", null, "EPA Synthetic", null),
            null));
    return cases;
  }

  @Test
  void debugOutputOfEveryCaseHoldsOnlyReasonNames(CapturedOutput output) {
    Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Logger own = (Logger) LoggerFactory.getLogger(MediaFilter.class);
    Level rootBefore = root.getLevel();
    Level ownBefore = own.getLevel();
    root.setLevel(Level.DEBUG);
    own.setLevel(Level.DEBUG);
    List<Optional<Reason>> results = new ArrayList<>();
    try {
      for (Case c : cases()) {
        results.add(filter.check(record(c.fields()), c.origin()));
      }
    } finally {
      root.setLevel(rootBefore);
      own.setLevel(ownBefore);
    }

    List<Case> cases = cases();
    for (int i = 0; i < cases.size(); i++) {
      assertThat(results.get(i))
          .as("case %d", i)
          .isEqualTo(Optional.ofNullable(cases.get(i).expected()));
    }
    String log = output.getAll();
    assertThat(log)
        .contains("reason=blocked_id", "reason=blocked_author", "reason=blocked_channel")
        .contains("reason=excluded_credit");
    for (String secret :
        List.of(
            "Synthetic",
            "synthetic",
            "blocked_photo",
            "blocked photo",
            "Kept one",
            "Kept two",
            "Credit one",
            "Credit five",
            "ZZZZZZZZZZZ",
            "YYYYYYYYYYY",
            "UCsynthetic",
            "123456789",
            "AP Photo",
            "연합뉴스",
            "합성기자",
            "뉴시스",
            "Reuters",
            "AFP",
            "Getty",
            "EPA",
            "Japan",
            "example-image",
            "1988-09-17",
            "country:KR")) {
      assertThat(log).as("output holds %s", secret).doesNotContain(secret);
    }
  }

  @Test
  void keptItemsLogNothing(CapturedOutput output) {
    Logger own = (Logger) LoggerFactory.getLogger(MediaFilter.class);
    Level before = own.getLevel();
    own.setLevel(Level.TRACE);
    try {
      filter.check(record(image()), new MediaOrigin("commons", "Kept.jpg", "Someone", null, null));
    } finally {
      own.setLevel(before);
    }

    assertThat(output.getAll()).doesNotContain("Media filtered");
  }

  // ---- T2 (5): the filter does not use the normalizer ----

  @Test
  void filterNeitherHoldsNorTakesTheNormalizer() {
    for (Field field : MediaFilter.class.getDeclaredFields()) {
      assertThat(field.getType()).as(field.getName()).isNotEqualTo(MediaNormalizer.class);
    }
    for (Constructor<?> constructor : MediaFilter.class.getConstructors()) {
      assertThat(constructor.getParameterTypes()).containsExactly(MediaProperties.class);
    }
    for (Method method : MediaFilter.class.getDeclaredMethods()) {
      assertThat(method.getParameterTypes()).doesNotContain(MediaNormalizer.class);
      assertThat(method.getReturnType()).isNotEqualTo(MediaNormalizer.class);
    }
  }

  @Test
  void checkLeavesTheRecordUnchanged() {
    MediaRecord input = record(image());
    MediaRecord copy = record(image());

    filter.check(input, new MediaOrigin("commons", "Credit.jpg", null, "AP", null));

    assertThat(input).isEqualTo(copy);
  }

  // ---- T3 (8): the public parts M3a3b and M3a4 call ----

  private static List<String> publicMethods(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
        .map(
            m ->
                (Modifier.isStatic(m.getModifiers()) ? "static " : "")
                    + m.getReturnType().getSimpleName()
                    + " "
                    + m.getName()
                    + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).toList())
        .sorted()
        .toList();
  }

  @Test
  void publicPartsKeepTheirShape() throws Exception {
    assertThat(Modifier.isPublic(CaptureTimeParser.class.getModifiers())).isTrue();
    assertThat(publicMethods(CaptureTimeParser.class))
        .containsExactly("static CaptureTime parse[String]");
    assertThat(publicMethods(LicenseClassifier.class))
        .containsExactly("static LicenseStatus classify[String]");
    assertThat(publicMethods(MediaFilter.class))
        .containsExactly("Optional check[MediaRecord, MediaOrigin]");
    assertThat(CaptureTime.class.isRecord()).isTrue();
    assertThat(MediaOrigin.class.isRecord()).isTrue();
    assertThat(
            Arrays.stream(MediaOrigin.class.getRecordComponents()).map(c -> c.getName()).toList())
        .containsExactly("source", "itemId", "author", "credit", "channel");
    assertThat(
            Arrays.stream(CaptureTime.class.getRecordComponents()).map(c -> c.getName()).toList())
        .containsExactly("capturedAt", "datePrecision");
    assertThat(Arrays.stream(BlockEntry.class.getRecordComponents()).map(c -> c.getName()).toList())
        .containsExactly("source", "id", "author", "channel");
    assertThat(Arrays.stream(Reason.values()).map(Reason::value))
        .containsExactly("blocked_id", "blocked_author", "blocked_channel", "excluded_credit");
    assertThat(CaptureTime.class.getField("UNKNOWN").get(null)).isEqualTo(CaptureTime.UNKNOWN);
    assertThat(CaptureTimeParser.class.getDeclaredConstructors())
        .allSatisfy(c -> assertThat(Modifier.isPrivate(c.getModifiers())).isTrue());
    assertThat(LicenseClassifier.class.getDeclaredConstructors())
        .allSatisfy(c -> assertThat(Modifier.isPrivate(c.getModifiers())).isTrue());
  }

  @Test
  void publicPartsNeverThrowOnOddInput() {
    List<String> odd =
        Arrays.asList(null, "", "\u0000", "<", "<<>>", "&nbsp;", "\uD800", "9".repeat(10_000));
    for (String value : odd) {
      CaptureTimeParser.parse(value);
      LicenseClassifier.classify(value);
      filter.check(record(image()), new MediaOrigin(value, value, value, value, value));
      filter.check(record(image()), new MediaOrigin("commons", value, value, value, value));
    }
  }
}
