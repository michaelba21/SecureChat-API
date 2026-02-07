package com.securechat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePermission {
  
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id; // Primary key

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "file_id", nullable = false)
  private File file; // The file this permission applies to

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User grantedTo; // User receiving the permission

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PermissionType permissionType; // READ, WRITE, or DELETE

  @Column(nullable = false)
  private LocalDateTime grantedAt; // When permission was granted

  @PrePersist
  protected void onCreate() {
    grantedAt = LocalDateTime.now(); // Auto-set timestamp
  }

  public enum PermissionType {
    READ, WRITE, DELETE
  }
}