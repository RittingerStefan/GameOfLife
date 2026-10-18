public class Main {
    public static void main(String[] args) throws InterruptedException {
        final int initialFood = 80;
        final int initialCells = 6;
        final int timeFull = 5;
        final int timeStarve = 4;
        final long simulationDurationMs = 25_000;

        FoodPool foodPool = new FoodPool(initialFood);
        Population population = new Population(foodPool, timeFull, timeStarve);

        System.out.println("Starting simulation with " + initialCells + " cells and " + initialFood + " food units.");
        for (int i = 0; i < initialCells; i++) {
            population.spawnRandomType(CellState.HUNGRY);
        }

        Thread.sleep(simulationDurationMs);
        population.shutdown();

        System.out.println();
        System.out.println("=== Simulation ended ===");
        System.out.println("Food units remaining in pool: " + foodPool.remaining());
        System.out.println("Cells alive: " + population.countAlive() + " / " + population.getCells().size() + " ever created");
    }
}
