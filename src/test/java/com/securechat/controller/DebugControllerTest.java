
package com.securechat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DebugController.
 * Tests the debug endpoints for OAuth 2.0 JWT token analysis.
 */
@ExtendWith(MockitoExtension.class) // Enable Mockito with JUnit 5
@DisplayName("DebugController - Tests") 
class DebugControllerTest {

    @InjectMocks // Injects mocks into DebugController instance
    private DebugController debugController;

    @Mock // Mock authentication object
    private Authentication authentication;

    @Mock // Mock JWT authentication token
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock // Mock JWT token
    private Jwt jwt;

    private UUID testUserId; // Test user ID

    @BeforeEach // Runs before each test method
    void setUp() {
        testUserId = UUID.randomUUID(); // Generate random user ID for each test
    }

    // ==================== Token Info Endpoint Tests ====================

    @Nested // Group tests for token info endpoint
    @DisplayName("GET /api/debug/token-info")
    class GetTokenInfo {

        @Test
        @DisplayName("Should return error when authentication is null")
        void shouldReturnErrorWhenAuthenticationIsNull() {
            // Act: call method with null authentication
            Map<String, Object> result = debugController.getTokenInfo(null);

            // Assert: verify error response
            assertThat(result).isNotNull(); // Result should not be null
            assertThat(result.get("error")).isEqualTo("No authentication provided"); // Error message
            assertThat(result.get("authenticationType")).isEqualTo("null"); // Null type
            assertThat(result.get("principalName")).isEqualTo("null"); // Null principal
        }

        @Test
        @DisplayName("Should return basic info for non-JWT authentication")
        void shouldReturnBasicInfoForNonJwtAuthentication() {
            // Arrange: mock non-JWT authentication
            when(authentication.getName()).thenReturn("testuser"); // Username
            when(authentication.isAuthenticated()).thenReturn(true); // Authenticated
            when(authentication.getAuthorities()).thenReturn((Collection) createAuthorities()); // Authorities

            // Act: call method with non-JWT authentication
            Map<String, Object> result = debugController.getTokenInfo(authentication);

            // Assert: verify basic info returned
            assertThat(result).isNotNull();
            assertThat(result.get("authenticationType")).isEqualTo(authentication.getClass().getName()); // Class name
            assertThat(result.get("principalName")).isEqualTo("testuser"); 
            assertThat(result.get("isAuthenticated")).isEqualTo(true); // Authentication status
            assertThat(result.get("authorities")).isNotNull(); 
            assertThat(result.get("tokenType")).isEqualTo("Not JWT"); // Non-JWT token type
            assertThat(result.get("message")).isEqualTo("Authentication is not a JWT token"); // Info message
        }

