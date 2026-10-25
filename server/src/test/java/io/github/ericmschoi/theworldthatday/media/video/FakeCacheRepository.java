package io.github.ericmschoi.theworldthatday.media.video;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A cache repository without a database (plan M3a4c T3 (5), (6)): it records which methods were
 * called, finds nothing, and can wait before it answers or throw from {@code find} or {@code
 * upsert}. Any other method is a test error.
 */
final class FakeCacheRepository {

  final List<String> calls = new CopyOnWriteArrayList<>();
  RuntimeException findThrows;
  RuntimeException upsertThrows;
  Duration findDelay = Duration.ZERO;

  EventMediaSearchCacheRepository proxy() {
    return (EventMediaSearchCacheRepository)
        Proxy.newProxyInstance(
            EventMediaSearchCacheRepository.class.getClassLoader(),
            new Class<?>[] {EventMediaSearchCacheRepository.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "find" -> {
                  calls.add("find");
                  if (!findDelay.isZero()) {
                    Thread.sleep(findDelay);
                  }
                  if (findThrows != null) {
                    throw findThrows;
                  }
                  return List.of();
                }
                case "upsert" -> {
                  calls.add("upsert");
                  if (upsertThrows != null) {
                    throw upsertThrows;
                  }
                  return null;
                }
                case "toString" -> {
                  return "FakeCacheRepository";
                }
                default -> throw new UnsupportedOperationException(method.getName());
              }
            });
  }

  long count(String method) {
    return calls.stream().filter(method::equals).count();
  }
}
