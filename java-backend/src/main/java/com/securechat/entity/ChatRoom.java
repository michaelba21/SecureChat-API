package com.securechat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_chatroom_created_by", columnList = "created_by"),      // Optimizes creator-based queries
    @Index(name = "idx_chatroom_is_private", columnList = "is_private"),      // Optimizes privacy-based filtering
    @Index(name = "idx_chatroom_created_at", columnList = "created_at")       // Optimizes chronological sorting
})
public class ChatRoom {
  
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;  // Unique identifier for the chatroom

  @Column(nullable = false, length = 100)
  private String name;  // Display name with 100-character limit

  @Column(length = 500)
  private String description;  // Optional description with 500-character limit

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  @JsonIgnore  // Prevents circular JSON serialization when serializing ChatRoom
  private User createdBy;  

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;  // Timestamp of chatroom creation

  @Column(name = "is_private", nullable = false)
  private Boolean isPrivate = false;  // Privacy flag - default is public chatroom

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants = 100;  // Capacity limit with default of 100 users

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;  // Soft delete timestamp (null if active)

  @Column(name = "deleted_by")
  private UUID deletedBy;  // User ID who performed soft delete (nullable)

  @OneToMany(
      mappedBy = "chatRoom",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @JsonIgnore  // Prevents serializing all messages when fetching chatroom
  private java.util.List<Message> messages = new java.util.ArrayList<>();  // All messages in chatroom

  @OneToMany(
      mappedBy = "chatRoom",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @JsonIgnore  // Prevents serializing all members when fetching chatroom
  private java.util.List<ChatRoomMember> members = new java.util.ArrayList<>();  

  @PrePersist  // JPA lifecycle callback - executes before entity is persisted
  protected void onCreate() {
    createdAt = LocalDateTime.now();  
  }

  // Manual Getters and Setters (no Lombok for explicit control)
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public User getCreatedBy() { return createdBy; }
  public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public Boolean getIsPrivate() { return isPrivate; }
  public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

  public Integer getMaxParticipants() { return maxParticipants; }
  public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

  public LocalDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

  public UUID getDeletedBy() { return deletedBy; }
  public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }

  public java.util.List<Message> getMessages() { return messages; }
  public void setMessages(java.util.List<Message> messages) { this.messages = messages; }

  public java.util.List<ChatRoomMember> getMembers() { return members; }
  public void setMembers(java.util.List<ChatRoomMember> members) { this.members = members; }
}