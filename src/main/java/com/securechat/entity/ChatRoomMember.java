package com.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
// Maps to database table with indexes for performance and unique constraint to prevent duplicate memberships
@Table(name = "chat_room_members", indexes = {
    @Index(name = "idx_member_chatroom", columnList = "chat_room_id"),           // Fast lookup by chat room
    @Index(name = "idx_member_user", columnList = "user_id"),                   
    @Index(name = "idx_member_chatroom_user", columnList = "chat_room_id, user_id"), // Composite index for membership queries
    @Index(name = "idx_member_is_active", columnList = "is_active")             
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})                // One membership per user per chat room
})
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generates UUID as primary key
    private UUID id;

    // Foreign key column - read-only (managed by JPA relationship)
    @Column(name = "chat_room_id", nullable = false, insertable = false, updatable = false)
    private UUID chatRoomId;

    // Many-to-one relationship with ChatRoom (lazy loading for performance)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)  // Maps to same column as chatRoomId
    @com.fasterxml.jackson.annotation.JsonIgnore  
    private ChatRoom chatRoom;

   
    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    // Many-to-one relationship with User (lazy loading for performance)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  
    @com.fasterxml.jackson.annotation.JsonIgnore  // Prevents infinite recursion in JSON serialization
    private User user;

    @Column(name = "role")
    private String role = "MEMBER"; // Role hierarchy: ADMIN (full control), MODERATOR (some permissions), MEMBER (basic access)

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;  

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt; 

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;  

    @Column(name = "left_at")
    private LocalDateTime leftAt;  

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // Soft delete flag - true if currently in room, false if left/removed

    @Column(name = "removed_by")
    private UUID removedBy;  // User ID of admin/moderator who removed this member (null if left voluntarily)

    //  Default constructor required by JPA
    public ChatRoomMember() {
    
    }

    // Convenience constructor for creating new memberships
    public ChatRoomMember(ChatRoom chatRoom, User user, String role) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.role = role;
        this.isActive = true;  // New members are active by default
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UUID getChatRoomId() {
        return chatRoomId;
    }

    // chatRoomId should be set automatically when chatRoom is set
    public void setChatRoomId(UUID chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public UUID getUserId() {
        return userId;
    }

    // Note: userId should be set automatically when user is set
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public UUID getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(UUID removedBy) {
        this.removedBy = removedBy;
    }

    // Utility methods
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);  // Check if user has admin privileges
    }

    public void markAsRead() {
        this.lastReadAt = LocalDateTime.now();  // Update last read time to current timestamp
    }

    public void leaveRoom() {
        this.isActive = false;  // Mark as inactive (soft delete)
        this.lastReadAt = LocalDateTime.now();  
       
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    // JPA lifecycle callback - executes before entity is persisted to database
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();  // Auto-set join time if not provided
        }
        if (isActive == null) {
            isActive = true;  
        }
        if (role == null) {
            role = "MEMBER";  // Default to MEMBER role
        }
    }

    /**
     * Public method for testing purposes to simulate JPA @PrePersist lifecycle callback
     * Allows unit tests to trigger the same initialization logic without JPA
     */
    public void simulatePrePersist() {
        onCreate();  // Manually trigger the pre-persist logic
    }
}