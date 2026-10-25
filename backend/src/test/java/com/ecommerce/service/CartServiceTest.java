package com.ecommerce.service;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private UserPrincipal user1;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        user1 = new UserPrincipal("user-1", "user1@example.com", UserRole.CUSTOMER);

        product1 = new Product(
                "prod-1",
                "Wireless Mouse",
                "Optical wireless mouse",
                "Accessories",
                new BigDecimal("25.00"),
                10,
                "https://example.com/mouse.png",
                "seller-1",
                Instant.now(),
                Instant.now()
        );

        product2 = new Product(
                "prod-2",
                "Mechanical Keyboard",
                "RGB mechanical keyboard",
                "Accessories",
                new BigDecimal("75.00"),
                5,
                "https://example.com/kb.png",
                "seller-1",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("addItemToCart creates new item when not already in cart")
    void shouldAddNewItemToCart() {
        CartItemRequest request = new CartItemRequest("prod-1", 2);

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.empty());

        CartItem savedItem = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(savedItem));

        CartResponse response = cartService.addItemToCart(request, user1);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo("prod-1");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.totalItems()).isEqualTo(2);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItemToCart accumulates quantity when product already in cart")
    void shouldAccumulateQuantityWhenItemAlreadyInCart() {
        CartItemRequest request = new CartItemRequest("prod-1", 3);
        CartItem existingItem = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingItem);
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(existingItem));

        CartResponse response = cartService.addItemToCart(request, user1);

        assertThat(existingItem.getQuantity()).isEqualTo(5);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("addItemToCart throws BadRequestException when accumulated quantity exceeds stock")
    void shouldRejectWhenAccumulatedQuantityExceedsStock() {
        CartItemRequest request = new CartItemRequest("prod-1", 9); // existing 2 + 9 = 11 > stock 10
        CartItem existingItem = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> cartService.addItemToCart(request, user1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItemToCart throws ResourceNotFoundException when product does not exist")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        CartItemRequest request = new CartItemRequest("nonexistent", 1);
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(request, user1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 'nonexistent' not found.");
    }

    @Test
    @DisplayName("addItemToCart throws BadRequestException when quantity <= 0")
    void shouldRejectInvalidQuantity() {
        CartItemRequest request = new CartItemRequest("prod-1", 0);

        assertThatThrownBy(() -> cartService.addItemToCart(request, user1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("updateItemQuantity updates quantity successfully when within stock")
    void shouldUpdateQuantitySuccessfully() {
        UpdateCartItemRequest request = new UpdateCartItemRequest(4);
        CartItem item = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());

        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.of(item));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(item);
        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(item));

        CartResponse response = cartService.updateItemQuantity("prod-1", request, user1);

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(response.items().get(0).quantity()).isEqualTo(4);
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("updateItemQuantity throws BadRequestException when requested quantity exceeds stock")
    void shouldRejectUpdateWhenQuantityExceedsStock() {
        UpdateCartItemRequest request = new UpdateCartItemRequest(15); // stock is 10
        CartItem item = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());

        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.of(item));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));

        assertThatThrownBy(() -> cartService.updateItemQuantity("prod-1", request, user1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("removeItemFromCart deletes item from repository")
    void shouldRemoveItemFromCart() {
        CartItem item = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now());
        when(cartItemRepository.findByUserIdAndProductId("user-1", "prod-1")).thenReturn(Optional.of(item));

        cartService.removeItemFromCart("prod-1", user1);

        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("clearCart deletes all items for user")
    void shouldClearCart() {
        cartService.clearCart(user1);
        verify(cartItemRepository).deleteByUserId("user-1");
    }

    @Test
    @DisplayName("getCart calculates subtotals and total price correctly across multiple items")
    void shouldCalculateSubtotalsAndTotalPriceCorrectly() {
        CartItem item1 = new CartItem("item-1", "user-1", "prod-1", 2, Instant.now()); // 2 * $25.00 = $50.00
        CartItem item2 = new CartItem("item-2", "user-1", "prod-2", 3, Instant.now()); // 3 * $75.00 = $225.00

        when(cartItemRepository.findByUserId("user-1")).thenReturn(List.of(item1, item2));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product1));
        when(productRepository.findById("prod-2")).thenReturn(Optional.of(product2));

        CartResponse response = cartService.getCart(user1);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("275.00"));
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo(new BigDecimal("225.00"));
    }

    @Test
    @DisplayName("Operations throw AccessDeniedException when principal is null")
    void shouldRejectUnauthenticatedAccess() {
        assertThatThrownBy(() -> cartService.getCart(null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }
}
