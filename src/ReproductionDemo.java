import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReproductionDemo {
    public record Result(boolean deadlocked, int committedParents, int children, boolean cleanedUp,
                         String evidence) {}

    public static Result run(ReproductionMode mode) throws InterruptedException {
        CyclicBarrier exchange = new CyclicBarrier(2);
        CyclicBarrier firstLocks = new CyclicBarrier(2);
        CountDownLatch locked = new CountDownLatch(2);
        CountDownLatch committed = new CountDownLatch(2);
        AtomicInteger commits = new AtomicInteger();
        ReproductionStation station = new ReproductionStation(mode, new ReproductionStation.Observer() {
            public void beforeExchange() throws InterruptedException {
                rendezvous(exchange);
            }
            public void afterFirstLock() throws InterruptedException {
                if (mode == ReproductionMode.DEADLOCK) {
                    locked.countDown();
                    rendezvous(firstLocks);
                }
            }
            public void afterCommit() {
                commits.incrementAndGet();
                committed.countDown();
            }
        });
        // Exactly ten meals each. One full tick between meals; no food remains for children.
        Population population = new Population(new FoodPool(20), 1, Integer.MAX_VALUE, station);
        Cell a = population.spawn(false, CellState.HUNGRY);
        Cell b = population.spawn(false, CellState.HUNGRY);
        boolean deadlocked = false;
        String evidence = "";
        try {
            if (mode == ReproductionMode.DEADLOCK) {
                if (!locked.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Parents did not acquire their first locks");
                }
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (System.nanoTime() < deadline) {
                    long[] ids = bean.findDeadlockedThreads();
                    if (ids != null && Arrays.stream(ids).anyMatch(id -> id == a.getId())
                            && Arrays.stream(ids).anyMatch(id -> id == b.getId())) {
                        ThreadInfo ai = bean.getThreadInfo(a.getId());
                        ThreadInfo bi = bean.getThreadInfo(b.getId());
                        deadlocked = ai.getLockOwnerId() == b.getId() && bi.getLockOwnerId() == a.getId();
                        if (deadlocked) {
                            evidence = ai.getThreadName() + " waits for " + ai.getLockOwnerName()
                                    + "; " + bi.getThreadName() + " waits for " + bi.getLockOwnerName();
                            break;
                        }
                    }
                    Thread.sleep(5);
                }
                if (!deadlocked) throw new IllegalStateException("No JVM-confirmed parent lock cycle");
            } else {
                if (!committed.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Safe parents did not commit");
                }
                // Spawn follows commit in Cell. Wait for the actual child, not just a claim.
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (population.getCells().size() < 3 && System.nanoTime() < deadline) Thread.sleep(5);
                if (population.getCells().size() != 3) throw new IllegalStateException("Expected one child");
            }
        } finally {
            population.shutdown();
        }
        ReproductionStation.History ah = station.history(a.getCellId());
        ReproductionStation.History bh = station.history(b.getCellId());
        long expected = mode == ReproductionMode.SAFE ? 1 : 0;
        if (ah.completedMatings() != expected || bh.completedMatings() != expected
                || (expected == 1 && (ah.lastPartner() != b.getCellId() || bh.lastPartner() != a.getCellId()))) {
            throw new IllegalStateException("Parental reproductive histories are inconsistent");
        }
        return new Result(deadlocked, commits.get(), population.getCells().size() - 2,
                population.getCells().stream().noneMatch(Thread::isAlive), evidence);
    }

    private static void rendezvous(CyclicBarrier barrier) throws InterruptedException {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.BrokenBarrierException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("Reproduction demo coordination failed", e);
        }
    }

    public static void printResult(Result result) {
        System.out.println("JVM-confirmed parent deadlock: " + result.deadlocked());
        if (!result.evidence().isEmpty()) System.out.println(result.evidence());
        System.out.println("Committed parents: " + result.committedParents());
        System.out.println("Children created: " + result.children());
        System.out.println("All cell threads stopped: " + result.cleanedUp());
    }
}
