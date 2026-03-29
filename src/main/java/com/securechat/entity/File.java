package com.securechat.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "files")
public class File {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id; // Primary key - automatically generated UUID

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploader_id", nullable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore // Prevents JSON serialization of the uploader object
  private User uploader; 

  @Column(nullable = false)
  private String filename; // Original name of the uploaded file

  @Column(name = "file_path", nullable = false, length = 500)
  private String filePath; // Storage path on server (max 500 characters)

  @Column(name = "file_size", nullable = false)
  private Long fileSize; 

  @Column(name = "mime_type", nullable = false)
  private String mimeType; // Content type (e.g., image/jpeg, application/pdf)

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt; // Timestamp when file was uploaded

  @Column(name = "is_public", nullable = false)
  private Boolean isPublic = false; // Visibility flag - false means only authorized users can access

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt; 

  @Column(name = "deleted_by")
  private UUID deletedBy; 

  // JPA lifecycle callback - executes before entity is persisted to database
  @PrePersist
  protected void onCreate() {
    // Auto-set upload timestamp if not already provided
    if (uploadedAt == null) {
      uploadedAt = LocalDateTime.now();
    }
  }

  // Manual Getters and Setters with nullability annotations
  public UUID getId() { return id; }
  public void setId(@Nullable UUID id) { this.id = id; } // ID can be null before saving

  public User getUploader() { return uploader; }
  public void setUploader(@Nullable User uploader) { this.uploader = uploader; }

  @Nonnull
  public String getFilename() { return filename; }
  public void setFilename(@Nullable String filename) { this.filename = filename; }

  @Nonnull
  public String getFilePath() { return filePath; }
  public void setFilePath(@Nullable String filePath) { this.filePath = filePath; }

  @Nonnull
  public Long getFileSize() { return fileSize; }
  public void setFileSize(@Nullable Long fileSize) { this.fileSize = fileSize; }

  @Nonnull
  public String getMimeType() { return mimeType; }
  public void setMimeType(@Nullable String mimeType) { this.mimeType = mimeType; }

  @Nonnull
  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(@Nullable LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

  @Nullable
  public Boolean getIsPublic() { return isPublic; }
  public void setIsPublic(@Nullable Boolean isPublic) { this.isPublic = isPublic; }

  @Nullable
  public LocalDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(@Nullable LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

  @Nullable
  public UUID getDeletedBy() { return deletedBy; }
  public void setDeletedBy(@Nullable UUID deletedBy) { this.deletedBy = deletedBy; }
}