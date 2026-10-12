import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReproductionStation {
    private final Exchanger<Long> exchanger = new Exchanger<>();

    public long findMate(long myId, long timeoutMs) throws TimeoutException {
        try {
            return exchanger.exchange(myId, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while looking for a mate", e);
        }
    }
}
