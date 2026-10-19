public class Main {
    public static void main(String[] args) throws InterruptedException {
        EatingMode eatingMode = EatingMode.SAFE;
        boolean eatingDemo = false;
        boolean reproductionDemo = false;
        ReproductionMode reproductionMode = ReproductionMode.SAFE;
        for (String arg : args) {
            switch (arg) {
                case "--eating=safe" -> eatingMode = EatingMode.SAFE;
                case "--eating=race" -> eatingMode = EatingMode.RACE;
                case "--reproduction=safe" -> reproductionMode = ReproductionMode.SAFE;
                case "--reproduction=deadlock" -> reproductionMode = ReproductionMode.DEADLOCK;
                case "--demo=reproduction" -> reproductionDemo = true;
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
        if (eatingDemo && reproductionDemo) {
            throw new IllegalArgumentException("Choose only one demo");
        }
        System.out.println("Reproduction mode: " + reproductionMode);
        if (reproductionDemo) {
            ReproductionDemo.printResult(ReproductionDemo.run(reproductionMode));
            return;
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
        Population population = new Population(foodPool, timeFull, timeStarve, new ReproductionStation(reproductionMode));

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
        System.out.println("Usage: java -cp out Main [--eating=safe|race] [--reproduction=safe|deadlock] [--demo=eating|reproduction]");
        System.out.println("Default: normal 25-second simulation with safe eating and reproduction.");
        System.out.println("--demo=reproduction: verify a parent lock cycle or one successful birth, then stop all cells.");
        System.out.println("--demo=eating: coordinate two hungry cell threads competing for one food unit.");
    }
}
