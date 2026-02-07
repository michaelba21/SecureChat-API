package com.securechat.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    private String error;  // Short error type/code (e.g., "NOT_FOUND", "VALIDATION_ERROR")
    private String message;  // Human-readable error description
    private LocalDateTime timestamp; 

    // Primary constructor - automatically sets timestamp to current time
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now(); // Auto-captures error time
    }

    // Alternative constructor for custom timestamp (testing or delayed responses)
    public ErrorResponse(String error, String message, LocalDateTime timestamp) {
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Standard getters and setters for serialization and modification
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}