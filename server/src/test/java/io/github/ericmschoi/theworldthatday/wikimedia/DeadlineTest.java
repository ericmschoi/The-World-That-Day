package io.github.ericmschoi.theworldthatday.wikimedia;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Done;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Failed;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Interrupted;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Outcome;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.Task;
import io.github.ericmschoi.theworldthatday.wikimedia.Deadline.TimedOut;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Plan M3a3c T0 (1): one end time shared by several tasks, cancellation with a grace of at most 1s
 * past the end, and outcomes that tell a value, a timeout and an exception apart. Plan M3a3d T0 (1)
 * and (2): {@code close()} interrupts the tasks nobody waited for, and an interrupted waiter is
 * told apart from a task that throws {@link InterruptedException} itself.
 */
class DeadlineTest {

  private static final Duration SLACK = Duration.ofMillis(500);

  private static String sleepThen(long millis, String value) throws InterruptedException {
    Thread.sleep(millis);
    return value;
  }

  private static Duration since(long start) {
    return Duration.ofNanos(System.nanoTime() - start);
  }

  @Test
  void twoTasksShareOneEndAndTheFinishedValueStays() {
    long start = System.nanoTime();
    Deadline deadline = Deadline.after(Duration.ofSeconds(1));
    Task<String> quick = deadline.start("quick", () -> sleepThen(100, "first"));
    Task<String> silent = deadline.start("silent", () -> sleepThen(60_000, "never"));

    Outcome<String> first = quick.await();
    Outcome<String> second = silent.await();
    Duration took = since(start);

    assertThat(first).isEqualTo(new Done<>("first"));
    assertThat(second).isEqualTo(new TimedOut<String>(true, null)); // interrupted sleep threw
    assertThat(took)
        .as("the second wait ends at the shared end, not 1s after the first")
        .isGreaterThanOrEqualTo(Duration.ofMillis(950))
        .isLessThan(Duration.ofSeconds(1).plus(Deadline.CANCEL_GRACE).plus(SLACK));
  }

  @Test
  void finishedValueStaysWhenTheSlowTaskIsAwaitedFirst() {
    Deadline deadline = Deadline.after(Duration.ofMillis(400));
    Task<String> quick = deadline.start("quick", () -> sleepThen(50, "first"));
    Task<String> silent = deadline.start("silent", () -> sleepThen(60_000, "never"));

    assertThat(silent.await()).isInstanceOf(TimedOut.class);
    assertThat(quick.await()).isEqualTo(new Done<>("first"));
  }

  @Test
  void twoSilentTasksTakeOneWindowAndOneGrace() {
    long start = System.nanoTime();
    Deadline deadline = Deadline.after(Duration.ofMillis(500));
    Task<String> a = deadline.start("a", () -> sleepThen(60_000, "never"));
    Task<String> b = deadline.start("b", () -> sleepThen(60_000, "never"));

    assertThat(a.await()).isEqualTo(new TimedOut<String>(true, null));
    assertThat(b.await()).isEqualTo(new TimedOut<String>(true, null));
    assertThat(since(start)).isLessThan(Duration.ofMillis(500).plus(SLACK));
  }

  /** A task that ignores interrupts until {@code release} opens. */
  private static Task<String> stubborn(Deadline deadline, CountDownLatch release) {
    return deadline.start(
        "stubborn",
        () -> {
          while (true) {
            try {
              if (release.await(10, TimeUnit.SECONDS)) {
                return "too late";
              }
            } catch (InterruptedException ignored) {
              // keeps waiting on purpose
            }
          }
        });
  }

  @Test
  void twoStubbornTasksShareOneGrace() {
    CountDownLatch release = new CountDownLatch(1);
    long start = System.nanoTime();
    Deadline deadline = Deadline.after(Duration.ofMillis(300));
    Task<String> a = stubborn(deadline, release);
    Task<String> b = stubborn(deadline, release);
    try {
      assertThat(a.await()).isEqualTo(new TimedOut<String>(false, null));
      assertThat(b.await()).isEqualTo(new TimedOut<String>(false, null));
      assertThat(since(start))
          .isLessThan(Duration.ofMillis(300).plus(Deadline.CANCEL_GRACE).plus(SLACK));
    } finally {
      release.countDown();
    }
  }

  @Test
  void taskIgnoringTheInterruptIsReportedAsNotStoppedAfterTheGrace() {
    CountDownLatch release = new CountDownLatch(1);
    long start = System.nanoTime();
    Task<String> stubborn = stubborn(Deadline.after(Duration.ofMillis(300)), release);
    try {
      Outcome<String> outcome = stubborn.await();
      Duration took = since(start);

      assertThat(outcome).isEqualTo(new TimedOut<String>(false, null));
      assertThat(took)
          .isGreaterThanOrEqualTo(
              Duration.ofMillis(300).plus(Deadline.CANCEL_GRACE).minusMillis(50))
          .isLessThan(Duration.ofMillis(300).plus(Deadline.CANCEL_GRACE).plus(SLACK));
    } finally {
      release.countDown();
    }
  }

  @Test
  void valueReturnedAfterTheInterruptIsKeptAsLate() {
    Deadline deadline = Deadline.after(Duration.ofMillis(200));
    Task<String> polite =
        deadline.start(
            "polite",
            () -> {
              try {
                return sleepThen(60_000, "never");
              } catch (InterruptedException e) {
                return "unavailable";
              }
            });

    assertThat(polite.await()).isEqualTo(new TimedOut<>(true, "unavailable"));
  }

