package com.securechat.dto;

import org.junit.jupiter.api.DisplayName;  
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;  // JUnit assertions

@DisplayName("AuthResponse DTO Tests")  // Custom display name for test class
class AuthResponseTest {

    @Test
    @DisplayName("No-args constructor with setters/getters")  // Test name
    void testNoArgsConstructorAndAccessors() {
        // Test: Default constructor with field setting via setters
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();  // Create nested UserInfo object
        userInfo.setUserId("id-001");  
        userInfo.setUsername("tester");  // Set username
        userInfo.setEmail("tester@example.com");  

        AuthResponse ar = new AuthResponse();  // Create AuthResponse with no-args constructor
        ar.setToken("jwt-token");  
        ar.setRefreshToken("refresh-token");  // Set refresh token
        ar.setMessage("ok"); 
        ar.setUserInfo(userInfo);  // Set user info object
        ar.setError(null);  

        // Assert: Verify all fields are correctly set and retrieved
        assertEquals("jwt-token", ar.getToken());  // Verify token
        assertEquals("refresh-token", ar.getRefreshToken());  
        assertEquals("ok", ar.getMessage());  
        assertNotNull(ar.getUserInfo());  // UserInfo should not be null
        assertEquals("id-001", ar.getUserInfo().getUserId());  
        assertEquals("tester", ar.getUserInfo().getUsername());  // Verify username
        assertEquals("tester@example.com", ar.getUserInfo().getEmail());  
        assertNull(ar.getError());  // Error should be null
    }

    @Test
    @DisplayName("All-args constructor sets all fields")  // Test: Full constructor
    void testAllArgsConstructor() {
        // Arrange: Create UserInfo and AuthResponse using all-args constructor
        AuthResponse.UserInfo info = new AuthResponse.UserInfo("42", "john", "john@example.com");
        AuthResponse ar = new AuthResponse("t1", "rt1", "m1", info, "err");

        // Assert: All fields should match constructor arguments
        assertEquals("t1", ar.getToken());  // Token
        assertEquals("rt1", ar.getRefreshToken());  // Refresh token
        assertEquals("m1", ar.getMessage()); 
        assertSame(info, ar.getUserInfo());  // Same UserInfo object reference
        assertEquals("err", ar.getError());  
    }

    @Test
    @DisplayName("6-arg convenience constructor maps username/email/userId into UserInfo and clears error")  // Test: Convenience constructor
    void testSixArgConstructor() {
        // Arrange: Use 6-argument constructor (creates UserInfo internally)
        AuthResponse ar = new AuthResponse("t2", "rt2", "welcome", "alice", "alice@example.com", "u-123");

        // Assert: Fields should be set, UserInfo created automatically
        assertEquals("t2", ar.getToken());  // Token
        assertEquals("rt2", ar.getRefreshToken());  
        assertEquals("welcome", ar.getMessage());  
        assertNotNull(ar.getUserInfo());  // UserInfo should be created
        assertEquals("u-123", ar.getUserInfo().getUserId());  
        assertEquals("alice", ar.getUserInfo().getUsername());  // Username in UserInfo
        assertEquals("alice@example.com", ar.getUserInfo().getEmail());  
        assertNull(ar.getError(), "error should be null per constructor");  // Error should be null for success responses
    }

    @Test
    @DisplayName("3-arg constructor sets token/userInfo/error and nulls refreshToken/message")  // Test: Error response constructor
    void testThreeArgConstructor() {
        // Arrange: Use 3-argument constructor for error responses
        AuthResponse.UserInfo info = new AuthResponse.UserInfo("7", "bob", "bob@example.com");
        AuthResponse ar = new AuthResponse("t3", info, "invalid credentials");

        // Assert: Only token, userInfo, and error are set
        assertEquals("t3", ar.getToken());  // Token (may be null or expired token)
        assertSame(info, ar.getUserInfo());  
        assertEquals("invalid credentials", ar.getError());  // Error message
        assertNull(ar.getRefreshToken(), "refreshToken should be null");  // No refresh token for errors
        assertNull(ar.getMessage(), "message should be null");  
    }

