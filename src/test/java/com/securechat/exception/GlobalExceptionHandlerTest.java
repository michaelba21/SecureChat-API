

package com.securechat.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito with JUnit 5
@DisplayName("GlobalExceptionHandler → Tests for existing exceptions") // Test class description
class GlobalExceptionHandlerTest {

    @InjectMocks // Injects mocks into GlobalExceptionHandler instance
    private GlobalExceptionHandler handler;

    // -------------------------------------------------------------------------
    //   Spring validation & binding exceptions
    //   Tests for validation and request binding errors
    // -------------------------------------------------------------------------

    @Nested // Group validation and binding tests
    @DisplayName("Spring validation & binding")
    class ValidationAndBinding {

        @Test
        void methodArgumentNotValid_shouldReturn400WithFieldErrors() {
            // Arrange: mock binding result with field errors
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(
                    new FieldError("dto", "name", "must not be blank"), // Name field error
                    new FieldError("dto", "email", "invalid email format") // Email field error
            ));

            // Mock MethodArgumentNotValidException
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleValidationExceptions(ex);

            // Assert: verify 400 Bad Request with field errors
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody()) // Response body
                    .hasSize(2) // Two field errors
                    .containsEntry("name", "must not be blank") // Name error message
                    .containsEntry("email", "invalid email format"); // Email error message
        }

        @Test
        void httpMessageNotReadable_shouldReturn400() {
            // Arrange: create JSON parsing exception
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Cannot deserialize value");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleHttpMessageNotReadable(ex);

            // Assert: verify 400 Bad Request
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody()).containsEntry("error", "Malformed JSON request"); // JSON error message
        }

        @Test
        void missingServletRequestParameter_shouldReturn400WithParamName() {
            // Arrange: missing required request parameter
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("chatRoomId", "String");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleMissingServletRequestParameterException(ex);

            // Assert: verify 400 with parameter name in error message
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody()).containsEntry("error", "Missing required parameter: chatRoomId");
        }

        @Test
        void missingServletRequestPart_shouldReturn400WithPartName() {
            // Arrange: missing request part (e.g., file upload)
            MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleMissingServletRequestPartException(ex);

            // Assert: verify 400 with part name
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody()).containsEntry("error", "Missing required request part: file");
        }

        @Test
        void methodArgumentTypeMismatch_shouldReturn400WithTypeInfo() {
            // Arrange: type mismatch exception (e.g., UUID expected but got string)
            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException(null, UUID.class, "chatRoomId", null, null);

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleMethodArgumentTypeMismatch(ex);

            // Assert: verify 400 with type information
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody().get("error")) 
                    .contains("Invalid path parameter: chatRoomId should be UUID"); // Type mismatch info
        }
    }

    // -------------------------------------------------------------------------
    //   Security & authorization exceptions
    //   Tests for access control and security errors
    // -------------------------------------------------------------------------

    @Nested // Group security exception tests
    @DisplayName("Security & authorization")
    class SecurityExceptions {

        @Test
        void accessDenied_shouldReturn403WithMessage() {
            // Arrange: user lacks required permissions
            AccessDeniedException ex = new AccessDeniedException("Access is denied");

            // Act: call exception handler
            ResponseEntity<String> resp = handler.handleAccessDenied(ex);

            // Assert: verify 403 Forbidden
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN); // HTTP 403
            assertThat(resp.getBody()).isEqualTo("Access is denied"); // Original error message
        }
    }

    // -------------------------------------------------------------------------
    //   Database & infrastructure exceptions
    //   Tests for database connectivity and infrastructure errors
    // -------------------------------------------------------------------------

    @Nested // Group database exception tests
    @DisplayName("Database & infrastructure")
    class DatabaseExceptions {

        @Test
        void dataAccessException_shouldReturn503WithDegradedMessage() {
            // Arrange: mock database access exception
            DataAccessException ex = mock(DataAccessException.class);
            when(ex.getMessage()).thenReturn("Connection refused"); // Database error message

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleDataAccessException(ex);

            // Assert: verify 503 Service Unavailable
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE); // HTTP 503
            assertThat(resp.getBody()) // Response body
                    .containsEntry("error", "Service temporarily unavailable. Please try again later.") // User-friendly message
                    .containsEntry("status", "degraded"); // Service status
        }

        @Test
        void cannotCreateTransactionException_shouldReturn503() {
            // Arrange: database transaction creation failed
            CannotCreateTransactionException ex = new CannotCreateTransactionException("Cannot obtain connection");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleCannotCreateTransactionException(ex);

            // Assert: verify 503 Service Unavailable
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE); // HTTP 503
            assertThat(resp.getBody()) // Response body
                    .containsEntry("error", "Service temporarily unavailable due to database issues.") // Database-specific message
                    .containsEntry("status", "degraded"); // Service status
        }
    }

    // -------------------------------------------------------------------------
    //   Generic & fallback handlers
    //   Tests for generic exceptions and fallback error handling
    // -------------------------------------------------------------------------

    @Nested // Group generic exception tests
    @DisplayName("Generic & fallback")
    class GenericHandlers {

        @Test
        void illegalArgumentException_shouldReturn400() {
            // Arrange: invalid argument exception
            IllegalArgumentException ex = new IllegalArgumentException("Invalid UUID format");

            // Act: call exception handler
            ResponseEntity<String> resp = handler.handleIllegalArgumentException(ex);

            // Assert: verify 400 Bad Request
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // HTTP 400
            assertThat(resp.getBody()).isEqualTo("Invalid UUID format"); // Original error message
        }

        @Test
        void runtimeException_shouldReturn500AndLog() {
            // Arrange: unexpected runtime exception
            RuntimeException ex = new RuntimeException("Unexpected null pointer");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleRuntimeException(ex);

            // Assert: verify 500 Internal Server Error
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); // HTTP 500
            String actualError = resp.getBody().get("error"); 
            assertThat(actualError).isNotNull(); // Error message should not be null
            
            // Check for "Unexpected null pointer" (the actual exception message)
            assertThat(actualError).isEqualTo("Unexpected null pointer"); // Original error message
        }

        @Test
        void genericException_shouldReturn500AndLog() {
            // Arrange: generic exception (catch-all)
            Exception ex = new Exception("Something went wrong");

            // Act: call exception handler
            ResponseEntity<Map<String, String>> resp = handler.handleGenericException(ex);

            // Assert: verify 500 Internal Server Error
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); // HTTP 500
            assertThat(resp.getBody()) // Response body
                    .containsEntry("error", "Internal server error"); // Generic error message
        }
    }
}