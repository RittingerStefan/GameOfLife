import java.util.concurrent.TimeUnit;

public class Cell extends Thread {
    private static final int TICK_MS = 100;
    private static final int REPRODUCTION_THRESHOLD = 10;

    private final int cellId;
    private final boolean isAsexual;
    private final int timeFull;
    private final int timeStarve;
    private final Population population;

    private volatile CellState state;
    private int timeFullCounter;
    private int timeStarveCounter;
    private int eatenCount;

    public Cell(int cellId, boolean isAsexual, int timeFull, int timeStarve, CellState initialState, Population population) {
        super("Cell-" + cellId + (isAsexual ? "-asexual" : "-sexual"));
        this.cellId = cellId;
        this.isAsexual = isAsexual;
        this.timeFull = timeFull;
        this.timeStarve = timeStarve;
        this.population = population;
        this.state = initialState;
        if (initialState == CellState.FULL) {
            this.timeFullCounter = timeFull;
        } else {
            this.timeStarveCounter = timeStarve;
        }
    }

    public int getCellId() {
        return cellId;
    }

    public boolean isAsexual() {
        return isAsexual;
    }

    public int getTimeFull() {
        return timeFull;
    }

    public int getTimeStarve() {
        return timeStarve;
    }

    public CellState getCellState() {
        return state;
    }

    @Override
    public void run() {
        log("born as " + (isAsexual ? "asexual" : "sexual") + ", starting " + state);
        try {
            while (state != CellState.DEAD && !Thread.currentThread().isInterrupted()) {
                switch (state) {
                    case FULL:
                        if (isReadyToMate()) {
                            tryMateDuringTick();
                        } else {
                            sleepTick();
                        }
                        tickDownFull();
                        break;
                    case HUNGRY:
                        if (!tryEat()) {
                            sleepTick();
                            tickDownStarving();
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("interrupted, stopping");
        }
    }

    private void sleepTick() throws InterruptedException {
        Thread.sleep(TICK_MS);
    }

    private boolean tryEat() {
        if (!population.getFoodPool().tryConsume()) {
            return false;
        }
        eatenCount++;
        log("ate a food unit (total eaten: " + eatenCount + "), food left=" + population.getFoodPool().remaining());
        state = CellState.FULL;
        timeFullCounter = timeFull;
        if (isAsexual && eatenCount >= REPRODUCTION_THRESHOLD) {
            divide();
        } else if (isReadyToMate()) {
            log("ready to reproduce, looking for a mate while full");
        }
        return true;
    }

    private void tickDownFull() {
        timeFullCounter--;
        if (timeFullCounter <= 0) {
            state = CellState.HUNGRY;
            timeStarveCounter = timeStarve;
            log("got hungry");
        }
    }

    private void tickDownStarving() {
        timeStarveCounter--;
        if (timeStarveCounter <= 0) {
            die("starved to death");
        }
    }

    private void die(String reason) {
        state = CellState.DEAD;
        int yield = FoodPool.randomDeathYield();
        population.getFoodPool().deposit(yield);
        log(reason + ", releasing " + yield + " food unit(s) back to the pool");
    }

    private boolean isReadyToMate() {
        return !isAsexual && eatenCount >= REPRODUCTION_THRESHOLD;
    }

    private void divide() {
        log("dividing (asexual reproduction)");
        population.spawn(true, CellState.HUNGRY);
        population.spawn(true, CellState.HUNGRY);
        state = CellState.DEAD;
        log("divided into two hungry cells and ceased to exist");
    }

    // Searching and reproduction share one tick; an early match waits out the remainder.
    private void tryMateDuringTick() throws InterruptedException {
        long tickStart = System.nanoTime();
        Long partnerId = population.getReproductionStation().tryFindMate(cellId, TICK_MS);
        if (partnerId != null) {
            eatenCount = 0;
            log("found a mate: Cell-" + partnerId);
            if (cellId < partnerId) {
                population.spawn(false, CellState.HUNGRY);
                log("produced offspring together with Cell-" + partnerId);
            }
        }
        long remainingNanos = TimeUnit.MILLISECONDS.toNanos(TICK_MS) - (System.nanoTime() - tickStart);
        if (remainingNanos > 0) {
            TimeUnit.NANOSECONDS.sleep(remainingNanos);
        }
    }

    private void log(String message) {
        System.out.printf("[%s] %s%n", getName(), message);
    }
}
