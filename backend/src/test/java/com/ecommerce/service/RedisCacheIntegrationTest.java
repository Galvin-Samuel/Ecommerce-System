package com.ecommerce.service;

import com.ecommerce.config.RedisCacheConfig;
import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UserPrincipal seller;
    private final List<String> createdProductIds = new ArrayList<>();
    private Product seededProduct;

    @BeforeEach
    void setUp() {
        seller = new UserPrincipal("seller-cache-1", "seller_cache@example.com", UserRole.SELLER);

        // Clear Redis cache namespaces
        Cache productsCache = cacheManager.getCache(RedisCacheConfig.PRODUCTS_CACHE);
        if (productsCache != null) {
            productsCache.clear();
        }
        Cache searchesCache = cacheManager.getCache(RedisCacheConfig.PRODUCT_SEARCHES_CACHE);
        if (searchesCache != null) {
            searchesCache.clear();
        }

        seededProduct = new Product(
                "4K Ultra HD Monitor",
                "32-inch IPS panel monitor with HDR",
                "Electronics",
                new BigDecimal("399.99"),
                25,
                "https://example.com/monitor.jpg",
                "seller-cache-1"
        );
        seededProduct = productRepository.save(seededProduct);
        createdProductIds.add(seededProduct.getId());
    }

    @AfterEach
    void tearDown() {
        if (!createdProductIds.isEmpty()) {
            productRepository.deleteAllById(createdProductIds);
            createdProductIds.clear();
        }

        Cache productsCache = cacheManager.getCache(RedisCacheConfig.PRODUCTS_CACHE);
        if (productsCache != null) {
            productsCache.clear();
        }
        Cache searchesCache = cacheManager.getCache(RedisCacheConfig.PRODUCT_SEARCHES_CACHE);
        if (searchesCache != null) {
            searchesCache.clear();
        }
    }

    @Test
    @DisplayName("Gate 1 & 2 & 9: First read misses cache, repeated read hits cache, Redis key exists with positive TTL")
    void shouldCacheProductOnReadAndReturnOnSubsequentReads() {
        String productId = seededProduct.getId();
        String expectedRedisKey = "products::" + productId;

        // 1. Prior to read, key should not exist in Redis
        Boolean existsBefore = redisTemplate.hasKey(expectedRedisKey);
        assertThat(existsBefore).isFalse();

        // First read (Cache Miss -> loads from MongoDB and populates Redis)
        long startMiss = System.nanoTime();
        ProductResponse firstRead = productService.getProductById(productId);
        long durationMiss = System.nanoTime() - startMiss;

        assertThat(firstRead).isNotNull();
        assertThat(firstRead.id()).isEqualTo(productId);
        assertThat(firstRead.name()).isEqualTo("4K Ultra HD Monitor");

        // Verify key now exists in Redis
        Boolean existsAfter = redisTemplate.hasKey(expectedRedisKey);
        assertThat(existsAfter).isTrue();

        // Verify TTL is configured and positive
        Long ttl = redisTemplate.getExpire(expectedRedisKey, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);

        // 2. Second read (Cache Hit -> served from Redis)
        long startHit = System.nanoTime();
        ProductResponse secondRead = productService.getProductById(productId);
        long durationHit = System.nanoTime() - startHit;

        assertThat(secondRead).isNotNull();
        assertThat(secondRead.id()).isEqualTo(productId);
        assertThat(secondRead.name()).isEqualTo(firstRead.name());
        assertThat(secondRead.price()).isEqualByComparingTo(firstRead.price());
    }

    @Test
    @DisplayName("Gate 4: Updating a product invalidates the cached product and search results")
    void shouldInvalidateCacheOnProductUpdate() {
        String productId = seededProduct.getId();
        String redisKey = "products::" + productId;

        // Populate cache
        productService.getProductById(productId);
        productService.searchProducts("Monitor", "Electronics", null, null, 0, 10, null);

        assertThat(redisTemplate.hasKey(redisKey)).isTrue();
        Set<String> searchKeys = redisTemplate.keys("product_searches::*");
        assertThat(searchKeys).isNotEmpty();

        // Update product
        ProductRequest updateRequest = new ProductRequest(
                "4K Ultra HD Monitor - 2026 Edition",
                "Updated 32-inch monitor",
                "Electronics",
                new BigDecimal("429.99"),
                20,
                null
        );

        productService.updateProduct(productId, updateRequest, seller);

        // Verify Redis key for product is evicted
        Boolean existsAfterUpdate = redisTemplate.hasKey(redisKey);
        assertThat(existsAfterUpdate).isFalse();

        // Verify search cache entries are evicted
        Set<String> remainingSearchKeys = redisTemplate.keys("product_searches::*");
        assertThat(remainingSearchKeys).isEmpty();

        // Next read repopulates cache with the updated product name
        ProductResponse freshRead = productService.getProductById(productId);
        assertThat(freshRead.name()).isEqualTo("4K Ultra HD Monitor - 2026 Edition");
        assertThat(freshRead.price()).isEqualByComparingTo(new BigDecimal("429.99"));
    }

    @Test
    @DisplayName("Gate 5: Deleting a product removes cached product and does not serve stale data")
    void shouldEvictCacheOnProductDeletion() {
        String productId = seededProduct.getId();
        String redisKey = "products::" + productId;

        // Populate cache
        productService.getProductById(productId);
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        // Delete product
        productService.deleteProduct(productId, seller);

        // Verify cache key was evicted
        Boolean existsAfterDelete = redisTemplate.hasKey(redisKey);
        assertThat(existsAfterDelete).isFalse();
    }

    @Test
    @DisplayName("Gate 8: Search queries with distinct parameters generate unique cache keys")
    void shouldCacheSearchResultsWithDistinctKeys() {
        // Query 1: Electronics
        PagedResponse<ProductResponse> r1 = productService.searchProducts("Monitor", "Electronics", null, null, 0, 10, "price,asc");
        // Query 2: Different category
        PagedResponse<ProductResponse> r2 = productService.searchProducts("Monitor", "Office", null, null, 0, 10, "price,asc");

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();

        Set<String> searchKeys = redisTemplate.keys("product_searches::*");
        assertThat(searchKeys).hasSize(2);
    }

    @Test
    @DisplayName("Gate 10: MongoDB remains the source of truth when cache is cleared")
    void shouldVerifyMongoDBRemainsSourceOfTruth() {
        String productId = seededProduct.getId();

        // Populate cache
        productService.getProductById(productId);

        // Clear Redis cache manually
        Cache productsCache = cacheManager.getCache(RedisCacheConfig.PRODUCTS_CACHE);
        if (productsCache != null) {
            productsCache.clear();
        }

        // Read still succeeds by reading from MongoDB
        ProductResponse response = productService.getProductById(productId);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("4K Ultra HD Monitor");
    }
}
