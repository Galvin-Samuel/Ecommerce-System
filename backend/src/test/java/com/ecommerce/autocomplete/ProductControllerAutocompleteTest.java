package com.ecommerce.autocomplete;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerAutocompleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AutocompleteService autocompleteService;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Product p1 = new Product("Wireless Mechanical Keyboard", "RGB Gaming", "Accessories", BigDecimal.valueOf(89.99), 20, "img.jpg", "seller-1");
        Product p2 = new Product("Wireless Ergonomic Mouse", "High precision", "Accessories", BigDecimal.valueOf(49.99), 15, "img.jpg", "seller-1");
        Product p3 = new Product("Sold Out Wireless Headphones", "Noise cancelling", "Audio", BigDecimal.valueOf(199.99), 0, "img.jpg", "seller-1");

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        autocompleteService.initTrie();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/products/suggestions?q=wire should return in-stock matching products")
    void testGetSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/products/suggestions").param("q", "wire").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", containsString("Wireless")))
                .andExpect(jsonPath("$.data[1].name", containsString("Wireless")));
    }

    @Test
    @DisplayName("GET /api/v1/products/suggestions with empty query should return empty list")
    void testGetSuggestionsEmptyQuery() throws Exception {
        mockMvc.perform(get("/api/v1/products/suggestions").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
