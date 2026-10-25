package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OrderService orderService;

    private UserPrincipal customerPrincipal;
    private UserPrincipal otherCustomerPrincipal;
    private UserPrincipal adminPrincipal;
    private Product sampleProduct;
    private CartItem sampleCartItem;

    @BeforeEach
    void setUp() {
        customerPrincipal = new UserPrincipal("user-1", "user1@example.com", UserRole.CUSTOMER);
        otherCustomerPrincipal = new UserPrincipal("user-2", "user2@example.com", UserRole.CUSTOMER);
        adminPrincipal = new UserPrincipal("admin-1", "admin@example.com", UserRole.ADMIN);

        sampleProduct = new Product(
                "prod-1",
                "Noise-Cancelling Headphones",
                "Wireless over-ear headphones",
                "Electronics",
                new BigDecimal("150.00"),
                10,
                "https://example.com/headphones.jpg",
                "seller-1",
                Instant.now(),
                Instant.now()
        );

        sampleCartItem = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());
    }

    @Test
    @DisplayName("createOrder succeeds when cart has valid items and sufficient stock")
    void shouldCreateOrderSuccessfully() {
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(sampleCartItem));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(updateResult);

        Order savedOrder = new Order(
                "order-100",
                "user-1",
                List.of(new OrderItem("prod-1", "Noise-Cancelling Headphones", 2, new BigDecimal("150.00"), new BigDecimal("300.00"))),
                new BigDecimal("300.00"),
                OrderStatus.CONFIRMED,
                Instant.now(),
                Instant.now()
        );
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(new OrderRequest(), customerPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("order-100");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);

        // Verify stock was updated and cart was cleared
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
        verify(orderRepository).save(any(Order.class));
        verify(cartItemRepository).deleteByUserId("user-1");
    }

    @Test
    @DisplayName("createOrder throws BadRequestException when shopping cart is empty")
    void shouldRejectOrderCreationForEmptyCart() {
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), customerPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create an order with an empty shopping cart.");

        verify(orderRepository, never()).save(any());
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
    }

    @Test
    @DisplayName("createOrder throws BadRequestException when product stock is insufficient")
    void shouldRejectOrderCreationWhenStockInsufficient() {
        CartItem highQtyItem = new CartItem("item-1", "user-1", "prod-1", 20, Instant.now()); // stock is 10
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(highQtyItem));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), customerPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock for product");

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("createOrder throws ResourceNotFoundException when product in cart is deleted")
    void shouldRejectWhenProductInCartNotFound() {
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(sampleCartItem));
        when(productRepository.findById("prod-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), customerPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 'prod-1' not found.");
    }

    @Test
    @DisplayName("createOrder rolls back previous stock decrements if subsequent decrement fails")
    void shouldCompensateStockWhenSubsequentDecrementFails() {
        CartItem item1 = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());
        CartItem item2 = new CartItem("item-2", "user-1", "prod-2", 3, Instant.now());

        Product product2 = new Product("prod-2", "Product 2", "Cat", new BigDecimal("50.00"), 5, null, "seller-1");

        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(item1, item2));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));
        when(productRepository.findById("prod-2")).thenReturn(Optional.of(product2));

        UpdateResult successResult = mock(UpdateResult.class);
        when(successResult.getModifiedCount()).thenReturn(1L);

        UpdateResult failureResult = mock(UpdateResult.class);
        when(failureResult.getModifiedCount()).thenReturn(0L);

        // First decrement succeeds, second fails
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(successResult)
                .thenReturn(failureResult);

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(), customerPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Order placement failed due to concurrent stock change");

        // Verify compensation rollback was executed for item 1 (2 decrements + 1 rollback = 3 calls)
        verify(mongoTemplate, org.mockito.Mockito.times(3)).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("getOrders returns orders for authenticated user")
    void shouldGetOrdersForUser() {
        Order order = new Order("order-1", "user-1", List.of(), new BigDecimal("100.00"), OrderStatus.CONFIRMED, Instant.now(), Instant.now());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getOrders(customerPrincipal);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).id()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("getOrderById succeeds when caller is owner")
    void shouldGetOrderByIdWhenOwner() {
        Order order = new Order("order-1", "user-1", List.of(), new BigDecimal("100.00"), OrderStatus.CONFIRMED, Instant.now(), Instant.now());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("order-1", customerPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("getOrderById succeeds when caller is ADMIN even if not owner")
    void shouldGetOrderByIdWhenAdmin() {
        Order order = new Order("order-1", "user-1", List.of(), new BigDecimal("100.00"), OrderStatus.CONFIRMED, Instant.now(), Instant.now());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("order-1", adminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("getOrderById throws AccessDeniedException when caller is other user")
    void shouldRejectGetOrderByIdWhenNotOwner() {
        Order order = new Order("order-1", "user-1", List.of(), new BigDecimal("100.00"), OrderStatus.CONFIRMED, Instant.now(), Instant.now());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("order-1", otherCustomerPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to access this order.");
    }

    @Test
    @DisplayName("getOrderById throws ResourceNotFoundException when order does not exist")
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("nonexistent", customerPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID 'nonexistent' not found.");
    }
}
