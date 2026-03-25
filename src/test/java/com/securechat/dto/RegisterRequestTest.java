package com.securechat.dto;

import org.junit.jupiter.api.DisplayName;  // Custom test names
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;  // JUnit assertions

@DisplayName("RegisterRequest DTO Tests")  // Custom display name for test class
class RegisterRequestTest {

    @Test
    @DisplayName("Should create RegisterRequest with no-args constructor and use setters/getters")  // Test name
    void testNoArgsConstructorAndSettersGetters() {
        // Test: Default constructor with field setting via setters (JavaBean pattern)
        RegisterRequest request = new RegisterRequest();  // Create RegisterRequest with no-args constructor

        // Test data
        String username = "testuser"; 
        String password = "secret";     // Password (sensitive)
        String email = "test@example.com";  

        // Act: Set fields using setters
        request.setUsername(username);  
        request.setPassword(password);  // Set password
        request.setEmail(email);       

        // Assert: Verify getters return the values that were set
        assertEquals(username, request.getUsername());  
        assertEquals(password, request.getPassword());  // Verify password
        assertEquals(email, request.getEmail());       
    }

    @Test
    @DisplayName("Should create RegisterRequest with all-args constructor")  
    void testAllArgsConstructor() {
        // Test: Constructor with all fields as parameters (Lombok @AllArgsConstructor)
        
        // Test data
        String username = "constructorUser"; 
        String password = "constructorPass";  // Password
        String email = "constructor@example.com"; 

        // Act: Create RegisterRequest using all-args constructor
        RegisterRequest request = new RegisterRequest(username, password, email);

        // Assert: Verify fields are set correctly by constructor
        assertEquals(username, request.getUsername());  // Username from constructor
        assertEquals(password, request.getPassword());  
        assertEquals(email, request.getEmail());      
    }

    @Test
    @DisplayName("Should use Lombok-generated equals and hashCode correctly")  // Test name
    void testEqualsAndHashCode() {
        // Test: Verify Lombok-generated equals() and hashCode() methods
        
        // Arrange: Create three RegisterRequest objects
        RegisterRequest r1 = new RegisterRequest("user1", "pass1", "user1@example.com");  // First object
        RegisterRequest r2 = new RegisterRequest("user1", "pass1", "user1@example.com");  // Same values as r1
        RegisterRequest r3 = new RegisterRequest("user2", "pass2", "user2@example.com");  // Different values

       
        assertEquals(r1, r2);  // equals() should return true for identical objects
        
        // Assert: Equal objects must have same hashCode (Java contract)
        assertEquals(r1.hashCode(), r2.hashCode());
        
        // Assert: Objects with different values should not be equal
        assertNotEquals(r1, r3); 
        
        // Assert: Different objects should ideally have different hashCodes (not guaranteed but likely)
        assertNotEquals(r1.hashCode(), r3.hashCode());
    }

    @Test
    @DisplayName("toString should not include password but include other fields")  // Test name: Security concern
    void testToString() {
        // Test: Verify toString() method includes non-sensitive fields but excludes password
        
        // Test data
        String username = "stringUser"; 
        String password = "stringPass";  // Password (sensitive, should not appear in toString)
        String email = "string@example.com";  

        // Act: Create RegisterRequest and get string representation
        RegisterRequest request = new RegisterRequest(username, password, email);
        String toString = request.toString(); 

        // Assert: toString should include non-sensitive fields
        assertTrue(toString.contains(username));  
        assertTrue(toString.contains(email));     // Email should be in toString
        
        // Security Assertions: Password should NOT appear in toString
        assertFalse(toString.contains("password"));  
        assertFalse(toString.contains(password));    // Should not contain actual password value
    }
}