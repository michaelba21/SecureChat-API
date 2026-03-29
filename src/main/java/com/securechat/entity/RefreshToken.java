package com.securechat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token", columnList = "token"), // Optimized for token lookup during refresh
    @Index(name = "idx_refresh_user_id", columnList = "user_id"), 
    @Index(name = "idx_refresh_expiry_date", columnList = "expiry_date") // Optimized for cleanup of expired tokens
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Primary key - unique identifier for each refresh token record

    @Column(nullable = false, unique = true, length = 512)
    private String token; // The actual JWT refresh token (unique, max 512 chars)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user; // User associated with this refresh token (lazy loaded for performance)

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate; // When this token expires (using Instant for precision)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; 

    @Column(name = "last_used_at")
    private Instant lastUsedAt; // Last time this token was used (null if never used)

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IP address where token was created (supports IPv4 and IPv6)

    @Column(name = "user_agent", length = 512)
    private String userAgent; // Browser/device info where token was created (max 512 chars)

    // JPA lifecycle callback - executes before entity is persisted to database
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now(); // Auto-set creation timestamp if not provided
        }
    }

    /**
     * Checks if the refresh token has expired.
     * @return true if token is expired, false if still valid
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false; 
        }
        // Security: Treat "now" as expired for safety (token expires at the moment it reaches expiry)
        // Uses !now.isBefore(expiryDate) instead of now.isAfter(expiryDate) to include exact expiry time
        return !Instant.now().isBefore(expiryDate);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}