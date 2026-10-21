package com.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation conflicts with the current state of a resource (e.g., duplicate unique key).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

