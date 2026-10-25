package com.ecommerce.controller;

import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.OrderService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private OrderService orderService;

    private String customerToken;
    private OrderResponse sampleOrder;

    @BeforeEach
    void setUp() {
        User customer = new User("cust-20", "Customer Twenty", "cust20@example.com", "hash", UserRole.CUSTOMER, Instant.now(), Instant.now());
        customerToken = jwtTokenProvider.generateToken(customer);

        OrderItemResponse item = new OrderItemResponse(
                "prod-1",
                "Ergonomic Chair",
                1,
                new BigDecimal("299.99"),
                new BigDecimal("299.99")
        );

        sampleOrder = new OrderResponse(
                "order-200",
                "cust-20",
                List.of(item),
                new BigDecimal("299.99"),
                OrderStatus.CONFIRMED,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/orders - Authenticated user creates order (201 Created)")
    void shouldCreateOrderWhenAuthenticated() throws Exception {
        when(orderService.createOrder(any(), any(UserPrincipal.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("order-200")))
                .andExpect(jsonPath("$.data.totalAmount", is(299.99)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Returns 401 Unauthorized when unauthenticated")
    void shouldRejectUnauthenticatedOrderCreation() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("GET /api/v1/orders - Returns user's order list (200 OK)")
    void shouldGetOrdersForUser() throws Exception {
        when(orderService.getOrders(any(UserPrincipal.class))).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is("order-200")));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Returns order details when caller is owner (200 OK)")
    void shouldGetOrderByIdWhenOwner() throws Exception {
        when(orderService.getOrderById(eq("order-200"), any(UserPrincipal.class))).thenReturn(sampleOrder);

        mockMvc.perform(get("/api/v1/orders/order-200")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("order-200")))
                .andExpect(jsonPath("$.data.totalAmount", is(299.99)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Returns 403 Forbidden when caller is not owner")
    void shouldRejectGetOrderWhenNotOwner() throws Exception {
        when(orderService.getOrderById(eq("order-200"), any(UserPrincipal.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to access this order."));

        mockMvc.perform(get("/api/v1/orders/order-200")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Returns 404 Not Found when order does not exist")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.getOrderById(eq("nonexistent"), any(UserPrincipal.class)))
                .thenThrow(new ResourceNotFoundException("Order with ID 'nonexistent' not found."));

        mockMvc.perform(get("/api/v1/orders/nonexistent")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
