package com.ecommerce.autocomplete;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service orchestrating search suggestions via an in-memory prefix Trie,
 * backed by Redis caching and MongoDB lifecycle synchronization.
 */
@Service
public class AutocompleteService {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);
    private static final String CACHE_PREFIX = "autocomplete:prefix:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AutocompleteTrie trie;

    @Autowired
    public AutocompleteService(ProductRepository productRepository,
                               @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.trie = new AutocompleteTrie();
    }

    @PostConstruct
    public void initTrie() {
        try {
            trie.clear();
            log.info("Initializing AutocompleteTrie from MongoDB catalog...");
            List<Product> products = productRepository.findAll();
            int indexedCount = 0;
            for (Product product : products) {
                if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
                    trie.insert(product);
                    indexedCount++;
                }
            }
            log.info("AutocompleteTrie initialized with {} in-stock products", indexedCount);
        } catch (Exception e) {
            log.warn("Failed to initialize AutocompleteTrie during startup: {}", e.getMessage());
        }
    }

    /**
     * Retrieves up to limit autocomplete product suggestions for a prefix query.
     */
    public List<ProductSuggestionResponse> getSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String prefix = query.trim().toLowerCase();
        if (prefix.length() > 50) {
            prefix = prefix.substring(0, 50);
        }

        int boundedLimit = Math.max(1, Math.min(limit, 20));
        String cacheKey = CACHE_PREFIX + prefix + ":" + boundedLimit;

        // 1. Try Redis cache lookup
        if (redisTemplate != null) {
            try {
                @SuppressWarnings("unchecked")
                List<ProductSuggestionResponse> cached = (List<ProductSuggestionResponse>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                log.debug("Redis cache read failed for autocomplete key '{}': {}", cacheKey, e.getMessage());
            }
        }

        // 2. Query in-memory prefix Trie
        List<ProductSuggestionResponse> suggestions = trie.search(prefix, boundedLimit);

        // 3. Cache results in Redis
        if (redisTemplate != null && !suggestions.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, suggestions, CACHE_TTL);
            } catch (Exception e) {
                log.debug("Redis cache write failed for autocomplete key '{}': {}", cacheKey, e.getMessage());
            }
        }

        return suggestions;
    }

    /**
     * Indexes or updates a product in the autocomplete trie and invalidates cached prefixes.
     */
    public void indexProduct(Product product) {
        trie.insert(product);
        invalidateCache();
    }

    /**
     * Removes a product from the autocomplete index.
     */
    public void removeProduct(String productId) {
        trie.remove(productId);
        invalidateCache();
    }

    public int getIndexSize() {
        return trie.size();
    }

    private void invalidateCache() {
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                log.debug("Redis cache invalidation failed: {}", e.getMessage());
            }
        }
    }
}
