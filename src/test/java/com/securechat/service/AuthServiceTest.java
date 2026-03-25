package com.securechat.service;

import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.LoginRequest;
import com.securechat.entity.RefreshToken;
import com.securechat.entity.User;
import com.securechat.exception.InvalidCredentialsException;
import com.securechat.exception.UserNotFoundException;
import com.securechat.exception.ValidationException;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")  // Descriptive test class name
class AuthServiceTest {
    // This test class validates the AuthService which handles user authentication (e.g.registration, and token management)
    
    @Mock
    private UserRepository userRepository;  // Repository for user data access

    @Mock
    private RefreshTokenService refreshTokenService;  // Service for refresh token management

    @InjectMocks
    private AuthService authService;  // Service under test with mocked dependencies

    // JWT configuration constants (matching production configuration)
    private final String secret = "0123456789_0123456789_0123456789_0123456789"; // 44 chars ==> >256 bits
    private final long expirationMs = 3_600_000L; // 1 hour in milliseconds
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);  // BCrypt with cost factor 12

    // HTTP request context constants
    private static final String IP_ADDRESS = "127.0.0.1";  
    private static final String USER_AGENT = "Mozilla/5.0"; 

    @BeforeEach
    void setup() {
        // Inject JWT configuration into the AuthService using reflection
        // This simulates @Value injection in production
        ReflectionTestUtils.setField(authService, "jwtSecret", secret);
        ReflectionTestUtils.setField(authService, "jwtExpiration", expirationMs);
    }

    // Helper method to create AuthRequest DTO for testing
    private AuthRequest makeAuthRequest(String username, String email, String password) {
        AuthRequest req = new AuthRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    // Helper method to create LoginRequest DTO for testing
    private LoginRequest makeLoginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    // Helper method to create a fully configured active User entity
    private User makeActiveUser(UUID id, String username, String email, String rawPassword) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setIsActive(true);  // User account is active
        u.setPasswordHash(encoder.encode(rawPassword));  // Hash the password
        Set<User.UserRole> roles = new HashSet<>();
        roles.add(User.UserRole.ROLE_USER);  // Default role for regular users
        u.setRoles(roles);
        return u;
    }

    @Nested
    @DisplayName("register")  // Nested test class for registration functionality
    class RegisterTests {

        @Test
        @DisplayName("should register user and return AuthResponse with tokens")
        void register_success() {
            // Tests successful user registration with all valid inputs
            AuthRequest req = makeAuthRequest("alice", "alice@example.com", "p@ssw0rd");

            // Mock repository to return empty (no existing users with same username/email)
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

            UUID userId = UUID.randomUUID();
            User savedUser = new User();
            savedUser.setId(userId);
            savedUser.setUsername("alice");
            savedUser.setEmail("alice@example.com");
            savedUser.setIsActive(true);
            Set<User.UserRole> roles = new HashSet<>();
            roles.add(User.UserRole.ROLE_USER);
            savedUser.setRoles(roles);

            // Mock save operation to assign ID to the user
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(userId);  // Simulate database ID generation
                return u;
            });

            // Mock refresh token creation
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("dummy-refresh-token");
            when(refreshTokenService.createToken(any(User.class), eq(IP_ADDRESS), eq(USER_AGENT)))
                    .thenReturn(refreshToken);

            // Execute registration
            AuthResponse resp = authService.register(req, IP_ADDRESS, USER_AGENT);

            // Verify response contains all expected data
            assertNotNull(resp);
            assertNotNull(resp.getToken());  // JWT access token
            assertNotNull(resp.getRefreshToken());  // Refresh token
            assertEquals("Registration successful", resp.getMessage());
            assertEquals("alice", resp.getUserInfo().getUsername());
            assertEquals("alice@example.com", resp.getUserInfo().getEmail());

            // Verify all expected interactions occurred
            verify(userRepository).findByUsername("alice");
            verify(userRepository).findByEmail("alice@example.com");
            verify(userRepository).save(any(User.class));
            verify(refreshTokenService).createToken(any(User.class), eq(IP_ADDRESS), eq(USER_AGENT));
        }

        @Test
        @DisplayName("should fail when email is blank")
        void register_blankEmail() {
            // Tests validation: blank email should fail
            AuthRequest req = makeAuthRequest("bob", " ", "pw");
            assertThrows(ValidationException.class, () -> authService.register(req, IP_ADDRESS, USER_AGENT));
            verify(userRepository, never()).save(any());  // No save should occur
        }

        @Test
        @DisplayName("should fail when email format is invalid")
        void register_invalidEmail() {
            // Tests validation: invalid email format should fail
            AuthRequest req = makeAuthRequest("bob", "invalid-email", "pw");
            assertThrows(ValidationException.class, () -> authService.register(req, IP_ADDRESS, USER_AGENT));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fail when username already exists")
        void register_usernameExists() {
            // Tests duplicate username detection
            AuthRequest req = makeAuthRequest("carol", "carol@example.com", "pw");
            when(userRepository.findByUsername("carol")).thenReturn(Optional.of(new User()));  // User exists
            assertThrows(RuntimeException.class, () -> authService.register(req, IP_ADDRESS, USER_AGENT));
            verify(userRepository, never()).save(any());  // Should not save duplicate
        }

        @Test
        @DisplayName("should fail when email already exists")
        void register_emailExists() {
            // Tests duplicate email detection
            AuthRequest req = makeAuthRequest("dave", "dave@example.com", "pw");
            when(userRepository.findByUsername("dave")).thenReturn(Optional.empty());  // Username available
            when(userRepository.findByEmail("dave@example.com")).thenReturn(Optional.of(new User()));  // Email taken
            assertThrows(RuntimeException.class, () -> authService.register(req, IP_ADDRESS, USER_AGENT));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")  // Nested test class for login functionality
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return AuthResponse")
        void login_success() {
            // Tests successful login with correct credentials
            String username = "erin";
            String email = "erin@example.com";
            String rawPassword = "secret";
            UUID id = UUID.randomUUID();
            User user = makeActiveUser(id, username, email, rawPassword);

            // Mock user lookup by email
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);  // For last login update

            // Mock refresh token creation
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("dummy-refresh-token");
            when(refreshTokenService.createToken(eq(user), eq(IP_ADDRESS), eq(USER_AGENT)))
                    .thenReturn(refreshToken);

            LoginRequest req = makeLoginRequest(email, rawPassword);

            // Execute login
            AuthResponse resp = authService.login(req, IP_ADDRESS, USER_AGENT);

            // Verify successful response
            assertNotNull(resp);
            assertNotNull(resp.getToken());
            assertNotNull(resp.getRefreshToken());
            assertEquals("Login successful", resp.getMessage());
            assertEquals(username, resp.getUserInfo().getUsername());
            assertEquals(email, resp.getUserInfo().getEmail());

            // Verify expected interactions
            verify(userRepository).findByEmail(email);
            verify(userRepository).save(user);  // Should update last login timestamp
            verify(refreshTokenService).createToken(eq(user), eq(IP_ADDRESS), eq(USER_AGENT));
        }

        @Test
        @DisplayName("should fail when email is blank")
        void login_blankEmail() {
            // Tests validation: blank email should fail
            LoginRequest req = makeLoginRequest(" ", "pw");
            assertThrows(ValidationException.class, () -> authService.login(req, IP_ADDRESS, USER_AGENT));
        }

        @Test
        @DisplayName("should fail when user not found")
        void login_userNotFound() {
            // Tests non-existent user
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
            LoginRequest req = makeLoginRequest("ghost@example.com", "pw");
            assertThrows(UserNotFoundException.class, () -> authService.login(req, IP_ADDRESS, USER_AGENT));
        }

        @Test
        @DisplayName("should fail when password is invalid")
        void login_invalidPassword() {
            // Tests incorrect password
            String email = "harry@example.com";
            UUID userId = UUID.randomUUID();
            User user = makeActiveUser(userId, "harry", email, "correct");
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            LoginRequest req = makeLoginRequest(email, "wrong");  // Wrong password
            assertThrows(InvalidCredentialsException.class, () -> authService.login(req, IP_ADDRESS, USER_AGENT));
        }

        @Test
        @DisplayName("should fail when user is inactive")
        void login_inactiveUser() {
            // Tests login attempt for deactivated/blocked account
            String email = "ian@example.com";
            UUID userId = UUID.randomUUID();
            User user = makeActiveUser(userId, "ian", email, "pw");
            user.setIsActive(false);  // Account is inactive

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            LoginRequest req = makeLoginRequest(email, "pw");
            assertThrows(InvalidCredentialsException.class, () -> authService.login(req, IP_ADDRESS, USER_AGENT));
        }
    }

    @Nested
    @DisplayName("refresh")  // Nested test class for token refresh functionality
    class RefreshTests {

        @Test
        @DisplayName("should issue new access token for valid refresh token")
        void refresh_success() {
            // Tests successful token refresh
            UUID userId = UUID.randomUUID();
            User user = makeActiveUser(userId, "jack", "jack@example.com", "pw");

            RefreshToken validToken = new RefreshToken();
            validToken.setToken("valid-refresh-token");
            validToken.setUser(user);

            // Mock valid token validation
            when(refreshTokenService.validateAndRefresh("valid-refresh-token"))
                    .thenReturn(Optional.of(validToken));

            // Execute refresh
            AuthResponse resp = authService.refresh("valid-refresh-token", IP_ADDRESS, USER_AGENT);

            // Verify new tokens issued
            assertNotNull(resp);
            assertNotNull(resp.getToken());  // New access token
            assertEquals("valid-refresh-token", resp.getRefreshToken());  // Same refresh token (rotated internally)
            assertEquals("Token refreshed", resp.getMessage());
            assertEquals("jack", resp.getUserInfo().getUsername());
        }

        @Test
        @DisplayName("should fail for invalid or expired refresh token")
        void refresh_invalidToken() {
            // Tests invalid/expired refresh token
            when(refreshTokenService.validateAndRefresh("bad-token"))
                    .thenReturn(Optional.empty());  // Token is invalid

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.refresh("bad-token", IP_ADDRESS, USER_AGENT));
        }

        @Test
        @DisplayName("should fail when user is inactive")
        void refresh_inactiveUser() {
            // Tests refresh attempt for inactive user account
            UUID userId = UUID.randomUUID();
            User user = makeActiveUser(userId, "kate", "kate@example.com", "pw");
            user.setIsActive(false);  // Account deactivated

            RefreshToken token = new RefreshToken();
            token.setToken("some-token");
            token.setUser(user);

            when(refreshTokenService.validateAndRefresh("some-token"))
                    .thenReturn(Optional.of(token));  // Token is valid but user inactive

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.refresh("some-token", IP_ADDRESS, USER_AGENT));
        }
    }
}