    @Test
    @DisplayName("Factory success(token, refresh, message, username, email, userId)")  // Test: Static factory method for success
    void testFactorySuccessSixArgs() {
        // Arrange: Use static factory method for successful authentication
        AuthResponse ar = AuthResponse.success("t4", "rt4", "logged in", "carol", "carol@example.com", "u-456");

        // Assert: All fields set correctly for success response
        assertEquals("t4", ar.getToken());  
        assertEquals("rt4", ar.getRefreshToken());  // Refresh token
        assertEquals("logged in", ar.getMessage()); 
        assertNotNull(ar.getUserInfo());  // UserInfo created
        assertEquals("u-456", ar.getUserInfo().getUserId());  
        assertEquals("carol", ar.getUserInfo().getUsername()); 
        assertEquals("carol@example.com", ar.getUserInfo().getEmail()); 
        assertNull(ar.getError());  // No error for success
    }

    @Test
    @DisplayName("Factory success(token, userInfo) clears error/refresh/message")  // Test: Alternative success factory
    void testFactorySuccessTokenUserInfo() {
        // Arrange: Use factory method with existing UserInfo object
        AuthResponse.UserInfo info = new AuthResponse.UserInfo("55", "dave", "dave@example.com");
        AuthResponse ar = AuthResponse.success("t5", info);

        // Assert: Only token and userInfo set, others null
        assertEquals("t5", ar.getToken());  
        assertSame(info, ar.getUserInfo());  // Same UserInfo object
        assertNull(ar.getError());  
        assertNull(ar.getRefreshToken());  // No refresh token (simplified success)
        assertNull(ar.getMessage());  
    }

    @Test
    @DisplayName("Factory error(errorMessage) sets only error")  // Test: Static factory method for errors
    void testFactoryError() {
        // Arrange: Use static factory method for error responses
        AuthResponse ar = AuthResponse.error("something went wrong");

        // Assert: Only error field is set, all others null
        assertEquals("something went wrong", ar.getError());  // Error message
        assertNull(ar.getToken());  
        assertNull(ar.getRefreshToken());  // No refresh token
        assertNull(ar.getMessage());  
        assertNull(ar.getUserInfo());  // No user info for errors
    }

    @Test
    @DisplayName("UserInfo 3-arg constructor sets userId/username/email")  // Test: UserInfo nested class
    void testUserInfoThreeArgConstructor() {
        // Arrange: Create UserInfo with 3-argument constructor
        AuthResponse.UserInfo info = new AuthResponse.UserInfo("user-1", "userx", "x@example.com");

        // Assert: All UserInfo fields set correctly
        assertEquals("user-1", info.getUserId());  
        assertEquals("userx", info.getUsername());  // Username
        assertEquals("x@example.com", info.getEmail());  
    }

    @Test
    @DisplayName("Lombok equals/hashCode should consider all fields")  // Test: Lombok-generated equals and hashCode
    void testEqualsAndHashCode() {
        // Arrange: Create two AuthResponse objects with same values (different creation methods)
        AuthResponse a1 = AuthResponse.success("t6", "rt6", "m6", "eve", "eve@example.com", "u-789");
        AuthResponse a2 = new AuthResponse("t6", "rt6", "m6",
                new AuthResponse.UserInfo("u-789", "eve", "eve@example.com"), null);

        // Assert: Objects should be equal despite different creation methods
        assertEquals(a1, a2, "Objects with same field values should be equal");  // equals() should return true
        assertEquals(a1.hashCode(), a2.hashCode(), "Equal objects must have same hashCode"); 

        // Arrange: Different AuthResponse
        AuthResponse a3 = AuthResponse.success("t7", "rt7", "m7", "frank", "frank@example.com", "u-999");
        // Assert: Should not equal different object
        assertNotEquals(a1, a3); 
    }

    @Test
    @DisplayName("toString should include key field values")  // Test: Lombok-generated toString
    void testToString() {
        // Arrange: Create AuthResponse
        AuthResponse ar = AuthResponse.success("t8", "rt8", "ok", "gina", "gina@example.com", "u-111");
        String ts = ar.toString();  // Get string representation

        // Assert: toString should contain all important field values
        assertTrue(ts.contains("t8")); 
        assertTrue(ts.contains("rt8"));  // Refresh token
        assertTrue(ts.contains("ok")); 
        assertTrue(ts.contains("gina"));  
        assertTrue(ts.contains("gina@example.com"));  // Email
        assertTrue(ts.contains("u-111")); 
    }
}