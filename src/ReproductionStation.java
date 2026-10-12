import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReproductionStation {
    private final Exchanger<Long> exchanger = new Exchanger<>();

    /**
     * Waits up to timeoutMs for another cell looking to reproduce.
     * Returns the partner's id, or null if nobody showed up in time.
     */
    public Long tryFindMate(long myId, long timeoutMs) throws InterruptedException {
        try {
            return exchanger.exchange(myId, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        }
    }
}
