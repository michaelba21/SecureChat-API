package com.securechat.dto;

import java.util.UUID;

public class FileUploadRequest {
    private String filename;  // Original name of the uploaded file
    private String filePath;  
    private Long fileSize;   
    private String mimeType;  // Media type for content handling (e.g., "image/png")
    private UUID userId;      
    private Boolean isPublic; // Visibility flag - true=accessible to all, false=restricted

    // Getters and setters for all properties
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}