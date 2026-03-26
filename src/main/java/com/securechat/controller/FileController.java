package com.securechat.controller;

import com.securechat.dto.FileUploadResponse;
import com.securechat.service.FileService;
import com.securechat.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for file upload operations.
 * 
 * Sequence:
 * Client ==> Gateway ==> FileController ==> FileService ==> Database
 * 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/files") // Base endpoint for file operations
@RequiredArgsConstructor // Lombok generates constructor with final fields
public class FileController {

    private final FileService fileService;
    private final AuthUtil authUtil; // Centralized authentication utility

    /**
     * I have Uploaded here a file to the system.
     * 
     * @param file
     * @param principal Current user (injected by Spring Security)
     * @return File metadata and download URL
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file, // Multipart file from request
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Use AuthUtil to safely get user ID
        String userId = authUtil.getCurrentUserId(authentication).toString();

        FileUploadResponse response = fileService.uploadFile(file, userId); // Delegate to service
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created with file metadata
    }

    /**
     * Poll for new files in a chat room since a given timestamp.
     *
     * @param chatRoomId     The chat room ID
     * @param sinceTimestamp
     * @return List of new files since the timestamp
     */
    @GetMapping("/poll")
    public ResponseEntity<?> pollNewFiles(
            @RequestParam("chatRoomId") String chatRoomId, // Chat room identifier
            @RequestParam("since") String sinceTimestamp) {

        // Validate sinceTimestamp format
        try {
            LocalDateTime.parse(sinceTimestamp); // Parse to validate ISO format
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid timestamp format"); // 400 if invalid
        }

        var newFiles = fileService.getFilesSince(chatRoomId, sinceTimestamp); // Get files uploaded after timestamp
        var response = newFiles.stream() // Convert entities to response DTOs
                .map(file -> FileUploadResponse.builder()
                        .id(file.getId())
                        .filename(file.getFilename()) // Original filename
                        .downloadUrl("/api/files/download/" + file.getId()) // Download endpoint URL
                        .size(file.getFileSize())
                        .uploadedAt(file.getUploadedAt()) // Upload timestamp
                        .build())
                .toList(); // Collect to list
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable UUID fileId) { // File ID from URL path

        org.springframework.core.io.Resource resource = fileService.downloadFile(fileId); // Get file as Resource

        return ResponseEntity.ok() // 200 OK response
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"") // Force download
                .body(resource); // File content as response body
    }
}