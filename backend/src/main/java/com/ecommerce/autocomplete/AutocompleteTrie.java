package com.ecommerce.autocomplete;

import com.ecommerce.model.Product;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Concurrency-safe prefix Trie for high-throughput product autocomplete and search suggestions.
 */
public class AutocompleteTrie {

    public record IndexedItem(
            String productId,
            String name,
            String category,
            BigDecimal price,
            int stockQuantity
    ) {
    }

    private static class TrieNode {
        final Map<Character, TrieNode> children = new ConcurrentHashMap<>();
        final Set<String> productIds = ConcurrentHashMap.newKeySet();
    }

    private final TrieNode root = new TrieNode();
    private final Map<String, IndexedItem> itemRegistry = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Inserts or updates a product in the autocomplete Trie.
     */
    public void insert(Product product) {
        if (product == null || product.getId() == null || product.getName() == null) {
            return;
        }

        // Only index products that are in stock
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            remove(product.getId());
            return;
        }

        rwLock.writeLock().lock();
        try {
            // Remove previous version if already indexed
            remove(product.getId());

            IndexedItem item = new IndexedItem(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    product.getPrice(),
                    product.getStockQuantity()
            );
            itemRegistry.put(product.getId(), item);

            // Index full name prefix
            insertText(product.getName().toLowerCase(), product.getId());

            // Index individual words in the product name
            String[] tokens = product.getName().toLowerCase().split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 2) {
                    insertText(token, product.getId());
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Removes a product from the autocomplete index.
     */
    public void remove(String productId) {
        if (productId == null) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            IndexedItem removed = itemRegistry.remove(productId);
            if (removed != null) {
                removeIdFromTrie(root, productId);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Searches matching product suggestions for a prefix query.
     */
    public List<ProductSuggestionResponse> search(String query, int limit) {
        if (query == null || query.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        String prefix = query.toLowerCase().trim();

        rwLock.readLock().lock();
        try {
            TrieNode current = root;
            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);
                current = current.children.get(c);
                if (current == null) {
                    return Collections.emptyList(); // No prefix match
                }
            }

            // Collect all candidate product IDs reachable from this node
            Set<String> matchedIds = new HashSet<>();
            collectProductIds(current, matchedIds, limit * 3);

            List<ProductSuggestionResponse> results = new ArrayList<>();
            for (String id : matchedIds) {
                IndexedItem item = itemRegistry.get(id);
                if (item != null && item.stockQuantity() > 0) {
                    String highlight = generateHighlight(item.name(), prefix);
                    results.add(new ProductSuggestionResponse(
                            item.productId(),
                            item.name(),
                            item.category(),
                            item.price(),
                            highlight
                    ));
                }
            }

            // Rank results: exact name prefix first, then token prefix, then alphabetically
            results.sort((a, b) -> {
                boolean aStartsWith = a.name().toLowerCase().startsWith(prefix);
                boolean bStartsWith = b.name().toLowerCase().startsWith(prefix);
                if (aStartsWith && !bStartsWith) return -1;
                if (!aStartsWith && bStartsWith) return 1;
                return a.name().compareToIgnoreCase(b.name());
            });

            return results.size() > limit ? results.subList(0, limit) : results;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int size() {
        return itemRegistry.size();
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            root.children.clear();
            root.productIds.clear();
            itemRegistry.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void insertText(String text, String productId) {
        TrieNode current = root;
        current.productIds.add(productId);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
            current.productIds.add(productId);
        }
    }

    private void removeIdFromTrie(TrieNode node, String productId) {
        node.productIds.remove(productId);
        for (TrieNode child : node.children.values()) {
            removeIdFromTrie(child, productId);
        }
    }

    private void collectProductIds(TrieNode node, Set<String> target, int maxCollect) {
        if (target.size() >= maxCollect) {
            return;
        }
        for (String pid : node.productIds) {
            target.add(pid);
            if (target.size() >= maxCollect) {
                return;
            }
        }
        for (TrieNode child : node.children.values()) {
            if (target.size() >= maxCollect) {
                break;
            }
            collectProductIds(child, target, maxCollect);
        }
    }

    private String generateHighlight(String fullName, String prefix) {
        int idx = fullName.toLowerCase().indexOf(prefix);
        if (idx >= 0) {
            int end = idx + prefix.length();
            return fullName.substring(0, idx) + "<b>" + fullName.substring(idx, end) + "</b>" + fullName.substring(end);
        }
        return fullName;
    }
}
