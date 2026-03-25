package com.securechat.exception;

/**
 * Exception thrown when business logic validation fails.
 * and involves complex business rules or multi-field validation.
 * Corresponds to HTTP 400 Bad Request status in the API.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message); 
    }
}