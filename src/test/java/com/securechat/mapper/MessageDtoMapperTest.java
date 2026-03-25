package com.securechat.mapper;

import com.securechat.dto.MessageDTO;
import com.securechat.dto.MessageResponse;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.Message;
import com.securechat.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDtoMapperTest {
    // This test class validates the MessageDtoMapper which converts Message entities
    // to different DTO types for various API responses (MessageResponse and MessageDTO)

    // Test constants for consistent test data
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final String CONTENT = "Hello, world!";  // Message text content
    private static final String USERNAME = "alice";         // Sender's display name
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2025, 12, 21, 12, 0);  // Original sent time
    private static final LocalDateTime EDITED_AT = LocalDateTime.of(2025, 12, 21, 12, 30); // Last edit time

    @Test
    void toResponse_mapsAllFieldsCorrectly_whenAllDataPresent() {
        // Tests mapping from Message entity to MessageResponse DTO (likely for REST API responses)
        // MessageResponse is a simplified view for client consumption
        Message message = createFullMessage();

        MessageResponse response = MessageDtoMapper.toResponse(message);

        // Verify all fields are correctly mapped to MessageResponse
        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", MESSAGE_ID)              
                .hasFieldOrPropertyWithValue("content", CONTENT)             // Message text
                .hasFieldOrPropertyWithValue("messageType", Message.MessageType.TEXT)  // Enum type
                .hasFieldOrPropertyWithValue("timestamp", TIMESTAMP)        
                .hasFieldOrPropertyWithValue("isEdited", true)             
                .hasFieldOrPropertyWithValue("editedAt", EDITED_AT)         
                .hasFieldOrPropertyWithValue("userId", USER_ID)              // Sender's UUID
                .hasFieldOrPropertyWithValue("username", USERNAME)        
                .hasFieldOrPropertyWithValue("chatRoomId", ROOM_ID);         // Room UUID
    }

    @Test
    void toResponse_returnsNull_whenInputIsNull() {
        // Tests null safety: Should handle null input gracefully
        MessageResponse response = MessageDtoMapper.toResponse(null);

        assertThat(response).isNull();  // Should return null when input is null
    }

    @Test
    void toResponse_handlesMissingSender_andChatRoom() {
        // Tests partial data scenario: Message without sender or chatroom relationships
        // This could happen with orphaned messages or system messages
        Message message = new Message();
        message.setId(MESSAGE_ID);
        message.setContent(CONTENT);
        message.setMessageType(Message.MessageType.TEXT);
        message.setTimestamp(TIMESTAMP);
        message.setIsEdited(false);
        message.setUsername(USERNAME); // fallback username (may be set even without User entity)

        MessageResponse response = MessageDtoMapper.toResponse(message);

        // Verify mapper handles missing relationships gracefully
        assertThat(response)
                .hasFieldOrPropertyWithValue("id", MESSAGE_ID)
                .hasFieldOrPropertyWithValue("content", CONTENT)
                .hasFieldOrPropertyWithValue("userId", null)     
                .hasFieldOrPropertyWithValue("username", USERNAME) // Username from field, not from User entity
                .hasFieldOrPropertyWithValue("chatRoomId", null); // No chatroom relationship
    }

    @Test
    void toDto_mapsAllFieldsCorrectly_whenAllDataPresent() {
        // Tests mapping from Message entity to MessageDTO
        // MessageDTO is a more comprehensive DTO, possibly for internal use or detailed views
        Message message = createFullMessage();

        MessageDTO dto = MessageDtoMapper.toDto(message);

        // MessageDTO includes additional fields like isDeleted and string messageType
        assertThat(dto)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", MESSAGE_ID)
                .hasFieldOrPropertyWithValue("content", CONTENT)
                .hasFieldOrPropertyWithValue("messageType", "TEXT") // Enum converted to String for JSON
                .hasFieldOrPropertyWithValue("timestamp", TIMESTAMP)
                .hasFieldOrPropertyWithValue("isEdited", true)
                .hasFieldOrPropertyWithValue("editedAt", EDITED_AT)
                .hasFieldOrPropertyWithValue("isDeleted", false)    // Additional field in MessageDTO
                .hasFieldOrPropertyWithValue("userId", USER_ID)
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("chatRoomId", ROOM_ID);
    }

    @Test
    void toDto_returnsNull_whenInputIsNull() {
        // Tests null safety for MessageDTO mapping
        MessageDTO dto = MessageDtoMapper.toDto(null);

        assertThat(dto).isNull();  // Should return null when input is null
    }

    @Test
    void toDto_convertsMessageTypeToStringCorrectly() {
        // Tests enum-to-string conversion for different message types
        // Important for JSON serialization (enums become strings in JSON)
        Message message = new Message();
        message.setMessageType(Message.MessageType.IMAGE);  // Different message type

        MessageDTO dto = MessageDtoMapper.toDto(message);

        assertThat(dto.getMessageType()).isEqualTo("IMAGE");  // Enum value converted to string
        // Possible message types: TEXT, IMAGE, FILE, SYSTEM, etc.
    }

    @Test
    void toDto_includesIsDeletedField() {
        // Tests soft delete functionality: Messages can be marked deleted without actual removal
        Message message = createFullMessage();
        message.setIsDeleted(true);  // Soft delete flag

        MessageDTO dto = MessageDtoMapper.toDto(message);
        // Soft delete allows message history while hiding from normal views
        assertThat(dto.getIsDeleted()).isTrue();  // Should reflect deleted state

    }

    @Test
    void toDto_handlesMissingSender_andChatRoom() {
        // Tests MessageDTO mapping with partial data
        Message message = new Message();
        message.setId(MESSAGE_ID);
        message.setContent(CONTENT);
        message.setMessageType(Message.MessageType.FILE);  // File attachment message
        message.setTimestamp(TIMESTAMP);
        message.setIsDeleted(false);
        message.setUsername("bob");  // Username directly on message

        MessageDTO dto = MessageDtoMapper.toDto(message);

        // Verify all fields are handled even without relationships
        assertThat(dto)
                .hasFieldOrPropertyWithValue("id", MESSAGE_ID)
                .hasFieldOrPropertyWithValue("content", CONTENT)
                .hasFieldOrPropertyWithValue("messageType", "FILE")
                .hasFieldOrPropertyWithValue("userId", null)     
                .hasFieldOrPropertyWithValue("username", "bob")   // Username from field
                .hasFieldOrPropertyWithValue("chatRoomId", null)  
                .hasFieldOrPropertyWithValue("isDeleted", false); // Default deleted state
    }

    private Message createFullMessage() {
        // Helper method to create a complete Message entity with all relationships
        // Used as test data for "complete" scenarios
        User sender = new User();
        sender.setId(USER_ID);  // Sender with UUID

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(ROOM_ID);  // Chatroom with UUID

        // Build complete message with all fields and relationships
        Message message = new Message();
        message.setId(MESSAGE_ID);                    // Unique message identifier
        message.setContent(CONTENT);                  
        message.setMessageType(Message.MessageType.TEXT); 
        message.setTimestamp(TIMESTAMP);              // When message was sent
        message.setIsEdited(true);                   
        message.setEditedAt(EDITED_AT);              
        message.setIsDeleted(false);                  // Not deleted (soft delete)
        message.setSender(sender);                    // Relationship to User
        message.setUsername(USERNAME);                
        message.setChatRoom(chatRoom);                // Relationship to ChatRoom

        return message;
    }
}