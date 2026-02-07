

package com.securechat.controller;

import com.securechat.dto.ChatRoomDTO;
import com.securechat.entity.User;
import com.securechat.exception.UnauthorizedException;
import com.securechat.service.ChatRoomService;
import com.securechat.service.UserService;
import com.securechat.util.AuthUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito for dependency injection and mocking
@MockitoSettings(strictness = Strictness.LENIENT)  
@DisplayName("UserController Tests")  // Descriptive name for the test class
class UserControllerTest {

    @Mock
    private UserService userService;  // Mock for business logic layer (user operations)

    @Mock
    private ChatRoomService chatRoomService;  // Mock for chat room related operations

    @Mock
    private AuthUtil authUtil;  // Mock for authentication utility methods

    @InjectMocks
    private UserController userController;  // Controller under test with injected mocks

    private UUID userId;  // Test user ID for consistent testing
    private User testUser;  

    @BeforeEach
    void setUp() {
        // Initialize test data for each test
        userId = UUID.randomUUID();  // Generate unique ID for test isolation
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setIsActive(true);  // Active user state
        testUser.setRoles(Set.of(User.UserRole.ROLE_USER));  // Single role assignment

        // Stub authUtil for all tests - lenient to avoid unnecessary stubbing warnings
        // This setup assumes authUtil.getCurrentUserId() will be used in many tests
        lenient().when(authUtil.getCurrentUserId(any(Authentication.class))).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        // Clear SecurityContext after each test to prevent test pollution
        // Important for tests that modify SecurityContextHolder
        SecurityContextHolder.clearContext();
    }

