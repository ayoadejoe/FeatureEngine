package cleaning;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Spliterator;
import java.util.function.Consumer;

// Custom Spliterator to fill gaps incrementally
class GapFillingSpliterator implements Spliterator<PowerRecord> {
    private final Spliterator<String> lineSpliterator;
    private PowerRecord previous = null;
    private LocalDateTime nextExpectedTime = null;

    public GapFillingSpliterator(Spliterator<String> lineSpliterator) {
        this.lineSpliterator = lineSpliterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super PowerRecord> action) {
        while (true) {
            if (nextExpectedTime != null && previous != null) {
                // Fill gap if there's a time skip
                if (lineSpliterator.tryAdvance(line -> {
                    PowerRecord current;
                    try {
                        current = new PowerRecord(line, 0);
                    } catch (Exception e) {
                        System.err.println("Skipping: " + e.getMessage());
                        return;
                    }
                    long gap = ChronoUnit.MINUTES.between(nextExpectedTime, current.time());
                    if (gap > 0) {
                        // Emit filler for the next expected time
                        action.accept(PowerRecord.createFiller(nextExpectedTime, previous, 0));
                        nextExpectedTime = nextExpectedTime.plusMinutes(1);
                    } else {
                        // No gap, emit the current record
                        action.accept(current);
                        previous = current;
                        nextExpectedTime = current.time().plusMinutes(1);
                    }
                })) {
                    // Continue processing if there's more data
                    if (nextExpectedTime.equals(previous.time().plusMinutes(1))) {
                        return true; // Move to next real record
                    }
                    // Otherwise, we emitted a filler and loop to check the next gap
                    return true;
                } else {
                    // No more lines, stop
                    return false;
                }
            } else {
                // First record initialization
                if (lineSpliterator.tryAdvance(line -> {
                    try {
                        previous = new PowerRecord(line, 0);
                        action.accept(previous);
                        nextExpectedTime = previous.time().plusMinutes(1);
                    } catch (Exception e) {
                        System.err.println("Skipping: " + e.getMessage());
                    }
                })) {
                    return true;
                } else {
                    return false; // Empty file
                }
            }
        }
    }

    @Override
    public Spliterator<PowerRecord> trySplit() { return null; } // Non-parallel for simplicity
    @Override
    public long estimateSize() { return lineSpliterator.estimateSize(); }
    @Override
    public int characteristics() { return lineSpliterator.characteristics() & ~Spliterator.SIZED; }
}
