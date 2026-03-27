package com.securechat.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller to analyze OAuth 2.0 JWT tokens and authentication.
 * This helps diagnose issues with principal.getName() and role mapping.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /**
     * Get detailed information about the current JWT token and authentication.
     * This endpoint helps diagnose OAuth 2.0 integration issues.
     */
    @GetMapping("/token-info")
    public Map<String, Object> getTokenInfo(Authentication authentication) {
        Map<String, Object> info = new HashMap<>();

        if (authentication == null) {
            info.put("error", "No authentication provided");
            info.put("authenticationType", "null");
            info.put("principalName", "null");
            return info;
        }

        info.put("authenticationType", authentication.getClass().getName());
        info.put("principalName", authentication.getName());
        info.put("isAuthenticated", authentication.isAuthenticated());
        info.put("authorities", authentication.getAuthorities());

        // If it's a JWT token, extract detailed information
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();

            info.put("tokenType", "JWT");
            info.put("subject", jwt.getSubject());
            info.put("issuer", jwt.getIssuer());
            info.put("audience", jwt.getAudience());
            info.put("issuedAt", jwt.getIssuedAt());
            info.put("expiresAt", jwt.getExpiresAt());
            info.put("notBefore", jwt.getNotBefore());

            // Get all claims
            Map<String, Object> claims = jwt.getClaims();
            info.put("claims", claims);

            // Check for Keycloak-specific claims
            if (claims.containsKey("resource_access")) {
                info.put("hasResourceAccess", true);
                info.put("resourceAccess", claims.get("resource_access"));
            } else {
                info.put("hasResourceAccess", false);
            }

            if (claims.containsKey("realm_access")) {
                info.put("hasRealmAccess", true);
                info.put("realmAccess", claims.get("realm_access"));
            } else {
                info.put("hasRealmAccess", false);
            }

            if (claims.containsKey("preferred_username")) {
                info.put("preferredUsername", claims.get("preferred_username"));
            }

            if (claims.containsKey("email")) {
                info.put("email", claims.get("email"));
            }

            if (claims.containsKey("given_name")) {
                info.put("givenName", claims.get("given_name"));
            }

            if (claims.containsKey("family_name")) {
                info.put("familyName", claims.get("family_name"));
            }
        } else {
            info.put("tokenType", "Not JWT");
            info.put("message", "Authentication is not a JWT token");
        }

        return info;
    }

    /**
     * Health check endpoint to verify the application is running.
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "Debug endpoint is accessible");
        return status;
    }
}
