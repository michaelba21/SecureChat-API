

package com.securechat.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse → Full coverage of constructors, getters & setters")  // Comprehensive DTO testing
class ErrorResponseTest {

    @Test
    @DisplayName("Primary constructor → sets fields correctly and timestamp is recent")
    void primaryConstructor_setsErrorAndMessage_andTimestampIsNow() {
        // Test the main constructor with two parameters (timestamp should be auto-generated)
        String errorCode = "NOT_FOUND";
        String messageText = "Resource with id 42 was not found";

        ErrorResponse response = new ErrorResponse(errorCode, messageText);

        // Verify error and message fields are set correctly
        assertEquals(errorCode, response.getError());
        assertEquals(messageText, response.getMessage());

        // timestamp should be very close to now (within 2 seconds is safe)
        // This tests that timestamp is auto-generated with current time
        LocalDateTime now = LocalDateTime.now();
        assertThat(response.getTimestamp())
                .isAfterOrEqualTo(now.minusSeconds(2))  
                .isBeforeOrEqualTo(now.plusSeconds(2));  // Should not be more than 2 seconds in the future
    }

    @Test
    @DisplayName("Primary constructor → allows null values for error and message")
    void primaryConstructor_allowsNullErrorAndMessage() {
        // Test constructor behavior with null inputs
        ErrorResponse response = new ErrorResponse(null, null);

        // Both error and message should be null as passed
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNotNull(response.getTimestamp());  // But timestamp should still be generated
    }

    @Test
    @DisplayName("Secondary constructor → uses provided timestamp")
    void secondaryConstructor_usesProvidedTimestamp() {
        // Test three-parameter constructor with custom timestamp
        String error = "VALIDATION_ERROR";
        String msg = "Email format is invalid";
        LocalDateTime fixedTime = LocalDateTime.of(2025, 12, 31, 23, 59, 59);  // Fixed timestamp for testing

        ErrorResponse response = new ErrorResponse(error, msg, fixedTime);

        // Verify all three parameters are set correctly
        assertEquals(error, response.getError());
        assertEquals(msg, response.getMessage());
        assertEquals(fixedTime, response.getTimestamp());  // Should use provided timestamp, not current time
    }

    @Test
    @DisplayName("Secondary constructor → allows null timestamp")
    void secondaryConstructor_allowsNullTimestamp() {
        // Test three-parameter constructor with null timestamp
        ErrorResponse response = new ErrorResponse("SERVER_ERROR", "Internal failure", null);

        // timestamp should be null as passed, not auto-generated
        assertNull(response.getTimestamp());
        assertEquals("SERVER_ERROR", response.getError());
        assertEquals("Internal failure", response.getMessage());
    }

    // ────────────────────────────────────────────────
    //  Getters & Setters
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("setError() + getError() round-trip")
    void error_field_roundTrip() {
        // Test error field setter/getter combination
        ErrorResponse response = new ErrorResponse("BAD_REQUEST", "Invalid input");

        response.setError("UNAUTHORIZED"); 
        assertEquals("UNAUTHORIZED", response.getError());  // Verify getter returns new value

        response.setError(null);  
        assertNull(response.getError());  // Verify null is accepted
    }

    @Test
    @DisplayName("setMessage() + getMessage() round-trip")
    void message_field_roundTrip() {
        // Test message field setter/getter combination
        ErrorResponse response = new ErrorResponse("CONFLICT", "Already exists");

        response.setMessage("Resource already taken");  // Change message
        assertEquals("Resource already taken", response.getMessage());

        response.setMessage("");  
        assertEquals("", response.getMessage());  // Verify empty string is accepted

        response.setMessage(null); 
        assertNull(response.getMessage());  // Verify null is accepted
    }

    @Test
    @DisplayName("setTimestamp() + getTimestamp() round-trip")
    void timestamp_field_roundTrip() {
        // Test timestamp field setter/getter combination
        ErrorResponse response = new ErrorResponse("FORBIDDEN", "Access denied");

        LocalDateTime customTime = LocalDateTime.of(2026, 6, 15, 14, 30);  // Specific future timestamp
        response.setTimestamp(customTime); 
        assertEquals(customTime, response.getTimestamp());  // Verify getter returns custom timestamp

        response.setTimestamp(null);  
        assertNull(response.getTimestamp());  // Verify null is accepted
    }

    // ────────────────────────────────────────────────
    //  Immutability & object state verification
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("Multiple setters → fields are independent")
    void setters_areIndependent() {
        // Test that multiple setter calls work independently without interfering with each other
        ErrorResponse response = new ErrorResponse("INITIAL", "Old message");

        // Update all three fields with different setter calls
        response.setError("NEW_ERROR");
        response.setMessage("Updated description");
        response.setTimestamp(LocalDateTime.MIN);  // Use minimum possible timestamp

        // Verify each field was updated independently
        assertEquals("NEW_ERROR", response.getError());
        assertEquals("Updated description", response.getMessage());
        assertEquals(LocalDateTime.MIN, response.getTimestamp());  // Verify extreme timestamp value
    }
}