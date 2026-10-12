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

    public Cell spawn(boolean isAsexual, CellState initialState) {
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

    public long countAlive() {
        return cells.stream().filter(c -> c.getCellState() != CellState.DEAD).count();
    }
}
