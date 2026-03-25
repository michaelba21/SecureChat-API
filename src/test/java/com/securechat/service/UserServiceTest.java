package com.securechat.service;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Full Coverage Tests")
class UserServiceTest {
    // This test class validates the UserService which handles all user-related operations
    // including CRUD, search, role management, and soft deletion

    @Mock
    private UserRepository userRepository;  // Repository for user data

    @InjectMocks
    private UserService userService;  // Service under test

    // Test data
    private UUID userId;
    private User existingUser;

    @BeforeEach
    void setUp() {
        // Setup before each test
        userId = UUID.randomUUID();

        // Create a test user with typical properties
        existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("alice");
        existingUser.setEmail("alice@example.com");
        existingUser.setIsActive(true);  // User is active
        existingUser.setRoles(new HashSet<>(Set.of(User.UserRole.ROLE_USER)));  // Default role
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {
        // Tests retrieving all users (admin functionality)

        @Test
        void shouldReturnAllUsers() {
            List<User> users = List.of(existingUser, new User());  // 2 users
            when(userRepository.findAll()).thenReturn(users);

            List<User> result = userService.getAllUsers();

            assertThat(result).hasSize(2).contains(existingUser);  // Should return both users
            verify(userRepository).findAll();  // Should call repository
        }
    }

    @Nested
    @DisplayName("getUserById(UUID)")
    class GetUserById {
        // Tests retrieving a single user by ID

        @Test
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

            User result = userService.getUserById(userId);

            assertThat(result).isSameAs(existingUser);  // Should return the user
            verify(userRepository).findById(userId);  // Should call repository
        }

        @Test
        void shouldThrowRuntimeExceptionWhenNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");  // Clear error message
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenIdIsNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.getUserById(null))
                    .withMessage("User ID cannot be null");  // Validation error
        }
    }

    @Nested
    @DisplayName("updateUser(UUID, User)")
    class UpdateUser {
        // Tests complete user update with User object

        @Test
        void shouldUpdateBothUsernameAndEmail() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User updates = new User();
            updates.setUsername("bob");
            updates.setEmail("bob@example.com");

            User result = userService.updateUser(userId, updates);

            assertThat(result.getUsername()).isEqualTo("bob");  // Username updated
            assertThat(result.getEmail()).isEqualTo("bob@example.com");  // Email updated
            assertThat(result.getIsActive()).isTrue(); 
            verify(userRepository).save(existingUser);  // Should save updated user
        }

        @Test
        void shouldUpdateOnlyUsernameWhenEmailNull() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            User updates = new User();
            updates.setUsername("charlie");  // Only username provided

            userService.updateUser(userId, updates);

            assertThat(existingUser.getUsername()).isEqualTo("charlie");  // Username updated
            assertThat(existingUser.getEmail()).isEqualTo("alice@example.com");  // Email unchanged
        }

        @Test
        void shouldThrowWhenUserDetailsIsNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.updateUser(userId, null))
                    .withMessage("User details cannot be null");  // Validation
        }

        @Test
        void shouldThrowWhenIdIsNull() {
            User updates = new User();
            updates.setUsername("dave");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.updateUser(null, updates))
                    .withMessage("User ID cannot be null");  // Validation
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            User updates = new User();
            updates.setUsername("eve");

            assertThatThrownBy(() -> userService.updateUser(userId, updates))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");  // Error for non-existent user
        }
    }

    @Nested
    @DisplayName("searchUsers(String)")
    class SearchUsers {
        // Tests user search functionality

        @Test
        void shouldReturnMatchingUsers() {
            User user1 = new User(); user1.setUsername("alice");
            User user2 = new User(); user2.setUsername("malice");
            List<User> expected = List.of(user1, user2);

            when(userRepository.findByUsernameContainingIgnoreCase("ali"))
                    .thenReturn(expected);  // Case-insensitive search

            List<User> result = userService.searchUsers("ali");

            assertThat(result).hasSize(2).containsExactly(user1, user2);  // Both users returned
            verify(userRepository).findByUsernameContainingIgnoreCase("ali");  // Correct query
        }

        @Test
        void shouldThrowWhenQueryIsNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.searchUsers(null))
                    .withMessage("Search query cannot be null");  // Validation
        }

        @Test
        void shouldThrowWhenQueryTooShort() {
            assertThatThrownBy(() -> userService.searchUsers("ab"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Search query must be at least 3 characters");  // Min length validation
        }

        @Test
        void shouldAcceptQueryOfLength3() {
            when(userRepository.findByUsernameContainingIgnoreCase("abc"))
                    .thenReturn(List.of());  // Empty result

            assertThatCode(() -> userService.searchUsers("abc"))
                    .doesNotThrowAnyException();  // Should accept 3-character query
        }
    }

    @Nested
    @DisplayName("partialUpdateUser(UUID, Map)")
    class PartialUpdateUser {
        // Tests partial user updates using Map (PATCH-style updates)

        @Test
        void shouldUpdateBothFieldsFromMap() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> updates = new HashMap<>();
            updates.put("username", "frank");
            updates.put("email", "frank@example.com");

            User result = userService.partialUpdateUser(userId, updates);

            assertThat(result.getUsername()).isEqualTo("frank");  // Username updated
            assertThat(result.getEmail()).isEqualTo("frank@example.com");  // Email updated
        }

        @Test
        void shouldUpdateOnlyUsername() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Map<String, Object> updates = Map.of("username", "grace");  // Only username

            userService.partialUpdateUser(userId, updates);

            assertThat(existingUser.getUsername()).isEqualTo("grace");  // Username updated
            assertThat(existingUser.getEmail()).isEqualTo("alice@example.com");  // Email unchanged
        }

        @Test
        void shouldThrowWhenUpdatesNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.partialUpdateUser(userId, null))
                    .withMessage("Updates cannot be null or empty");  // Validation
        }

        @Test
        void shouldThrowWhenUpdatesEmpty() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.partialUpdateUser(userId, Map.of()))
                    .withMessage("Updates cannot be null or empty");  
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            Map<String, Object> updates = Map.of("username", "hidden");

            assertThatThrownBy(() -> userService.partialUpdateUser(userId, updates))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");  // Error for non-existent user
        }
    }

    @Nested
    @DisplayName("updateUserRoles(UUID, Set)")
    class UpdateUserRoles {
        // Tests role management (admin functionality)

        @Test
        void shouldUpdateRolesSuccessfully() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Set<User.UserRole> newRoles = Set.of(User.UserRole.ROLE_ADMIN, User.UserRole.ROLE_USER);

            User result = userService.updateUserRoles(userId, newRoles);

            assertThat(result.getRoles()).containsExactlyInAnyOrderElementsOf(newRoles);  // Roles updated
            verify(userRepository).save(existingUser);  // Should save
        }

        @Test
        void shouldThrowWhenRolesNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.updateUserRoles(userId, null))
                    .withMessage("Roles cannot be null or empty");  // Validation
        }

        @Test
        void shouldThrowWhenRolesEmpty() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.updateUserRoles(userId, Set.of()))
                    .withMessage("Roles cannot be null or empty");  // Validation
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserRoles(userId, Set.of(User.UserRole.ROLE_ADMIN)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");  // Error for non-existent user
        }
    }

    @Nested
    @DisplayName("deleteUser(UUID)")
    class DeleteUser {
        // Tests user deletion (soft delete)

        @Test
        void shouldSoftDeleteUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            userService.deleteUser(userId);

            assertThat(existingUser.getIsActive()).isFalse();  // Should be marked inactive (soft delete)
            verify(userRepository).save(existingUser);  // Should save with updated status
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");  // Error for non-existent user
        }

        @Test
        void shouldThrowWhenIdIsNull() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> userService.deleteUser(null))
                    .withMessage("User ID cannot be null");  // Validation
        }
    }
}