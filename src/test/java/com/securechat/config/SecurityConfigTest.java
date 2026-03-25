
package com.securechat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SecurityConfig → Unit tests (no Spring context)") // Test class description
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig(); // Create SecurityConfig instance to test

    @Test
    @DisplayName("passwordEncoder → BCryptPasswordEncoder") // Test method 1: password encoder type
    void passwordEncoder_isBCrypt() {
        var encoder = config.passwordEncoder(); // Call method to get encoder
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class); // Verify it's BCryptPasswordEncoder
    }

    @Test
    @DisplayName("bCryptPasswordEncoder → returns primary instance") // Test method 2: bean method
    void bCryptPasswordEncoder_returnsSameInstance() {
        var primary = config.passwordEncoder(); // Get primary encoder
        var secondary = config.bCryptPasswordEncoder(primary); // Call bean method with primary

        assertThat(secondary).isSameAs(primary); // Should return same instance (identity bean)
    }

    @Test
    @DisplayName("bCryptPasswordEncoder → throws on wrong type") // Test method 3: validation error case
    void bCryptPasswordEncoder_wrongType_throws() {
        assertThatThrownBy(() -> config.bCryptPasswordEncoder(mock(PasswordEncoder.class))) // Pass wrong encoder type
                .isInstanceOf(IllegalStateException.class) // Should throw IllegalStateException
                .hasMessageContaining("Expected BCryptPasswordEncoder but got"); // With specific message
    }

    @Test
    @DisplayName("jwtAuthenticationConverter → extracts roles from resource_access") // Test method 4: JWT converter happy path
    void jwtConverter_extractsRoles() {
        // Mock JWT token with claims
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("sub")).thenReturn("user-id"); // Subject claim

        // Build mock resource_access structure with roles
        Map<String, Object> client = Map.of("roles", List.of("USER", "ADMIN")); // Client roles
        Map<String, Object> access = Map.of("securechat-backend", client); // resource_access
        when(jwt.getClaim("resource_access")).thenReturn(access);

        // Get converter from config and convert JWT
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        var auth = converter.convert(jwt);

        // Verify authorities have ROLE_ prefix
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("jwtConverter → missing resource_access → no authorities") // Test method 5: missing resource_access
    void missingResourceAccess_noAuthorities() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("resource_access")).thenReturn(null); // Null resource_access

        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).isEmpty(); // Should have no authorities
    }

    @Test
    @DisplayName("jwtConverter → missing client → no authorities") // Test method 6: wrong client in resource_access
    void missingClient_noAuthorities() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("resource_access")).thenReturn(Map.of("other", Map.of())); // Different client name

        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).isEmpty(); // Should have no authorities
    }

    @Test
    @DisplayName("jwtConverter → missing roles → no authorities") // Test method 7: client without roles
    void missingRoles_noAuthorities() {
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> client = Map.of("other", "value"); // Client without "roles" key
        when(jwt.getClaim("resource_access")).thenReturn(Map.of("securechat-backend", client));

        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).isEmpty(); // Should have no authorities
    }
}