package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the authenticated user's shopping cart.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Retrieves the current authenticated user's shopping cart.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse cart = cartService.getCart(principal);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    /**
     * Adds an item to the authenticated user's cart (or accumulates quantity if already present).
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse cart = cartService.addItemToCart(request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart successfully.", cart));
    }

    /**
     * Updates the quantity of a specific product in the authenticated user's cart.
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse cart = cartService.updateItemQuantity(productId, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated successfully.", cart));
    }

    /**
     * Removes a specific product item from the authenticated user's cart.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable String productId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.removeItemFromCart(productId, principal);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears all items from the authenticated user's shopping cart.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.clearCart(principal);
        return ResponseEntity.noContent().build();
    }
}
