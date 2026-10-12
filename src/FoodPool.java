import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodPool {
    private final AtomicInteger units;

    public FoodPool(int initialUnits) {
        this.units = new AtomicInteger(initialUnits);
    }

    public boolean tryConsume() {
        return units.getAndUpdate(u -> u > 0 ? u - 1 : u) > 0;
    }

    public void deposit(int amount) {
        units.addAndGet(amount);
    }

    public int remaining() {
        return units.get();
    }

    public static int randomDeathYield() {
        return ThreadLocalRandom.current().nextInt(1, 6);
    }
}
