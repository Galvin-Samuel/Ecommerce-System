package com.ecommerce.repository;

import com.ecommerce.model.CartItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for CartItem persistence operations.
 */
@Repository
public interface CartItemRepository extends MongoRepository<CartItem, String> {

    /**
     * Retrieves all shopping cart items for a specific user.
     *
     * @param userId user ID
     * @return List of user's cart items
     */
    List<CartItem> findByUserId(String userId);

    /**
     * Finds a specific cart item by user ID and product ID.
     *
     * @param userId user ID
     * @param productId product ID
     * @return Optional containing the CartItem if present
     */
    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);

    /**
     * Deletes all cart items associated with a user (e.g., after checkout).
     *
     * @param userId user ID whose cart should be cleared
     */
    void deleteByUserId(String userId);
}

