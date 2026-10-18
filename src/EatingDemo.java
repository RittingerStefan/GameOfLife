import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class EatingDemo {
    public record Result(int successfulMeals, int remainingFood) {
        public boolean respectsFoodLimit() {
            return successfulMeals <= 1 && remainingFood >= 0;
        }
    }

    public static Result run(EatingMode mode) throws InterruptedException {
        CountDownLatch finished = new CountDownLatch(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger attempts = new AtomicInteger();
        FoodPool.AttemptObserver observer = new FoodPool.AttemptObserver() {
            private final ThreadLocal<Boolean> coordinated = ThreadLocal.withInitial(() -> false);

            @Override
            public void beforeAttempt() throws InterruptedException {
                // Only coordinate each cell's first meal attempt, never subsequent retries.
                coordinated.set(attempts.getAndIncrement() < 2);
                if (coordinated.get() && mode == EatingMode.SAFE) {
                    rendezvous();
                }
            }

            @Override
            public void afterAvailabilityCheck() throws InterruptedException {
                if (coordinated.get()) {
                    // Both faulty consumers have now checked the same last unit.
                    rendezvous();
                }
            }

            @Override
            public void afterAttempt() {
                if (coordinated.get()) {
                    finished.countDown();
                }
                coordinated.remove();
            }

            private void rendezvous() throws InterruptedException {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (BrokenBarrierException | TimeoutException e) {
                    throw new IllegalStateException("Eating demo could not coordinate both cells", e);
                }
            }
        };

        FoodPool foodPool = new FoodPool(1, mode, observer);
        // Long lifetimes isolate the first attempts from starvation, deposits, and reproduction.
        Population population = new Population(foodPool, Integer.MAX_VALUE, Integer.MAX_VALUE);
        try {
            population.spawn(true, CellState.HUNGRY);
            population.spawn(true, CellState.HUNGRY);
            if (!finished.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Eating demo did not finish both attempts");
            }
        } finally {
            population.shutdown();
        }
        return new Result(foodPool.successfulMeals(), foodPool.remaining());
    }

    public static void printResult(Result result) {
        System.out.println("Starting food units: 1; hungry cells: 2");
        System.out.println("Successful meals: " + result.successfulMeals());
        System.out.println("Food units remaining: " + result.remainingFood());
        System.out.println("Food limit (at most one meal, no negative food): "
                + (result.respectsFoodLimit() ? "PASS" : "VIOLATED"));
    }
}
