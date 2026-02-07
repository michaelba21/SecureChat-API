package com.securechat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_chatroom_timestamp", columnList = "chat_room_id, timestamp"), // Optimized for retrieving messages by chat room and time
    @Index(name = "idx_message_user", columnList = "user_id"), 
    @Index(name = "idx_message_is_deleted", columnList = "is_deleted"), // Optimized for filtering deleted messages
    @Index(name = "idx_message_timestamp", columnList = "timestamp") 
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Primary key - unique identifier for each message

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User sender; // User who sent the message (lazy loaded for performance)

    @Column(name = "username", nullable = false, length = 50)
    private String username; // Cached username to avoid joining User table for display purposes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ChatRoom chatRoom; // Chat room where message was sent (lazy loaded for performance)

    @Column(nullable = false, length = 5000)
    private String content; // Message text content (max 5000 characters)

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT; // Type of message: TEXT, IMAGE, or FILE

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", unique = true)
    private File attachment; // Optional file attachment (one-to-one relationship)

    @Column(nullable = false)
    private LocalDateTime timestamp; // When the message was sent

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false; // Flag indicating if message has been edited

    @Column(name = "edited_at")
    private LocalDateTime editedAt; // Timestamp of last edit (null if never edited)

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false; // Soft delete flag - true if message is deleted

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // When message was deleted (null if not deleted)
    
    @Column(name = "deleted_by")
    private UUID deletedBy; // ID of user who deleted the message (for audit purposes)

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MessageAuditLog> auditLogs = new ArrayList<>(); // Audit trail for message edits/deletions

    // JPA lifecycle callback - executes before entity is persisted to database
    @PrePersist
    public void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now(); // Auto-set timestamp if not provided
        }
        if (isDeleted == null) {
            isDeleted = false; // Default to not deleted
        }
        if (isEdited == null) {
            isEdited = false; 
        }
        if (messageType == null) {
            messageType = MessageType.TEXT; // Default to TEXT type
        }
    }

    /**
     * Marks the message as deleted (soft delete).
     * Performs logical deletion without physically removing from database.
     * @param userId The ID of the user performing the deletion.
     */
    public void softDelete(UUID userId) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = userId;
    }

    // Defines the types of messages that can be sent
    public enum MessageType {
        TEXT, 
        IMAGE,  // Image file message
        FILE   
    }

    // Manual Getters and Setters (required even with Lombok @Builder annotation)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public ChatRoom getChatRoom() { return chatRoom; }
    public void setChatRoom(ChatRoom chatRoom) { this.chatRoom = chatRoom; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public File getAttachment() { return attachment; }
    public void setAttachment(File attachment) { this.attachment = attachment; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public UUID getDeletedBy() { return deletedBy; }
    public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }

    public List<MessageAuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<MessageAuditLog> auditLogs) { this.auditLogs = auditLogs; }
}