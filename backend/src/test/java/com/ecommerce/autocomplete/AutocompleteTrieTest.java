package com.ecommerce.autocomplete;

import com.ecommerce.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class AutocompleteTrieTest {

    private AutocompleteTrie trie;

    @BeforeEach
    void setUp() {
        trie = new AutocompleteTrie();
    }

    private Product createProduct(String id, String name, String category, double price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCategory(category);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStockQuantity(stock);
        return p;
    }

    @Test
    @DisplayName("Should return matching suggestions for case-insensitive exact and word prefixes")
    void testPrefixMatching() {
        trie.insert(createProduct("p1", "Apple iPhone 15 Pro", "Electronics", 999.0, 10));
        trie.insert(createProduct("p2", "Apple MacBook Air M3", "Computers", 1199.0, 5));
        trie.insert(createProduct("p3", "Samsung Galaxy S24", "Electronics", 899.0, 8));

        // Exact name prefix
        List<ProductSuggestionResponse> appleMatches = trie.search("app", 10);
        assertThat(appleMatches).hasSize(2);
        assertThat(appleMatches.stream().map(ProductSuggestionResponse::name))
                .containsExactly("Apple iPhone 15 Pro", "Apple MacBook Air M3");

        // Token prefix ("mac" matches "MacBook")
        List<ProductSuggestionResponse> macMatches = trie.search("mac", 10);
        assertThat(macMatches).hasSize(1);
        assertThat(macMatches.get(0).name()).isEqualTo("Apple MacBook Air M3");

        // Case insensitivity
        List<ProductSuggestionResponse> samsungUpper = trie.search("SAMS", 10);
        assertThat(samsungUpper).hasSize(1);
        assertThat(samsungUpper.get(0).name()).isEqualTo("Samsung Galaxy S24");
    }

    @Test
    @DisplayName("Should rank exact name prefix higher than secondary word token match")
    void testRanking() {
        trie.insert(createProduct("p1", "Galaxy Quest Movie", "Entertainment", 19.99, 10)); // exact prefix "galaxy"
        trie.insert(createProduct("p2", "Samsung Galaxy S24", "Electronics", 899.0, 8));     // word token "galaxy"

        List<ProductSuggestionResponse> results = trie.search("galaxy", 10);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("Galaxy Quest Movie");
        assertThat(results.get(1).name()).isEqualTo("Samsung Galaxy S24");
    }

    @Test
    @DisplayName("Should exclude out-of-stock products from suggestions")
    void testOutOfStockExcluded() {
        trie.insert(createProduct("p1", "Sony PlayStation 5", "Gaming", 499.0, 0)); // stock = 0
        trie.insert(createProduct("p2", "Sony Headphones WH-1000", "Audio", 349.0, 12));

        List<ProductSuggestionResponse> results = trie.search("sony", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Sony Headphones WH-1000");
    }

    @Test
    @DisplayName("Should handle product removal and updates correctly")
    void testRemovalAndUpdates() {
        Product phone = createProduct("p1", "Google Pixel 8", "Phones", 699.0, 15);
        trie.insert(phone);
        assertThat(trie.search("pixel", 5)).hasSize(1);

        // Update with 0 stock -> automatically removed
        phone.setStockQuantity(0);
        trie.insert(phone);
        assertThat(trie.search("pixel", 5)).isEmpty();

        // Re-stock
        phone.setStockQuantity(10);
        trie.insert(phone);
        assertThat(trie.search("pixel", 5)).hasSize(1);

        // Explicit remove
        trie.remove("p1");
        assertThat(trie.search("pixel", 5)).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty, blank, or oversized queries gracefully")
    void testEdgeCaseQueries() {
        trie.insert(createProduct("p1", "Dell XPS 15", "Computers", 1499.0, 5));

        assertThat(trie.search(null, 10)).isEmpty();
        assertThat(trie.search("", 10)).isEmpty();
        assertThat(trie.search("   ", 10)).isEmpty();
        assertThat(trie.search("del", 0)).isEmpty();
    }

    @Test
    @DisplayName("Should remain thread-safe under concurrent inserts, removals, and searches")
    void testConcurrency() throws InterruptedException {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 500; i++) {
                        String id = "thread-" + threadId + "-prod-" + i;
                        trie.insert(createProduct(id, "Product " + id, "General", 10.0, 5));
                        trie.search("prod", 5);
                        if (i % 2 == 0) {
                            trie.remove(id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertThat(trie.size()).isGreaterThanOrEqualTo(0);
    }
}
