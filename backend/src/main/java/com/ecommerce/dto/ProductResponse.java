package com.ecommerce.dto;

import com.ecommerce.model.Product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Safe response payload representing product details.
 */
public record ProductResponse(
        String id,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        String sellerId,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Maps a Product entity to a safe ProductResponse DTO.
     *
     * @param product source Product document
     * @return ProductResponse record
     */
    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getSellerId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
