package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private String createdProductId;

    @AfterEach
    void tearDown() {
        if (createdProductId != null) {
            productRepository.deleteById(createdProductId);
        }
    }

    @Test
    @DisplayName("Verify created product actually persists in MongoDB repository")
    void shouldPersistProductInMongoDB() {
        UserPrincipal seller = new UserPrincipal("seller-int-1", "seller_int@example.com", UserRole.SELLER);
        ProductRequest request = new ProductRequest(
                "Mechanical Keyboard Pro",
                "RGB backlit keyboard",
                "Accessories",
                new BigDecimal("89.99"),
                40,
                "https://example.com/keyboard.png"
        );

        ProductResponse response = productService.createProduct(request, seller);
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotBlank();
        createdProductId = response.id();

        // Direct database verification
        Optional<Product> persisted = productRepository.findById(createdProductId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getName()).isEqualTo("Mechanical Keyboard Pro");
        assertThat(persisted.get().getPrice()).isEqualByComparingTo(new BigDecimal("89.99"));
        assertThat(persisted.get().getSellerId()).isEqualTo("seller-int-1");
    }
}