  @Test
  void exceptionBeforeTheEndIsAFailureWithItsCause() {
    Task<String> broken =
        Deadline.after(Duration.ofSeconds(1))
            .start(
                "broken",
                () -> {
                  throw new IllegalStateException("synthetic");
                });

    Outcome<String> outcome = broken.await();

    assertThat(outcome).isInstanceOf(Failed.class);
    assertThat(((Failed<String>) outcome).cause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void interruptedWaiterGetsInterruptedAndCancelsTheTasks() throws Exception {
    AtomicReference<Outcome<String>> seen = new AtomicReference<>();
    AtomicReference<Boolean> flagSet = new AtomicReference<>();
    CountDownLatch taskStopped = new CountDownLatch(1);
    Thread waiter =
        Thread.ofVirtual()
            .start(
                () -> {
                  Task<String> silent =
                      Deadline.after(Duration.ofSeconds(30))
                          .start(
                              "silent",
                              () -> {
                                try {
                                  return sleepThen(60_000, "never");
                                } finally {
                                  taskStopped.countDown();
                                }
                              });
                  seen.set(silent.await());
                  flagSet.set(Thread.currentThread().isInterrupted());
                });
    Thread.sleep(100);
    waiter.interrupt();
    waiter.join(Duration.ofSeconds(2));

    assertThat(seen.get()).isEqualTo(new Interrupted<String>());
    assertThat(flagSet.get()).isTrue();
    assertThat(taskStopped.await(1, TimeUnit.SECONDS)).as("the task was cancelled").isTrue();
  }

  @Test
  void taskThrowingItsOwnInterruptedExceptionIsAFailureAndTheWaiterFlagStaysClear() {
    Task<String> broken =
        Deadline.after(Duration.ofSeconds(1))
            .start(
                "broken",
                () -> {
                  throw new InterruptedException("synthetic");
                });

    Outcome<String> outcome = broken.await();
    boolean flagSet = Thread.interrupted(); // also clears it for the next test

    assertThat(outcome).isInstanceOf(Failed.class);
    assertThat(((Failed<String>) outcome).cause()).isInstanceOf(InterruptedException.class);
    assertThat(flagSet).isFalse();
  }

  @Test
  void closeInterruptsATaskNobodyWaitedForAtOnce() throws Exception {
    CountDownLatch taskStopped = new CountDownLatch(1);
    Deadline deadline = Deadline.after(Duration.ofSeconds(30));
    deadline.start(
        "unawaited",
        () -> {
          try {
            return sleepThen(60_000, "never");
          } finally {
            taskStopped.countDown();
          }
        });
    Thread.sleep(50);

    long start = System.nanoTime();
    deadline.close();
    Duration took = since(start);

    assertThat(took).as("close() does not wait").isLessThan(Duration.ofMillis(50));
    assertThat(taskStopped.await(200, TimeUnit.MILLISECONDS)).as("interrupted").isTrue();
  }

  @Test
  void closeAfterEveryTaskEndedChangesNothingAndCanBeCalledTwice() {
    Deadline deadline = Deadline.after(Duration.ofSeconds(1));
    Task<String> quick = deadline.start("quick", () -> "first");
    assertThat(quick.await()).isEqualTo(new Done<>("first"));

    long start = System.nanoTime();
    deadline.close();
    deadline.close();
    Duration took = since(start);

    assertThat(quick.await()).isEqualTo(new Done<>("first"));
    assertThat(took).isLessThan(Duration.ofMillis(50));
  }

  @Test
  void closeKeepsTheValueOfAnEndedTaskThatWasNotAwaitedYet() throws Exception {
    Deadline deadline = Deadline.after(Duration.ofSeconds(1));
    CountDownLatch ended = new CountDownLatch(1);
    Task<String> quick =
        deadline.start(
            "quick",
            () -> {
              ended.countDown();
              return "first";
            });
    assertThat(ended.await(1, TimeUnit.SECONDS)).isTrue();
    Thread.sleep(50); // the value is stored

    deadline.close();

    assertThat(quick.await()).isEqualTo(new Done<>("first"));
  }

  @Test
  void tryWithResourcesInterruptsTheSecondTaskWhenTheFirstWaitThrows() throws Exception {
    CountDownLatch taskStopped = new CountDownLatch(1);
    try (Deadline deadline = Deadline.after(Duration.ofSeconds(30))) {
      deadline.start(
          "second",
          () -> {
            try {
              return sleepThen(60_000, "never");
            } finally {
              taskStopped.countDown();
            }
          });
      Task<String> first =
          deadline.start(
              "first",
              () -> {
                throw new IllegalStateException("synthetic");
              });
      if (first.await() instanceof Failed<String>) {
        throw new IllegalStateException("caller gives up between the two waits");
      }
    } catch (IllegalStateException expected) {
      // the second task was never awaited
    }

    assertThat(taskStopped.await(200, TimeUnit.MILLISECONDS)).as("interrupted").isTrue();
  }

  @Test
  void eachTaskRunsOnItsOwnNamedVirtualThread() {
    Deadline deadline = Deadline.after(Duration.ofSeconds(1));
    Task<Thread> one = deadline.start("one", Thread::currentThread);
    Task<Thread> two = deadline.start("two", Thread::currentThread);

    Thread first = ((Done<Thread>) one.await()).value();
    Thread second = ((Done<Thread>) two.await()).value();

    assertThat(first.isVirtual()).isTrue();
    assertThat(first.getName()).isEqualTo("one");
    assertThat(second.getName()).isEqualTo("two");
    assertThat(first).isNotSameAs(second);
  }
}
