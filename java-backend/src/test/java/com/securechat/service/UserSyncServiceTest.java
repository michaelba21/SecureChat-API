

package com.securechat.service;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito with JUnit 5
@DisplayName("UserSyncService - Tests") // Test class description
class UserSyncServiceTest {

    @Mock // Mock UserRepository for database operations
    private UserRepository userRepository;

    @Mock // Mock PasswordEncoder for hashing passwords
    private PasswordEncoder passwordEncoder;

    @Mock // Mock JWT token for authentication
    private Jwt jwt;

    @InjectMocks // Injects mocks into UserSyncService instance
    private UserSyncService userSyncService;

    @Captor // Captures User objects passed to save method
    private ArgumentCaptor<User> userCaptor;

    private UUID userId; // Test user ID

    @BeforeEach // Runs before each test method
    void setUp() {
        userId = UUID.randomUUID(); // Generate random user ID
        when(jwt.getSubject()).thenReturn(userId.toString()); // Mock JWT subject (user ID)
    }

    // ==================== Main Method Tests ====================
    @Nested // Groups tests for getOrCreateUser method
    @DisplayName("getOrCreateUser(Jwt)")
    class GetOrCreateUser {

        @Test
        @DisplayName("Should return existing user when found")
        void shouldReturnExistingUserWhenFound() {
            // Arrange: create existing user in database
            User existingUser = new User();
            existingUser.setId(userId);
            existingUser.setUsername("existinguser");
            existingUser.setEmail("existing@example.com");
            existingUser.setIsActive(true);
            existingUser.setRoles(new HashSet<>(Set.of(User.UserRole.ROLE_USER)));

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

            // Act: call service method
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: should return existing user without creating new one
            assertThat(result).isSameAs(existingUser); // Same object reference
            verify(userRepository).findById(userId); 
            verify(userRepository, never()).save(any(User.class)); // No save needed
            verify(passwordEncoder, never()).encode(anyString()); // No password encoding needed
        }

        @Test
        @DisplayName("Should throw exception when JWT subject is null")
        void shouldThrowExceptionWhenJwtSubjectIsNull() {
            // Arrange: null subject in JWT
            when(jwt.getSubject()).thenReturn(null);

            // Act & Assert: should throw IllegalArgumentException
            assertThatThrownBy(() -> userSyncService.getOrCreateUser(jwt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JWT subject claim is null or empty");
        }

        @Test
        @DisplayName("Should throw exception when JWT subject is empty")
        void shouldThrowExceptionWhenJwtSubjectIsEmpty() {
            // Arrange: empty subject (whitespace)
            when(jwt.getSubject()).thenReturn("   ");

            // Act & Assert: should throw IllegalArgumentException
            assertThatThrownBy(() -> userSyncService.getOrCreateUser(jwt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JWT subject claim is null or empty");
        }

        @Test
        @DisplayName("Should throw exception when JWT subject is invalid UUID")
        void shouldThrowExceptionWhenJwtSubjectIsInvalidUuid() {
            // Arrange: invalid UUID format
            when(jwt.getSubject()).thenReturn("invalid-uuid");

            // Act & Assert: should throw IllegalArgumentException (UUID parsing fails)
            assertThatThrownBy(() -> userSyncService.getOrCreateUser(jwt))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should create new user with preferred_username")
        void shouldCreateNewUserWithPreferredUsername() {
            // Arrange: JWT has preferred_username claim
            when(jwt.getClaimAsString("preferred_username")).thenReturn("myusername");
            when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty()); // User not found
            when(userRepository.findByUsername("myusername")).thenReturn(Optional.empty()); // Username available
            when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.empty()); // Email available
            when(passwordEncoder.encode(anyString())).thenReturn("encoded"); // Mock password encoding
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0)); // Return saved user

            // Act: create new user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: user created with correct properties
            assertThat(result.getUsername()).isEqualTo("myusername"); // Username from JWT
            assertThat(result.getEmail()).isEqualTo("myemail@example.com"); // Email from JWT
            assertThat(result.getId()).isEqualTo(userId); // ID from JWT subject
            assertThat(result.getIsActive()).isTrue(); // Active by default
            assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER); // Default role
            verify(userRepository).save(any(User.class)); // User saved
            verify(passwordEncoder).encode("keycloak:" + userId); // Password encoded with prefix
        }

