package com.securechat.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id"; // Header for user ID from gateway
    private static final String HEADER_USER_ROLES = "X-User-Roles"; // Header for user roles from gateway

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID); // Extract user ID from request header
        String rolesHeader = request.getHeader(HEADER_USER_ROLES); // Extract roles from request header

        if (userId != null && !userId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Process authentication only if user ID exists, is not blank, and no existing authentication
            List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader); // Parse roles into authorities

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities); // Create authentication token
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // Add request details

            SecurityContextHolder.getContext().setAuthentication(authentication); // Set authentication in security context
        }

        filterChain.doFilter(request, response); // Continue filter chain
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of(); // Return empty list if no roles provided
        }

        return Arrays.stream(rolesHeader.split(",")) // Split comma-separated roles
            .map(String::trim) // Trim whitespace from each role
            .filter(role -> !role.isEmpty()) 
            .map(this::normalizeRole) 
            .map(SimpleGrantedAuthority::new) // Convert to Spring Security authority
            .collect(Collectors.toList()); 
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String trimmed = role.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String uppercased = trimmed.toUpperCase(); // Normalize to uppercase
        return uppercased.startsWith("ROLE_") ? uppercased : "ROLE_" + uppercased; // Ensure role has ROLE_ prefix
    }
}