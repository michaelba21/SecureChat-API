
package com.securechat.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnauthorizedException → 100% coverage of both constructors") // Test class description
class UnauthorizedExceptionTest {

    @Test
    @DisplayName("Constructor with message only → message is set, cause is null")
    void constructorWithMessageOnly_shouldSetMessageAndNoCause() {
        // Arrange: error message
        String errorMessage = "You do not have permission to delete this chat room";

        // Act: create exception with message only
        UnauthorizedException ex = new UnauthorizedException(errorMessage);

        // Assert: verify message is set
        assertEquals(errorMessage, ex.getMessage()); // JUnit assertion
        assertThat(ex.getMessage()).isEqualTo(errorMessage); // AssertJ assertion

        // Assert: no cause should be present
        assertNull(ex.getCause()); // Cause should be null

        // Assert: stack trace should exist (inherited from RuntimeException)
        assertNotNull(ex.getStackTrace()); 
        assertThat(ex.getStackTrace()).isNotEmpty(); // Stack trace not empty
    }

    @Test
    @DisplayName("Constructor with message and cause → both message and cause are preserved")
    void constructorWithMessageAndCause_shouldPreserveBoth() {
        // Arrange: error message and root cause
        String errorMessage = "Access denied: insufficient role";
        Throwable rootCause = new SecurityException("Token validation failed");

        // Act: create exception with message and cause
        UnauthorizedException ex = new UnauthorizedException(errorMessage, rootCause);

        // Assert: verify message is preserved
        assertEquals(errorMessage, ex.getMessage());

        // Assert: verify cause chain is preserved
        assertSame(rootCause, ex.getCause()); // Same object reference
        assertThat(ex.getCause()).isSameAs(rootCause); 
        assertThat(ex.getCause().getMessage()).isEqualTo("Token validation failed"); // Cause message

        // Assert: verify type of cause
        assertThat(ex.getCause()).isInstanceOf(SecurityException.class); // Cause type
    }

    @Test
    @DisplayName("Constructor with null message → allowed (message = null)")
    void constructorWithNullMessage_shouldAcceptNull() {
        // Act: create exception with null message
        UnauthorizedException ex = new UnauthorizedException(null);

        // Assert: message should be null
        assertNull(ex.getMessage()); 
        assertNull(ex.getCause()); // Null cause
    }

    @Test
    @DisplayName("Constructor with null cause → allowed (cause = null)")
    void constructorWithNullCause_shouldAcceptNullCause() {
        // Arrange: message without cause
        String message = "Action requires moderator privileges";

        // Act: create exception with message and null cause
        UnauthorizedException ex = new UnauthorizedException(message, null);

        // Assert: message preserved, cause null
        assertEquals(message, ex.getMessage()); // Message set
        assertNull(ex.getCause()); // Cause null
    }

    @Test
    @DisplayName("Exception is a RuntimeException")
    void shouldExtendRuntimeException() {
        // Act: create exception instance
        UnauthorizedException ex = new UnauthorizedException("Test");

        // Assert: should be instance of RuntimeException
        assertTrue(ex instanceof RuntimeException); 
        assertThat(ex).isInstanceOf(RuntimeException.class); // AssertJ instance check
    }

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void exceptionCanBeThrownAndCaught() {
        // Arrange: expected error message
        String expectedMessage = "You are not allowed to join this private chat room";

        // Act & Assert: verify exception is thrown with correct properties
        assertThatThrownBy(() -> {
            throw new UnauthorizedException(expectedMessage); // Throw exception
        })
                .isInstanceOf(UnauthorizedException.class) 
                .hasMessage(expectedMessage) // Correct message
                .hasNoCause(); // No cause
    }

    @Test
    @DisplayName("Exception with cause → getCause() returns wrapped exception")
    void exceptionWithCause_shouldReturnOriginalCause() {
        // Arrange: inner exception to be wrapped
        IllegalStateException inner = new IllegalStateException("Invalid token signature");
        
        // Act: create exception with cause
        UnauthorizedException ex = new UnauthorizedException("Access denied", inner);

        // Assert: cause should be the same inner exception
        assertSame(inner, ex.getCause()); 
        assertThat(ex.getCause()).hasMessage("Invalid token signature"); // Cause message
    }
}