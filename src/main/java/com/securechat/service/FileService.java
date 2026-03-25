package com.securechat.service;

import com.securechat.dto.FileUploadResponse;
import com.securechat.entity.File;
import com.securechat.entity.User;
import com.securechat.repository.FileRepository;
import com.securechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for file upload and management.
 * 
 * Implementation of the file upload sequence:
 * 1. Check file type & size
 * 2. Save file metadata to database
 * 3. Return file information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageService storageService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif",
        "application/pdf", "text/plain"
    );

    public FileUploadResponse uploadFile(MultipartFile file, String userId) {
        // Step 1: Check file type & size
        validateFile(file);
        
        // Step 2: Get user
        User uploader = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Step 3: Store file physically
        String storedPath;
        try {
            storedPath = storageService.storeFile(file);
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
        
        // Step 4: Save metadata to database
        File fileEntity = new File();
        fileEntity.setId(UUID.randomUUID());
        fileEntity.setFilename(file.getOriginalFilename());
        fileEntity.setFilePath(storedPath);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setUploader(uploader);
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setIsPublic(false);
        
        fileRepository.save(fileEntity);
        
        log.info("File uploaded successfully: {} by user {}", file.getOriginalFilename(), userId);
        
        // Step 5: Return file info
        return FileUploadResponse.builder()
            .id(fileEntity.getId())
            .filename(file.getOriginalFilename())
            .downloadUrl("/api/files/download/" + fileEntity.getId())
            .size(file.getSize())
            .uploadedAt(fileEntity.getUploadedAt())
            .build();
    }

    private void validateFile(MultipartFile file) {
        // Check size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds 50MB limit");
        }
        
        // Check MIME type
        if (file.getContentType() == null) {
            throw new RuntimeException("File type not allowed: null");
        }
        
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("File type not allowed: " + file.getContentType());
        }
        
        // Magic byte validation (stub - as mentioned in report)
        validateMagicBytes(file);
    }

    private void validateMagicBytes(MultipartFile file) {
        // Implementation stub for magic byte validation
        // This would check file signatures to prevent malicious uploads
    }

    public org.springframework.core.io.Resource downloadFile(UUID fileId) {
        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
        
        try {
            return storageService.loadFile(file.getFilePath());
        } catch (IOException e) {
            log.error("Failed to load file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Failed to load file", e);
        }
    }

    public List<File> getFilesSince(String chatRoomId, String sinceTimestamp) {  
        // This is a placeholder implementation - would need to be implemented
        // via a join table or additional relationship
        LocalDateTime since;
        try {
            since = LocalDateTime.parse(sinceTimestamp);
        } catch (Exception e) {
            throw new RuntimeException("Invalid timestamp format", e);
        }
        
      
        return List.of();
    }
}