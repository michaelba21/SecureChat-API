package com.securechat.dto;

import com.securechat.entity.Message.MessageType;  // Imported enum for type-safe message classification

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    private UUID id;              
    private String content;       // The actual message text/content
    private MessageType messageType; 
    private LocalDateTime timestamp; 
    private Boolean isEdited;     // Flag indicating if message has been modified
    private LocalDateTime editedAt; 
    private UUID userId;          // Identifier of the user who sent the message
    private String username;      
    private UUID chatRoomId;      // Identifier of the chatroom containing this message

    // Getters and setters for all properties
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public UUID getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(UUID chatRoomId) { this.chatRoomId = chatRoomId; }
}