
package com.securechat.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceNotFoundException → 100% coverage of both constructors")  // Aiming for full test coverage
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Constructor with message only → message is set correctly, no cause")
    void constructorWithMessageOnly_shouldSetMessageAndNoCause() {
        // Test single-parameter constructor (message only, no cause)
        String errorMessage = "Chat room with id 12345 not found";

        ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

        // Verify message is correctly stored and accessible
        assertEquals(errorMessage, ex.getMessage());
        assertThat(ex.getMessage()).isEqualTo(errorMessage);  // AssertJ alternative syntax

        // No cause should be present in single-parameter constructor
        assertNull(ex.getCause());

        // Stack trace should be present (inherited from RuntimeException)
        assertNotNull(ex.getStackTrace());
        assertThat(ex.getStackTrace()).isNotEmpty();  // Verify stack trace is generated
    }

    @Test
    @DisplayName("Constructor with message and cause → both are preserved")
    void constructorWithMessageAndCause_shouldPreserveBoth() {
        // Test two-parameter constructor (message with cause)
        String errorMessage = "User profile not found";
        Throwable rootCause = new IllegalArgumentException("Database connection timeout");  // Example root cause

        ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage, rootCause);

        // Verify message is correctly stored
        assertEquals(errorMessage, ex.getMessage());

        // Verify cause is correctly chained and preserved
        assertSame(rootCause, ex.getCause()); 
        assertThat(ex.getCause()).isSameAs(rootCause);  // AssertJ version
        assertThat(ex.getCause().getMessage()).isEqualTo("Database connection timeout");  // Verify cause message

        // Verify full cause chain is accessible and of correct type
        assertThat(ex.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Constructor with null message → allowed, message is null")
    void constructorWithNullMessage_shouldAcceptNull() {
        // Test edge case: null message in single-parameter constructor
        ResourceNotFoundException ex = new ResourceNotFoundException(null);

        assertNull(ex.getMessage());  
        assertNull(ex.getCause());  // No cause in single-parameter constructor
    }

    @Test
    @DisplayName("Constructor with null cause → allowed, cause is null")
    void constructorWithNullCause_shouldAcceptNullCause() {
        // Test edge case: null cause in two-parameter constructor
        String msg = "File attachment not found";

        ResourceNotFoundException ex = new ResourceNotFoundException(msg, null);

        assertEquals(msg, ex.getMessage());  // Message should be preserved
        assertNull(ex.getCause());  // Cause should be null as passed
    }

    @Test
    @DisplayName("Exception is instance of RuntimeException")
    void shouldBeRuntimeException() {
        // Verify inheritance hierarchy
        ResourceNotFoundException ex = new ResourceNotFoundException("Test");

        assertTrue(ex instanceof RuntimeException); 
        assertThat(ex).isInstanceOf(RuntimeException.class);  // AssertJ assertion (more expressive)
    }

    @Test
    @DisplayName("Can be thrown and caught correctly")
    void exceptionCanBeThrownAndCaught() {
        // Test that the exception behaves correctly when thrown
        String expectedMessage = "Message with id abc-123 not found";

        // Use AssertJ's assertThatThrownBy for fluent exception testing
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException(expectedMessage);  // Simulate throwing the exception
        })
                .isInstanceOf(ResourceNotFoundException.class)  // Verify exception type
                .hasMessage(expectedMessage)  
                .hasNoCause();  // Verify no cause (single-parameter constructor)
    }

    @Test
    @DisplayName("Exception with cause → getCause() returns original exception")
    void exceptionWithCause_shouldReturnOriginalCause() {
        // Test cause chaining and retrieval
        RuntimeException root = new RuntimeException("Inner database error");  // Simulated root cause
        ResourceNotFoundException wrapper = new ResourceNotFoundException("Resource not found", root);  // Wrapper exception

        // Verify cause chaining works correctly
        assertSame(root, wrapper.getCause());  // Same instance reference
        assertThat(wrapper.getCause()).hasMessage("Inner database error");  // AssertJ fluent assertion
    }
}