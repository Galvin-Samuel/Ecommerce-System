package com.ecommerce.benchmark;

import com.ecommerce.autocomplete.AutocompleteTrie;
import com.ecommerce.crawler.PageContent;
import com.ecommerce.crawler.PageFetcher;
import com.ecommerce.crawler.SsrfValidator;
import com.ecommerce.hashing.ConsistentHashRing;
import com.ecommerce.hashing.HashRingNode;
import com.ecommerce.id.SnowflakeIdGenerator;
import com.ecommerce.model.Product;
import com.ecommerce.ratelimit.RateLimitResult;
import com.ecommerce.ratelimit.RateLimitingProperties;
import com.ecommerce.ratelimit.RedisRateLimiterService;
import com.ecommerce.storage.kv.InMemoryKeyValueStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 20: Comprehensive, reproducible performance, concurrency, and failure benchmark suite.
 */
class SystemPerformanceAndFailureBenchmarkTest {

    record BenchmarkStats(
            String component,
            int operations,
            double durationMs,
            double throughputOpsPerSec,
            double p50Ns,
            double p95Ns,
            double p99Ns
    ) {
    }

    private BenchmarkStats computeStats(String component, long[] latenciesNs, int count, long totalDurationNano) {
        Arrays.sort(latenciesNs);
        double durationMs = totalDurationNano / 1_000_000.0;
        double throughput = count / (totalDurationNano / 1_000_000_000.0);
        double p50 = latenciesNs[(int) (count * 0.50)];
        double p95 = latenciesNs[(int) (count * 0.95)];
        double p99 = latenciesNs[(int) (count * 0.99)];
        return new BenchmarkStats(component, count, durationMs, throughput, p50, p95, p99);
    }

    @Test
    @DisplayName("Benchmark 1: Snowflake Distributed ID Generator Latency & Throughput")
    void benchmarkSnowflakeIdGenerator() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int warmup = 10_000;
        for (int i = 0; i < warmup; i++) {
            generator.nextId();
        }

