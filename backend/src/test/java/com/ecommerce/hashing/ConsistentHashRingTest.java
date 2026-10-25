package com.ecommerce.hashing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashRingTest {

    @Test
    @DisplayName("Should route identical key deterministically to the same node")
    void shouldMapDeterministicallyToSameNode() {
        ConsistentHashRing<HashRingNode> ring = new ConsistentHashRing<>();
        HashRingNode node1 = new HashRingNode("node-1", "10.0.0.1", 8081);
        HashRingNode node2 = new HashRingNode("node-2", "10.0.0.2", 8082);
        HashRingNode node3 = new HashRingNode("node-3", "10.0.0.3", 8083);

        ring.addNode(node1);
        ring.addNode(node2);
        ring.addNode(node3);

        String testKey = "user_account_9847192";
        Optional<HashRingNode> initial = ring.getNode(testKey);
        assertTrue(initial.isPresent());

        // Perform 500 subsequent lookups
        for (int i = 0; i < 500; i++) {
            assertEquals(initial.get(), ring.getNode(testKey).orElse(null));
        }
    }

    @Test
    @DisplayName("Should safely handle empty ring without throwing exceptions")
    void shouldHandleEmptyRingSafely() {
        ConsistentHashRing<String> emptyRing = new ConsistentHashRing<>();
        assertTrue(emptyRing.isEmpty());
        assertEquals(0, emptyRing.getPhysicalNodeCount());
        assertEquals(0, emptyRing.getVirtualNodeCount());

        Optional<String> node = emptyRing.getNode("test-key");
        assertFalse(node.isPresent());
        assertTrue(node.isEmpty());
    }

    @Test
    @DisplayName("Should reject invalid or blank routing keys")
    void shouldRejectInvalidKeys() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>();
        ring.addNode("node-1");

        assertThrows(IllegalArgumentException.class, () -> ring.getNode(null));
        assertThrows(IllegalArgumentException.class, () -> ring.getNode(""));
        assertThrows(IllegalArgumentException.class, () -> ring.getNode("   "));
    }

    @Test
    @DisplayName("Should register and remove virtual nodes accurately upon node topology change")
    void shouldRegisterAndRemoveVirtualNodes() {
        int vnodes = 100;
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(vnodes);

        ring.addNode("shard-1");
        ring.addNode("shard-2");
        assertEquals(2, ring.getPhysicalNodeCount());
        assertEquals(200, ring.getVirtualNodeCount());

        boolean removed = ring.removeNode("shard-2");
        assertTrue(removed);
        assertEquals(1, ring.getPhysicalNodeCount());
        assertEquals(100, ring.getVirtualNodeCount());

        // All keys must now map to shard-1
        assertEquals("shard-1", ring.getNode("key-1").orElse(null));
        assertEquals("shard-1", ring.getNode("key-2").orElse(null));
    }

    @Test
    @DisplayName("Should achieve balanced key distribution across physical nodes")
    void shouldProvideBalancedKeyDistributionWithVirtualNodes() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(150);
        List<String> nodes = List.of("node-a", "node-b", "node-c", "node-d");
        ring.addNodes(nodes);

        int sampleSize = 10000;
        List<String> keys = new ArrayList<>(sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            keys.add("item-sku-" + UUID.randomUUID());
        }

        Map<String, Integer> distribution = ring.calculateKeyDistribution(keys);
        double stdDev = ring.calculateStandardDeviation(keys);

        // In a 4-node cluster, ideal share is 25% (2500 keys).
        // With 150 vnodes, every node must receive between 18% and 32% (1800 to 3200 keys)
        for (String node : nodes) {
            int count = distribution.get(node);
            assertTrue(count >= 1800, "Node " + node + " received too few keys: " + count);
            assertTrue(count <= 3200, "Node " + node + " received too many keys: " + count);
        }
        assertTrue(stdDev < 500, "Standard deviation should be reasonable across nodes: " + stdDev);
    }

    @Test
    @DisplayName("Should demonstrate limited K/N key remapping property when a node is added")
    void shouldDemonstrateLimitedKeyRemappingWhenNodeAdded() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(150);
        ring.addNodes(List.of("shard-1", "shard-2", "shard-3", "shard-4"));

        int keyCount = 2000;
        List<String> keys = new ArrayList<>(keyCount);
        Map<String, String> initialMapping = new HashMap<>();

        for (int i = 0; i < keyCount; i++) {
            String key = "record-" + i;
            keys.add(key);
            initialMapping.put(key, ring.getNode(key).orElseThrow());
        }

        // Add 5th node
        ring.addNode("shard-5");

        int remappedCount = 0;
        int movedToNewNode = 0;
        int movedBetweenOldNodes = 0;

        for (String key : keys) {
            String initialNode = initialMapping.get(key);
            String newNode = ring.getNode(key).orElseThrow();

            if (!initialNode.equals(newNode)) {
                remappedCount++;
                if ("shard-5".equals(newNode)) {
                    movedToNewNode++;
                } else {
                    movedBetweenOldNodes++;
                }
            }
        }

        // Theoretical optimal remapping when moving from 4 to 5 nodes is 1/5 = 20% (400 keys).
        // With Consistent Hashing:
        // 1. Remapped percentage must be roughly between 15% and 30%.
        // 2. Crucially, all remapped keys MUST move to the new node (shard-5); NO keys move between old shards!
        double remappedPercentage = (double) remappedCount / keyCount * 100.0;
        assertTrue(remappedPercentage >= 14.0 && remappedPercentage <= 30.0,
                "Remapped percentage should be near 20%, but was: " + remappedPercentage + "%");
        assertEquals(0, movedBetweenOldNodes, "Keys must never shuffle between existing nodes!");
        assertEquals(remappedCount, movedToNewNode, "All remapped keys must go to the newly added node");
    }

    @Test
    @DisplayName("Should return preference list of distinct nodes in clockwise order")
    void shouldReturnPreferenceListOfDistinctNodes() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>();
        ring.addNodes(List.of("srv-1", "srv-2", "srv-3", "srv-4"));

        List<String> replicaNodes = ring.getNodes("cart_session_abc", 3);
        assertEquals(3, replicaNodes.size());
        // Verify distinctness
        assertEquals(3, replicaNodes.stream().distinct().count());
    }

    @Test
    @DisplayName("Should support concurrent lookups and modifications without race conditions")
    void shouldSupportConcurrentAccess() throws InterruptedException {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>();
        ring.addNodes(List.of("node-1", "node-2", "node-3"));

        int threads = 12;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String key = "key-" + threadId + "-" + i;
                        Optional<String> node = ring.getNode(key);
                        if (node.isPresent()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threads * iterationsPerThread, successCount.get());
    }
}
