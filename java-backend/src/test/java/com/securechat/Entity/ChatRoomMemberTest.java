package com.securechat.Entity;  

import org.junit.jupiter.api.*;  // JUnit 5 annotations

import com.securechat.entity.ChatRoomMember;  
import com.securechat.entity.ChatRoom;  // ChatRoom entity (relationship)
import com.securechat.entity.User;  

import java.time.LocalDateTime;  
import java.util.UUID;  

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

@DisplayName("ChatRoomMember Entity Tests - 100% Coverage")  // Custom display name
class ChatRoomMemberTest {

    private ChatRoom chatRoom; 
    private User user;  // Test user
    private UUID chatRoomId; 
    private UUID userId;  // Test user ID

    @BeforeEach
    void setUp() {
        // Setup before each test: create test data
        chatRoomId = UUID.randomUUID();  // Generate random chat room ID
        userId = UUID.randomUUID();  

        chatRoom = new ChatRoom();  // Create chat room
        chatRoom.setId(chatRoomId);  
        chatRoom.setName("Test Room");  // Set chat room name

        user = new User();  
        user.setId(userId);  // Set user ID
        user.setUsername("alice");  
        user.setEmail("alice@example.com");  // Set email
    }

    @Test
    @DisplayName("Default constructor initializes defaults")
    void defaultConstructor() {
        // Test: Default constructor sets default values
        ChatRoomMember member = new ChatRoomMember();  // Create with default constructor

        // Verify all fields have expected default values
        assertThat(member.getId()).isNull();  // ID should be null initially
        assertThat(member.getRole()).isEqualTo("MEMBER"); 
        assertThat(member.getIsActive()).isTrue();  // Default active status is true
        assertThat(member.getJoinedAt()).isNull();  
        assertThat(member.getLastReadAt()).isNull();  // Last read timestamp null
        assertThat(member.getLastActivity()).isNull(); 
        assertThat(member.getLeftAt()).isNull();  // Left timestamp null
        assertThat(member.getRemovedBy()).isNull();  
        assertThat(member.getChatRoomId()).isNull();  // Chat room ID null
        assertThat(member.getUserId()).isNull();  
    }

    @Test
    @DisplayName("Parameterized constructor sets associations and defaults")
    void parameterizedConstructor() {
        // Test: Constructor with parameters sets relationships and defaults
        ChatRoomMember member = new ChatRoomMember(chatRoom, user, "ADMIN");  // Create with chat room, user, and role

        // Verify relationships are set
        assertThat(member.getChatRoom()).isSameAs(chatRoom);  // Chat room reference
        assertThat(member.getUser()).isSameAs(user);  // User reference
        assertThat(member.getRole()).isEqualTo("ADMIN"); 
        assertThat(member.getIsActive()).isTrue();  

        // These fields are NOT set by constructor
        assertThat(member.getJoinedAt()).isNull();  // Joined timestamp not set
        assertThat(member.getChatRoomId()).isNull(); 
        assertThat(member.getUserId()).isNull();  // User ID not set directly
    }

    @Test
    @DisplayName("@PrePersist sets defaults only when null")
    void prePersistSetsDefaultsOnlyWhenNull() {
        // Test: @PrePersist callback doesn't override existing values
        ChatRoomMember member = new ChatRoomMember();  

        // Set explicit values (non-null)
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        member.setJoinedAt(fixedTime);  // Set joined time
        member.setRole("MODERATOR");  // Set role
        member.setIsActive(false);  

        member.simulatePrePersist();  // Simulate @PrePersist callback

        // Values should NOT be overridden (were non-null)
        assertThat(member.getJoinedAt()).isEqualTo(fixedTime);  // Joined time unchanged
        assertThat(member.getRole()).isEqualTo("MODERATOR");  
        assertThat(member.getIsActive()).isFalse();  // Active status unchanged
    }

