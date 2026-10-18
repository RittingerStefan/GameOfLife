import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodPool {
    private final AtomicInteger units;
    private final AtomicInteger successfulMeals = new AtomicInteger();
    private final EatingMode eatingMode;
    private final AttemptObserver observer;

    // Demo scheduling only. These hooks do not read or change the food supply.
    interface AttemptObserver {
        default void beforeAttempt() throws InterruptedException {}
        default void afterAvailabilityCheck() throws InterruptedException {}
        default void afterAttempt() {}
    }

    public FoodPool(int initialUnits) {
        this(initialUnits, EatingMode.SAFE);
    }

    public FoodPool(int initialUnits, EatingMode eatingMode) {
        this(initialUnits, eatingMode, new AttemptObserver() {});
    }

    FoodPool(int initialUnits, EatingMode eatingMode, AttemptObserver observer) {
        this.units = new AtomicInteger(initialUnits);
        this.eatingMode = eatingMode;
        this.observer = observer;
    }

    public boolean tryConsume() throws InterruptedException {
        try {
            observer.beforeAttempt();
            boolean consumed;
            if (eatingMode == EatingMode.RACE) {
                // Intentional check-then-act bug: another cell can consume the last
                // unit after this check, even though each individual operation is atomic.
                boolean available = units.get() > 0;
                observer.afterAvailabilityCheck();
                if (!available) {
                    return false;
                }
                units.decrementAndGet();
                consumed = true;
            } else {
                // The availability check and decrement are one atomic operation.
                consumed = units.getAndUpdate(u -> u > 0 ? u - 1 : u) > 0;
            }
            if (consumed) {
                successfulMeals.incrementAndGet();
            }
            return consumed;
        } finally {
            observer.afterAttempt();
        }
    }

    public void deposit(int amount) {
        units.addAndGet(amount);
    }

    public int remaining() {
        return units.get();
    }

    public int successfulMeals() {
        return successfulMeals.get();
    }

    public static int randomDeathYield() {
        return ThreadLocalRandom.current().nextInt(1, 6);
    }
}
