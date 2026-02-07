
package com.securechat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*; // AssertJ assertions
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito annotations in JUnit 5
@DisplayName("GatewayHeaderAuthenticationFilter – 100% Coverage") // Test class description
class GatewayHeaderAuthenticationFilterTest {

    private GatewayHeaderAuthenticationFilter filter; // The filter being tested

    // Mock dependencies for HTTP request/response simulation
    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain; // Mock filter chain to verify continuation

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor; // Captures auth token

    @BeforeEach
    void setUp() {
        filter = new GatewayHeaderAuthenticationFilter(); // Create new filter instance
        SecurityContextHolder.clearContext(); // Reset security context before each test
    }

    // ────────────────────────────────────────────────────────────────
    // Happy path: valid headers → sets authentication
    // Tests successful authentication scenarios
    // ────────────────────────────────────────────────────────────────

    @Nested // Group related tests
    @DisplayName("Valid headers") // Test group: cases with valid headers
    class ValidHeaders {

        @Test
        @DisplayName("User ID + roles → creates authentication with ROLE_ prefix")
        void userIdAndRoles_setsAuthWithRolePrefix() throws ServletException, IOException {
            // Arrange: set up mock headers
            when(request.getHeader("X-User-Id")).thenReturn("123e4567-e89b-12d3-a456-426614174000");
            when(request.getHeader("X-User-Roles")).thenReturn("ADMIN,USER,MODERATOR");

            // Act: execute filter
            filter.doFilterInternal(request, response, filterChain);

            // Assert: verify filter chain continues
            verify(filterChain).doFilter(request, response);

            // Retrieve authentication from security context
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            // Verify authentication properties
            assertThat(auth).isNotNull(); // Authentication should be created
            assertThat(auth.getPrincipal()).isEqualTo("123e4567-e89b-12d3-a456-426614174000"); // User ID as principal
            assertThat(auth.getCredentials()).isNull(); // No credentials

            // Extract and verify authorities (roles)
            List<SimpleGrantedAuthority> authorities = auth.getAuthorities().stream()
                    .map(a -> (SimpleGrantedAuthority) a)
                    .toList();

            assertThat(authorities)
                    .extracting(SimpleGrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_MODERATOR"); // Check roles have ROLE_ prefix
        }

        @Test
        @DisplayName("User ID + roles with spaces → trims and normalizes")
        void rolesWithSpaces_trimsAndNormalizes() throws ServletException, IOException {
            // Test: headers with extra spaces should be trimmed
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn(" admin , user ");

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER"); // Verify trimmed roles
        }

        @Test
        @DisplayName("User ID only (no roles) → empty authorities")
        void userIdOnly_emptyAuthorities() throws ServletException, IOException {
            // Test: user with no roles should have empty authority list
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(auth.getAuthorities()).isEmpty(); // No roles assigned
        }
    }

    // ────────────────────────────────────────────────────────────────
    // No authentication cases
    // Tests scenarios where authentication should NOT be created
    // ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("No or invalid headers") // Test group: missing/invalid headers
    class NoOrInvalidHeaders {

        @Test
        @DisplayName("No X-User-Id header → skips authentication")
        void noUserId_skipsAuth() throws ServletException, IOException {
            // Test: missing user ID header should skip auth
            when(request.getHeader("X-User-Id")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response); // Filter chain should continue
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull(); // No auth set
        }

        @Test
        @DisplayName("Empty X-User-Id header → skips authentication")
        void emptyUserId_skipsAuth() throws ServletException, IOException {
            // Test: empty/whitespace user ID should skip auth
            when(request.getHeader("X-User-Id")).thenReturn("  ");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Existing authentication → skips processing")
        void existingAuth_skipsProcessing() throws ServletException, IOException {
            // Test: if authentication already exists, filter should not override it
            UsernamePasswordAuthenticationToken existing = new UsernamePasswordAuthenticationToken(
                    "existing", null, List.of()); // Simulate existing auth
            SecurityContextHolder.getContext().setAuthentication(existing);

            when(request.getHeader("X-User-Id")).thenReturn("new-user"); // New header present

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isSameAs(existing); // Authentication unchanged
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Edge cases in role parsing
    // Tests various role format edge cases
    // ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Role parsing edge cases") // Test group: special role parsing scenarios
    class RoleParsing {

        @Test
        @DisplayName("Null roles header → empty list")
        void nullRoles_emptyList() throws ServletException, IOException {
            // Test: null roles header should result in empty authorities
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("Blank roles header → empty list")
        void blankRoles_emptyList() throws ServletException, IOException {
            // Test: blank roles header should result in empty authorities
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn("   ");

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("Roles with only commas → empty list")
        void onlyCommas_emptyList() throws ServletException, IOException {
            // Test: header with only commas should be treated as empty
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn(",,,");

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(auth.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("Mixed valid/invalid roles → normalizes correctly")
        void mixedRoles_normalizes() throws ServletException, IOException {
            // Test: mixed valid roles with spaces and empty entries
            when(request.getHeader("X-User-Id")).thenReturn("user-uuid");
            when(request.getHeader("X-User-Roles")).thenReturn("admin, ,USER, moderator");

            filter.doFilterInternal(request, response, filterChain);

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            // Verify only valid roles are parsed and normalized
            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_MODERATOR");
        }
    }
}