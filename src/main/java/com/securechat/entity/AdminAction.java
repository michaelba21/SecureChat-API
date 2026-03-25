package com.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_actions")  // Database table name for admin audit logs
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;  // Unique identifier for each admin action record

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore  
    private User admin;  // Admin user who performed the action

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore  // Prevents serialization in API responses
    private User targetUser;  

    @Enumerated(EnumType.STRING)  // Stores enum values as readable strings in database
    @Column(name = "action_type", nullable = false)
    private AdminActionType actionType;  // Type of administrative action performed

    @Column(columnDefinition = "TEXT")  // Allows longer text for detailed explanations
    private String reason;  

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;  // Timestamp when action was executed

    @Column(name = "ip_address", length = 45)  
    private String ipAddress;  // IP address of admin at time of action

    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON string for additional context data (e.g., chatroom ID, message ID)

    @PrePersist  // JPA lifecycle callback - executes before entity is persisted
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();  // Auto-set timestamp if not provided
        }
    }

    // Enum defining all possible admin action types
    public enum AdminActionType {
        BAN_USER,      
        UNBAN_USER,      // Restore banned user access
        DELETE_USER,     
        DELETE_MESSAGE,  // Remove inappropriate message
        DELETE_CHATROOM, 
        REMOVE_MEMBER,   // Remove user from chatroom
        DEACTIVATE_USER, 
        LCK_CHATROOM,    // Lock chatroom (typo preserved in schema)
        UNLOCK_CHATROOM  
    }

    // Getters and Setters for all fields
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }

    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }

    public AdminActionType getActionType() { return actionType; }
    public void setActionType(AdminActionType actionType) { this.actionType = actionType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}