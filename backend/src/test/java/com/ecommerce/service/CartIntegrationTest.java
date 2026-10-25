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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CartIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private UserPrincipal userA;
    private UserPrincipal userB;
    private final List<String> seededProductIds = new ArrayList<>();

    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        userA = new UserPrincipal("cart-user-a", "userA@example.com", UserRole.CUSTOMER);
        userB = new UserPrincipal("cart-user-b", "userB@example.com", UserRole.CUSTOMER);

        productA = new Product(
                "Desk Organizer", "Bamboo desk organizer tray", "Office",
                new BigDecimal("20.00"), 50, null, "seller-cart"
        );
        productB = new Product(
                "Wireless Charger Pad", "Fast qi wireless charger", "Electronics",
                new BigDecimal("35.00"), 20, null, "seller-cart"
        );

        productA = productRepository.save(productA);
        productB = productRepository.save(productB);

        seededProductIds.add(productA.getId());
        seededProductIds.add(productB.getId());
    }

    @AfterEach
    void tearDown() {
        cartItemRepository.deleteByUserId("cart-user-a");
        cartItemRepository.deleteByUserId("cart-user-b");

        if (!seededProductIds.isEmpty()) {
            productRepository.deleteAllById(seededProductIds);
            seededProductIds.clear();
        }
    }

    @Test
    @DisplayName("Gate 1 & 8 & 11: Add one product and verify persistence in MongoDB")
    void shouldAddOneProductAndPersistInMongoDB() {
        CartResponse response = cartService.addItemToCart(
                new CartItemRequest(productA.getId(), 2), userA
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(productA.getId());
        assertThat(response.items().get(0).quantity()).isEqualTo(2);

        // Direct database verification
        List<CartItem> persisted = cartItemRepository.findByUserId("cart-user-a");
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getProductId()).isEqualTo(productA.getId());
        assertThat(persisted.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Gate 2: Add same product again and verify quantity accumulation")
    void shouldAccumulateQuantityWhenAddingSameProduct() {
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 3), userA);
        CartResponse response = cartService.addItemToCart(new CartItemRequest(productA.getId(), 4), userA);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(7);
        assertThat(response.totalItems()).isEqualTo(7);

        // Verify in MongoDB
        List<CartItem> persisted = cartItemRepository.findByUserId("cart-user-a");
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Gate 3 & 12: Add multiple products and verify subtotal and total calculations")
    void shouldAddMultipleProductsAndCalculateTotalsAccurately() {
        // 2 * $20.00 = $40.00
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 2), userA);
        // 3 * $35.00 = $105.00
        CartResponse response = cartService.addItemToCart(new CartItemRequest(productB.getId(), 3), userA);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalItems()).isEqualTo(5);
        // Total = $40.00 + $105.00 = $145.00
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("145.00"));
    }

    @Test
    @DisplayName("Gate 4: Update product quantity in cart")
    void shouldUpdateQuantityInCart() {
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 2), userA);

        CartResponse updated = cartService.updateItemQuantity(
                productA.getId(), new UpdateCartItemRequest(8), userA
        );

        assertThat(updated.items().get(0).quantity()).isEqualTo(8);
        assertThat(updated.totalPrice()).isEqualByComparingTo(new BigDecimal("160.00"));
    }

    @Test
    @DisplayName("Gate 5: Remove one product from cart")
    void shouldRemoveProductFromCart() {
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 2), userA);
        cartService.addItemToCart(new CartItemRequest(productB.getId(), 1), userA);

        cartService.removeItemFromCart(productA.getId(), userA);

        CartResponse remaining = cartService.getCart(userA);
        assertThat(remaining.items()).hasSize(1);
        assertThat(remaining.items().get(0).productId()).isEqualTo(productB.getId());
    }

    @Test
    @DisplayName("Gate 6: Clear the entire cart")
    void shouldClearCart() {
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 2), userA);
        cartService.addItemToCart(new CartItemRequest(productB.getId(), 1), userA);

        cartService.clearCart(userA);

        CartResponse emptyCart = cartService.getCart(userA);
        assertThat(emptyCart.items()).isEmpty();
        assertThat(emptyCart.totalItems()).isEqualTo(0);
        assertThat(emptyCart.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Gate 7: Reject invalid quantities (zero, negative, exceeding stock)")
    void shouldRejectInvalidQuantities() {
        assertThatThrownBy(() -> cartService.addItemToCart(new CartItemRequest(productA.getId(), 0), userA))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> cartService.addItemToCart(new CartItemRequest(productA.getId(), -5), userA))
                .isInstanceOf(BadRequestException.class);

        // Exceeding stock (productB has stock 20)
        assertThatThrownBy(() -> cartService.addItemToCart(new CartItemRequest(productB.getId(), 50), userA))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("Gate 8: Reject nonexistent products")
    void shouldRejectNonexistentProducts() {
        assertThatThrownBy(() -> cartService.addItemToCart(new CartItemRequest("nonexistent123", 1), userA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 'nonexistent123' not found.");
    }

    @Test
    @DisplayName("Gate 9: Reject unauthenticated cart access")
    void shouldRejectUnauthenticatedCartAccess() {
        assertThatThrownBy(() -> cartService.getCart(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Gate 10: Verify strict user cart isolation (User A cannot see or modify User B's cart)")
    void shouldEnforceUserCartIsolation() {
        // User A adds product A
        cartService.addItemToCart(new CartItemRequest(productA.getId(), 2), userA);
        // User B adds product B
        cartService.addItemToCart(new CartItemRequest(productB.getId(), 3), userB);

        CartResponse cartA = cartService.getCart(userA);
        CartResponse cartB = cartService.getCart(userB);

        // User A only sees product A
        assertThat(cartA.items()).hasSize(1);
        assertThat(cartA.items().get(0).productId()).isEqualTo(productA.getId());

        // User B only sees product B
        assertThat(cartB.items()).hasSize(1);
        assertThat(cartB.items().get(0).productId()).isEqualTo(productB.getId());

        // User A attempting to update product B in cart throws 404 (not found in A's cart)
        assertThatThrownBy(() -> cartService.updateItemQuantity(productB.getId(), new UpdateCartItemRequest(5), userA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in cart");

        // User A attempting to delete product B throws 404
        assertThatThrownBy(() -> cartService.removeItemFromCart(productB.getId(), userA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in cart");
    }
}
