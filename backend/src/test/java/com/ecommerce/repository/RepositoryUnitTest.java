package com.ecommerce.repository;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("UserRepository should query by email and check existence")
    void testUserRepository() {
        User user = new User("u1", "John", "john@example.com", "hash", UserRole.CUSTOMER, Instant.now(), Instant.now());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);

        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());

        assertTrue(userRepository.existsByEmail("john@example.com"));
        assertFalse(userRepository.existsByEmail("missing@example.com"));
    }

    @Test
    @DisplayName("ProductRepository should query by category and sellerId")
    void testProductRepository() {
        Product p = new Product("p1", "Laptop", "Gaming Laptop", "Electronics",
                new BigDecimal("1200.00"), 10, "url", "seller1", Instant.now(), Instant.now());
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(p));
        when(productRepository.findBySellerId("seller1")).thenReturn(List.of(p));

        List<Product> electronics = productRepository.findByCategory("Electronics");
        assertEquals(1, electronics.size());
        assertEquals("Laptop", electronics.get(0).getName());

        List<Product> sellerProducts = productRepository.findBySellerId("seller1");
        assertEquals(1, sellerProducts.size());
    }

    @Test
    @DisplayName("CartItemRepository should query by userId, find item, and delete by userId")
    void testCartItemRepository() {
        CartItem item = new CartItem("c1", "user100", "prod200", 2, Instant.now());
        when(cartItemRepository.findByUserId("user100")).thenReturn(List.of(item));
        when(cartItemRepository.findByUserIdAndProductId("user100", "prod200")).thenReturn(Optional.of(item));

        List<CartItem> userCart = cartItemRepository.findByUserId("user100");
        assertEquals(1, userCart.size());

        Optional<CartItem> foundItem = cartItemRepository.findByUserIdAndProductId("user100", "prod200");
        assertTrue(foundItem.isPresent());

        cartItemRepository.deleteByUserId("user100");
        verify(cartItemRepository).deleteByUserId("user100");
    }

    @Test
    @DisplayName("OrderRepository should query orders by userId and ordered by date")
    void testOrderRepository() {
        OrderItem orderItem = new OrderItem("p1", "Book", 1, new BigDecimal("15.00"), new BigDecimal("15.00"));
        Order order = new Order("o1", "user100", List.of(orderItem), new BigDecimal("15.00"),
                OrderStatus.PROCESSING, Instant.now(), Instant.now());

        when(orderRepository.findByUserId("user100")).thenReturn(List.of(order));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user100")).thenReturn(List.of(order));

        List<Order> orders = orderRepository.findByUserId("user100");
        assertEquals(1, orders.size());
        assertEquals(OrderStatus.PROCESSING, orders.get(0).getStatus());

        List<Order> sortedOrders = orderRepository.findByUserIdOrderByCreatedAtDesc("user100");
        assertEquals(1, sortedOrders.size());
    }
}

