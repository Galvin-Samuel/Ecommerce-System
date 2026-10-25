package com.ecommerce.repository;

import com.ecommerce.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for Product catalog persistence operations.
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * Retrieves all products belonging to a specified category.
     *
     * @param category product category name
     * @return List of matching products
     */
    List<Product> findByCategory(String category);

    /**
     * Retrieves products belonging to a specified category with pagination.
     *
     * @param category product category name
     * @param pageable pagination details
     * @return Page of matching products
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Retrieves all products listed by a specified seller.
     *
     * @param sellerId seller's user ID
     * @return List of matching products
     */
    List<Product> findBySellerId(String sellerId);

    /**
     * Retrieves products listed by a specified seller with pagination.
     *
     * @param sellerId seller's user ID
     * @param pageable pagination details
     * @return Page of matching products
     */
    Page<Product> findBySellerId(String sellerId, Pageable pageable);
}

