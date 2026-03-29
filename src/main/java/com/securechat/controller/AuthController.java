package com.securechat.controller;
import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.LoginRequest;
import com.securechat.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth") // Base path for authentication endpoints
@CrossOrigin(origins = "*")  // For local development testing - allow all origins
public class AuthController {

    @Autowired
    private AuthService authService; // Inject authentication service

    /*
     * LOCAL REGISTRATION & LOGIN DISABLED
     * 
     * Switch to Keycloak-only authentication for single source of truth.
     * All users must now register/login via Keycloak to ensure UUID consistency
     * and prevent 401/403 errors from UUID mismatches.
     */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
            AuthResponse.error("⚠️  Local login disabled. Please use Keycloak OAuth2 flow. " +
                "Token endpoint: http://localhost:9090/realms/SecureChat/protocol/openid-connect/token")
        );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
            AuthResponse.error("⚠️  Local registration disabled. Please register via Keycloak admin console. " +
                "Admin URL: http://localhost:9090/admin/master/console/")
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody java.util.Map<String, String> payload, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
            AuthResponse.error("⚠️  Keycloak handles token refresh. Use Keycloak token endpoint with refresh_token grant type.")
        );
    }

    /**
     * Check authentication status - useful for debugging and verification.
     * Returns user info from the Keycloak JWT token.
     */
    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Object>> authStatus(
            org.springframework.security.oauth2.jwt.Jwt jwt,
            org.springframework.security.core.Authentication auth) {
        
        if (jwt == null) {
            return ResponseEntity.ok(java.util.Map.of(
                "authenticated", false,
                "message", "Not authenticated. Please login via Keycloak",
                "keycloakUrl", "http://localhost:9090/realms/SecureChat/protocol/openid-connect/auth"
            ));
        }
        
        return ResponseEntity.ok(java.util.Map.ofEntries(
            java.util.Map.entry("authenticated", true),
            java.util.Map.entry("userId", jwt.getSubject()),
            java.util.Map.entry("username", jwt.getClaimAsString("preferred_username")),
            java.util.Map.entry("email", jwt.getClaimAsString("email")),
            java.util.Map.entry("roles", jwt.getClaimAsStringList("roles")),
            java.util.Map.entry("expiresAt", jwt.getExpiresAt()),
            java.util.Map.entry("message", "✅ Valid Keycloak token")
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For"); // Check proxy header first
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim(); // Use first IP in chain (client's original IP)
        }
        return request.getRemoteAddr(); // Fallback to direct connection IP
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent"); 
        // Some clients (e.g., PowerShell Invoke-RestMethod) may omit User-Agent.
        // Keep refresh_tokens.user_agent non-null by providing a safe default.
        return (userAgent == null || userAgent.isBlank()) ? "unknown" : userAgent; // Default to "unknown" if missing
    }
}