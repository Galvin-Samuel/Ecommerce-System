package com.ecommerce.id;

/**
 * Exception thrown when the system clock moves backward beyond the allowed tolerance.
 */
public class ClockMovedBackwardException extends RuntimeException {

    private final long clockDifferenceMs;
    private final long lastTimestamp;
    private final long currentTimestamp;

    public ClockMovedBackwardException(long clockDifferenceMs, long lastTimestamp, long currentTimestamp) {
        super(String.format("Clock moved backward by %d ms. Current timestamp: %d, last timestamp: %d",
                clockDifferenceMs, currentTimestamp, lastTimestamp));
        this.clockDifferenceMs = clockDifferenceMs;
        this.lastTimestamp = lastTimestamp;
        this.currentTimestamp = currentTimestamp;
    }

    public long getClockDifferenceMs() {
        return clockDifferenceMs;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public long getCurrentTimestamp() {
        return currentTimestamp;
    }
}
