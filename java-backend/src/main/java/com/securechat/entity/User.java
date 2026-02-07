package com.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username"), // Optimized for username lookup
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_is_active", columnList = "is_active"), // Optimized for filtering active/inactive users
    @Index(name = "idx_user_created_at", columnList = "created_at") 
})
public class User {
  
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id; // Primary key - unique identifier for each user

  @Column(unique = true, nullable = false)
  private String username; // Unique username for login and display

  @Column(unique = true, nullable = false)
  private String email; // Unique email address for login and notifications

  @Column(name = "password_hash", nullable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore // Security: never expose password hash in JSON
  private String passwordHash; // Hashed password (not plain text)

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt; // Account creation timestamp

  @Column(name = "last_login")
  private LocalDateTime lastLogin; // Most recent login timestamp

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true; // Soft delete flag - false means account is deactivated

  @Column(columnDefinition = "TEXT")
  private String bio; // User biography/description (unlimited text)

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl; // URL to user's profile picture (max 500 chars)

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private UserStatus status = UserStatus.OFFLINE; // Current online status: ONLINE, OFFLINE, or AWAY

  @ElementCollection(fetch = FetchType.EAGER)
  @Enumerated(EnumType.STRING)
  private Set<UserRole> roles = new HashSet<>(); // User roles (EAGER: loaded with user for security checks)

  // Defines possible user roles for authorization
  public enum UserRole {
    ROLE_USER,   
    ROLE_ADMIN   // Administrative permissions
  }

  // Defines user online status
  public enum UserStatus {
    ONLINE,  
    OFFLINE, // User is not connected
    AWAY     // User is connected but idle
  }

  // Relationship: One user can create many chat rooms
  @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore // Prevents infinite recursion in JSON
  private java.util.List<ChatRoom> createdChatRooms = new java.util.ArrayList<>();

  // Relationship: One user can perform many audit actions
  @OneToMany(mappedBy = "performer", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<MessageAuditLog> auditActions = new java.util.ArrayList<>();

  // Relationship: One user can send many messages
  @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<Message> sentMessages = new java.util.ArrayList<>();

  // Relationship: One user can be a member of many chat rooms
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<ChatRoomMember> memberships = new java.util.ArrayList<>();

  // Relationship: One user can upload many files
  @OneToMany(mappedBy = "uploader", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<File> uploadedFiles = new java.util.ArrayList<>();

  // Relationship: One user can have many refresh tokens (for multiple devices)
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<RefreshToken> refreshTokens = new java.util.ArrayList<>();

  @Column(name = "deactivated_by")
  private UUID deactivatedBy; // Admin who deactivated this user (for audit)

  @Column(name = "deactivated_at")
  private LocalDateTime deactivatedAt; // When user was deactivated

  @Column(name = "deactivation_reason", columnDefinition = "TEXT")
  private String deactivationReason; // Reason for deactivation (unlimited text)

  // Relationship: One admin can perform many admin actions
  @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<AdminAction> performedAdminActions = new java.util.ArrayList<>();

  // Relationship: One user can be the target of many admin actions
  @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, orphanRemoval = true)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private java.util.List<AdminAction> receivedAdminActions = new java.util.ArrayList<>();

  // JPA lifecycle callback - executes before entity is persisted to database
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now(); // Auto-set creation timestamp
    if (lastLogin == null) {
      lastLogin = LocalDateTime.now(); // Default last login to creation time
    }
  }

  // Manual Getters and Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getLastLogin() { return lastLogin; }
  public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }

  public Set<UserRole> getRoles() { return roles; }
  public void setRoles(Set<UserRole> roles) { this.roles = roles != null ? new HashSet<>(roles) : null; }

  public String getBio() { return bio; }
  public void setBio(String bio) { this.bio = bio; }

  public String getAvatarUrl() { return avatarUrl; }
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

  public UserStatus getStatus() { return status; }
  public void setStatus(UserStatus status) { this.status = status; }

  public java.util.List<ChatRoom> getCreatedChatRooms() { return createdChatRooms; }
  public void setCreatedChatRooms(java.util.List<ChatRoom> createdChatRooms) { this.createdChatRooms = createdChatRooms; }

  public java.util.List<Message> getSentMessages() { return sentMessages; }
  public void setSentMessages(java.util.List<Message> sentMessages) { this.sentMessages = sentMessages; }

  public java.util.List<ChatRoomMember> getMemberships() { return memberships; }
  public void setMemberships(java.util.List<ChatRoomMember> memberships) { this.memberships = memberships; }

  public java.util.List<File> getUploadedFiles() { return uploadedFiles; }
  public void setUploadedFiles(java.util.List<File> uploadedFiles) { this.uploadedFiles = uploadedFiles; }

  public java.util.List<RefreshToken> getRefreshTokens() { return refreshTokens; }
  public void setRefreshTokens(java.util.List<RefreshToken> refreshTokens) { this.refreshTokens = refreshTokens; }

  public java.util.List<MessageAuditLog> getAuditActions() { return auditActions; }
  public void setAuditActions(java.util.List<MessageAuditLog> auditActions) { this.auditActions = auditActions; }

  public UUID getDeactivatedBy() { return deactivatedBy; }
  public void setDeactivatedBy(UUID deactivatedBy) { this.deactivatedBy = deactivatedBy; }

  public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
  public void setDeactivatedAt(LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }

  public String getDeactivationReason() { return deactivationReason; }
  public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }

  public java.util.List<AdminAction> getPerformedAdminActions() { return performedAdminActions; }
  public void setPerformedAdminActions(java.util.List<AdminAction> performedAdminActions) { this.performedAdminActions = performedAdminActions; }

  public java.util.List<AdminAction> getReceivedAdminActions() { return receivedAdminActions; }
  public void setReceivedAdminActions(java.util.List<AdminAction> receivedAdminActions) { this.receivedAdminActions = receivedAdminActions; }

  public static Object builder() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'builder'");
  }
}