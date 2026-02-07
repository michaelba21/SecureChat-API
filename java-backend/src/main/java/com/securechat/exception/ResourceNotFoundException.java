package com.securechat.exception;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 * This is a generic "not found" exception that can be used for various resources:
 * - User,Chat room,File and Message are not found.This Corresponds to HTTP 404 Not Found status in the API.
 */
public class ResourceNotFoundException extends RuntimeException {
    // Constructor with only message (most common use case)
    public ResourceNotFoundException(String message) {
        super(message); 
    }

    // Constructor with message and cause (useful for debugging and chaining exceptions)
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}