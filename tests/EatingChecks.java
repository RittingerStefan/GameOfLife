import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EatingChecks {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            EatingDemo.Result safe = EatingDemo.run(EatingMode.SAFE);
            check(safe.successfulMeals() == 1 && safe.remainingFood() == 0,
                    "Safe demo must allow exactly one meal from one food unit");

            EatingDemo.Result race = EatingDemo.run(EatingMode.RACE);
            check(race.successfulMeals() == 2 && race.remainingFood() == -1,
                    "Faulty demo must expose two meals from one food unit");
            check(!race.respectsFoodLimit(), "Negative food must violate the resource limit");
        }
        check(Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(t -> t.isAlive() && t.getName().startsWith("Cell-")),
                "Demo shutdown must leave no cell threads running");

        // Without an overlapping check/decrement, the faulty mode behaves normally.
        FoodPool serial = new FoodPool(1, EatingMode.RACE);
        check(serial.tryConsume() && !serial.tryConsume() && serial.remaining() == 0,
                "Faulty mode must require a race, rather than manufacture negative food");

        // Exercise the default constructor and real contention without demo scheduling hooks.
        FoodPool safe = new FoodPool(1_000);
        AtomicInteger meals = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread consumer = new Thread(() -> {
                try {
                    start.await();
                    while (safe.tryConsume()) {
                        meals.incrementAndGet();
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            });
            consumers.add(consumer);
            consumer.start();
        }
        start.countDown();
        for (Thread consumer : consumers) {
            consumer.join();
        }
        check(failure.get() == null, "Concurrent consumers must finish without errors");
        check(meals.get() == 1_000 && safe.successfulMeals() == 1_000 && safe.remaining() == 0,
                "Safe consumption must account for exactly 1,000 meals under contention");
        System.out.println("PASS: 10 comparisons, demo cleanup, serial faulty mode, and safe contention");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
