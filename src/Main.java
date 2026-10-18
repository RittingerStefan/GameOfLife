public class Main {
    public static void main(String[] args) throws InterruptedException {
        EatingMode eatingMode = EatingMode.SAFE;
        boolean eatingDemo = false;
        for (String arg : args) {
            switch (arg) {
                case "--eating=safe" -> eatingMode = EatingMode.SAFE;
                case "--eating=race" -> eatingMode = EatingMode.RACE;
                case "--demo=eating" -> eatingDemo = true;
                case "--help" -> {
                    printUsage();
                    return;
                }
                default -> {
                    System.err.println("Unknown argument: " + arg);
                    printUsage();
                    System.exit(2);
                    return;
                }
            }
        }
        System.out.println("Eating mode: " + eatingMode
                + (eatingMode == EatingMode.RACE ? " (intentional check-then-act fault)" : ""));
        if (eatingDemo) {
            EatingDemo.printResult(EatingDemo.run(eatingMode));
            return;
        }

        final int initialFood = 80;
        final int initialCells = 6;
        final int timeFull = 5;
        final int timeStarve = 4;
        final long simulationDurationMs = 25_000;

        FoodPool foodPool = new FoodPool(initialFood, eatingMode);
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
        System.out.println("Successful meals: " + foodPool.successfulMeals());
        System.out.println("Cells alive: " + population.countAlive() + " / " + population.getCells().size() + " ever created");
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp out Main [--eating=safe|race] [--demo=eating]");
        System.out.println("Default: normal 25-second simulation with safe eating.");
        System.out.println("--demo=eating: coordinate two hungry cell threads competing for one food unit.");
    }
}
