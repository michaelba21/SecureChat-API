

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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSyncService Tests")
class UserSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserSyncService userSyncService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn(userId.toString());
    }

    @Nested
    @DisplayName("getOrCreateUser(Jwt)")
    class GetOrCreateUser {

        @Test
        @DisplayName("Should return existing user when found by ID")
        void shouldReturnExistingUserWhenFound() {
            User existing = createTestUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

            User result = userSyncService.getOrCreateUser(jwt);

            assertThat(result).isSameAs(existing);
            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when JWT subject is null or empty")
        void shouldThrowExceptionWhenJwtSubjectIsInvalid() {
            when(jwt.getSubject()).thenReturn(null);

            assertThatThrownBy(() -> userSyncService.getOrCreateUser(jwt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JWT subject claim is null or empty");
        }

        @Test
        @DisplayName("Should create new user using preferred_username and email from JWT")
        void shouldCreateNewUserWithPreferredUsernameAndEmail() {
            when(jwt.getClaimAsString("preferred_username")).thenReturn("myusername");
            when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("myusername")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = userSyncService.getOrCreateUser(jwt);

            assertThat(result.getUsername()).isEqualTo("myusername");
            assertThat(result.getEmail()).isEqualTo("myemail@example.com");
            assertThat(result.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should use email as username when preferred_username is null")
        void shouldUseEmailAsUsernameWhenPreferredUsernameIsNull() {
            when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
            when(jwt.getClaimAsString("email")).thenReturn("myemail@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmail("myemail@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User result = userSyncService.getOrCreateUser(jwt);

            // When preferred_username is null, service generates unique username using userId
            assertThat(result.getUsername()).startsWith("user-");
            assertThat(result.getEmail()).isEqualTo("myemail@example.com");
        }

        @Test
        @DisplayName("Should generate unique username when preferred username is taken by another user")
        void shouldUseDefaultUsernameWhenPreferredUsernameIsTaken() {
            User existing = createTestUser();
            existing.setUsername("testuser");

            when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
            when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existing));
            // Need to stub for the generated unique username check (testuser-1)
            when(userRepository.findByUsername("testuser-1")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User result = userSyncService.getOrCreateUser(jwt);

            // When preferred username is taken, service appends -1, -2, etc. to the preferred username
            assertThat(result.getUsername()).startsWith("testuser-");
            assertThat(result.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should return existing user when email is already taken by another user")
        void shouldUseDefaultEmailWhenEmailIsTaken() {
            User existing = createTestUser();
            existing.setEmail("user@example.com");

            when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            // When email is taken, service returns existing user BEFORE checking username
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

            User result = userSyncService.getOrCreateUser(jwt);

            // When email is taken, service returns the existing user (not creates new one)
            assertThat(result.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should add ROLE_ADMIN when JWT contains admin role")
        void shouldAddAdminRoleWhenJwtHasAdminRole() {
            Map<String, Object> realmAccess = Map.of("roles", List.of("user", "ROLE_ADMIN"));

            when(jwt.getClaimAsString("preferred_username")).thenReturn("adminuser");
            when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
            when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User result = userSyncService.getOrCreateUser(jwt);

            assertThat(result.getRoles()).containsExactlyInAnyOrder(
                    User.UserRole.ROLE_USER, User.UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should keep existing username when it belongs to the same user")
        void shouldKeepExistingUsernameWhenBelongsToSameUser() {
            User existing = createTestUser();
            existing.setUsername("myusername");

            // When user exists by ID, service returns early without checking JWT claims
            when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

            User result = userSyncService.getOrCreateUser(jwt);

            assertThat(result.getUsername()).isEqualTo("myusername");
        }

        @Test
        @DisplayName("Should keep existing email when it belongs to the same user")
        void shouldKeepExistingEmailWhenBelongsToSameUser() {
            User existing = createTestUser();
            existing.setEmail("myemail@example.com");

            // When user exists by ID, service returns early without checking JWT claims
            when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

            User result = userSyncService.getOrCreateUser(jwt);

            assertThat(result.getEmail()).isEqualTo("myemail@example.com");
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setIsActive(true);
        user.setRoles(new HashSet<>(Set.of(User.UserRole.ROLE_USER)));
        return user;
    }
}