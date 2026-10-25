package com.ecommerce.controller;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartItemResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CartService cartService;

    private String customerToken;
    private CartResponse sampleCart;

    @BeforeEach
    void setUp() {
        User customer = new User("cust-10", "Customer Ten", "cust10@example.com", "hash", UserRole.CUSTOMER, Instant.now(), Instant.now());
        customerToken = jwtTokenProvider.generateToken(customer);

        CartItemResponse item = new CartItemResponse(
                "cart-item-1",
                "prod-1",
                "Wireless Mouse",
                "https://example.com/mouse.png",
                new BigDecimal("29.99"),
                2,
                new BigDecimal("59.98"),
                Instant.now()
        );

        sampleCart = new CartResponse(List.of(item), new BigDecimal("59.98"), 2);
    }

    @Test
    @DisplayName("GET /api/v1/cart - Returns 200 OK with cart details for authenticated user")
    void shouldGetCartForAuthenticatedUser() throws Exception {
        when(cartService.getCart(any(UserPrincipal.class))).thenReturn(sampleCart);

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalItems", is(2)))
                .andExpect(jsonPath("$.data.totalPrice", is(59.98)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName", is("Wireless Mouse")));
    }

    @Test
    @DisplayName("GET /api/v1/cart - Returns 401 Unauthorized when unauthenticated")
    void shouldRejectUnauthenticatedGetCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Adds item and returns 201 Created")
    void shouldAddItemToCart() throws Exception {
        CartItemRequest request = new CartItemRequest("prod-1", 2);

        when(cartService.addItemToCart(any(CartItemRequest.class), any(UserPrincipal.class)))
                .thenReturn(sampleCart);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalItems", is(2)))
                .andExpect(jsonPath("$.data.totalPrice", is(59.98)));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Returns 400 Bad Request on invalid quantity")
    void shouldRejectInvalidQuantityOnAdd() throws Exception {
        CartItemRequest request = new CartItemRequest("prod-1", 0);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)));
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{productId} - Updates quantity and returns 200 OK")
    void shouldUpdateItemQuantity() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        when(cartService.updateItemQuantity(eq("prod-1"), any(UpdateCartItemRequest.class), any(UserPrincipal.class)))
                .thenReturn(sampleCart);

        mockMvc.perform(put("/api/v1/cart/items/prod-1")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalItems", is(2)));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{productId} - Removes item and returns 204 No Content")
    void shouldRemoveItemFromCart() throws Exception {
        doNothing().when(cartService).removeItemFromCart(eq("prod-1"), any(UserPrincipal.class));

        mockMvc.perform(delete("/api/v1/cart/items/prod-1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/cart - Clears cart and returns 204 No Content")
    void shouldClearCart() throws Exception {
        doNothing().when(cartService).clearCart(any(UserPrincipal.class));

        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }
}
