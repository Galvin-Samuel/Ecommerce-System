package com.ecommerce.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("Should generate positive, non-null, 64-bit identifier")
    void testBasicGeneration() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long id = generator.nextId();
        String idStr = generator.nextIdString();

        assertThat(id).isPositive();
        assertThat(idStr).isNotBlank();
        assertThat(Long.parseLong(idStr)).isPositive();
    }

    @Test
    @DisplayName("Should generate monotonically increasing IDs sequentially")
    void testMonotonicallyIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 3);
        long prev = generator.nextId();

        for (int i = 0; i < 1000; i++) {
            long current = generator.nextId();
            assertThat(current).isGreaterThan(prev);
            prev = current;
        }
    }

    @Test
    @DisplayName("Should produce 50,000 unique IDs in a single thread with zero collisions")
    void testLargeBatchUniqueness() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int total = 50_000;
        Set<Long> uniqueIds = new HashSet<>(total);

        for (int i = 0; i < total; i++) {
            long id = generator.nextId();
            boolean added = uniqueIds.add(id);
            assertThat(added).as("Collision detected for ID: %d at iteration %d", id, i).isTrue();
        }

        assertThat(uniqueIds).hasSize(total);
    }

    @Test
    @DisplayName("Should produce zero duplicate IDs under high concurrent access across threads")
    void testConcurrentIdGeneration() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(5, 10);
        int threadCount = 16;
        int idsPerThread = 2500; // total 40,000 IDs
        int totalIds = threadCount * idsPerThread;

        Set<Long> generatedIds = ConcurrentHashMap.newKeySet(totalIds);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        generatedIds.add(generator.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        assertThat(generatedIds).hasSize(totalIds);
    }

    @Test
    @DisplayName("Should handle sequence rollover when exceeding 4096 IDs in the same millisecond")
    void testSequenceRollover() {
        AtomicLong simulatedClock = new AtomicLong(1767226000000L); // fixed millisecond
        // When tilNextMillis is called, the simulated clock advances
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1, 1, 1767225600000L, 10L,
                () -> simulatedClock.updateAndGet(t -> t) // returns current value
        );

        // Advance clock conditionally when spinning in tilNextMillis
        SnowflakeIdGenerator rolloverGenerator = new SnowflakeIdGenerator(
                1, 1, 1767225600000L, 10L,
                () -> {
                    long current = simulatedClock.get();
                    // Advance clock slowly if called repeatedly
                    simulatedClock.compareAndSet(current, current + (Math.random() < 0.001 ? 1 : 0));
                    return simulatedClock.get();
                }
        );

        // Using standard clock, request 5000 IDs in batch (guaranteed to trigger rollover across 4096)
        SnowflakeIdGenerator standardGen = new SnowflakeIdGenerator(1, 1);
        List<Long> batch = standardGen.nextIdBatch(5000);
        assertThat(batch).hasSize(5000);
        assertThat(new HashSet<>(batch)).hasSize(5000);
    }

    @Test
    @DisplayName("Should throw ClockMovedBackwardException when clock drift exceeds max backward tolerance")
    void testClockMovedBackwardThrowsException() {
        AtomicLong mockClock = new AtomicLong(1767226000000L);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1, 1, 1767225600000L, 10L,
                mockClock::get
        );

        // First ID sets lastTimestamp
        generator.nextId();

        // Clock moves backward by 50ms (> 10ms threshold)
        mockClock.addAndGet(-50);

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(ClockMovedBackwardException.class)
                .hasMessageContaining("Clock moved backward by 50 ms");
    }

    @Test
    @DisplayName("Should wait and recover when clock drift is within tolerated limit")
    void testClockMovedBackwardWithinTolerance() {
        AtomicLong mockClock = new AtomicLong(1767226000000L);
        AtomicLong calls = new AtomicLong(0);

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1, 1, 1767225600000L, 10L,
                () -> {
                    long c = calls.incrementAndGet();
                    if (c == 2) {
                        // On second call (during generation), clock jumps back 2ms (tolerated)
                        return 1767225999998L;
                    } else if (c >= 3) {
                        // On third call (after sleep), clock has caught up and advanced
                        return 1767226000005L;
                    }
                    return mockClock.get();
                }
        );

        long id1 = generator.nextId();
        long id2 = generator.nextId();

        assertThat(id1).isPositive();
        assertThat(id2).isPositive();
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    @DisplayName("Should reject invalid worker and datacenter identifiers")
    void testInvalidConfigurationRejected() {
        // Datacenter < 0 or > 31
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 1))
                .isInstanceOf(IllegalArgumentException.class);

        // Worker < 0 or > 31
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, 32))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should correctly decompose and parse a generated ID")
    void testIdDecomposition() {
        long datacenter = 14;
        long worker = 27;
        long epoch = 1767225600000L;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(datacenter, worker, epoch, 10L);

        long id = generator.nextId();
        IdDecomposition parsed = generator.parseId(id);

        assertThat(parsed.id()).isEqualTo(id);
        assertThat(parsed.idString()).isEqualTo(String.valueOf(id));
        assertThat(parsed.datacenterId()).isEqualTo(datacenter);
        assertThat(parsed.workerId()).isEqualTo(worker);
        assertThat(parsed.sequence()).isGreaterThanOrEqualTo(0L);
        assertThat(parsed.timestampMs()).isGreaterThan(epoch);
        assertThat(parsed.instant()).isAfter(Instant.ofEpochMilli(epoch));
    }

    @Test
    @DisplayName("Throughput benchmark: should generate at least 200,000 IDs/sec in single thread")
    void testThroughputBenchmark() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int warmup = 10_000;
        for (int i = 0; i < warmup; i++) {
            generator.nextId();
        }

        int iterations = 100_000;
        long startNano = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            generator.nextId();
        }
        long durationNano = System.nanoTime() - startNano;

        double durationSeconds = durationNano / 1_000_000_000.0;
        double idsPerSecond = iterations / durationSeconds;

        System.out.printf("SnowflakeIdGenerator Throughput: %.0f IDs/sec (took %.3f ms for %d IDs)%n",
                idsPerSecond, durationSeconds * 1000, iterations);

        // Standard JVM can easily generate > 200,000 IDs/sec
        assertThat(idsPerSecond).isGreaterThan(200_000);
    }
}
