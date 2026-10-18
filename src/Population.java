import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Population {
    private final FoodPool foodPool;
    private final ReproductionStation reproductionStation;
    private final List<Cell> cells = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final int timeFull;
    private final int timeStarve;
    private boolean stopping;

    public Population(FoodPool foodPool, int timeFull, int timeStarve) {
        this.foodPool = foodPool;
        this.reproductionStation = new ReproductionStation();
        this.timeFull = timeFull;
        this.timeStarve = timeStarve;
    }

    public FoodPool getFoodPool() {
        return foodPool;
    }

    public ReproductionStation getReproductionStation() {
        return reproductionStation;
    }

    public List<Cell> getCells() {
        return cells;
    }

    // Returns null if shutdown has already started.
    public synchronized Cell spawn(boolean isAsexual, CellState initialState) {
        if (stopping) {
            return null;
        }
        int id = nextId.getAndIncrement();
        Cell cell = new Cell(id, isAsexual, timeFull, timeStarve, initialState, this);
        cells.add(cell);
        cell.setDaemon(true);
        cell.start();
        return cell;
    }

    public Cell spawnRandomType(CellState initialState) {
        boolean asexual = ThreadLocalRandom.current().nextBoolean();
        return spawn(asexual, initialState);
    }

    // Counts biological survivors, including cells paused by shutdown.
    public long countAlive() {
        return cells.stream().filter(c -> c.getCellState() != CellState.DEAD).count();
    }

    public void shutdown() throws InterruptedException {
        List<Cell> snapshot;
        synchronized (this) {
            stopping = true;
            snapshot = List.copyOf(cells);
            for (Cell cell : snapshot) {
                cell.interrupt();
            }
        }
        // Release the population lock before waiting: a cell may be finishing a spawn.
        for (Cell cell : snapshot) {
            cell.join();
        }
    }
}
