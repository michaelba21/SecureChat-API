package com.securechat.exception;

/**
 * Exception thrown when attempting to create or save a resource
 * that would violate uniqueness constraints (e.g., duplicate username, email, etc.).
 * This typically corresponds to database unique constraint violations.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message); // Passes the error message to the parent RuntimeException class
    }
}