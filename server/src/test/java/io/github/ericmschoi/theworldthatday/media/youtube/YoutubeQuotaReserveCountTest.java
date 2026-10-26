package io.github.ericmschoi.theworldthatday.media.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.ericmschoi.theworldthatday.MutableClock;
import io.github.ericmschoi.theworldthatday.media.youtube.YoutubeQuota.Bucket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plan M3a5 T7 (1), PM decision 3 (2026-10-26): {@code reserve(bucket, n)} with n below 1 is a
 * mistake of the caller. It throws, whatever the bucket and whatever it holds, and the counts and
 * marks stay as they were. (The 2026 instant is a quota-clock value, not a story date.)
 */
class YoutubeQuotaReserveCountTest {

  private final MutableClock clock =
      new MutableClock(Instant.parse("2026-06-10T19:00:00Z"), ZoneOffset.UTC);

  private YoutubeQuota quota(int searchBudget, int unitBudget) {
    return new YoutubeQuota(
        new YoutubeProperties(
            "",
            URI.create("http://localhost:9/youtube/v3"),
            List.of(YoutubePropertiesTest.KTV_ARCHIVE),
            searchBudget,
            unitBudget,
            25,
            Duration.ofSeconds(2),
            List.of()),
        clock);
  }

  @ParameterizedTest(name = "n={0} throws in both buckets and changes nothing")
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void aCountBelowOneThrowsAndLeavesTheUsedCountAsItWas(int n) {
    YoutubeQuota quota = quota(80, 8000);
    assertThat(quota.reserve(Bucket.SEARCH, 5)).isTrue();
    assertThat(quota.reserve(Bucket.UNITS, 7)).isTrue();

    assertThatIllegalArgumentException().isThrownBy(() -> quota.reserve(Bucket.SEARCH, n));
    assertThatIllegalArgumentException().isThrownBy(() -> quota.reserve(Bucket.UNITS, n));

    assertThat(quota.used(Bucket.SEARCH)).isEqualTo(5);
    assertThat(quota.used(Bucket.UNITS)).isEqualTo(7);
    // the mistake took nothing: the rest of each bucket is still there, to the last unit
    assertThat(quota.reserve(Bucket.SEARCH, 75)).isTrue();
    assertThat(quota.reserve(Bucket.SEARCH, 1)).isFalse();
    assertThat(quota.reserve(Bucket.UNITS, 7993)).isTrue();
    assertThat(quota.reserve(Bucket.UNITS, 1)).isFalse();
  }

  @Test
  void aCountBelowOneThrowsAlsoWhenTheBucketIsFullExhaustedOrHasNoCap() {
    YoutubeQuota full = quota(1, 1);
    assertThat(full.reserve(Bucket.SEARCH, 1)).isTrue();
    assertThatIllegalArgumentException().isThrownBy(() -> full.reserve(Bucket.SEARCH, 0));
    assertThat(full.used(Bucket.SEARCH)).isEqualTo(1);

    YoutubeQuota marked = quota(80, 8000);
    marked.exhausted(Bucket.UNITS);
    assertThatIllegalArgumentException().isThrownBy(() -> marked.reserve(Bucket.UNITS, -1));
    assertThat(marked.used(Bucket.UNITS)).isZero();
    assertThat(marked.reserve(Bucket.UNITS, 1)).as("still marked").isFalse();

    YoutubeQuota noCap = quota(0, 0);
    assertThatIllegalArgumentException().isThrownBy(() -> noCap.reserve(Bucket.SEARCH, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> noCap.reserve(Bucket.UNITS, -1));
  }
}
