package com.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an incoming request contains invalid or inconsistent data.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

