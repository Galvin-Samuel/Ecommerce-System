package com.ecommerce.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response structure conforming to project API specifications.
 *
 * @param <T> Item payload type
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * Constructs a PagedResponse envelope from a Spring Data Page object.
     *
     * @param page Spring Data Page instance
     * @param <T>  Item payload type
     * @return populated PagedResponse record
     */
    public static <T> PagedResponse<T> fromPage(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
