package com.securechat.exception;

/**
 * Exception thrown when a user attempts to perform an action they are not authorized to do.
 * This includes: insufficient permissions, invalid access tokens, or trying to access
 * Corresponds to HTTP 403 Forbidden status in the API (not HTTP 401 which is authentication).
 */
public class UnauthorizedException extends RuntimeException {
  
    public UnauthorizedException(String message) {
        super(message); 
    }

    // Constructor with message and cause (useful for debugging when exception is chained)
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause); 
    }
}