package com.ecommerce.hashing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe consistent hashing ring implementing circular key partitioning
 * with configurable virtual nodes and pluggable hash functions.
 *
 * @param <T> Node type representing a shard, server, or cache partition
 */
public class ConsistentHashRing<T> {

    public static final int DEFAULT_VIRTUAL_NODES = 150;

    private final HashFunction hashFunction;
    private final int virtualNodes;
    private final NavigableMap<Long, T> ring = new TreeMap<>();
    private final Set<T> physicalNodes = new HashSet<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ConsistentHashRing() {
        this(new KetamaHashFunction(), DEFAULT_VIRTUAL_NODES);
    }

    public ConsistentHashRing(int virtualNodes) {
        this(new KetamaHashFunction(), virtualNodes);
    }

    public ConsistentHashRing(HashFunction hashFunction, int virtualNodes) {
        if (hashFunction == null) {
            throw new IllegalArgumentException("HashFunction cannot be null");
        }
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException("virtualNodes must be strictly positive");
        }
        this.hashFunction = hashFunction;
        this.virtualNodes = virtualNodes;
    }

    /**
     * Registers a physical node and creates its corresponding virtual nodes along the ring.
     *
     * @param node physical node to register
     */
    public void addNode(T node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        rwLock.writeLock().lock();
        try {
            if (physicalNodes.add(node)) {
                for (int i = 0; i < virtualNodes; i++) {
                    String vnodeToken = node.toString() + "#vn-" + i;
                    long hash = hashFunction.hash(vnodeToken);
                    ring.put(hash, node);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Registers multiple physical nodes.
     */
    public void addNodes(Collection<T> nodes) {
        if (nodes != null) {
            for (T node : nodes) {
                addNode(node);
            }
        }
    }

    /**
     * Removes a physical node and prunes all of its associated virtual nodes from the ring.
     *
     * @param node physical node to deregister
     * @return true if node was present and removed, false otherwise
     */
    public boolean removeNode(T node) {
        if (node == null) {
            return false;
        }
        rwLock.writeLock().lock();
        try {
            if (physicalNodes.remove(node)) {
                for (int i = 0; i < virtualNodes; i++) {
                    String vnodeToken = node.toString() + "#vn-" + i;
                    long hash = hashFunction.hash(vnodeToken);
                    ring.remove(hash);
                }
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Maps a partition/data key deterministically to the nearest clockwise node.
     *
     * @param key data key to route
     * @return Optional containing the target node, or empty if ring has no nodes
     */
    public Optional<T> getNode(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Routing key cannot be null or blank");
        }
        rwLock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return Optional.empty();
            }
            long hash = hashFunction.hash(key);
            Map.Entry<Long, T> entry = ring.ceilingEntry(hash);
            if (entry == null) {
                // Wrap around to beginning of ring
                entry = ring.firstEntry();
            }
            return Optional.ofNullable(entry != null ? entry.getValue() : null);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Locates a sequence of distinct physical nodes for replication / redundancy.
     *
     * @param key   data key to route
     * @param count maximum distinct nodes to return
     * @return List of up to 'count' distinct physical nodes
     */
    public List<T> getNodes(String key, int count) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Routing key cannot be null or blank");
        }
        if (count <= 0) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return Collections.emptyList();
            }
            Set<T> distinctNodes = new LinkedHashSet<>();
            long hash = hashFunction.hash(key);
            NavigableMap<Long, T> tailMap = ring.tailMap(hash, true);

            for (T node : tailMap.values()) {
                distinctNodes.add(node);
                if (distinctNodes.size() == count || distinctNodes.size() == physicalNodes.size()) {
                    return new ArrayList<>(distinctNodes);
                }
            }

            // Wrap around ring
            for (T node : ring.values()) {
                distinctNodes.add(node);
                if (distinctNodes.size() == count || distinctNodes.size() == physicalNodes.size()) {
                    break;
                }
            }
            return new ArrayList<>(distinctNodes);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        rwLock.readLock().lock();
        try {
            return physicalNodes.isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getPhysicalNodeCount() {
        rwLock.readLock().lock();
        try {
            return physicalNodes.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getVirtualNodeCount() {
        rwLock.readLock().lock();
        try {
            return ring.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Set<T> getPhysicalNodes() {
        rwLock.readLock().lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(physicalNodes));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Computes key distribution metrics across all registered nodes for diagnostic analysis.
     */
    public Map<T, Integer> calculateKeyDistribution(Iterable<String> keys) {
        rwLock.readLock().lock();
        try {
            Map<T, Integer> distribution = new HashMap<>();
            for (T node : physicalNodes) {
                distribution.put(node, 0);
            }
            if (keys == null || physicalNodes.isEmpty()) {
                return distribution;
            }
            for (String key : keys) {
                getNode(key).ifPresent(node -> distribution.put(node, distribution.getOrDefault(node, 0) + 1));
            }
            return distribution;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Calculates the standard deviation of key distribution across nodes.
     */
    public double calculateStandardDeviation(Iterable<String> keys) {
        Map<T, Integer> distribution = calculateKeyDistribution(keys);
        if (distribution.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (int count : distribution.values()) {
            sum += count;
        }
        double mean = sum / distribution.size();
        double varianceSum = 0.0;
        for (int count : distribution.values()) {
            varianceSum += Math.pow(count - mean, 2);
        }
        return Math.sqrt(varianceSum / distribution.size());
    }
}
