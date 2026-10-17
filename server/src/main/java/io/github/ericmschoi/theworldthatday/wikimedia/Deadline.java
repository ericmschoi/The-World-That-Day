package io.github.ericmschoi.theworldthatday.wikimedia;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * One end time shared by several tasks (plan M3a3c T0). Each task runs on its own virtual thread,
 * and waiting for one lasts only until the shared end. When the end passes, every unfinished task
 * of this deadline is interrupted and the caller waits until at most {@link #CANCEL_GRACE} past the
 * end for it to stop, so all tasks together take at most the budget plus the grace. The {@link
 * Outcome} tells a value, a timeout (and whether the task stopped within the grace) and an
 * exception apart, so a caller can keep one WARN line per failure.
 */
public final class Deadline {

  public static final Duration CANCEL_GRACE = Duration.ofSeconds(1);

  private final long end;
  private final List<Task<?>> tasks = new CopyOnWriteArrayList<>();

  private Deadline(long end) {
    this.end = end;
  }

  /** A deadline {@code budget} from now. */
  public static Deadline after(Duration budget) {
    return new Deadline(System.nanoTime() + budget.toNanos());
  }

  /** Starts {@code call} now on a virtual thread named {@code name}. */
  public <T> Task<T> start(String name, Callable<T> call) {
    Task<T> task = new Task<>(call);
    task.thread = Thread.ofVirtual().name(name).unstarted(task::run);
    tasks.add(task);
    task.thread.start();
    return task;
  }

  private void cancelAll() {
    tasks.forEach(Task::cancel);
  }

  private static long left(long until) {
    return Math.max(0, until - System.nanoTime());
  }

  /** What waiting for a task gave. */
  public sealed interface Outcome<T> permits Done, TimedOut, Failed {}

  /** The task returned {@code value} before the end. */
  public record Done<T>(T value) implements Outcome<T> {}

  /**
   * The end passed first. {@code stopped}: the interrupted task ended within the grace; {@code
   * late}: what it returned then, or null (it threw, or did not stop).
   */
  public record TimedOut<T>(boolean stopped, T late) implements Outcome<T> {}

  /**
   * The task threw {@code cause} before the end. An {@link InterruptedException} means the waiting
   * thread was interrupted: its flag is set again and the deadline's tasks are cancelled.
   */
  public record Failed<T>(Throwable cause) implements Outcome<T> {}

  /** One task of this deadline. */
  public final class Task<T> {

    private final Callable<T> call;
    private final CountDownLatch ended = new CountDownLatch(1);
    private Thread thread;
    private volatile boolean cancelled;
    private volatile Outcome<T> result;

    private Task(Callable<T> call) {
      this.call = call;
    }

    private void run() {
      try {
        result = new Done<>(call.call());
      } catch (Throwable e) {
        result = new Failed<>(e);
      } finally {
        ended.countDown();
      }
    }

    private void cancel() {
      if (ended.getCount() > 0) {
        cancelled = true;
        thread.interrupt();
      }
    }

    /** Waits until the task ends or the shared end passes (then cancels and waits the grace). */
    public Outcome<T> await() {
      try {
        if (ended.await(left(end), TimeUnit.NANOSECONDS) && !cancelled) {
          return result;
        }
        cancelAll();
        boolean stopped = ended.await(left(end + CANCEL_GRACE.toNanos()), TimeUnit.NANOSECONDS);
        return new TimedOut<>(
            stopped, stopped && result instanceof Done<T> done ? done.value() : null);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        cancelAll();
        return new Failed<>(e);
      }
    }
  }
}
