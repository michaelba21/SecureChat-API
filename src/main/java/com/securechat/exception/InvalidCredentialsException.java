package com.securechat.exception;

/**
 * Exception thrown when user authentication fails due to invalid credentials.
 * This includes: wrong password, non-existent username, or incorrect email.
 * Corresponds to HTTP 401 Unauthorized status in the API.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message); // Passes the authentication error message to parent RuntimeException
    }
}