    // Helper method to set up authenticated context for tests
    // Creates mock authentication and sets it in SecurityContextHolder
    private void setAuthenticatedContext() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.isAuthenticated()).thenReturn(true);  // Mock authenticated state
        lenient().when(auth.getName()).thenReturn(userId.toString());  // Mock user identifier

        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);  // Link auth to context
        SecurityContextHolder.setContext(context);  // Set thread-local security context
    }

    // === GET /users - getAllUsers() ===
    // Tests for retrieving all users endpoint

    @Nested  // Nested test class for better organization
    @DisplayName("GET /users - getAllUsers()")
    class GetAllUsersTest {

        @Test
        void shouldReturnAllUsers() {
            // Positive test: service returns user list
            List<User> users = List.of(testUser);  // Single user in list
            when(userService.getAllUsers()).thenReturn(users);  // Mock service response

            ResponseEntity<List<User>> response = userController.getAllUsers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);  
            assertThat(response.getBody()).hasSize(1).contains(testUser);  // Verify response content
            verify(userService).getAllUsers();  // Verify service was called
        }

        @Test
        void shouldReturnBadRequestOnException() {
            // Error handling test: service throws exception
            when(userService.getAllUsers()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<List<User>> response = userController.getAllUsers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);  // Verify HTTP 400
            assertThat(response.getBody()).isNull();  // No body on error
        }
    }

    // === GET /{id} - getUser() ===
    // Tests for retrieving single user by ID

    @Nested
    @DisplayName("GET /{id} - getUser()")
    class GetUserTest {

        @Test
        void shouldReturnUserWhenFound() {
            // Success case: user exists
            when(userService.getUserById(userId)).thenReturn(testUser);  // Mock found user

            ResponseEntity<User> response = userController.getUser(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);  // HTTP 200
            assertThat(response.getBody()).isSameAs(testUser); 
        }

        @Test
        void shouldReturnNotFoundWhenUserNotFound() {
            // Edge case: user doesn't exist
            when(userService.getUserById(userId)).thenReturn(null);  // Mock null return

            ResponseEntity<User> response = userController.getUser(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);  // HTTP 404
        }
    }

    // === PUT /{id} - updateUser() ===
    // Tests for full user update

    @Nested
    @DisplayName("PUT /{id} - updateUser()")
    class UpdateUserTest {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")  // Spring Security test annotation
        void shouldUpdateAndReturnUser() {
            User updates = new User();
            updates.setUsername("newuser");  // Only username being updated

            User updated = new User();
            updated.setId(userId);
            updated.setUsername("newuser");  // Expected updated user

            when(userService.updateUser(eq(userId), any(User.class))).thenReturn(updated);

            ResponseEntity<User> response = userController.updateUser(userId, updates);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUsername()).isEqualTo("newuser");  // Verify update
            verify(userService).updateUser(userId, updates);  // Verify correct parameters
        }

        @Test
        void shouldReturnBadRequestOnInvalidData() {
            // Error case: invalid update data
            when(userService.updateUser(eq(userId), any(User.class)))
                    .thenThrow(new IllegalArgumentException("Invalid data"));

            ResponseEntity<User> response = userController.updateUser(userId, new User());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);  // HTTP 400
        }
    }

    // === PATCH /{id} - partialUpdateUser() ===
    // Tests for partial user updates

    @Nested
    @DisplayName("PATCH /{id} - partialUpdateUser()")
    class PartialUpdateUserTest {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        void shouldPartiallyUpdateUser() {
            Map<String, Object> updates = Map.of("username", "updated");  // Partial update map

            User updated = new User();
            updated.setId(userId);
            updated.setUsername("updated");

            when(userService.partialUpdateUser(eq(userId), any(Map.class))).thenReturn(updated);

            ResponseEntity<User> response = userController.partialUpdateUser(userId, updates);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUsername()).isEqualTo("updated");
        }

        @Test
        void shouldReturnBadRequestOnInvalidUpdates() {
            // Error case: invalid partial update
            when(userService.partialUpdateUser(eq(userId), any(Map.class)))
                    .thenThrow(new IllegalArgumentException("Invalid"));

            ResponseEntity<User> response = userController.partialUpdateUser(userId, Map.of());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // === GET /me and /profile - getMe() ===
    // Tests for current user profile retrieval

    @Nested
    @DisplayName("GET /me and /profile - getMe()")
    class GetMeTest {

        @Test
        void getMe_shouldReturnCurrentUser() {
            // Manual SecurityContext setup for unit test (no Spring context)
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(userId.toString());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);  // Set thread-local context

            when(userService.getUserById(userId)).thenReturn(testUser);

            ResponseEntity<User> response = userController.getMe(auth);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(testUser);  // Verify correct user returned
        }

        @Test
        void getMe_shouldReturnUnauthorizedWhenAuthenticationNull() {
            // Edge case: no authentication provided
            assertThrows(UnauthorizedException.class, () -> userController.getMe(null));
        }

        @Test
        void getMe_shouldReturnUnauthorizedWhenNameNull() {
            // Edge case: authentication present but name is null
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(null);  // Null name should trigger exception

            assertThrows(UnauthorizedException.class, () -> userController.getMe(auth));
        }
    }

    // === PUT /profile - updateProfile() ===
    // Tests for current user profile updates

    @Nested
    @DisplayName("PUT /profile - updateProfile()")
    class UpdateProfileTest {

        @Test
        void shouldUpdateProfileSuccessfully() {
            // Manual SecurityContext setup similar to getMe test
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(userId.toString());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            User updates = new User();
            updates.setUsername("updated");

            User updated = new User();
            updated.setId(userId);
            updated.setUsername("updated");

            when(userService.updateUser(eq(userId), any(User.class))).thenReturn(updated);

            ResponseEntity<User> response = userController.updateProfile(auth, updates);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUsername()).isEqualTo("updated");
        }

        @Test
        void shouldReturnUnauthorizedWhenAuthenticationNull() {
            // Authorization check: null authentication
            assertThrows(UnauthorizedException.class, () -> 
                userController.updateProfile(null, new User()));
        }

        @Test
        void shouldReturnBadRequestOnInvalidData() {
            // Error case: service rejects update data
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(userId.toString());

            when(userService.updateUser(eq(userId), any(User.class)))
                    .thenThrow(new IllegalArgumentException("Invalid"));

            ResponseEntity<User> response = userController.updateProfile(auth, new User());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // === GET /{id}/chatrooms/created - getCreatedChatrooms() ===
    // Tests for retrieving chatrooms created by a user

    @Nested
    @DisplayName("GET /{id}/chatrooms/created")
    class GetCreatedChatroomsTest {

        @Test
        void shouldReturnCreatedChatrooms() {
            List<ChatRoomDTO> rooms = List.of(new ChatRoomDTO());  // Single empty DTO
            when(chatRoomService.getChatroomsByCreator(userId)).thenReturn(rooms);

            ResponseEntity<List<ChatRoomDTO>> response = userController.getCreatedChatrooms(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);  // Verify list size
        }

        @Test
        void shouldReturnInternalServerErrorOnException() {
            // Error case: service throws runtime exception
            when(chatRoomService.getChatroomsByCreator(userId))
                    .thenThrow(new RuntimeException("Service error"));

            ResponseEntity<List<ChatRoomDTO>> response = userController.getCreatedChatrooms(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);  // HTTP 500
        }
    }
}