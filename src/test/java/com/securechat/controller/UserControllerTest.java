
package com.securechat.controller;

import com.securechat.dto.ChatRoomDTO;
import com.securechat.entity.User;
import com.securechat.exception.UnauthorizedException;
import com.securechat.service.ChatRoomService;
import com.securechat.service.UserService;
import com.securechat.service.UserSyncService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private UserSyncService userSyncService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private User testUser;
    private Jwt jwt;
    private JwtAuthenticationToken jwtAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setIsActive(true);
        testUser.setRoles(Set.of(User.UserRole.ROLE_USER));

        // Mock JWT + JwtAuthenticationToken
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());

        jwtAuth = mock(JwtAuthenticationToken.class);
        when(jwtAuth.getToken()).thenReturn(jwt);
        when(jwtAuth.isAuthenticated()).thenReturn(true);
    }

    // ==================== GET /me and /profile ====================

    @Nested
    @DisplayName("GET /me and /profile - getMe()")
    class GetMeTest {

        @Test
        void getMe_shouldReturnCurrentUser() {
            when(userSyncService.getOrCreateUser(jwt)).thenReturn(testUser);

            ResponseEntity<User> response = userController.getMe(jwtAuth);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(testUser);
            verify(userSyncService).getOrCreateUser(jwt);
        }

        @Test
        void getMe_shouldReturnUnauthorizedWhenAuthenticationNull() {
            ResponseEntity<User> response = userController.getMe(null);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void getMe_shouldReturnUnauthorizedWhenNameNull() {
            JwtAuthenticationToken invalidAuth = mock(JwtAuthenticationToken.class);
            when(invalidAuth.getToken()).thenReturn(jwt);
            when(jwt.getSubject()).thenReturn(null);

            ResponseEntity<User> response = userController.getMe(invalidAuth);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ==================== PUT /profile - updateProfile() ====================

    @Nested
    @DisplayName("PUT /profile - updateProfile()")
    class UpdateProfileTest {

        @Test
        void shouldUpdateProfileSuccessfully() {
            User updates = new User();
            updates.setUsername("updateduser");

            User updatedUser = new User();
            updatedUser.setId(userId);
            updatedUser.setUsername("updateduser");

            when(userSyncService.getOrCreateUser(jwt)).thenReturn(testUser);
            when(userService.updateUser(eq(userId), any(User.class))).thenReturn(updatedUser);

            ResponseEntity<User> response = userController.updateProfile(jwtAuth, updates);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUsername()).isEqualTo("updateduser");
            verify(userService).updateUser(userId, updates);
        }

        @Test
        void shouldReturnBadRequestOnInvalidData() {
            User updates = new User();

            when(userService.updateUser(eq(userId), any(User.class)))
                    .thenThrow(new IllegalArgumentException("Invalid data"));

            ResponseEntity<User> response = userController.updateProfile(jwtAuth, updates);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ==================== Other Tests ====================

    @Nested
    @DisplayName("GET /users - getAllUsers()")
    class GetAllUsersTest {

        @Test
        void shouldReturnAllUsers() {
            List<User> users = List.of(testUser);
            when(userService.getAllUsers()).thenReturn(users);

            ResponseEntity<List<User>> response = userController.getAllUsers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsExactly(testUser);
        }
    }

    @Nested
    @DisplayName("GET /{id} - getUser()")
    class GetUserTest {

        @Test
        void shouldReturnUserWhenFound() {
            when(userService.getUserById(userId)).thenReturn(testUser);

            ResponseEntity<User> response = userController.getUser(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(testUser);
        }

        @Test
        void shouldReturnNotFoundWhenUserNotFound() {
            when(userService.getUserById(userId)).thenReturn(null);

            ResponseEntity<User> response = userController.getUser(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /{id}/chatrooms/created")
    class GetCreatedChatroomsTest {

        @Test
        void shouldReturnCreatedChatrooms() {
            List<ChatRoomDTO> rooms = List.of(new ChatRoomDTO());
            when(chatRoomService.getChatroomsByCreator(userId)).thenReturn(rooms);

            ResponseEntity<List<ChatRoomDTO>> response = userController.getCreatedChatrooms(userId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }
}