        @Test
        @DisplayName("Should use email as username when preferred_username is null")
        void shouldUseEmailAsUsernameWhenPreferredUsernameIsNull() {
            // Arrange: no preferred_username, use email as fallback
            when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
            when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("myemail@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user with email as username
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: email used as username
            assertThat(result.getUsername()).isEqualTo("myemail@example.com");
            assertThat(result.getEmail()).isEqualTo("myemail@example.com");
        }

        @Test
        @DisplayName("Should use default username when both preferred_username and email are null")
        void shouldUseDefaultUsernameWhenBothAreNull() {
            // Arrange: both claims null, use generated defaults
            when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
            when(jwt.getClaimAsString("email")).thenReturn(null);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user-" + userId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(userId + "@keycloak.local")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user with generated defaults
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: generated username and email
            assertThat(result.getUsername()).isEqualTo("user-" + userId); // Generated username
            assertThat(result.getEmail()).isEqualTo(userId + "@keycloak.local"); // Generated email
        }

        @Test
        @DisplayName("Should use default username when both preferred_username and email are blank")
        void shouldUseDefaultUsernameWhenBothAreBlank() {
            // Arrange: blank claims (whitespace only)
            when(jwt.getClaimAsString("preferred_username")).thenReturn(" ");
            when(jwt.getClaimAsString("email")).thenReturn(" ");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user-" + userId)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(userId + "@keycloak.local")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user with blank claims
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: generated defaults used
            assertThat(result.getUsername()).isEqualTo("user-" + userId);
            assertThat(result.getEmail()).isEqualTo(userId + "@keycloak.local");
        }

        @Test
        @DisplayName("Should use default username when preferred_username is taken by another user")
        void shouldUseDefaultUsernameWhenPreferredUsernameIsTaken() {
            // Arrange: username already taken by different user
            User existingUser = new User();
            existingUser.setId(UUID.randomUUID()); // Different user ID
            existingUser.setUsername("testuser");

            when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
            when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser)); // Username taken
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user with taken username
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: generated username used instead
            assertThat(result.getUsername()).isEqualTo("user-" + userId); // Generated username
            assertThat(result.getEmail()).isEqualTo("user@example.com"); // Original email kept
        }

