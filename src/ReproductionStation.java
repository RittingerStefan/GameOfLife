import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

public class ReproductionStation {
    private static final class Record {
        final ReentrantLock lock = new ReentrantLock();
        long completedMatings;
        long lastPartner;
    }

    interface Observer {
        default void beforeExchange() throws InterruptedException {}
        default void afterFirstLock() throws InterruptedException {}
        default void afterCommit() {}
    }

    private static final class Offer {
        final long cellId;
        final long ticket;
        // Shared by both callers; protected by both parental record locks.
        boolean committed;
        Offer(long cellId, long ticket) { this.cellId = cellId; this.ticket = ticket; }
    }
    private final Exchanger<Offer> exchanger = new Exchanger<>();
    private final AtomicLong tickets = new AtomicLong();
    private final ThreadLocal<Offer> matchedRound = new ThreadLocal<>();
    private final ConcurrentHashMap<Long, Record> records = new ConcurrentHashMap<>();
    private final ReproductionMode mode;
    private final Observer observer;

    public ReproductionStation() {
        this(ReproductionMode.SAFE);
    }

    public ReproductionStation(ReproductionMode mode) {
        this(mode, new Observer() {});
    }

    ReproductionStation(ReproductionMode mode, Observer observer) {
        this.mode = mode;
        this.observer = observer;
    }

    public Long tryFindMate(long myId, long timeoutMs) throws InterruptedException {
        observer.beforeExchange();
        try {
            Offer own = new Offer(myId, tickets.incrementAndGet());
            Offer other = exchanger.exchange(own, timeoutMs, TimeUnit.MILLISECONDS);
            matchedRound.set(own.ticket < other.ticket ? own : other);
            return other.cellId;
        } catch (TimeoutException e) {
            return null;
        }
    }

    public record History(long completedMatings, long lastPartner) {}

    public History history(long cellId) throws InterruptedException {
        Record record = records.get(cellId);
        if (record == null) return new History(0, 0);
        record.lock.lockInterruptibly();
        try {
            return new History(record.completedMatings, record.lastPartner);
        } finally {
            record.lock.unlock();
        }
    }

    /** Commit both parents' mating round atomically; exactly one caller claims the child. */
    public boolean commitPair(long myId, long partnerId) throws InterruptedException {
        Offer round = matchedRound.get();
        Record mine = records.computeIfAbsent(myId, id -> new Record());
        Record partner = records.computeIfAbsent(partnerId, id -> new Record());
        // Fault: caller-relative ordering feels natural, but the partner does the reverse.
        // Fix: every caller follows the same global order.
        Record first = mode == ReproductionMode.DEADLOCK || myId < partnerId ? mine : partner;
        Record second = first == mine ? partner : mine;
        first.lock.lockInterruptibly();
        try {
            observer.afterFirstLock();
            second.lock.lockInterruptibly();
            try {
                boolean claim = !round.committed;
                if (claim) {
                    mine.completedMatings++;
                    partner.completedMatings++;
                    mine.lastPartner = partnerId;
                    partner.lastPartner = myId;
                    round.committed = true;
                }
                observer.afterCommit();
                return claim;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
