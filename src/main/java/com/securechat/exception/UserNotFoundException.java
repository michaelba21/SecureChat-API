package com.securechat.exception;

/**
 * Exception thrown specifically when a user cannot be found in the system. This  Corresponds to HTTP 404 Not Found status in the API.
 * More specific version of ResourceNotFoundException used for user-related operations.
 * This is thrown when: user ID doesn't exist, username not found, or email not registered.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message); 
    }
}