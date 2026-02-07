package com.securechat.dto;

import org.junit.jupiter.api.DisplayName;  // Custom test names
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;  

@DisplayName("LoginRequest DTO Tests")  // Custom display name for test class
public class LoginRequestTest {

    @Test
    @DisplayName("No-args constructor with setters/getters")  // Test name: Basic JavaBean functionality
    void testNoArgsConstructorAndAccessors() {
        // Test: Default constructor with field setting via setters
        LoginRequest req = new LoginRequest();  // Create LoginRequest with no-args constructor
        req.setEmail("alice@example.com");  
        req.setPassword("secret");  

        // Assert: Verify getters return the values that were set
        assertEquals("alice@example.com", req.getEmail());  
        assertEquals("secret", req.getPassword());  
    }

    @Test
    @DisplayName("All-args constructor sets fields")  // Test name: Full constructor
    void testAllArgsConstructor() {
        // Test: Constructor with all fields as parameters (Lombok @AllArgsConstructor)
        LoginRequest req = new LoginRequest("bob@example.com", "p@ssw0rd");  

        // Assert: Verify fields are set correctly by constructor
        assertEquals("bob@example.com", req.getEmail());  
        assertEquals("p@ssw0rd", req.getPassword()); 
    }

    @Test
    @DisplayName("Lombok equals and hashCode")  // Test name: Lombok-generated equality methods
    void testEqualsAndHashCode() {
        // Test: Verify equals() and hashCode() methods work correctly
        // Arrange: Create three LoginRequest objects
        LoginRequest r1 = new LoginRequest("charlie@example.com", "pw1");  
        LoginRequest r2 = new LoginRequest("charlie@example.com", "pw1");  // Same values as r1
        LoginRequest r3 = new LoginRequest("charlie@example.com", "different");  // Different password

        // Assert: Objects with same field values should be equal
        assertEquals(r1, r2, "Instances with same field values should be equal");  // equals() should return true
        // Assert: Equal objects must have same hashCode (hashCode contract)
        assertEquals(r1.hashCode(), r2.hashCode(), "Equal instances must have same hashCode");
        // Assert: Objects with different passwords should not be equal
        assertNotEquals(r1, r3, "Different password should make instances not equal"); 
    }

    @Test
    @DisplayName("toString contains field values")  // Test name: Lombok-generated toString
    void testToString() {
        // Test: Verify toString() method includes field values (but not sensitive data)
        LoginRequest req = new LoginRequest("diana@example.com", "topsecret"); 
        String ts = req.toString();  // Get string representation

        // Assert: toString should include email (non-sensitive)
        assertTrue(ts.contains("diana@example.com"));  
        // Security Assertion: toString should NOT include password (sensitive)
        assertFalse(ts.contains("topsecret"), "toString() should not leak passwords");  // Password should NOT be exposed
    }
}