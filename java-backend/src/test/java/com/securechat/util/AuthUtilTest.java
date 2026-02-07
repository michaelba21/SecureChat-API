

package com.securechat.util;

import com.securechat.entity.User;
import com.securechat.service.UserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito for mocking dependencies
@MockitoSettings(strictness = Strictness.LENIENT) 
@DisplayName("AuthUtil Tests – 100% Coverage")  // Goal: comprehensive test coverage of AuthUtil
class AuthUtilTest {

    @Mock
    private UserSyncService userSyncService;  // Mock for user synchronization service

    @Mock
    private Jwt jwt;  // Mock JWT token for authentication testing

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;  // Mock JWT authentication token

    @Mock
    private Principal principal;  // Mock Principal interface for basic authentication

    @InjectMocks
    private AuthUtil authUtil;  // Utility class under test with injected mocks

    private UUID userId;  // Test user ID for consistent testing

    @BeforeEach
    void setUp() {
        // Common setup for all tests
        userId = UUID.randomUUID();  // Generate unique user ID for each test
        lenient().when(jwt.getSubject()).thenReturn(userId.toString());  // Default JWT subject
        lenient().when(jwtAuthenticationToken.getToken()).thenReturn(jwt);  // Link JWT to auth token
    }

    // ────────────────────────────────────────────────────────────────
    // getCurrentUserId(Principal)
    // ────────────────────────────────────────────────────────────────
    // Tests for extracting user ID from Principal interface

    @Nested
    @DisplayName("getCurrentUserId(Principal)")
    class GetCurrentUserId {

        @Test
        @DisplayName("Valid principal → returns UUID")
        void validPrincipal_returnsUuid() {
            // Test normal case: Principal has valid UUID string as name
            when(principal.getName()).thenReturn(userId.toString());

            UUID result = authUtil.getCurrentUserId(principal);

            assertThat(result).isEqualTo(userId);  // Verify UUID is parsed correctly
        }

        @Test
        @DisplayName("Null principal → AuthenticationCredentialsNotFoundException")
        void nullPrincipal_throwsNotFound() {
            // Test edge case: null principal should throw exception
            assertThatThrownBy(() -> authUtil.getCurrentUserId(null))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessage("No authenticated user");
        }

        @Test
        @DisplayName("Null or blank principal name → AuthenticationCredentialsNotFoundException")
        void blankPrincipalName_throwsNotFound() {
            // Test edge cases: empty or whitespace-only principal names
            when(principal.getName()).thenReturn(null);
            assertThatThrownBy(() -> authUtil.getCurrentUserId(principal))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessage("Principal name is null or empty");

            when(principal.getName()).thenReturn("   ");  // Whitespace only
            assertThatThrownBy(() -> authUtil.getCurrentUserId(principal))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessage("Principal name is null or empty");
        }

        @Test
        @DisplayName("Invalid UUID format → BadCredentialsException")
        void invalidUuidFormat_throwsBadCredentials() {
            // Test error case: Principal name is not a valid UUID
            when(principal.getName()).thenReturn("not-a-uuid");

            assertThatThrownBy(() -> authUtil.getCurrentUserId(principal))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid user ID format: not-a-uuid");
        }
    }

    // ────────────────────────────────────────────────────────────────
    // getAuthenticatedUser(Authentication)
    // ────────────────────────────────────────────────────────────────
    // Tests for extracting authenticated user from Authentication object

    @Nested
    @DisplayName("getAuthenticatedUser(Authentication)")
    class GetAuthenticatedUser {

        @Test
        @DisplayName("Valid JWT → returns User")
        void validJwt_returnsUser() {
            // Test normal case: JWT authentication returns valid user
            User user = new User();
            user.setId(userId);
            when(userSyncService.getOrCreateUser(jwt)).thenReturn(user);  // Mock user synchronization

            User result = authUtil.getAuthenticatedUser(jwtAuthenticationToken);

            assertThat(result).isSameAs(user);  // Verify same user object returned
        }

        @Test
        @DisplayName("Null authentication → AuthenticationCredentialsNotFoundException")
        void nullAuthentication_throwsNotFound() {
            // Test edge case: null authentication
            assertThatThrownBy(() -> authUtil.getAuthenticatedUser(null))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessage("No authentication provided");
        }

        @Test
        @DisplayName("Non-JWT authentication → AuthenticationCredentialsNotFoundException")
        void nonJwtAuthentication_throwsNotFound() {
            // Test error case: authentication is not JWT-based
            Authentication nonJwt = mock(Authentication.class);

            assertThatThrownBy(() -> authUtil.getAuthenticatedUser(nonJwt))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessage("Authentication is not a JWT token");
        }

        @Test
        @DisplayName("Null or blank subject → BadCredentialsException")
        void nullSubject_throwsBadCredentials() {
            // Test edge case: JWT has no subject claim
            lenient().when(jwt.getSubject()).thenReturn(null);

            assertThatThrownBy(() -> authUtil.getAuthenticatedUser(jwtAuthenticationToken))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("JWT subject claim is null or empty");
        }

