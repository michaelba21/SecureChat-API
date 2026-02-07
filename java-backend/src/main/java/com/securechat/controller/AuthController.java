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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Call auth service with client IP and User-Agent
            AuthResponse response = authService.login(request, getClientIp(httpRequest), getUserAgent(httpRequest));
            return ResponseEntity.ok(response); // Return 200 OK with auth tokens
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body(AuthResponse.error(e.getMessage())); // Use static method for error response
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(request, getClientIp(httpRequest), getUserAgent(httpRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created for new user
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict for duplicate data
                .body(AuthResponse.error("Username or email already exists")); // Database constraint violation
        } catch (com.securechat.exception.DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT) 
                .body(AuthResponse.error(e.getMessage())); // Custom duplicate resource exception
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) 
                .body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) 
                .body(AuthResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody java.util.Map<String, String> payload, HttpServletRequest httpRequest) {
        String refreshToken = payload != null ? payload.get("refreshToken") : null; // Extract refresh token from payload
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400 if token missing
                    .body(AuthResponse.error("Refresh token is required"));
        }
        try {
            AuthResponse response = authService.refresh(refreshToken, getClientIp(httpRequest), getUserAgent(httpRequest));
            return ResponseEntity.ok(response); // 200 OK with new access token
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401 if refresh fails
                    .body(AuthResponse.error(e.getMessage()));
        }
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