    @Test
    @DisplayName("@PrePersist sets defaults when fields are null")
    void prePersistSetsDefaultsWhenNull() {
        // Test: @PrePersist callback sets defaults for null fields
        ChatRoomMember member = new ChatRoomMember();  
        member.setChatRoom(chatRoom);  // Set chat room
        member.setUser(user);  // Set user

        member.simulatePrePersist();  // Simulate @PrePersist callback

        // Null fields should get default values
        assertThat(member.getJoinedAt())  // Joined timestamp set to current time
                .isCloseTo(LocalDateTime.now(), within(2, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(member.getIsActive()).isTrue();  
        assertThat(member.getRole()).isEqualTo("MEMBER");  
    }

    @Test
    @DisplayName("All getters and setters work correctly")
    void allGettersAndSetters() {
        // Test: All field getters and setters function correctly
        ChatRoomMember member = new ChatRoomMember();  
        LocalDateTime now = LocalDateTime.now();  
        UUID removedById = UUID.randomUUID();  // Random remover ID

        // Set all fields
        member.setId(UUID.randomUUID());  // Set random ID
        member.setChatRoom(chatRoom);  
        member.setUser(user);  // Set user
        member.setChatRoomId(chatRoomId);  
        member.setUserId(userId);  // Set user ID
        member.setRole("MODERATOR");  
        member.setJoinedAt(now);  // Set joined time
        member.setLastReadAt(now.minusDays(1));  // Set last read time
        member.setLastActivity(now.minusHours(1));  
        member.setLeftAt(now.minusHours(2));  // Set left time
        member.setIsActive(false);  
        member.setRemovedBy(removedById);  // Set remover ID

        // Verify all fields
        assertThat(member.getId()).isNotNull();  // ID should not be null
        assertThat(member.getChatRoom()).isSameAs(chatRoom);  
        assertThat(member.getUser()).isSameAs(user);  
        assertThat(member.getChatRoomId()).isEqualTo(chatRoomId);  // Chat room ID
        assertThat(member.getUserId()).isEqualTo(userId);  // User ID
        assertThat(member.getRole()).isEqualTo("MODERATOR");  
        assertThat(member.getJoinedAt()).isEqualTo(now);  // Joined time
        assertThat(member.getLastReadAt()).isEqualTo(now.minusDays(1));  
        assertThat(member.getLastActivity()).isEqualTo(now.minusHours(1));  // Last activity time
        assertThat(member.getLeftAt()).isEqualTo(now.minusHours(2));  
        assertThat(member.getIsActive()).isFalse();  // Inactive status
        assertThat(member.getRemovedBy()).isEqualTo(removedById);  // Remover ID
    }

    @Test
    @DisplayName("isAdmin() returns true only for 'ADMIN' role")
    void isAdmin() {
        // Test: isAdmin() helper method
        ChatRoomMember member = new ChatRoomMember();  // Create member

        member.setRole("ADMIN");  // Set to ADMIN (uppercase)
        assertThat(member.isAdmin()).isTrue();  

        member.setRole("admin");  // Set to lowercase "admin"
        assertThat(member.isAdmin()).isFalse();  // Should return false (case-sensitive)

        member.setRole("MODERATOR");  // Set to MODERATOR
        assertThat(member.isAdmin()).isFalse();  

        member.setRole("MEMBER");  // Set to MEMBER
        assertThat(member.isAdmin()).isFalse();  

        member.setRole(null);  // Set to null
        assertThat(member.isAdmin()).isFalse();  
    }

    @Test
    @DisplayName("markAsRead() updates lastReadAt to current time")
    void markAsRead() {
        // Test: markAsRead() business method
        ChatRoomMember member = new ChatRoomMember();  
        LocalDateTime before = LocalDateTime.now();  // Time before marking as read

        member.markAsRead();  // Call markAsRead()

        // Last read time should be set to current time
        assertThat(member.getLastReadAt())
                .isCloseTo(LocalDateTime.now(), within(2, java.time.temporal.ChronoUnit.SECONDS))
                .isAfterOrEqualTo(before);  // Should be after or equal to before time
    }

    @Test
    @DisplayName("leaveRoom() sets isActive=false and updates lastReadAt")
    void leaveRoom() {
        // Test: leaveRoom() business method
        ChatRoomMember member = new ChatRoomMember(); 
        member.setIsActive(true);  
        LocalDateTime before = LocalDateTime.now();  // Time before leaving

        member.leaveRoom();  // Call leaveRoom()

        assertThat(member.getIsActive()).isFalse();  // Should be inactive
        // Last read time should be updated
        assertThat(member.getLastReadAt())
                .isCloseTo(LocalDateTime.now(), within(2, java.time.temporal.ChronoUnit.SECONDS))
                .isAfterOrEqualTo(before);  // Should be after or equal to before time
    }

    @Test
    @DisplayName("Full lifecycle simulation: create → activity → read → leave")
    void fullLifecycleSimulation() {
        // Test: Simulate complete member lifecycle
        ChatRoomMember member = new ChatRoomMember(chatRoom, user, null);  // Create with null role (will default)

        member.simulatePrePersist();  // Simulate persistence

        // After persistence: defaults should be set
        assertThat(member.getRole()).isEqualTo("MEMBER");  // Default role
        assertThat(member.getIsActive()).isTrue();  
        assertThat(member.getJoinedAt()).isNotNull();  // Joined time set

        // Simulate user activity
        LocalDateTime activityTime = LocalDateTime.now().minusMinutes(5);  // 5 minutes ago
        member.setLastActivity(activityTime);  // Set last activity
        assertThat(member.getLastActivity()).isEqualTo(activityTime);  

        // User reads messages
        member.markAsRead();  // Mark as read
        LocalDateTime readTime = member.getLastReadAt();  

        // User leaves room
        member.leaveRoom();  

        assertThat(member.getIsActive()).isFalse();  // Should be inactive
        assertThat(member.getLastReadAt()).isAfterOrEqualTo(readTime);  // Read time updated

        // Optional: Set additional leave details
        LocalDateTime leftTime = LocalDateTime.now();  
        UUID removerId = UUID.randomUUID();  
        member.setLeftAt(leftTime);  
        member.setRemovedBy(removerId);  // Set remover ID

        assertThat(member.getLeftAt()).isEqualTo(leftTime);  
        assertThat(member.getRemovedBy()).isEqualTo(removerId);  // Verify remover ID
    }
}