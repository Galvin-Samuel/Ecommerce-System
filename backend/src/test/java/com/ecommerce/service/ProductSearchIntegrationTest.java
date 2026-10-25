package com.ecommerce.service;

import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProductSearchIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private final List<String> seededProductIds = new ArrayList<>();

    @BeforeEach
    void seedProducts() {
        // Seed 15 diverse products to test search, category filter, price bands, sorting and special characters
        List<Product> products = List.of(
                new Product("Apple iPhone 15 Pro", "Flagship smartphone with titanium body", "Electronics", new BigDecimal("999.00"), 50, null, "seller-seed"),
                new Product("Samsung Galaxy S24 Ultra", "Android powerhouse with S-Pen stylus", "Electronics", new BigDecimal("1199.99"), 30, null, "seller-seed"),
                new Product("Sony WH-1000XM5 Headphones", "Industry leading noise-cancelling wireless headphones", "Electronics", new BigDecimal("349.99"), 20, null, "seller-seed"),
                new Product("Budget Wireless Earbuds", "Affordable Bluetooth earphones", "Electronics", new BigDecimal("29.99"), 100, null, "seller-seed"),
                new Product("USB-C Fast Charger (65W)", "Compact GaN wall charger with cable", "Accessories", new BigDecimal("39.99"), 80, null, "seller-seed"),
                new Product("Mechanical Gaming Keyboard", "RGB clicky mechanical switches", "Accessories", new BigDecimal("89.99"), 45, null, "seller-seed"),
                new Product("Ergonomic Vertical Mouse", "Wireless optical mouse for wrist support", "Accessories", new BigDecimal("49.50"), 60, null, "seller-seed"),
                new Product("Clean Code Book", "A Handbook of Agile Software Craftsmanship", "Books", new BigDecimal("35.00"), 40, null, "seller-seed"),
                new Product("Designing Data-Intensive Applications", "Reliable, scalable, and maintainable systems guide", "Books", new BigDecimal("45.50"), 25, null, "seller-seed"),
                new Product("C++ Primer (5th Edition)", "Definitive C++ reference and tutorial", "Books", new BigDecimal("55.00"), 15, null, "seller-seed"),
                new Product("Men's Running Shoes [Pro Edition]", "Lightweight breathable marathon sneakers (v2)", "Footwear", new BigDecimal("120.00"), 35, null, "seller-seed"),
                new Product("Leather Travel Wallet", "RFID blocking passport wallet", "Fashion", new BigDecimal("25.00"), 70, null, "seller-seed"),
                new Product("Stainless Steel Water Bottle", "Insulated 32oz sports flask", "Fitness", new BigDecimal("19.99"), 150, null, "seller-seed"),
                new Product("Adjustable Desk Lamp", "LED desk light with wireless charging pad", "Home", new BigDecimal("65.00"), 40, null, "seller-seed"),
                new Product("Espresso Coffee Maker", "15-bar pump espresso machine for home barista", "Kitchen", new BigDecimal("189.00"), 18, null, "seller-seed")
        );

        for (Product p : products) {
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            Product saved = productRepository.save(p);
            seededProductIds.add(saved.getId());
        }
    }

    @AfterEach
    void cleanUp() {
        if (!seededProductIds.isEmpty()) {
            productRepository.deleteAllById(seededProductIds);
            seededProductIds.clear();
        }
    }

    @Test
    @DisplayName("Gate 1: Search with matching products returns expected results")
    void shouldFindMatchingProducts() {
        PagedResponse<ProductResponse> result = productService.searchProducts(
                "iPhone", null, null, null, 0, 10, "createdAt,desc"
        );

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).anyMatch(p -> p.name().contains("Apple iPhone 15 Pro"));
    }

    @Test
    @DisplayName("Gate 2: Search with no results returns empty items list and 0 elements")
    void shouldReturnEmptyForNonMatchingQuery() {
        PagedResponse<ProductResponse> result = productService.searchProducts(
                "NonexistentSuperGizmoXYZ999", null, null, null, 0, 10, "createdAt,desc"
        );

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Gate 3: Case-insensitive search matches regardless of input casing")
    void shouldPerformCaseInsensitiveSearch() {
        PagedResponse<ProductResponse> lowerResult = productService.searchProducts(
                "galaxy", null, null, null, 0, 10, null
        );
        PagedResponse<ProductResponse> upperResult = productService.searchProducts(
                "GALAXY", null, null, null, 0, 10, null
        );
        PagedResponse<ProductResponse> mixedResult = productService.searchProducts(
                "GaLaXy", null, null, null, 0, 10, null
        );

        assertThat(lowerResult.items()).isNotEmpty();
        assertThat(upperResult.items()).isNotEmpty();
        assertThat(mixedResult.items()).isNotEmpty();

        assertThat(lowerResult.items().get(0).id()).isEqualTo(upperResult.items().get(0).id());
        assertThat(lowerResult.items().get(0).id()).isEqualTo(mixedResult.items().get(0).id());
    }

    @Test
    @DisplayName("Gate 4: Category filtering limits results strictly to specified category")
    void shouldFilterStrictlyByCategory() {
        PagedResponse<ProductResponse> booksResult = productService.searchProducts(
                null, "Books", null, null, 0, 20, null
        );

        assertThat(booksResult.items()).isNotEmpty();
        assertThat(booksResult.items()).allMatch(p -> "Books".equals(p.category()));
        assertThat(booksResult.items()).anyMatch(p -> p.name().contains("Clean Code"));
    }

    @Test
    @DisplayName("Gate 5: Price-range filtering returns items within min and max boundaries")
    void shouldFilterByPriceRange() {
        BigDecimal min = new BigDecimal("30.00");
        BigDecimal max = new BigDecimal("50.00");

        PagedResponse<ProductResponse> result = productService.searchProducts(
                null, null, min, max, 0, 20, "price,asc"
        );

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allMatch(p ->
                p.price().compareTo(min) >= 0 && p.price().compareTo(max) <= 0
        );
    }

    @Test
    @DisplayName("Gate 6: Pagination and sorting produces predictable ordered subsets")
    void shouldPaginateAndSortPredictably() {
        // Page 0 with 2 items sorted by price asc
        PagedResponse<ProductResponse> page0 = productService.searchProducts(
                null, null, null, null, 0, 2, "price,asc"
        );
        // Page 1 with 2 items sorted by price asc
        PagedResponse<ProductResponse> page1 = productService.searchProducts(
                null, null, null, null, 1, 2, "price,asc"
        );

        assertThat(page0.items()).hasSize(2);
        assertThat(page1.items()).hasSize(2);

        // Price on page 0 item 1 should be <= price on page 1 item 0
        BigDecimal lastPage0Price = page0.items().get(1).price();
        BigDecimal firstPage1Price = page1.items().get(0).price();
        assertThat(lastPage0Price).isLessThanOrEqualTo(firstPage1Price);
    }

    @Test
    @DisplayName("Gate 7: Special regex characters and blank search terms handled safely")
    void shouldHandleSpecialCharactersAndBlankTermsSafely() {
        // C++ includes '+' regex metacharacters
        PagedResponse<ProductResponse> cppResult = productService.searchProducts(
                "C++", null, null, null, 0, 10, null
        );
        assertThat(cppResult.items()).isNotEmpty();
        assertThat(cppResult.items().get(0).name()).contains("C++");

        // '[Pro Edition]' includes brackets
        PagedResponse<ProductResponse> bracketResult = productService.searchProducts(
                "[Pro Edition]", null, null, null, 0, 10, null
        );
        assertThat(bracketResult.items()).isNotEmpty();
        assertThat(bracketResult.items().get(0).name()).contains("[Pro Edition]");

        // Blank or whitespace query should return all items without error
        PagedResponse<ProductResponse> blankResult = productService.searchProducts(
                "   ", "Books", null, null, 0, 10, null
        );
        assertThat(blankResult.items()).isNotEmpty();
        assertThat(blankResult.items()).allMatch(p -> "Books".equals(p.category()));
    }

    @Test
    @DisplayName("Gate 8: Invalid pagination parameters are safely bounded")
    void shouldSafelyBoundInvalidPaginationParameters() {
        // Negative page (-5) and negative size (-10) should safely bound to page=0 and size=1
        PagedResponse<ProductResponse> result = productService.searchProducts(
                null, null, null, null, -5, -10, null
        );

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result.items()).isNotEmpty();
    }

    @Test
    @DisplayName("Gate 9: Combined query with larger seeded dataset")
    void shouldExecuteCombinedQueryAccurately() {
        PagedResponse<ProductResponse> result = productService.searchProducts(
                "wireless", "Electronics", new BigDecimal("20.00"), new BigDecimal("400.00"), 0, 10, "price,desc"
        );

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allMatch(p -> "Electronics".equals(p.category()));
        assertThat(result.items()).allMatch(p ->
                p.name().toLowerCase().contains("wireless") ||
                        (p.description() != null && p.description().toLowerCase().contains("wireless"))
        );
    }

    @Test
    @DisplayName("Gate 10: Verify search results match actual MongoDB records")
    void shouldVerifyAgainstMongoDBDirectly() {
        PagedResponse<ProductResponse> searchResult = productService.searchProducts(
                "Espresso", null, null, null, 0, 1, null
        );

        assertThat(searchResult.items()).hasSize(1);
        String foundId = searchResult.items().get(0).id();

        // Direct database verification
        Product directFromDb = productRepository.findById(foundId).orElse(null);
        assertThat(directFromDb).isNotNull();
        assertThat(directFromDb.getName()).isEqualTo(searchResult.items().get(0).name());
        assertThat(directFromDb.getPrice()).isEqualByComparingTo(searchResult.items().get(0).price());
    }
}
