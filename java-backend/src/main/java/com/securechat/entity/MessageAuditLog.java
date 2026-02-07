package com.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_audit_logs")
public class MessageAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Primary key - unique identifier for each audit log entry

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Message message; // The message being audited (lazy loaded for performance)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User performer; // User who performed the audited action (lazy loaded for performance)

    @Column(columnDefinition = "TEXT")
    private String content; // Current content after the action (or new content for CREATE/UPDATE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action; // Type of action performed: CREATE, UPDATE, DELETE, or RESTORE

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt; // Timestamp when the action occurred

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IP address of the performer (supports IPv4 and IPv6 - max 45 chars)

    @Column(name = "user_agent")
    private String userAgent; // Browser/device information of the performer

    @Column(columnDefinition = "TEXT")
    private String reason; // Optional reason provided for the action (especially for DELETE)

    @Column(name = "old_content", columnDefinition = "TEXT")
    private String oldContent; // Content before update (only populated for UPDATE actions)

    // JPA lifecycle callback - executes before entity is persisted to database
    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now(); // Auto-set timestamp if not provided
        }
    }

    // Defines the audit actions that can be logged
    public enum AuditAction {
        CREATE,   // When a new message is created
        UPDATE,   
        DELETE,   // When a message is soft-deleted
        RESTORE   
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }

    public User getPerformer() { return performer; }
    public void setPerformer(User performer) { this.performer = performer; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getOldContent() { return oldContent; }
    public void setOldContent(String oldContent) { this.oldContent = oldContent; }
}