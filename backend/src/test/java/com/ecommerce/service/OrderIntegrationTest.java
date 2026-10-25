package com.ecommerce.service;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private UserPrincipal userA;
    private UserPrincipal userB;
    private final List<String> seededProductIds = new ArrayList<>();
    private final List<String> createdOrderIds = new ArrayList<>();

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        userA = new UserPrincipal("order-user-a", "userA@example.com", UserRole.CUSTOMER);
        userB = new UserPrincipal("order-user-b", "userB@example.com", UserRole.CUSTOMER);

        product1 = new Product(
                "Monitor Stand Pro", "Aluminum dual monitor riser", "Office",
                new BigDecimal("60.00"), 15, null, "seller-ord"
        );
        product2 = new Product(
                "Cable Management Box", "Fire retardant wire organizer", "Office",
                new BigDecimal("25.00"), 20, null, "seller-ord"
        );

        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);

        seededProductIds.add(product1.getId());
        seededProductIds.add(product2.getId());
    }

    @AfterEach
    void tearDown() {
        cartItemRepository.deleteByUserId("order-user-a");
        cartItemRepository.deleteByUserId("order-user-b");

        if (!createdOrderIds.isEmpty()) {
            orderRepository.deleteAllById(createdOrderIds);
            createdOrderIds.clear();
        }

        if (!seededProductIds.isEmpty()) {
            productRepository.deleteAllById(seededProductIds);
            seededProductIds.clear();
        }
    }

    @Test
    @DisplayName("Gate 1, 2, 8, 9, 12: Successful order creation, details, calculated totals, stock updates, cart cleared, MongoDB persistence")
    void shouldCreateOrderAndUpdateStockAndClearCart() {
        // Add 2 of product1 ($60 each = $120) and 3 of product2 ($25 each = $75)
        cartService.addItemToCart(new CartItemRequest(product1.getId(), 2), userA);
        cartService.addItemToCart(new CartItemRequest(product2.getId(), 3), userA);

        OrderResponse orderResponse = orderService.createOrder(new OrderRequest("Deliver to front desk"), userA);
        createdOrderIds.add(orderResponse.id());

        // 1 & 2: Verify order details and calculated totals
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.userId()).isEqualTo("order-user-a");
        assertThat(orderResponse.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderResponse.totalAmount()).isEqualByComparingTo(new BigDecimal("195.00"));
        assertThat(orderResponse.items()).hasSize(2);

        // 8: Verify stock updates in MongoDB
        Product updatedP1 = productRepository.findById(product1.getId()).orElseThrow();
        Product updatedP2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(updatedP1.getStockQuantity()).isEqualTo(13); // 15 - 2 = 13
        assertThat(updatedP2.getStockQuantity()).isEqualTo(17); // 20 - 3 = 17

        // 9: Verify cart is cleared
        List<CartItem> remainingCart = cartItemRepository.findByUserId("order-user-a");
        assertThat(remainingCart).isEmpty();

        // 12: Verify order persistence in MongoDB
        Optional<Order> persistedOrder = orderRepository.findById(orderResponse.id());
        assertThat(persistedOrder).isPresent();
        assertThat(persistedOrder.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("195.00"));
        assertThat(persistedOrder.get().getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("Gate 3: Order retrieval by ID returns accurate snapshot")
    void shouldRetrieveOrderById() {
        cartService.addItemToCart(new CartItemRequest(product1.getId(), 1), userA);
        OrderResponse created = orderService.createOrder(new OrderRequest(), userA);
        createdOrderIds.add(created.id());

        OrderResponse retrieved = orderService.getOrderById(created.id(), userA);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.totalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("Gate 4: Listing only the authenticated user's orders")
    void shouldListOnlyAuthenticatedUserOrders() {
        // User A creates order
        cartService.addItemToCart(new CartItemRequest(product1.getId(), 1), userA);
        OrderResponse orderA = orderService.createOrder(new OrderRequest(), userA);
        createdOrderIds.add(orderA.id());

        // User B creates order
        cartService.addItemToCart(new CartItemRequest(product2.getId(), 1), userB);
        OrderResponse orderB = orderService.createOrder(new OrderRequest(), userB);
        createdOrderIds.add(orderB.id());

        List<OrderResponse> userAOrders = orderService.getOrders(userA);
        assertThat(userAOrders).hasSize(1);
        assertThat(userAOrders.get(0).id()).isEqualTo(orderA.id());

        List<OrderResponse> userBOrders = orderService.getOrders(userB);
        assertThat(userBOrders).hasSize(1);
        assertThat(userBOrders.get(0).id()).isEqualTo(orderB.id());
    }

    @Test
    @DisplayName("Gate 5: Rejection of an empty cart")
    void shouldRejectOrderCreationForEmptyCart() {
        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), userA))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty shopping cart");
    }

    @Test
    @DisplayName("Gate 6 & 10: Insufficient stock rejects order and leaves cart and stock unchanged")
    void shouldRejectWhenStockInsufficientAndKeepDataConsistent() {
        // Product1 has stock 15. Attempt to order 20.
        // First set cart directly with 20 items to simulate race or stock reduction
        CartItem item = new CartItem("order-user-a", product1.getId(), 20);
        cartItemRepository.save(item);

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), userA))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        // Stock should remain unmodified (15)
        Product p1After = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(p1After.getStockQuantity()).isEqualTo(15);

        // Cart should still contain the items (not cleared)
        List<CartItem> cartAfter = cartItemRepository.findByUserId("order-user-a");
        assertThat(cartAfter).hasSize(1);

        // No orders created
        List<Order> orders = orderRepository.findByUserId("order-user-a");
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("Gate 7: User B cannot access User A's order details")
    void shouldRejectUnauthorizedOrderAccess() {
        cartService.addItemToCart(new CartItemRequest(product1.getId(), 1), userA);
        OrderResponse orderA = orderService.createOrder(new OrderRequest(), userA);
        createdOrderIds.add(orderA.id());

        assertThatThrownBy(() -> orderService.getOrderById(orderA.id(), userB))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission");
    }
}
