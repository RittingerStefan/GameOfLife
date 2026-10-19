public class ReproductionChecks {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; i++) {
            ReproductionDemo.Result faulty = ReproductionDemo.run(ReproductionMode.DEADLOCK);
            check(faulty.deadlocked() && faulty.committedParents() == 0 && faulty.children() == 0,
                    "Fault must produce a JVM-confirmed cycle before any commit or birth");
            check(faulty.cleanedUp(), "Deadlocked cells must stop after interruption");
            ReproductionDemo.Result safe = ReproductionDemo.run(ReproductionMode.SAFE);
            check(!safe.deadlocked() && safe.committedParents() == 2 && safe.children() == 1,
                    "Both safe parents must commit exactly one shared birth");
            check(safe.cleanedUp(), "Safe demo must join parents and child");
        }
        check(Thread.getAllStackTraces().keySet().stream()
                .noneMatch(t -> t.isAlive() && t.getName().startsWith("Cell-")), "No leaked cells");
        // Without forced overlap, even the faulty order can finish a real mating.
        for (ReproductionMode mode : ReproductionMode.values()) {
            ReproductionStation station = new ReproductionStation(mode);
            java.util.concurrent.atomic.AtomicInteger claims = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.CountDownLatch firstCommit = new java.util.concurrent.CountDownLatch(1);
            Thread a = new Thread(() -> {
                try {
                    Long mate = station.tryFindMate(1, 5000);
                    if (mate == null) throw new AssertionError("No partner");
                    if (station.commitPair(1, mate)) claims.incrementAndGet();
                } catch (Throwable e) { error.set(e); }
                finally { firstCommit.countDown(); }
            });
            Thread b = new Thread(() -> {
                try {
                    Long mate = station.tryFindMate(2, 5000);
                    if (mate == null) throw new AssertionError("No partner");
                    if (!firstCommit.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("No first commit");
                    if (station.commitPair(2, mate)) claims.incrementAndGet();
                } catch (Throwable e) { error.set(e); }
            });
            a.start(); b.start(); a.join(); b.join();
            check(error.get() == null && claims.get() == 1, "Serial commits must claim one child in " + mode);
            check(station.tryFindMate(3, 1) == null, "An unmatched search must time out normally");
        }
        System.out.println("PASS: 5 reproduction comparisons, deadlock evidence, cleanup, serial commits, and unmatched search");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
