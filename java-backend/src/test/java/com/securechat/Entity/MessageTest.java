package com.securechat.Entity;  

import org.junit.jupiter.api.DisplayName;  
import org.junit.jupiter.api.Nested;  // Nested test classes
import org.junit.jupiter.api.Test;

import com.securechat.entity.ChatRoom;  // ChatRoom entity (relationship)
import com.securechat.entity.File;  // File entity (relationship)
import com.securechat.entity.Message; 
import com.securechat.entity.MessageAuditLog;  // Audit log entity (relationship)
import com.securechat.entity.User;  

import java.time.LocalDateTime;  // Date/time for timestamps
import java.util.ArrayList;  
import java.util.List;  
import java.util.UUID;  // UUID for unique identifiers

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

@DisplayName("Message Entity Tests")  
class MessageTest {

    // Test constants (reused across tests)
    private final UUID messageId = UUID.randomUUID();  
    private final UUID senderId = UUID.randomUUID();  // Sender user ID
    private final UUID chatRoomId = UUID.randomUUID();  
    private final UUID fileId = UUID.randomUUID();  // File attachment ID
    private final UUID deletedById = UUID.randomUUID();  

    // Helper: Create mock User for testing
    private User createMockUser() {
        User user = new User();  
        user.setId(senderId);  // Set sender ID
        user.setUsername("testuser");  
        return user;
    }

    // Helper: Create mock ChatRoom for testing
    private ChatRoom createMockChatRoom() {
        ChatRoom room = new ChatRoom();  
        room.setId(chatRoomId);  // Set chat room ID
        return room;
    }

    // Helper: Create mock File for testing
    private File createMockFile() {
        File file = new File(); 
        file.setId(fileId);  // Set file ID
        return file;
    }

    @Test
    @DisplayName("Default constructor and field defaults")
    void defaultConstructor_setsDefaults() {
        // Test: Default constructor sets expected default values
        Message message = new Message();  

        // Verify all fields have expected default/null values
        assertThat(message.getId()).isNull();  // ID should be null
        assertThat(message.getSender()).isNull();  
        assertThat(message.getUsername()).isNull();  // Username should be null
        assertThat(message.getChatRoom()).isNull();  
        assertThat(message.getContent()).isNull();  // Content should be null
        assertThat(message.getMessageType()).isEqualTo(Message.MessageType.TEXT);  // Default type is TEXT
        assertThat(message.getAttachment()).isNull();  // Attachment should be null
        assertThat(message.getTimestamp()).isNull();  
        assertThat(message.getIsEdited()).isFalse();  // Default not edited
        assertThat(message.getEditedAt()).isNull();  
        assertThat(message.getIsDeleted()).isFalse();  // Default not deleted
        assertThat(message.getDeletedAt()).isNull();  
        assertThat(message.getDeletedBy()).isNull();  
        assertThat(message.getAuditLogs()).isInstanceOf(ArrayList.class);  // Should be ArrayList
        assertThat(message.getAuditLogs()).isEmpty();  
    }

    @Test
    @DisplayName("@PrePersist sets timestamp on create")
    void prePersist_setsTimestamp() {
        // Test: @PrePersist lifecycle callback sets timestamp
        Message message = new Message();  

        // Timestamp should be null before @PrePersist
        assertThat(message.getTimestamp()).isNull();  

        // Simulate JPA @PrePersist callback
        message.onCreate();  // Call onCreate() method (annotated with @PrePersist)

        // Timestamp should be set to current time (within 2 seconds)
        assertThat(message.getTimestamp())
                .isCloseTo(LocalDateTime.now(), within(2, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("All getters and setters work correctly")
    void gettersAndSetters_allFields() {
        // Test: All field getters and setters function correctly
        Message message = new Message();  // Create message
        LocalDateTime now = LocalDateTime.now();  
        User sender = createMockUser();  // Mock sender
        ChatRoom chatRoom = createMockChatRoom();  
        File attachment = createMockFile();  
        List<MessageAuditLog> auditLogs = new ArrayList<>();  // Empty audit log list

        // Set all fields
        message.setId(messageId);  // Set ID
        message.setSender(sender);  // Set sender
        message.setUsername("testuser");  // Set username
        message.setChatRoom(chatRoom);  
        message.setContent("Hello world");  // Set content
        message.setMessageType(Message.MessageType.IMAGE);  // Set message type to IMAGE
        message.setAttachment(attachment);  // Set attachment
        message.setTimestamp(now);  // Set timestamp
        message.setIsEdited(true);  
        message.setEditedAt(now.plusHours(1));  // Set edit timestamp
        message.setIsDeleted(true);  // Set deleted flag
        message.setDeletedAt(now.plusHours(2));  
        message.setDeletedBy(deletedById);  
        message.setAuditLogs(auditLogs);  // Set audit logs

        // Verify all fields
        assertThat(message.getId()).isEqualTo(messageId); 
        assertThat(message.getSender()).isSameAs(sender);  // Sender reference
        assertThat(message.getUsername()).isEqualTo("testuser");  
        assertThat(message.getChatRoom()).isSameAs(chatRoom);  // Chat room reference
        assertThat(message.getContent()).isEqualTo("Hello world");  
        assertThat(message.getMessageType()).isEqualTo(Message.MessageType.IMAGE);  // Message type
        assertThat(message.getAttachment()).isSameAs(attachment);  // Attachment reference
        assertThat(message.getTimestamp()).isEqualTo(now); 
        assertThat(message.getIsEdited()).isTrue();  // Edited flag
        assertThat(message.getEditedAt()).isEqualTo(now.plusHours(1));  // Edit timestamp
        assertThat(message.getIsDeleted()).isTrue();  
        assertThat(message.getDeletedAt()).isEqualTo(now.plusHours(2));  // Delete timestamp
        assertThat(message.getDeletedBy()).isEqualTo(deletedById);  
        assertThat(message.getAuditLogs()).isSameAs(auditLogs);  // Audit logs reference
    }

    @Nested
    @DisplayName("MessageType Enum")  // Nested test class for MessageType enum
    class MessageTypeTests {

        @Test
        void messageType_values() {
            // Test: MessageType enum has expected values
            assertThat(Message.MessageType.values())  // Get all enum values
                    .containsExactly( 
                        Message.MessageType.TEXT,  // Text messages
                        Message.MessageType.IMAGE,  
                        Message.MessageType.FILE  // File messages
                    );
        }

        @Test
        void defaultMessageType_isTEXT() {
            // Test: Default message type is TEXT
            Message message = new Message();  
            assertThat(message.getMessageType()).isEqualTo(Message.MessageType.TEXT);  // Should be TEXT
        }
    }

    @Test
    @DisplayName("Audit logs list is initialized as empty ArrayList")
    void auditLogs_initializedAsEmptyArrayList() {
        // Test: Audit logs list is properly initialized
        Message message = new Message(); 
        assertThat(message.getAuditLogs()).isNotNull();  // Should not be null
        assertThat(message.getAuditLogs()).isInstanceOf(ArrayList.class);  // Should be ArrayList
        assertThat(message.getAuditLogs()).isEmpty();  
    }

    @Test
    @DisplayName("onCreate does not override existing timestamp")
    void onCreate_preservesExistingTimestamp() {
        // Test: @PrePersist doesn't override existing timestamp
        Message message = new Message(); 
        LocalDateTime fixed = LocalDateTime.of(2025, 1, 1, 12, 0);  // Fixed timestamp

        message.setTimestamp(fixed);  // Set timestamp before @PrePersist
        message.onCreate();  // Simulate @PrePersist

        assertThat(message.getTimestamp()).isEqualTo(fixed);  
    }
}