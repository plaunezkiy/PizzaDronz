package uk.ac.ed.inf;

public class ExecutionTimer {
    long start;
    long end;
    public ExecutionTimer() {}

    public void start() {
        start = System.nanoTime();
    }

    public void stop() {
        end = System.nanoTime();
    }

    /**
     *
     * @return time difference in ms
     */
    public long getDuration() {
        return (end - start) / 1000000;
    }
}
