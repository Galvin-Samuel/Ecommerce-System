package com.ecommerce.controller;

import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ProductService productService;

    private String sellerToken;
    private String customerToken;
    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        User seller = new User("seller-1", "Seller User", "seller@example.com", "hash", UserRole.SELLER, Instant.now(), Instant.now());
        User customer = new User("cust-1", "Customer User", "cust@example.com", "hash", UserRole.CUSTOMER, Instant.now(), Instant.now());

        sellerToken = jwtTokenProvider.generateToken(seller);
        customerToken = jwtTokenProvider.generateToken(customer);

        sampleResponse = new ProductResponse(
                "prod-100",
                "Ultra Gaming Laptop",
                "High performance laptop",
                "Electronics",
                new BigDecimal("1499.99"),
                15,
                "https://example.com/laptop.jpg",
                "seller-1",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/products - Public listing succeeds with pagination")
    void shouldListProductsPublicly() throws Exception {
        PagedResponse<ProductResponse> pagedResponse = new PagedResponse<>(
                List.of(sampleResponse), 0, 20, 1, 1, false, false
        );

        when(productService.getProducts(any(), anyInt(), anyInt(), anyString()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/products?page=0&size=20&sort=price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id", is("prod-100")))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Public retrieval succeeds for existing product")
    void shouldGetProductByIdPublicly() throws Exception {
        when(productService.getProductById("prod-100")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/prod-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("prod-100")))
                .andExpect(jsonPath("$.data.name", is("Ultra Gaming Laptop")))
                .andExpect(jsonPath("$.data.price", is(1499.99)));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Returns 404 Not Found for nonexistent product")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.getProductById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Product with ID 'nonexistent' not found."));

        mockMvc.perform(get("/api/v1/products/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Authenticated SELLER creates product (201 Created)")
    void shouldCreateProductWhenAuthenticatedAsSeller() throws Exception {
        ProductRequest request = new ProductRequest(
                "Ultra Gaming Laptop",
                "High performance laptop",
                "Electronics",
                new BigDecimal("1499.99"),
                15,
                "https://example.com/laptop.jpg"
        );

        when(productService.createProduct(any(ProductRequest.class), any(UserPrincipal.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("prod-100")))
                .andExpect(jsonPath("$.data.name", is("Ultra Gaming Laptop")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Returns 401 Unauthorized when unauthenticated")
    void shouldRejectUnauthenticatedProductCreation() throws Exception {
        ProductRequest request = new ProductRequest(
                "Laptop", "Desc", "Electronics", new BigDecimal("999.00"), 5, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/v1/products - Returns 403 Forbidden when CUSTOMER attempts to create product")
    void shouldRejectCustomerProductCreation() throws Exception {
        ProductRequest request = new ProductRequest(
                "Laptop", "Desc", "Electronics", new BigDecimal("999.00"), 5, null
        );

        when(productService.createProduct(any(ProductRequest.class), any(UserPrincipal.class)))
                .thenThrow(new AccessDeniedException("Only sellers and administrators can create products."));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("POST /api/v1/products - Returns 400 Bad Request on invalid input")
    void shouldRejectInvalidProductData() throws Exception {
        ProductRequest invalid = new ProductRequest(
                "", null, "", new BigDecimal("-10.00"), -5, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(4)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Authenticated owner updates product (200 OK)")
    void shouldUpdateProductWhenOwner() throws Exception {
        ProductRequest updateRequest = new ProductRequest(
                "Updated Laptop", "Updated desc", "Electronics", new BigDecimal("1299.99"), 10, null
        );
        ProductResponse updatedResponse = new ProductResponse(
                "prod-100", "Updated Laptop", "Updated desc", "Electronics",
                new BigDecimal("1299.99"), 10, null, "seller-1", Instant.now(), Instant.now()
        );

        when(productService.updateProduct(eq("prod-100"), any(ProductRequest.class), any(UserPrincipal.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/products/prod-100")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Laptop")))
                .andExpect(jsonPath("$.data.price", is(1299.99)));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Returns 403 Forbidden when caller is not owner")
    void shouldRejectUpdateWhenNotOwner() throws Exception {
        ProductRequest updateRequest = new ProductRequest(
                "Hacked Laptop", "Desc", "Electronics", new BigDecimal("10.00"), 100, null
        );

        when(productService.updateProduct(eq("prod-100"), any(ProductRequest.class), any(UserPrincipal.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to update this product."));

        mockMvc.perform(put("/api/v1/products/prod-100")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Owner deletes product (204 No Content)")
    void shouldDeleteProductWhenOwner() throws Exception {
        doNothing().when(productService).deleteProduct(eq("prod-100"), any(UserPrincipal.class));

        mockMvc.perform(delete("/api/v1/products/prod-100")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Returns 403 Forbidden when caller is not owner")
    void shouldRejectDeleteWhenNotOwner() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to delete this product."))
                .when(productService).deleteProduct(eq("prod-100"), any(UserPrincipal.class));

        mockMvc.perform(delete("/api/v1/products/prod-100")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/v1/products/search - Public search succeeds with 200 OK")
    void shouldSearchProductsPublicly() throws Exception {
        PagedResponse<ProductResponse> pagedResponse = new PagedResponse<>(
                List.of(sampleResponse), 0, 20, 1, 1, false, false
        );

        when(productService.searchProducts(eq("Gaming"), eq("Electronics"), any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/products/search?q=Gaming&category=Electronics&minPrice=1000&maxPrice=2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].name", is("Ultra Gaming Laptop")));
    }

    @Test
    @DisplayName("GET /api/v1/products/search - Returns 400 Bad Request when minPrice > maxPrice")
    void shouldReturn400WhenSearchPriceRangeInvalid() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new BadRequestException("minPrice cannot be greater than maxPrice."));

        mockMvc.perform(get("/api/v1/products/search?minPrice=500&maxPrice=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("minPrice cannot be greater than maxPrice.")));
    }
}
