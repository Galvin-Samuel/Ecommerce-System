package com.ecommerce.shorturl;

import com.ecommerce.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to resolve a short URL that has expired.
 */
public class ShortUrlExpiredException extends ApiException {

    public ShortUrlExpiredException(String message) {
        super(HttpStatus.GONE, message);
    }
}