        @Test
        @DisplayName("Should return detailed JWT token info")
        void shouldReturnDetailedJwtTokenInfo() throws Exception {
            // Arrange: create comprehensive JWT claims
            Map<String, Object> claims = createJwtClaims();
            List<String> audience = List.of("securechat-backend"); // Token audience

            // Mock JWT authentication token
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt); // JWT token
            when(jwtAuthenticationToken.getName()).thenReturn(testUserId.toString()); // User ID as name
            when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true); // Authenticated
            when(jwtAuthenticationToken.getAuthorities()).thenReturn((Collection) createAuthorities()); // Authorities
            when(jwt.getSubject()).thenReturn(testUserId.toString()); // JWT subject
            when(jwt.getIssuer()).thenReturn(new URL("http://localhost:8080/realms/securechat")); // Issuer URL
            when(jwt.getAudience()).thenReturn(audience); // Audience list
            when(jwt.getIssuedAt()).thenReturn(Instant.now()); 
            when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600)); // Expiry time
            when(jwt.getNotBefore()).thenReturn(Instant.now()); 
            when(jwt.getClaims()).thenReturn(claims); // All JWT claims

            // Act: call method with JWT authentication
            Map<String, Object> result = debugController.getTokenInfo(jwtAuthenticationToken);

            // Assert: verify detailed JWT info returned
            assertThat(result).isNotNull();
            assertThat(result.get("authenticationType")).isEqualTo(JwtAuthenticationToken.class.getName()); // JWT type
            assertThat(result.get("principalName")).isEqualTo(testUserId.toString()); // User ID
            assertThat(result.get("isAuthenticated")).isEqualTo(true); // Authenticated
            assertThat(result.get("authorities")).isNotNull(); // Authorities
            assertThat(result.get("tokenType")).isEqualTo("JWT"); 
            assertThat(result.get("subject")).isEqualTo(testUserId.toString()); // JWT subject
            assertThat(result.get("issuer")).isEqualTo(new URL("http://localhost:8080/realms/securechat")); // Issuer
            assertThat(result.get("audience")).isEqualTo(audience); // Audience
            assertThat(result.get("hasResourceAccess")).isEqualTo(true); // Has resource access claim
            assertThat(result.get("hasRealmAccess")).isEqualTo(true); 
            assertThat(result.get("preferredUsername")).isEqualTo("testuser"); // Preferred username
            assertThat(result.get("email")).isEqualTo("test@example.com"); // Email
            assertThat(result.get("givenName")).isEqualTo("Test"); // Given name
            assertThat(result.get("familyName")).isEqualTo("User"); 
        }

        @Test
        @DisplayName("Should handle JWT without resource_access claim")
        void shouldHandleJwtWithoutResourceAccess() throws Exception {
            // Arrange: create claims without resource_access
            Map<String, Object> claims = new HashMap<>();
            claims.put("realm_access", createRealmAccess()); // Only realm access
            claims.put("preferred_username", "testuser"); // Username
            claims.put("email", "test@example.com"); // Email

            // Mock JWT authentication token
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
            when(jwtAuthenticationToken.getName()).thenReturn(testUserId.toString());
            when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true);
            when(jwtAuthenticationToken.getAuthorities()).thenReturn((Collection) createAuthorities());
            when(jwt.getSubject()).thenReturn(testUserId.toString());
            when(jwt.getIssuer()).thenReturn(new URL("http://localhost:8080/realms/securechat"));
            when(jwt.getAudience()).thenReturn(List.of("securechat-backend"));
            when(jwt.getIssuedAt()).thenReturn(Instant.now());
            when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
            when(jwt.getNotBefore()).thenReturn(Instant.now());
            when(jwt.getClaims()).thenReturn(claims);

            // Act: call method with JWT without resource_access
            Map<String, Object> result = debugController.getTokenInfo(jwtAuthenticationToken);

            // Assert: verify resource_access is false
            assertThat(result).isNotNull();
            assertThat(result.get("hasResourceAccess")).isEqualTo(false); // No resource access
            assertThat(result.get("hasRealmAccess")).isEqualTo(true); // Has realm access
        }

        @Test
        @DisplayName("Should handle JWT without realm_access claim")
        void shouldHandleJwtWithoutRealmAccess() throws Exception {
            // Arrange: create claims without realm_access
            Map<String, Object> claims = new HashMap<>();
            claims.put("resource_access", createResourceAccess()); // Only resource access
            claims.put("preferred_username", "testuser"); 
            claims.put("email", "test@example.com"); // Email

            // Mock JWT authentication token
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
            when(jwtAuthenticationToken.getName()).thenReturn(testUserId.toString());
            when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true);
            when(jwtAuthenticationToken.getAuthorities()).thenReturn((Collection) createAuthorities());
            when(jwt.getSubject()).thenReturn(testUserId.toString());
            when(jwt.getIssuer()).thenReturn(new URL("http://localhost:8080/realms/securechat"));
            when(jwt.getAudience()).thenReturn(List.of("securechat-backend"));
            when(jwt.getIssuedAt()).thenReturn(Instant.now());
            when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
            when(jwt.getNotBefore()).thenReturn(Instant.now());
            when(jwt.getClaims()).thenReturn(claims);

            // Act: call method with JWT without realm_access
            Map<String, Object> result = debugController.getTokenInfo(jwtAuthenticationToken);

            // Assert: verify realm_access is false
            assertThat(result).isNotNull();
            assertThat(result.get("hasResourceAccess")).isEqualTo(true); // Has resource access
            assertThat(result.get("hasRealmAccess")).isEqualTo(false); 
        }

        @Test
        @DisplayName("Should handle JWT without optional claims")
        void shouldHandleJwtWithoutOptionalClaims() throws Exception {
            // Arrange: create minimal claims (only required ones)
            Map<String, Object> claims = new HashMap<>();
            claims.put("realm_access", createRealmAccess()); // Realm access
            claims.put("resource_access", createResourceAccess()); 
            // No optional claims: preferred_username, email, given_name, family_name

            // Mock JWT authentication token
            when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
            when(jwtAuthenticationToken.getName()).thenReturn(testUserId.toString());
            when(jwtAuthenticationToken.isAuthenticated()).thenReturn(true);
            when(jwtAuthenticationToken.getAuthorities()).thenReturn((Collection) createAuthorities());
            when(jwt.getSubject()).thenReturn(testUserId.toString());
            when(jwt.getIssuer()).thenReturn(new URL("http://localhost:8080/realms/securechat"));
            when(jwt.getAudience()).thenReturn(List.of("securechat-backend"));
            when(jwt.getIssuedAt()).thenReturn(Instant.now());
            when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
            when(jwt.getNotBefore()).thenReturn(Instant.now());
            when(jwt.getClaims()).thenReturn(claims);

            // Act: call method with minimal JWT
            Map<String, Object> result = debugController.getTokenInfo(jwtAuthenticationToken);

            // Assert: verify optional claims are not present
            assertThat(result).isNotNull();
            assertThat(result).doesNotContainKey("preferredUsername"); // No preferred username
            assertThat(result).doesNotContainKey("email"); // No email
            assertThat(result).doesNotContainKey("givenName"); 
            assertThat(result).doesNotContainKey("familyName"); // No family name
        }
    }

    // ==================== Health Endpoint Tests ====================

    @Nested // Group tests for health endpoint
    @DisplayName("GET /api/debug/health")
    class GetHealth {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() {
            // Act: call health endpoint method
            Map<String, String> result = debugController.health();

            // Assert: verify health response
            assertThat(result).isNotNull(); // Result should not be null
            assertThat(result.get("status")).isEqualTo("UP"); // Status should be UP
            assertThat(result.get("message")).isEqualTo("Debug endpoint is accessible"); // Status message
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a collection of GrantedAuthority for testing.
     * This method properly handles the generics issue with Collection<? extends GrantedAuthority>.
     * 
     */
    private Collection<GrantedAuthority> createAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // User role
        authorities.add(new SimpleGrantedAuthority("SCOPE_read")); 
        return authorities;
    }

    /**
     * Creates a map of JWT claims for testing.
     */
    private Map<String, Object> createJwtClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", createRealmAccess()); // Realm access claim
        claims.put("resource_access", createResourceAccess()); // Resource access claim
        claims.put("preferred_username", "testuser"); 
        claims.put("email", "test@example.com"); // Email
        claims.put("given_name", "Test"); 
        claims.put("family_name", "User"); // Family name
        return claims;
    }

    /**
     * Creates a realm_access claim for testing.
     */
    private Map<String, Object> createRealmAccess() {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user", "admin")); // Realm roles
        return realmAccess;
    }

    /**
     * Creates a resource_access claim for testing.
     */
    private Map<String, Object> createResourceAccess() {
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", List.of("user", "admin")); // Client roles

        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("securechat-backend", clientAccess); // Application client
        return resourceAccess;
    }
}