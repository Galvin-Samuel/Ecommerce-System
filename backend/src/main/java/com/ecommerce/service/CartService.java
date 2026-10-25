package com.ecommerce.service;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartItemResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user shopping cart lifecycle, persistence,
 * item accumulation, quantity updates, subtotals, and user isolation.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * Retrieves the current authenticated user's shopping cart.
     *
     * @param principal authenticated user principal
     * @return CartResponse containing all items, subtotals, and total price
     */
    public CartResponse getCart(UserPrincipal principal) {
        validateAuthenticated(principal);
        return buildCartResponse(principal.id());
    }

    /**
     * Adds an item to the authenticated user's cart.
     * If the item is already present, its quantity is accumulated.
     *
     * @param request   payload with productId and quantity
     * @param principal authenticated user principal
     * @return updated CartResponse
     */
    public CartResponse addItemToCart(CartItemRequest request, UserPrincipal principal) {
        validateAuthenticated(principal);

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be positive and greater than zero.");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + request.productId() + "' not found."));

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (availableStock <= 0) {
            throw new BadRequestException("Product '" + product.getName() + "' is out of stock.");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndProductId(principal.id(), request.productId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.quantity();
            if (newQuantity > availableStock) {
                log.warn("Cart add rejected: user '{}' requested total quantity {} but available stock is {}",
                        principal.id(), newQuantity, availableStock);
                throw new BadRequestException(String.format(
                        "Cannot add %d items. You already have %d in cart, which exceeds available stock (%d).",
                        request.quantity(), existingItem.getQuantity(), availableStock
                ));
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.info("Updated quantity to {} for product '{}' in user '{}' cart",
                    newQuantity, product.getId(), principal.id());
        } else {
            if (request.quantity() > availableStock) {
                log.warn("Cart add rejected: user '{}' requested quantity {} but available stock is {}",
                        principal.id(), request.quantity(), availableStock);
                throw new BadRequestException(String.format(
                        "Requested quantity (%d) exceeds available stock (%d).",
                        request.quantity(), availableStock
                ));
            }
            CartItem newItem = new CartItem(principal.id(), request.productId(), request.quantity());
            cartItemRepository.save(newItem);
            log.info("Added product '{}' (qty: {}) to user '{}' cart",
                    product.getId(), request.quantity(), principal.id());
        }

        return buildCartResponse(principal.id());
    }

    /**
     * Updates the exact quantity of an existing cart item.
     *
     * @param productId product ID
     * @param request   payload with updated quantity
     * @param principal authenticated user principal
     * @return updated CartResponse
     */
    public CartResponse updateItemQuantity(String productId, UpdateCartItemRequest request, UserPrincipal principal) {
        validateAuthenticated(principal);

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be positive and greater than zero.");
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(principal.id(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item with product ID '" + productId + "' not found in cart."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found."));

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (request.quantity() > availableStock) {
            log.warn("Cart quantity update rejected: user '{}' requested {} but stock is {}",
                    principal.id(), request.quantity(), availableStock);
            throw new BadRequestException(String.format(
                    "Requested quantity (%d) exceeds available stock (%d).",
                    request.quantity(), availableStock
            ));
        }

        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);
        log.info("Set quantity to {} for product '{}' in user '{}' cart",
                request.quantity(), productId, principal.id());

        return buildCartResponse(principal.id());
    }

    /**
     * Removes a single product item from the authenticated user's cart.
     *
     * @param productId product ID to remove
     * @param principal authenticated user principal
     */
    public void removeItemFromCart(String productId, UserPrincipal principal) {
        validateAuthenticated(principal);

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(principal.id(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item with product ID '" + productId + "' not found in cart."));

        cartItemRepository.delete(cartItem);
        log.info("Removed product '{}' from user '{}' cart", productId, principal.id());
    }

    /**
     * Clears all items from the authenticated user's shopping cart.
     *
     * @param principal authenticated user principal
     */
    public void clearCart(UserPrincipal principal) {
        validateAuthenticated(principal);
        cartItemRepository.deleteByUserId(principal.id());
        log.info("Cleared shopping cart for user '{}'", principal.id());
    }

    private CartResponse buildCartResponse(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return CartResponse.empty();
        }

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cartItems) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                totalPrice = totalPrice.add(subtotal);
                totalItems += item.getQuantity();

                items.add(new CartItemResponse(
                        item.getId(),
                        product.getId(),
                        product.getName(),
                        product.getImageUrl(),
                        unitPrice,
                        item.getQuantity(),
                        subtotal,
                        item.getAddedAt()
                ));
            } else {
                // If product was deleted from catalog, remove stale cart entry
                log.warn("Cleaning up orphaned cart item '{}' for deleted product '{}'", item.getId(), item.getProductId());
                cartItemRepository.delete(item);
            }
        }

        return new CartResponse(items, totalPrice, totalItems);
    }

    private void validateAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new AccessDeniedException("Authentication required to access the shopping cart.");
        }
    }
}
