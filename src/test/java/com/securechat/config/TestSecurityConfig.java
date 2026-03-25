package com.securechat.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-specific security configuration that disables OAuth2 JWT authentication
 * and uses basic authentication instead, allowing @WithMockUser and @WithAnonymousUser
 * to work correctly in tests.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/public/**",
                                "/api/auth/**",
                                "/api/security/public",
                                "/actuator/health",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        // ADMIN endpoints - use hasRole for "ADMIN"
                        .requestMatchers("/api/admin/**", "/api/security/admin").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .httpBasic(org.springframework.security.config.Customizer.withDefaults()); // Use HTTP Basic instead of OAuth2

        return http.build();
    }
}
