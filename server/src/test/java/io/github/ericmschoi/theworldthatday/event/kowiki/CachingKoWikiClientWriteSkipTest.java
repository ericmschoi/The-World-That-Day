package io.github.ericmschoi.theworldthatday.event.kowiki;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiFixtures.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.ericmschoi.theworldthatday.event.kowiki.KoWikiPage.State;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Plan M3a1c T4 (9)~(11): after a cache read fails with a database error, the same {@code fetch}
 * does not write, so an unreachable database costs one wait per page. Other cache behaviour stays.
 */
@ExtendWith(OutputCaptureExtension.class)
class CachingKoWikiClientWriteSkipTest {

  @RegisterExtension
  static final WireMockExtension wikimedia =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final String TITLE = "9월 17일";
  private static final Instant PRECISE = Instant.parse("2026-01-01T00:00:00.123456789Z");

  private final KoWikiSourceFixture parts =
      new KoWikiSourceFixture(wikimedia, Duration.ofSeconds(2), Duration.ofSeconds(5));
  private final ActionApiKoWikiClient delegate = mock(ActionApiKoWikiClient.class);
  private final WikiSectionCacheRepository repository = mock(WikiSectionCacheRepository.class);

  private CachingKoWikiClient client(KoWikiClient pages) {
    return new CachingKoWikiClient(
        pages instanceof ActionApiKoWikiClient api ? api : delegate,
        repository,
        parts.properties,
        parts.clock);
  }

  private void databaseUnreachable() {
    when(repository.findBySiteAndPageTitle(anyString(), anyString()))
        .thenThrow(new CannotCreateTransactionException("no connection"));
  }

  private static long lines(CapturedOutput output, String text) {
    return Pattern.compile(Pattern.quote(text)).matcher(output.getAll()).results().count();
  }

  @Test
  void freshPageAfterAFailedReadIsNotWritten(CapturedOutput output) {
    databaseUnreachable();
    when(delegate.fetch(TITLE))
        .thenReturn(new KoWikiPage(TITLE, State.FRESH, 1L, "== 사건 ==", null, PRECISE));

    KoWikiPage page = client(delegate).fetch(TITLE);

    assertThat(page)
        .isEqualTo(
            new KoWikiPage(
                TITLE,
                State.FRESH,
                1L,
                "== 사건 ==",
                null,
                Instant.parse("2026-01-01T00:00:00.123456Z")));
    verify(repository, times(1)).findBySiteAndPageTitle("localhost", TITLE);
    verifyNoMoreInteractions(repository);
    assertThat(lines(output, "Wiki section cache read failed")).isEqualTo(1);
    assertThat(lines(output, "Wiki section cache write failed")).isZero();
  }

  @Test
  void unavailableOrAbsentPageAfterAFailedReadMakesNoOtherDatabaseCall() {
    databaseUnreachable();
    when(delegate.fetch(TITLE)).thenReturn(KoWikiPage.unavailable(TITLE));
    when(delegate.fetch("2월 30일")).thenReturn(KoWikiPage.absent("2월 30일"));

    assertThat(client(delegate).fetch(TITLE)).isEqualTo(KoWikiPage.unavailable(TITLE));
    assertThat(client(delegate).fetch("2월 30일")).isEqualTo(KoWikiPage.absent("2월 30일"));
    verify(repository, times(2)).findBySiteAndPageTitle(anyString(), anyString());
    verifyNoMoreInteractions(repository);
  }

  @Test
  void nextCallReadsTheCacheAgain() {
    databaseUnreachable();
    when(delegate.fetch(TITLE)).thenReturn(KoWikiPage.unavailable(TITLE));
    CachingKoWikiClient client = client(delegate);

    client.fetch(TITLE);
    client.fetch(TITLE);

    verify(repository, times(2)).findBySiteAndPageTitle("localhost", TITLE);
  }

  @Test
  void successfulReadWithAFailedWriteStillReturnsTheFreshPage(CapturedOutput output) {
    when(repository.findBySiteAndPageTitle(anyString(), anyString())).thenReturn(Optional.empty());
    doThrow(new CannotCreateTransactionException("no connection"))
        .when(repository)
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
    when(delegate.fetch(TITLE))
        .thenReturn(new KoWikiPage(TITLE, State.FRESH, 1L, null, null, PRECISE));

    assertThat(client(delegate).fetch(TITLE).state()).isEqualTo(State.FRESH);
    verify(repository, times(1))
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
    assertThat(lines(output, "Wiki section cache write failed: title=9월 17일")).isEqualTo(1);
  }

  @Test
  void successfulReadWithoutARowStillWritesAFreshPage() {
    when(repository.findBySiteAndPageTitle(anyString(), anyString())).thenReturn(Optional.empty());
    when(delegate.fetch(TITLE))
        .thenReturn(new KoWikiPage(TITLE, State.FRESH, 1L, null, null, PRECISE));

    client(delegate).fetch(TITLE);

    verify(repository, times(1))
        .upsert(anyString(), anyString(), anyLong(), any(), any(), any(Instant.class));
  }

  /** T4 (10): one database call per page for a whole day, same events as with a healthy cache. */
  @Test
  void dayWithAnUnreachableDatabaseCallsItOncePerPage() {
    KoWikiSourceFixture.stubRecorded(wikimedia, TITLE);
    KoWikiSourceFixture.stubRecorded(wikimedia, "1988년");
    KoWikiSourceFixture.stubPageprops(wikimedia, read("pageprops-1988-09-17.json"));
    LocalDate opening = LocalDate.of(1988, 9, 17);
    KoWikiDayResult healthy = parts.source(parts.pages).find(opening);
    databaseUnreachable();

    KoWikiDayResult result = parts.source(client(parts.pages)).find(opening);

    verify(repository, times(2)).findBySiteAndPageTitle(anyString(), anyString());
    verifyNoMoreInteractions(repository);
    assertThat(result.docs()).extracting(KoWikiDocStatus::state).containsOnly(State.FRESH);
    assertThat(result.events()).isEqualTo(healthy.events());
    assertThat(result.mergedCount()).isEqualTo(healthy.mergedCount());
  }
}
