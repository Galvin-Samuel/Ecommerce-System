package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Encapsulates field-level validation error details.
 *
 * @param field         Name of the invalid property/field
 * @param rejectedValue Value that failed validation (if safe to display)
 * @param message       Human-readable error description
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {
}

