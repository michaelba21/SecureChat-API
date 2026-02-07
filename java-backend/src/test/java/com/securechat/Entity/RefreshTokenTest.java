package com.securechat.Entity;  // Note: Package name has capital 'E' (might be typo, should be 'entity')
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;  // Spring utility for testing private fields

import com.securechat.entity.RefreshToken;  
import com.securechat.entity.User;  // User entity (relationship)

import java.time.Instant;  // Precise timestamp (UTC)
import java.time.temporal.ChronoUnit; 
import java.util.UUID;  // UUID for unique identifiers

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

class RefreshTokenTest {

    @Test
    void isExpired_returnsFalse_whenExpiryDateIsInFuture() {
        // Test: Token not expired when expiry date is in the future
        RefreshToken token = new RefreshToken();  // Create refresh token

        Instant future = Instant.now().plus(7, ChronoUnit.DAYS);  // 7 days in the future
        token.setExpiryDate(future); 

        assertThat(token.isExpired()).isFalse();  // Should NOT be expired
    }

    @Test
    void isExpired_returnsTrue_whenExpiryDateIsInPast() {
        // Test: Token expired when expiry date is in the past
        RefreshToken token = new RefreshToken();  

        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);  // 1 day in the past
        token.setExpiryDate(past);  // Set past expiry date

        assertThat(token.isExpired()).isTrue();  
    }

    @Test
    void isExpired_returnsTrue_whenExpiryDateIsExactlyNow() {
        // Test: Token expired when expiry date is exactly now (edge case)
        RefreshToken token = new RefreshToken();  

        token.setExpiryDate(Instant.now());  // Set expiry to exactly now

        // Security consideration: Most implementations treat "now" as expired
        // This is a security preference - tokens expiring at current time should be considered expired
        assertThat(token.isExpired()).isTrue();  
    }

    @Test
    void isExpired_returnsFalse_whenExpiryDateIsNull() {
        // Test: Token not expired when expiry date is null (edge case)
        RefreshToken token = new RefreshToken();  

        token.setExpiryDate(null);  // Set null expiry date

        // Safe default: null means not expired (or handle differently in service layer)
        // This could vary based on implementation - some might throw exception
        assertThat(token.isExpired()).isFalse();  
    }

    @Test
    void gettersAndSetters_workCorrectly_forAllFields() {
        // Test: All field getters and setters function correctly
        RefreshToken token = new RefreshToken();  

        // Test data
        UUID id = UUID.randomUUID();  // Random ID
        String tokenString = "refresh-token-jwt-string-1234567890"; 
        UUID userId = UUID.randomUUID();  // User ID
        User user = new User();  // User entity
        ReflectionTestUtils.setField(user, "id", userId);  // Set user ID using reflection
        Instant createdAt = Instant.now();  
        Instant expiryDate = Instant.now().plus(30, ChronoUnit.DAYS);  // Expiry in 30 days
        Instant lastUsedAt = Instant.now().minus(1, ChronoUnit.HOURS);  // Last used 1 hour ago
        String ipAddress = "192.168.1.100";  // IP address
        String userAgent = "Mozilla/5.0 Test Agent"; 

        // Set all fields
        token.setId(id);  // Set ID
        token.setToken(tokenString);  // Set token string
        token.setUser(user);  
        token.setCreatedAt(createdAt);  
        token.setExpiryDate(expiryDate); 
        token.setLastUsedAt(lastUsedAt);  // Set last used time
        token.setIpAddress(ipAddress);  // Set IP address
        token.setUserAgent(userAgent);  

        // Verify all fields
        assertThat(token.getId()).isEqualTo(id);  // ID
        assertThat(token.getToken()).isEqualTo(tokenString);  // Token string
        assertThat(token.getUser()).isEqualTo(user);  
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);  // Creation time
        assertThat(token.getExpiryDate()).isEqualTo(expiryDate);  
        assertThat(token.getLastUsedAt()).isEqualTo(lastUsedAt);  // Last used time
        assertThat(token.getIpAddress()).isEqualTo(ipAddress);  
        assertThat(token.getUserAgent()).isEqualTo(userAgent);  // User agent
    }

    @Test
    void token_andExpiryDate_areRequiredForValidBehavior() {
        // Test: Core fields for token validity
        RefreshToken token = new RefreshToken();  // Create refresh token

        // Initially should be null
        assertThat(token.getToken()).isNull(); 
        assertThat(token.getExpiryDate()).isNull();  // Expiry date should be null

        // Set required fields
        token.setToken("valid-token");  
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));  // Set expiry (1 day future)

        // Verify
        assertThat(token.getToken()).isEqualTo("valid-token");  // Token should match
        assertThat(token.isExpired()).isFalse(); 
    }

    @Test
    void lastUsedAt_canBeNull_initially() {
        // Test: lastUsedAt is optional (can be null initially)
        RefreshToken token = new RefreshToken();  
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));  // Set valid expiry

        assertThat(token.getLastUsedAt()).isNull();  
        assertThat(token.isExpired()).isFalse();  // Should NOT be expired
    }

    @Test
    void fullRefreshToken_canBeConstructedCorrectly() {
        // Test: Complete refresh token construction
        User user = new User();  
        UUID userId = UUID.randomUUID();  
        ReflectionTestUtils.setField(user, "id", userId);  // Set user ID using reflection

        RefreshToken token = new RefreshToken();  // Create refresh token
        
        // Set all fields
        token.setToken("jwt.refresh.token.example");  
        token.setUser(user);  // User
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));  // Expires in 7 days
        token.setCreatedAt(Instant.now());  // Created now
        token.setIpAddress("127.0.0.1");  
        token.setUserAgent("TestClient/1.0");  // User agent

        // Verify all fields
        assertThat(token.getToken()).isEqualTo("jwt.refresh.token.example");  
        assertThat(token.getUser()).isEqualTo(user);  // User entity
        assertThat(token.isExpired()).isFalse();  
        assertThat(token.getIpAddress()).isEqualTo("127.0.0.1");  // IP address
        assertThat(token.getUserAgent()).isEqualTo("TestClient/1.0");  
    }
}