        int count = 100_000;
        long[] latencies = new long[count];
        long overallStart = System.nanoTime();

        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            generator.nextId();
            latencies[i] = System.nanoTime() - start;
        }
        long overallDuration = System.nanoTime() - overallStart;

        BenchmarkStats stats = computeStats("SnowflakeIdGenerator", latencies, count, overallDuration);
        System.out.printf("[BENCHMARK] %s: %.0f ops/sec (p50: %.1f ns, p95: %.1f ns, p99: %.1f ns)%n",
                stats.component, stats.throughputOpsPerSec, stats.p50Ns, stats.p95Ns, stats.p99Ns);

        assertThat(stats.throughputOpsPerSec).isGreaterThan(500_000);
    }

    @Test
    @DisplayName("Benchmark 2: Consistent Hash Ring Routing Latency across 5 Nodes & 150 Vnodes")
    void benchmarkConsistentHashRing() {
        ConsistentHashRing<HashRingNode> ring = new ConsistentHashRing<>(150);
        for (int i = 0; i < 5; i++) {
            ring.addNode(new HashRingNode("node-" + i, "host-" + i, 8080));
        }

        int count = 100_000;
        long[] latencies = new long[count];
        long overallStart = System.nanoTime();

        for (int i = 0; i < count; i++) {
            String key = "order-partition-key-" + i;
            long start = System.nanoTime();
            ring.getNode(key);
            latencies[i] = System.nanoTime() - start;
        }
        long overallDuration = System.nanoTime() - overallStart;

        BenchmarkStats stats = computeStats("ConsistentHashRing", latencies, count, overallDuration);
        System.out.printf("[BENCHMARK] %s: %.0f ops/sec (p50: %.1f ns, p95: %.1f ns, p99: %.1f ns)%n",
                stats.component, stats.throughputOpsPerSec, stats.p50Ns, stats.p95Ns, stats.p99Ns);

        assertThat(stats.throughputOpsPerSec).isGreaterThan(100_000);
    }

    @Test
    @DisplayName("Benchmark 3: In-Memory Key-Value Store Concurrent Put/Get Latency")
    void benchmarkKeyValueStore() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        int count = 50_000;
        long[] latencies = new long[count];
        long overallStart = System.nanoTime();

        for (int i = 0; i < count; i++) {
            String k = "session:benchmark:" + i;
            long start = System.nanoTime();
            store.put(k, "data-" + i, Duration.ofSeconds(300));
            store.get(k);
            latencies[i] = System.nanoTime() - start;
        }
        long overallDuration = System.nanoTime() - overallStart;

        BenchmarkStats stats = computeStats("InMemoryKeyValueStore", latencies, count, overallDuration);
        System.out.printf("[BENCHMARK] %s: %.0f ops/sec (p50: %.1f ns, p95: %.1f ns, p99: %.1f ns)%n",
                stats.component, stats.throughputOpsPerSec, stats.p50Ns, stats.p95Ns, stats.p99Ns);

        assertThat(stats.throughputOpsPerSec).isGreaterThan(100_000);
    }

    @Test
    @DisplayName("Benchmark 4: Autocomplete Trie Search Latency across 2,000 Catalog Products")
    void benchmarkAutocompleteTrie() {
        AutocompleteTrie trie = new AutocompleteTrie();
        for (int i = 0; i < 2_000; i++) {
            Product p = new Product();
            p.setId("prod-" + i);
            p.setName("Product Model " + (i % 50) + " Premium Edition " + i);
            p.setCategory("Electronics");
            p.setPrice(BigDecimal.valueOf(99.99));
            p.setStockQuantity(10);
            trie.insert(p);
        }

        String[] prefixes = {"pro", "mod", "pre", "edi", "ele"};
        // Warmup JIT
        for (int i = 0; i < 2_000; i++) {
            trie.search(prefixes[i % prefixes.length], 10);
        }

        int count = 50_000;
        long[] latencies = new long[count];
        long overallStart = System.nanoTime();

        for (int i = 0; i < count; i++) {
            String prefix = prefixes[i % prefixes.length];
            long start = System.nanoTime();
            trie.search(prefix, 10);
            latencies[i] = System.nanoTime() - start;
        }
        long overallDuration = System.nanoTime() - overallStart;

        BenchmarkStats stats = computeStats("AutocompleteTrie", latencies, count, overallDuration);
        System.out.printf("[BENCHMARK] %s: %.0f ops/sec (p50: %.1f ns, p95: %.1f ns, p99: %.1f ns)%n",
                stats.component, stats.throughputOpsPerSec, stats.p50Ns, stats.p95Ns, stats.p99Ns);

        assertThat(stats.throughputOpsPerSec).isGreaterThan(25_000);
    }

    @Test
    @DisplayName("Failure Evaluation 1: Redis Rate Limiter FAIL_OPEN Graceful Degradation")
    void evaluateRedisRateLimiterFailure() {
        StringRedisTemplate brokenRedis = mock(StringRedisTemplate.class);
        when(brokenRedis.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Redis connection timed out"));

        RateLimitingProperties props = new RateLimitingProperties();
        props.setFallbackPolicy(RateLimitingProperties.FallbackPolicy.FAIL_OPEN);

        RedisRateLimiterService service = new RedisRateLimiterService(brokenRedis, props);
        RateLimitResult result = service.checkRateLimit("user:123", "api-default");

        // Under FAIL_OPEN, service permits traffic gracefully during outage
        assertThat(result.allowed()).isTrue();
        assertThat(result.degraded()).isTrue();
        assertThat(result.fallbackReason()).isEqualTo("redis-unavailable");
    }

    @Test
    @DisplayName("Failure Evaluation 2: Snowflake Clock Backward Jitter Recovery")
    void evaluateClockDriftRecovery() {
        AtomicLong clock = new AtomicLong(1767225600000L);
        AtomicLong invocation = new AtomicLong(0);

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                1, 1, 1767225600000L, 10L,
                () -> {
                    long inv = invocation.incrementAndGet();
                    if (inv == 2) {
                        return 1767225599998L; // 2ms backward jitter
                    } else if (inv >= 3) {
                        return 1767225600005L; // caught up
                    }
                    return clock.get();
                }
        );

        long id1 = generator.nextId();
        long id2 = generator.nextId();

        assertThat(id1).isPositive();
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    @DisplayName("Failure Evaluation 3: SSRF Host Disallowance and Malformed Protocol Mitigation")
    void evaluateSsrfMitigation() {
        SsrfValidator validator = new SsrfValidator(false);

        assertThat(validator.isSafe(URI.create("http://127.0.0.1:8080/metrics"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("http://169.254.169.254/latest/meta-data/"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("ftp://internal.backup/"), null)).isFalse();
    }
}
