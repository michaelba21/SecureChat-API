package com.securechat.util;

import com.securechat.entity.User;
import com.securechat.service.UserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

/**
 * Centralized utility for authentication and user lookup.
 * Provides safe, consistent methods for extracting user information from JWT tokens.
 */
@Component
public class AuthUtil {

    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);

    private final UserSyncService userSyncService;

    public AuthUtil(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    /**
     * Get the current user ID from the principal.
     * Safely parses the UUID and throws meaningful exceptions if invalid.
     *
     * @param principal the security principal
     * @return the user ID as UUID
     * @throws AuthenticationCredentialsNotFoundException if principal is null
     * @throws BadCredentialsException if principal name is not a valid UUID
     */
    public UUID getCurrentUserId(Principal principal) {
        if (principal == null) {
            logger.error("Authentication failed: No authenticated user (principal is null)");
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }

        String name = principal.getName();
        if (name == null || name.trim().isEmpty()) {
            logger.error("Authentication failed: Principal name is null or empty");
            throw new AuthenticationCredentialsNotFoundException("Principal name is null or empty");
        }

        try {
            UUID uuid = UUID.fromString(name);
            logger.debug("Successfully parsed UUID from principal: {}", uuid);
            return uuid;
        } catch (IllegalArgumentException e) {
            logger.error("Authentication failed: Invalid UUID format in principal name: '{}'", name);
            throw new BadCredentialsException("Invalid user ID format: " + name, e);
        }
    }

    /**
     * Get the current user entity from the authentication.
     * Safely extracts user ID from JWT and fetches the user from the database.
     *
     * @param authentication the Spring Security authentication object
     * @return 
     * @throws AuthenticationCredentialsNotFoundException if authentication is null or not a JWT token
     * @throws BadCredentialsException 
     * @throws RuntimeException if user is not found in the database
     */
    public User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            logger.error("Authentication failed: No authentication object");
            throw new AuthenticationCredentialsNotFoundException("No authentication provided");
        }

        if (!(authentication instanceof JwtAuthenticationToken)) {
            logger.error("Authentication failed: Not a JWT token, type: {}", authentication.getClass().getName());
            throw new AuthenticationCredentialsNotFoundException("Authentication is not a JWT token");
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        // Extract user ID from JWT subject claim (set by SecurityConfig.setPrincipalClaimName("sub"))
        String userId = jwt.getSubject();
        if (userId == null || userId.trim().isEmpty()) {
            logger.error("Authentication failed: JWT subject claim is null or empty");
            throw new BadCredentialsException("JWT subject claim is null or empty");
        }

        try {
            UUID uuid = UUID.fromString(userId);
            logger.debug("Extracted UUID from JWT subject: {}", uuid);

            User user = userSyncService.getOrCreateUser(jwt);

            logger.debug("Successfully retrieved user: {}", user.getUsername());
            return user;
        } catch (IllegalArgumentException e) {
            logger.error("Authentication failed: JWT subject is not a valid UUID: '{}'", userId);
            throw new BadCredentialsException("JWT subject is not a valid UUID: " + userId, e);
        }
    }

    /**
     * Check if the authenticated user has a specific role/authority.
     *
     * @param authentication the Spring Security authentication object
     * @param role the role to check (e.g., "ROLE_ADMIN" or "ADMIN")
     * @return 
     */
    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role) ||
                                     authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Check if the authenticated user has the ADMIN role.
     *
     * @param authentication the Spring Security authentication object
     * @return true if the user is an admin, false otherwise
     */
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ADMIN");
    }

    /**
     * Get the username from the JWT token.
     *
     * @param authentication the Spring Security authentication object
     * @return the username, or null if not found
     */
    public String getUsername(Authentication authentication) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            return null;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        // Try to get username from preferred_username claim
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }

        // Fallback to email claim
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }

        return null;
    }
}