        @Test
        @DisplayName("Invalid UUID subject → BadCredentialsException")
        void invalidSubjectUuid_throwsBadCredentials() {
            // Test error case: JWT subject is not a valid UUID
            lenient().when(jwt.getSubject()).thenReturn("not-a-uuid");

            assertThatThrownBy(() -> authUtil.getAuthenticatedUser(jwtAuthenticationToken))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("JWT subject is not a valid UUID");
        }
    }

    // ────────────────────────────────────────────────────────────────
    // hasRole & isAdmin
    // ────────────────────────────────────────────────────────────────
    // Tests for role-based authorization checks

    @Nested
    @DisplayName("hasRole & isAdmin")
    class HasRole {

        @Test
        @DisplayName("hasRole → matches with ROLE_ prefix")
        void hasRole_withRolePrefix() {
            // Test role check with Spring Security's ROLE_ prefix convention
            Authentication auth = mock(Authentication.class);
            // Note: Using doReturn().when() avoids wildcard capture issue with Collection<? extends GrantedAuthority>
            doReturn(Set.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

            assertThat(authUtil.hasRole(auth, "ADMIN")).isTrue();  // Should match even with ROLE_ prefix
        }

        @Test
        @DisplayName("hasRole → matches without ROLE_ prefix")
        void hasRole_withoutRolePrefix() {
            // Test role check without ROLE_ prefix (flexible matching)
            Authentication auth = mock(Authentication.class);
            doReturn(Set.<GrantedAuthority>of(new SimpleGrantedAuthority("ADMIN"))).when(auth).getAuthorities();

            assertThat(authUtil.hasRole(auth, "ADMIN")).isTrue();  // Should match authority directly
        }

        @Test
        @DisplayName("hasRole → null authentication → false")
        void hasRole_nullAuthentication_returnsFalse() {
            // Test edge case: null authentication returns false (safe default)
            assertThat(authUtil.hasRole(null, "ADMIN")).isFalse();
        }

        @Test
        @DisplayName("hasRole → null authorities → false")
        void hasRole_nullAuthorities_returnsFalse() {
            // Test edge case: authentication has no authorities
            Authentication auth = mock(Authentication.class);
            when(auth.getAuthorities()).thenReturn(null);

            assertThat(authUtil.hasRole(auth, "ADMIN")).isFalse();  // Should handle null gracefully
        }

        @Test
        @DisplayName("isAdmin → calls hasRole('ADMIN')")
        void isAdmin_callsHasRole() {
            // Test convenience method for admin role check
            Authentication auth = mock(Authentication.class);
            doReturn(Set.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

            assertThat(authUtil.isAdmin(auth)).isTrue();  // Should return true for admin role
        }
    }

    // ────────────────────────────────────────────────────────────────
    // getUsername(Authentication)
    // ────────────────────────────────────────────────────────────────
    // Tests for extracting username from authentication

    @Nested
    @DisplayName("getUsername(Authentication)")
    class GetUsername {

        @Test
        @DisplayName("preferred_username present → returns it")
        void preferredUsername_present() {
            // Test normal case: preferred_username claim exists in JWT
            when(jwt.getClaimAsString("preferred_username")).thenReturn("john_doe");

            String result = authUtil.getUsername(jwtAuthenticationToken);

            assertThat(result).isEqualTo("john_doe");  // Should return preferred_username
        }

        @Test
        @DisplayName("No preferred_username → falls back to email")
        void noPreferredUsername_fallbackToEmail() {
            // Test fallback: use email claim when preferred_username is not present
            when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
            when(jwt.getClaimAsString("email")).thenReturn("john@example.com");

            String result = authUtil.getUsername(jwtAuthenticationToken);

            assertThat(result).isEqualTo("john@example.com");  // Should fall back to email
        }

        @Test
        @DisplayName("No usable claims → returns null")
        void noUsableClaims_returnsNull() {
            // Test edge case: neither claim is available
            when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
            when(jwt.getClaimAsString("email")).thenReturn(null);

            String result = authUtil.getUsername(jwtAuthenticationToken);

            assertThat(result).isNull();  // Should return null when no claims found
        }

        @Test
        @DisplayName("Non-JWT authentication → returns null")
        void nonJwt_returnsNull() {
            // Test edge case: authentication is not JWT-based
            Authentication nonJwt = mock(Authentication.class);

            String result = authUtil.getUsername(nonJwt);

            assertThat(result).isNull();  // Should return null for non-JWT authentication
        }

        @Test
        @DisplayName("Null authentication → returns null")
        void nullAuthentication_returnsNull() {
            // Test edge case: null authentication
            String result = authUtil.getUsername(null);

            assertThat(result).isNull();  // Should return null safely
        }
    }
}