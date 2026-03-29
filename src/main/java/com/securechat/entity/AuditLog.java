package com.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")  // Database table for system-wide audit trail
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;  // Unique identifier for each audit log entry

    @Column(name = "event_type", nullable = false)
    private String eventType;  // Category of event (e.g., "USER_LOGIN", "MESSAGE_SENT")

    @Column(name = "user_id")
    private UUID userId;  // Optional: Identifier of user who triggered the event

    @Column(name = "resource_type")
    private String resourceType;  // Type of resource affected (e.g., "ChatRoom", "Message")

    @Column(name = "resource_id")
    private UUID resourceId;  // Identifier of specific resource affected by event

    @Column(nullable = false)
    private String action;  

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;  // When the event occurred

    @Column(name = "ip_address")
    private String ipAddress;  

    @Column(name = "user_agent")
    private String userAgent; 

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;  // Additional context data stored as JSON string

    @PrePersist  // JPA lifecycle callback - executes before entity is persisted
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();  
        }
    }

    // === Getters and Setters (all NON-STATIC) ===

    public UUID getId() { 
        return id; 
    }

    public void setId(UUID id) { 
        this.id = id; 
    }

    public String getEventType() { 
        return eventType; 
    }

    public void setEventType(String eventType) { 
        this.eventType = eventType; 
    }

    public UUID getUserId() { 
        return userId; 
    }

    public void setUserId(UUID userId) { 
        this.userId = userId; 
    }

    public String getResourceType() { 
        return resourceType; 
    }

    public void setResourceType(String resourceType) { 
        this.resourceType = resourceType; 
    }

    public UUID getResourceId() { 
        return resourceId; 
    }

    public void setResourceId(UUID resourceId) { 
        this.resourceId = resourceId; 
    }

    public String getAction() { 
        return action; 
    }

    public void setAction(String action) { 
        this.action = action; 
    }

    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }

    public void setTimestamp(LocalDateTime timestamp) { 
        this.timestamp = timestamp; 
    }

    public String getIpAddress() { 
        return ipAddress; 
    }

    public void setIpAddress(String ipAddress) { 
        this.ipAddress = ipAddress; 
    }

    public String getUserAgent() { 
        return userAgent; 
    }

    public void setUserAgent(String userAgent) { 
        this.userAgent = userAgent; 
    }

    public String getDetailsJson() { 
        return detailsJson; 
    }

    public void setDetailsJson(String detailsJson) { 
        this.detailsJson = detailsJson; 
    }
}