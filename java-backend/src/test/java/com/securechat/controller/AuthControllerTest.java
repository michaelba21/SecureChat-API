
package com.securechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // JSON serialization/deserialization
import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.LoginRequest;
import com.securechat.exception.DuplicateResourceException;
import com.securechat.service.AuthService;
import com.securechat.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // Test slice for MVC layer only
import org.springframework.boot.test.mock.mockito.MockBean; // Mock Spring beans
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest; // Mock HTTP request
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc; // MVC test utility

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class) // Test only AuthController, not full application
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simpler testing
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc; 

        @Autowired
        private ObjectMapper objectMapper; // JSON mapper for request/response

        @MockBean
        private AuthService authService; // Mock service layer

        @MockBean
        private AuthUtil authUtil; 

        private UUID testUserId; // Test user ID
        private Authentication mockAuth; // Mock authentication
        private MockHttpServletRequest mockRequest; 

        @BeforeEach
        void setUp() {
                testUserId = UUID.randomUUID(); // Generate random user ID for tests

                // Mock authentication object
                mockAuth = mock(Authentication.class);
                when(mockAuth.isAuthenticated()).thenReturn(true); // Always authenticated
                when(mockAuth.getName()).thenReturn(testUserId.toString()); // Return user ID as name

                // Mock request with headers
                mockRequest = new MockHttpServletRequest();
                mockRequest.addHeader("X-Forwarded-For", "192.168.1.1"); // Client IP
                mockRequest.addHeader("User-Agent", "Test-Agent"); // Browser/agent
        }

        // === Login Tests ===
        // Test group for login endpoint functionality

        @Nested
        class LoginTests {

                @Test
                void login_success_returnsOkWithToken() throws Exception {
                        // Arrange: create login request
                        LoginRequest request = new LoginRequest("test@example.com", "Password123");

                        // Mock successful response from service
                        AuthResponse response = AuthResponse.success(
                                        "jwt-token", "refresh-token", "Login successful",
                                        "testuser", "test@example.com", testUserId.toString());

                        // Configure mock service to return response
                        when(authService.login(any(LoginRequest.class), eq("192.168.1.1"), eq("Test-Agent")))
                                        .thenReturn(response);

                        // Act & Assert: perform POST request and verify response
                        mockMvc.perform(post("/api/auth/login")
                                        .principal(mockAuth) // Set authentication principal
                                        .header("X-Forwarded-For", "192.168.1.1") // IP header
                                        .header("User-Agent", "Test-Agent") 
                                        .contentType(MediaType.APPLICATION_JSON) 
                                        .content(objectMapper.writeValueAsString(request))) // Request body as JSON
                                        .andExpect(status().isOk()) 
                                        .andExpect(jsonPath("$.token").value("jwt-token")) // Verify token
                                        .andExpect(jsonPath("$.refreshToken").value("refresh-token")) // Verify refresh token
                                        .andExpect(jsonPath("$.message").value("Login successful")) 
                                        .andExpect(jsonPath("$.userInfo.username").value("testuser")) // Verify username
                                        .andExpect(jsonPath("$.userInfo.email").value("test@example.com")) 
                                        .andExpect(jsonPath("$.userInfo.userId").value(testUserId.toString())); // Verify user ID
                }

                @Test
                void login_invalidCredentials_returnsUnauthorized() throws Exception {
                        // Arrange: invalid credentials
                        LoginRequest request = new LoginRequest("wrong@example.com", "wrongpass");

                        // Mock service throwing exception
                        when(authService.login(any(LoginRequest.class), anyString(), anyString()))
                                        .thenThrow(new RuntimeException("Invalid credentials"));

                        // Act & Assert: should return 401 Unauthorized
                        mockMvc.perform(post("/api/auth/login")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized()) // HTTP 401
                                        .andExpect(jsonPath("$.error").value("Invalid credentials")); // Error message
                }
        }

        // === Register Tests ===
        // Test group for registration endpoint

        @Nested
        class RegisterTests {

                @Test
                void register_success_returnsCreated() throws Exception {
                        // Arrange: registration request
                        AuthRequest request = new AuthRequest("newuser", "Password123", "new@test.com");

                        // Mock successful registration response
                        AuthResponse response = AuthResponse.success(
                                        "jwt-token", "refresh-token", "Registration successful",
                                        "newuser", "new@test.com", UUID.randomUUID().toString());

                        // Configure mock service
                        when(authService.register(any(AuthRequest.class), eq("192.168.1.1"), eq("Test-Agent")))
                                        .thenReturn(response);

                        // Act & Assert: should return 201 Created
                        mockMvc.perform(post("/api/auth/register")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated()) // HTTP 201
                                        .andExpect(jsonPath("$.token").value("jwt-token"))
                                        .andExpect(jsonPath("$.message").value("Registration successful"))
                                        .andExpect(jsonPath("$.userInfo.username").value("newuser"));
                }

                @Test
                void register_duplicateResourceException_returnsConflict() throws Exception {
                        // Arrange: duplicate resource (email/username already exists)
                        AuthRequest request = new AuthRequest("testuser", "Password123", "taken@test.com");

                        // Mock duplicate resource exception
                        when(authService.register(any(AuthRequest.class), anyString(), anyString()))
                                        .thenThrow(new DuplicateResourceException("Email already in use"));

                        // Act & Assert: should return 409 Conflict
                        mockMvc.perform(post("/api/auth/register")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isConflict()) // HTTP 409
                                        .andExpect(jsonPath("$.error").value("Email already in use"));
                }

                @Test
                void register_generalException_returnsBadRequest() throws Exception {
                        // Arrange: general validation error
                        AuthRequest request = new AuthRequest("testuser", "Password123", "email@test.com");

                        // Mock general exception
                        when(authService.register(any(AuthRequest.class), anyString(), anyString()))
                                        .thenThrow(new RuntimeException("Validation failed"));

                        // Act & Assert: should return 400 Bad Request
                        mockMvc.perform(post("/api/auth/register")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest()) // HTTP 400
                                        .andExpect(jsonPath("$.error").value("Validation failed"));
                }
        }

        // === Refresh Tests ===
        // Test group for token refresh endpoint

        @Nested
        class RefreshTests {

                @Test
                void refresh_validToken_returnsOk() throws Exception {
                        // Arrange: valid refresh token
                        AuthResponse response = AuthResponse.success(
                                        "new-jwt", "new-refresh", "Token refreshed",
                                        "user", "user@test.com", UUID.randomUUID().toString());

                        // Mock successful token refresh
                        when(authService.refresh(eq("valid-refresh-token"), eq("192.168.1.1"), eq("Test-Agent")))
                                        .thenReturn(response);

                        // Act & Assert: should return 200 OK with new tokens
                        mockMvc.perform(post("/api/auth/refresh")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"refreshToken\":\"valid-refresh-token\"}")) // JSON payload
                                        .andExpect(status().isOk()) // HTTP 200
                                        .andExpect(jsonPath("$.token").value("new-jwt")) 
                                        .andExpect(jsonPath("$.refreshToken").value("new-refresh")); // New refresh token
                }

                @Test
                void refresh_missingRefreshToken_returnsBadRequest() throws Exception {
                        // Act & Assert: empty request should return 400
                        mockMvc.perform(post("/api/auth/refresh")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}")) // Empty JSON object
                                        .andExpect(status().isBadRequest()) // HTTP 400
                                        .andExpect(jsonPath("$.error").value("Refresh token is required"));
                }

                @Test
                void refresh_invalidToken_returnsUnauthorized() throws Exception {
                        // Arrange: invalid refresh token
                        when(authService.refresh(eq("invalid-refresh-token"), anyString(), anyString()))
                                        .thenThrow(new RuntimeException("Invalid refresh token"));

                        // Act & Assert: should return 401 Unauthorized
                        mockMvc.perform(post("/api/auth/refresh")
                                        .principal(mockAuth)
                                        .header("X-Forwarded-For", "192.168.1.1")
                                        .header("User-Agent", "Test-Agent")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"refreshToken\":\"invalid-refresh-token\"}"))
                                        .andExpect(status().isUnauthorized()) // HTTP 401
                                        .andExpect(jsonPath("$.error").value("Invalid refresh token"));
                }
        }
}