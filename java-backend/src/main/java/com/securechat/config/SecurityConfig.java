package com.securechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
 // Main password encoder bean for the application
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
   // Specific BCrypt encoder that depends on the primary password encoder
    @Bean
    @DependsOn("passwordEncoder")
    public BCryptPasswordEncoder bCryptPasswordEncoder(PasswordEncoder passwordEncoder) {
        if (passwordEncoder instanceof BCryptPasswordEncoder) {
            return (BCryptPasswordEncoder) passwordEncoder;
        }
        throw new IllegalStateException("Expected BCryptPasswordEncoder but got: " +
                passwordEncoder.getClass());
    }
  // Converts JWT tokens to Spring Security authentication objects
    // Extracts roles from Keycloak JWT token claims
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub"); // Use 'sub' claim as username (MUST be UUID
         // Custom converter to extract roles from JWT token
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
// Get resource_access claim from JWT (Keycloak specific)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null)
                return List.of();
  
   // Get the client-specific roles for "securechat-backend"
            Map<String, Object> client = (Map<String, Object>) resourceAccess.get("securechat-backend");
            if (client == null)
                return List.of();
  // Extract roles list from client
            List<String> roles = (List<String>) client.get("roles");
            if (roles == null)
                return List.of();
  // Convert Keycloak roles to Spring Security authorities
            // Prefix with "ROLE_" as Spring Security convention
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        });
        return converter;
    }
 // Main security configuration for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/public/**",
                                "/api/auth/**",
                                "/api/security/public",
                                "/actuator/health",  // Health check endpoint
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")   // OpenAPI documentation
                        .permitAll()

                        // ADMIN endpoints - use hasAuthority for "ROLE_ADMIN"
                        .requestMatchers("/api/admin/**", "/api/security/admin").hasAuthority("ROLE_ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())) // Use custom
                                                                                                  // converter
                );

        return http.build();
    }
}
