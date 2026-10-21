package com.ecommerce.repository;

import com.ecommerce.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for customer Order persistence operations.
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId user ID
     * @return List of matching orders
     */
    List<Order> findByUserId(String userId);

    /**
     * Retrieves all orders for a user ordered by creation timestamp descending.
     *
     * @param userId user ID
     * @return List of user orders in reverse chronological order
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}