        @Test
        @DisplayName("Should use default email when email is taken by another user")
        void shouldUseDefaultEmailWhenEmailIsTaken() {
            // Arrange: email already taken by different user
            User existingUser = new User();
            existingUser.setId(UUID.randomUUID()); // Different user ID
            existingUser.setEmail("user@example.com");

            when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
            when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser)); // Email taken
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user with taken email
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: generated email used instead
            assertThat(result.getUsername()).isEqualTo("testuser"); // Original username kept
            assertThat(result.getEmail()).isEqualTo(userId + "@keycloak.local"); // Generated email
        }

        @Test
        @DisplayName("Should add ROLE_ADMIN when JWT has admin role in realm_access")
        void shouldAddAdminRoleWhenJwtHasAdminInRealmAccess() {
            // Arrange: JWT has "admin" role in realm_access
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", List.of("user", "admin")); // Contains "admin"

            when(jwt.getClaimAsString("preferred_username")).thenReturn("adminuser");
            when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
            when(jwt.getClaim("realm_access")).thenReturn(realmAccess); // Realm access with admin
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create admin user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: user has both USER and ADMIN roles
            assertThat(result.getRoles()).containsExactlyInAnyOrder(
                User.UserRole.ROLE_USER, User.UserRole.ROLE_ADMIN); // Both roles assigned
        }

        @Test
        @DisplayName("Should add ROLE_ADMIN when JWT has ROLE_ADMIN in realm_access")
        void shouldAddAdminRoleWhenJwtHasRoleAdminInRealmAccess() {
            // Arrange: JWT has "ROLE_ADMIN" role (Spring Security format)
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", List.of("user", "ROLE_ADMIN")); // Contains "ROLE_ADMIN"

            when(jwt.getClaimAsString("preferred_username")).thenReturn("adminuser");
            when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
            when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create admin user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: ADMIN role added (recognizes ROLE_ADMIN)
            assertThat(result.getRoles()).containsExactlyInAnyOrder(
                User.UserRole.ROLE_USER, User.UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should add ROLE_ADMIN when JWT has ADMIN in realm_access")
        void shouldAddAdminRoleWhenJwtHasAdminInRealmAccessUpperCase() {
            // Arrange: JWT has "ADMIN" role (uppercase)
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", List.of("user", "ADMIN")); // Contains "ADMIN"

            when(jwt.getClaimAsString("preferred_username")).thenReturn("adminuser");
            when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
            when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create admin user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: ADMIN role added (recognizes uppercase ADMIN)
            assertThat(result.getRoles()).containsExactlyInAnyOrder(
                User.UserRole.ROLE_USER, User.UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should add ROLE_ADMIN when JWT has admin role in resource_access")
        void shouldAddAdminRoleWhenJwtHasAdminInResourceAccess() {
            // Arrange: JWT has admin role in resource_access (client-specific)
            Map<String, Object> clientAccess = new HashMap<>();
            clientAccess.put("roles", List.of("user", "admin")); // Client roles

            Map<String, Object> resourceAccess = new HashMap<>();
            resourceAccess.put("securechat-backend", clientAccess); // Our application client

            when(jwt.getSubject()).thenReturn(userId.toString());
            when(jwt.getClaimAsString("preferred_username")).thenReturn("adminuser");
            when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");

            // FIX 1: Added missing realm_access stub - needed for hasRole method
            when(jwt.getClaim("realm_access")).thenReturn(null); // No realm roles

            when(jwt.getClaim("resource_access")).thenReturn(resourceAccess); // Client roles
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create admin user from client roles
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: ADMIN role added from client roles
            assertThat(result.getRoles()).containsExactlyInAnyOrder(
                User.UserRole.ROLE_USER, User.UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should not add ROLE_ADMIN when JWT has no admin role")
        void shouldNotAddAdminRoleWhenJwtHasNoAdminRole() {
            // Arrange: JWT has only USER role
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", List.of("user")); // Only user role

            when(jwt.getClaimAsString("preferred_username")).thenReturn("regularuser");
            when(jwt.getClaimAsString("email")).thenReturn("regular@example.com");
            when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("regularuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("regular@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create regular user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: only USER role, no ADMIN
            assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER); // Only USER
            assertThat(result.getRoles()).doesNotContain(User.UserRole.ROLE_ADMIN); // No ADMIN
        }

        @Test
        @DisplayName("Should not add ROLE_ADMIN when JWT has no roles")
        void shouldNotAddAdminRoleWhenJwtHasNoRoles() {
            // Arrange: JWT has no role claims
            when(jwt.getClaimAsString("preferred_username")).thenReturn("regularuser");
            when(jwt.getClaimAsString("email")).thenReturn("regular@example.com");
            // No realm_access or resource_access claims
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("regularuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("regular@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user without roles in JWT
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: default USER role only
            assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER);
            assertThat(result.getRoles()).doesNotContain(User.UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should set default values for new user")
        void shouldSetDefaultValuesForNewUser() {
            // Arrange: basic user creation
            when(jwt.getClaimAsString("preferred_username")).thenReturn("newuser");
            when(jwt.getClaimAsString("email")).thenReturn("new@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create new user
            User result = userSyncService.getOrCreateUser(jwt);

            // Assert: default values set correctly
            assertThat(result.getIsActive()).isTrue(); // Active by default
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getLastLogin()).isNotNull(); // Login timestamp
            assertThat(result.getStatus()).isEqualTo(User.UserStatus.OFFLINE); // Default status
        }

        @Test
        @DisplayName("Should encode password with keycloak prefix")
        void shouldEncodePasswordWithKeycloakPrefix() {
            // Arrange: user creation scenario
            when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
            when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // Act: create user
            userSyncService.getOrCreateUser(jwt);

            // Assert: password encoded with specific format
            verify(passwordEncoder).encode("keycloak:" + userId); // Prefix + user ID
        }
    }

    // ==================== Additional Tests for Edge Cases ====================

    @Test
    @DisplayName("Should find admin role in realm_access with mixed case")
    void shouldFindAdminRoleInRealmAccessMixedCase() {
        // Arrange: mixed case admin role
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("User", "AdMiN", "MODERATOR")); // Case-insensitive check

        when(jwt.getClaimAsString("preferred_username")).thenReturn("mixedcase");
        when(jwt.getClaimAsString("email")).thenReturn("mixed@example.com");
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("mixedcase")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mixed@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create user with mixed case admin
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: recognizes admin regardless of case
        assertThat(result.getRoles()).contains(User.UserRole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should not find admin role when only in wrong client")
    void shouldNotFindAdminRoleWhenOnlyInWrongClient() {
        // Arrange: admin role in wrong client (not our application)
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", List.of("user", "admin"));

        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("wrong-client", clientAccess); // Wrong client name

        when(jwt.getClaimAsString("preferred_username")).thenReturn("noadmin");
        when(jwt.getClaimAsString("email")).thenReturn("noadmin@example.com");

        // FIX 2: Added missing realm_access stub
        when(jwt.getClaim("realm_access")).thenReturn(null); // No realm roles

        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("noadmin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("noadmin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create user with admin in wrong client
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: only USER role (doesn't recognize wrong client)
        assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER);
        assertThat(result.getRoles()).doesNotContain(User.UserRole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should handle non-list roles in resource_access")
    void shouldHandleNonListRolesInResourceAccess() {
        // Arrange: roles is not a list (wrong data type)
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", "not-a-list"); // String instead of List

        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("securechat-backend", clientAccess);

        when(jwt.getClaimAsString("preferred_username")).thenReturn("wrongtype");
        when(jwt.getClaimAsString("email")).thenReturn("wrongtype@example.com");

        // FIX 3: Added missing realm_access stub
        when(jwt.getClaim("realm_access")).thenReturn(null);

        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("wrongtype")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("wrongtype@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create user with malformed roles data
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: default USER role only (graceful handling)
        assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER);
    }

        @Test
    @DisplayName("Should handle null client access in resource_access")
    void shouldHandleNullClientAccessInResourceAccess() {
        // Arrange: client access is null
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("securechat-backend", null); // Null client

        when(jwt.getClaimAsString("preferred_username")).thenReturn("nullclient");
        when(jwt.getClaimAsString("email")).thenReturn("nullclient@example.com");

        // FIX 4: Added missing realm_access stub
        when(jwt.getClaim("realm_access")).thenReturn(null);

        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nullclient")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nullclient@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create user with null client access
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: default USER role only
        assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER);
    }

    @Test
    @DisplayName("Should handle non-map client access in resource_access")
    void shouldHandleNonMapClientAccessInResourceAccess() {
        // Arrange: client access is not a Map (wrong type)
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("securechat-backend", "not-a-map"); // String instead of Map

        when(jwt.getClaimAsString("preferred_username")).thenReturn("notamap");
        when(jwt.getClaimAsString("email")).thenReturn("notamap@example.com");

        // FIX 5: Added missing realm_access stub
        when(jwt.getClaim("realm_access")).thenReturn(null);

        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("notamap")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("notamap@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create user with non-map client access
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: default USER role only (graceful handling)
        assertThat(result.getRoles()).containsExactly(User.UserRole.ROLE_USER);
    }

    @Test
    @DisplayName("Should keep existing username when belongs to same user")
    void shouldKeepExistingUsernameWhenBelongsToSameUser() {
        // Arrange: same user already has this username
        User existingUser = new User();
        existingUser.setId(userId); // Same user ID
        existingUser.setUsername("myusername");

        when(jwt.getClaimAsString("preferred_username")).thenReturn("myusername");
        when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // Not found (new sync)
        when(userRepository.findByUsername("myusername")).thenReturn(Optional.of(existingUser)); // But username exists
        when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: sync same user (re-login)
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: keeps username (same user owns it)
        assertThat(result.getUsername()).isEqualTo("myusername");
    }

    @Test
    @DisplayName("Should keep existing email when belongs to same user")
    void shouldKeepExistingEmailWhenBelongsToSameUser() {
        // Arrange: same user already has this email
        User existingUser = new User();
        existingUser.setId(userId); // Same user ID
        existingUser.setEmail("myemail@example.com");

        when(jwt.getClaimAsString("preferred_username")).thenReturn("myusername");
        when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // Not found (new sync)
        when(userRepository.findByUsername("myusername")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.of(existingUser)); // But email exists
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: sync same user (re-login)
        User result = userSyncService.getOrCreateUser(jwt);

        // Assert: keeps email (same user owns it)
        assertThat(result.getEmail()).isEqualTo("myemail@example.com");
    }
}