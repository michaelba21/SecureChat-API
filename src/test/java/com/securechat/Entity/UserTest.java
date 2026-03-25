package com.securechat.Entity;  
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;  

import com.securechat.entity.ChatRoom;  // ChatRoom entity (relationship)
import com.securechat.entity.ChatRoomMember; 
import com.securechat.entity.Message;  // Message entity (relationship)
import com.securechat.entity.User;  

import java.time.LocalDateTime; 
import java.util.HashSet;  // Set implementation
import java.util.Set;  
import java.util.UUID;  // UUID for unique identifiers

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

class UserTest {

    private User user;  

    @BeforeEach
    void setUp() {
        user = new User();  // Create fresh User before each test
    }

    @Test
    void defaultValues_areSetCorrectly() {
        // Test: Default field values after object creation
        assertThat(user.getIsActive()).isTrue();  // Should be active by default
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.OFFLINE);  
        assertThat(user.getRoles()).isEmpty();  
    }

    @Test
    void prePersist_setsCreatedAtAndLastLogin() {
        // Test: @PrePersist callback sets timestamps
        LocalDateTime before = LocalDateTime.now();  // Time before callback

        // Trigger the @PrePersist annotated method using reflection
        ReflectionTestUtils.invokeMethod(user, "onCreate");  

        LocalDateTime after = LocalDateTime.now();  

        // CreatedAt should be set to current time
        assertThat(user.getCreatedAt())
                .isNotNull()  // Should not be null
                .isAfterOrEqualTo(before)  // Should be after or equal to before time
                .isBeforeOrEqualTo(after);  

        // LastLogin should also be set (initially same as CreatedAt)
        assertThat(user.getLastLogin())
                .isNotNull()  
                .isAfterOrEqualTo(before) 
                .isBeforeOrEqualTo(after);  // Should be before or equal to after time
    }

    @Test
    void prePersist_doesNotOverrideExistingLastLogin() {
        // Test: @PrePersist doesn't override existing lastLogin
        LocalDateTime existingLastLogin = LocalDateTime.of(2024, 1, 1, 12, 0);  // Fixed timestamp
        user.setLastLogin(existingLastLogin);  // Set lastLogin before callback

        ReflectionTestUtils.invokeMethod(user, "onCreate");  

        // Should remain unchanged
        assertThat(user.getLastLogin()).isEqualTo(existingLastLogin);
    }

    @Test
    void id_getterAndSetter_workCorrectly() {
        // Test: ID field
        UUID id = UUID.randomUUID();  // Generate random UUID
        user.setId(id);  // Set ID
        assertThat(user.getId()).isEqualTo(id);  
    }

    @Test
    void username_getterAndSetter_workCorrectly() {
        // Test: Username field
        user.setUsername("john_doe");  // Set username
        assertThat(user.getUsername()).isEqualTo("john_doe");  
    }

    @Test
    void email_getterAndSetter_workCorrectly() {
        // Test: Email field
        user.setEmail("john@example.com");  // Set email
        assertThat(user.getEmail()).isEqualTo("john@example.com");  
    }

    @Test
    void passwordHash_getterAndSetter_workCorrectly() {
        // Test: Password hash field (hashed password, not plain text)
        user.setPasswordHash("hashed123"); 
        assertThat(user.getPasswordHash()).isEqualTo("hashed123");  // Verify hash
    }

    @Test
    void createdAt_getterAndSetter_workCorrectly() {
        // Test: CreatedAt timestamp field
        LocalDateTime now = LocalDateTime.now();  
        user.setCreatedAt(now);  // Set created time
        assertThat(user.getCreatedAt()).isEqualTo(now);  
    }

    @Test
    void lastLogin_getterAndSetter_workCorrectly() {
        // Test: LastLogin timestamp field
        LocalDateTime loginTime = LocalDateTime.now().minusDays(1);  // 1 day ago
        user.setLastLogin(loginTime);  
        assertThat(user.getLastLogin()).isEqualTo(loginTime);  
    }

    @Test
    void isActive_getterAndSetter_workCorrectly() {
        // Test: Active status field
        user.setIsActive(false);  
        assertThat(user.getIsActive()).isFalse();  // Verify inactive

        user.setIsActive(true);  
        assertThat(user.getIsActive()).isTrue();  // Verify active
    }

    @Test
    void bio_getterAndSetter_workCorrectly() {
        // Test: Bio field (optional user description)
        user.setBio("I love coding!");  // Set bio
        assertThat(user.getBio()).isEqualTo("I love coding!");  

        user.setBio(null);  
        assertThat(user.getBio()).isNull();  // Should be null
    }

    @Test
    void avatarUrl_getterAndSetter_workCorrectly() {
        // Test: Avatar URL field (profile picture)
        user.setAvatarUrl("https://example.com/avatar.jpg");  // Set avatar URL
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");  // Verify

        user.setAvatarUrl(null); 
        assertThat(user.getAvatarUrl()).isNull(); 
    }

    @Test
    void status_getterAndSetter_workCorrectly() {
        // Test: Status enum field (ONLINE, OFFLINE, AWAY)
        user.setStatus(User.UserStatus.ONLINE);  // Set to ONLINE
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ONLINE);  

        user.setStatus(User.UserStatus.AWAY); 
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.AWAY);  

        user.setStatus(null);  // Test null
        assertThat(user.getStatus()).isNull();  
    }

    @Test
    void roles_getterAndSetter_workCorrectly() {
        // Test: Roles set field (collection of UserRole enums)
        Set<User.UserRole> roles = new HashSet<>();  
        roles.add(User.UserRole.ROLE_ADMIN);  
        roles.add(User.UserRole.ROLE_USER);  // Add USER role

        user.setRoles(roles);  // Set roles
        assertThat(user.getRoles())  // Verify
                .hasSize(2)  // Should have 2 roles
                .containsExactlyInAnyOrder(User.UserRole.ROLE_ADMIN, User.UserRole.ROLE_USER);  // Both roles

        user.setRoles(null);  
        assertThat(user.getRoles()).isNull();  // Should be null (depends on implementation)
    }

    @Test
    void roles_isMutableAndIndependent() {
        // Test: Defensive copying - modifying original set doesn't affect entity
        Set<User.UserRole> original = new HashSet<>();  // Original set
        original.add(User.UserRole.ROLE_USER);  
        user.setRoles(original);  

        original.add(User.UserRole.ROLE_ADMIN);  // Modify original set (add ADMIN)

        // User's roles should NOT be affected by modifying original set
        assertThat(user.getRoles())
                .hasSize(1)  
                .containsOnly(User.UserRole.ROLE_USER);  // Only USER, not ADMIN
    }

    @Test
    void deactivationFields_getterAndSetter_workCorrectly() {
        // Test: Deactivation-related fields (for user deactivation/banning)
        UUID adminId = UUID.randomUUID();  
        LocalDateTime deactivatedAt = LocalDateTime.now();  
        String reason = "Inappropriate behavior";  // Reason for deactivation

        user.setDeactivatedBy(adminId);  // Set deactivated by
        user.setDeactivatedAt(deactivatedAt);  
        user.setDeactivationReason(reason);  // Set reason

        assertThat(user.getDeactivatedBy()).isEqualTo(adminId);  
        assertThat(user.getDeactivatedAt()).isEqualTo(deactivatedAt);  
        assertThat(user.getDeactivationReason()).isEqualTo(reason);  // Verify reason
    }

    @Test
    void collections_areInitializedAndNeverNull() {
        // Test: All collection fields are initialized (not null)
        assertThat(user.getCreatedChatRooms()).isNotNull().isEmpty();  // Created chat rooms
        assertThat(user.getSentMessages()).isNotNull().isEmpty();  
        assertThat(user.getMemberships()).isNotNull().isEmpty();  // Chat room memberships
        assertThat(user.getUploadedFiles()).isNotNull().isEmpty();  
        assertThat(user.getRefreshTokens()).isNotNull().isEmpty();  // Refresh tokens
        assertThat(user.getAuditActions()).isNotNull().isEmpty(); 
        assertThat(user.getPerformedAdminActions()).isNotNull().isEmpty();  // Admin actions performed
        assertThat(user.getReceivedAdminActions()).isNotNull().isEmpty();  
    }

    @Test
    void collections_canBeModified() {
        // Test: Collection fields can be modified
        user.getCreatedChatRooms().add(new ChatRoom());  // Add chat room
        user.getSentMessages().add(new Message());  // Add message
        user.getMemberships().add(new ChatRoomMember());  // Add membership

        assertThat(user.getCreatedChatRooms()).hasSize(1);  
        assertThat(user.getSentMessages()).hasSize(1);  // Should have 1 message
        assertThat(user.getMemberships()).hasSize(1);  
    }

    @Test
    void fullUser_canBeConstructedAndAccessed() {
        // Test: Complete User object with all fields
        UUID id = UUID.randomUUID();  
        UUID deactivatedBy = UUID.randomUUID();  // Admin ID

        // Set all fields
        user.setId(id);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setBio("Developer");
        user.setAvatarUrl("https://cdn.example.com/alice.jpg");
        user.setStatus(User.UserStatus.ONLINE);
        user.setIsActive(false);  // Inactive user
        user.setDeactivatedBy(deactivatedBy);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivationReason("Test deactivation");

        Set<User.UserRole> roles = Set.of(User.UserRole.ROLE_ADMIN);  // ADMIN role only
        user.setRoles(roles);

        // Trigger @PrePersist to set timestamps
        ReflectionTestUtils.invokeMethod(user, "onCreate");

        // Verify all fields
        assertThat(user.getId()).isEqualTo(id); 
        assertThat(user.getUsername()).isEqualTo("alice");  
        assertThat(user.getEmail()).isEqualTo("alice@example.com");  
        assertThat(user.getBio()).isEqualTo("Developer");  // Bio
        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.example.com/alice.jpg");  // Avatar
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ONLINE);  // Status
        assertThat(user.getIsActive()).isFalse();  
        assertThat(user.getRoles()).containsExactly(User.UserRole.ROLE_ADMIN);  
        assertThat(user.getDeactivatedBy()).isEqualTo(deactivatedBy);  // Deactivated by
        assertThat(user.getCreatedAt()).isNotNull();  
        assertThat(user.getLastLogin()).isNotNull();  // Last login timestamp
    }
}