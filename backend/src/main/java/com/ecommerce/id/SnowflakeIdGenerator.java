package com.ecommerce.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * High-performance, distributed Snowflake-style unique identifier generator.
 *
 * <p>Bit Layout (64-bit signed integer, positive):
 * <ul>
 *   <li>1 bit: Sign bit (always 0)</li>
 *   <li>41 bits: Millisecond timestamp offset from custom epoch (~69.7 years)</li>
 *   <li>5 bits: Datacenter ID (0 - 31)</li>
 *   <li>5 bits: Worker / Node ID (0 - 31)</li>
 *   <li>12 bits: Sequence counter (0 - 4095 per millisecond)</li>
 * </ul>
 *
 * <p>Throughput: up to 4,096,000 unique IDs per second per node.
 * Thread safety: Fully synchronized for thread-safe concurrent invocation.
 */
public class SnowflakeIdGenerator implements IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /**
     * Default custom epoch: 2026-01-01T00:00:00Z (1767225600000 ms).
     */
    public static final long DEFAULT_CUSTOM_EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    public static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);       // 31
    public static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS); // 31
    public static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);          // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                                  // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;              // 17
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    private final long datacenterId;
    private final long workerId;
    private final long customEpochMs;
    private final long maxBackwardMs;
    private final LongSupplier timeSource;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        this(datacenterId, workerId, DEFAULT_CUSTOM_EPOCH, 10L, System::currentTimeMillis);
    }

    public SnowflakeIdGenerator(long datacenterId, long workerId, long customEpochMs, long maxBackwardMs) {
        this(datacenterId, workerId, customEpochMs, maxBackwardMs, System::currentTimeMillis);
    }

    public SnowflakeIdGenerator(long datacenterId, long workerId, long customEpochMs, long maxBackwardMs, LongSupplier timeSource) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(String.format("Datacenter ID must be between 0 and %d (was %d)",
                    MAX_DATACENTER_ID, datacenterId));
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(String.format("Worker ID must be between 0 and %d (was %d)",
                    MAX_WORKER_ID, workerId));
        }
        if (customEpochMs <= 0) {
            throw new IllegalArgumentException("Custom epoch must be positive");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.customEpochMs = customEpochMs;
        this.maxBackwardMs = maxBackwardMs;
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");

        log.info("Initialized SnowflakeIdGenerator with datacenterId={}, workerId={}, epochMs={}, maxBackwardMs={}",
                datacenterId, workerId, customEpochMs, maxBackwardMs);
    }

    @Override
    public synchronized long nextId() {
        long currentTimestamp = timeSource.getAsLong();

        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= maxBackwardMs) {
                log.warn("Clock drifted backward by {} ms (tolerated up to {} ms). Waiting...", offset, maxBackwardMs);
                try {
                    Thread.sleep(offset + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted waiting for clock catch-up", e);
                }
                currentTimestamp = timeSource.getAsLong();
                if (currentTimestamp < lastTimestamp) {
                    throw new ClockMovedBackwardException(lastTimestamp - currentTimestamp, lastTimestamp, currentTimestamp);
                }
            } else {
                throw new ClockMovedBackwardException(offset, lastTimestamp, currentTimestamp);
            }
        }

        if (lastTimestamp == currentTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted in the current millisecond; spin wait until next millisecond
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - customEpochMs) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    @Override
    public synchronized List<Long> nextIdBatch(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Batch count must be greater than 0");
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(nextId());
        }
        return ids;
    }

    @Override
    public IdDecomposition parseId(long id) {
        long sequencePart = id & MAX_SEQUENCE;
        long workerPart = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long datacenterPart = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long timestampOffset = id >> TIMESTAMP_LEFT_SHIFT;
        long timestampMs = customEpochMs + timestampOffset;
        Instant instant = Instant.ofEpochMilli(timestampMs);

        return new IdDecomposition(
                id,
                String.valueOf(id),
                timestampMs,
                instant,
                datacenterPart,
                workerPart,
                sequencePart
        );
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeSource.getAsLong();
        while (timestamp <= lastTimestamp) {
            timestamp = timeSource.getAsLong();
        }
        return timestamp;
    }

    @Override
    public long getWorkerId() {
        return workerId;
    }

    @Override
    public long getDatacenterId() {
        return datacenterId;
    }

    public long getCustomEpochMs() {
        return customEpochMs;
    }

    public long getMaxBackwardMs() {
        return maxBackwardMs;
    }
}
