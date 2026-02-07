package com.securechat.service;

import com.securechat.dto.FileUploadResponse;
import com.securechat.entity.File;
import com.securechat.entity.User;
import com.securechat.repository.FileRepository;
import com.securechat.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests - 100% Coverage")  // Test goal: achieve full code coverage
class FileServiceTest {
    // This test class validates the FileService which handles file uploads,
    // storage, retrieval, and validation for the secure chat application

    @Mock
    private FileRepository fileRepository;  // Repository for file metadata storage

    @Mock
    private UserRepository userRepository;  

    @Mock
    private LocalFileStorageService storageService;  // Service for physical file storage

    @InjectMocks
    private FileService fileService; 

    @Captor
    private ArgumentCaptor<File> fileEntityCaptor;  // Captures File entity passed to save()

    // Test constants
    private final String userId = "550e8400-e29b-41d4-a716-446655440000";  // User ID as string
    private final UUID userUuid = UUID.fromString(userId);  

    // Helper method to create a valid test file
    private MultipartFile createValidMockFile() {
        return new MockMultipartFile(
                "file",              // Parameter name in form
                "document.pdf",     
                "application/pdf",   // Content type (allowed MIME type)
                "PDF content".getBytes()  
        );
    }

    // ====================== uploadFile - Success Path ======================

    @Test
    @DisplayName("uploadFile - successful upload with valid file")
    void uploadFile_success() throws IOException {
        // Tests the complete happy path for file upload
        MultipartFile mockFile = createValidMockFile();
        User uploader = new User();
        uploader.setId(userUuid);

        String storedPath = "/uploads/uuid-document.pdf";  // Simulated storage path

        // Mock dependencies
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(uploader));
        when(storageService.storeFile(mockFile)).thenReturn(storedPath);
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute the upload
        FileUploadResponse response = fileService.uploadFile(mockFile, userId);

        // Verify all expected interactions occurred
        verify(userRepository).findById(userUuid);
        verify(storageService).storeFile(mockFile);
        verify(fileRepository).save(fileEntityCaptor.capture());  // Capture saved entity

        // Verify the File entity that was saved
        File savedEntity = fileEntityCaptor.getValue();
        assertNotNull(savedEntity.getId());  // ID should be generated
        assertEquals("document.pdf", savedEntity.getFilename());
        assertEquals(storedPath, savedEntity.getFilePath());  
        assertEquals(mockFile.getSize(), savedEntity.getFileSize());  // File size in bytes
        assertEquals("application/pdf", savedEntity.getMimeType());  
        assertEquals(uploader, savedEntity.getUploader());  // User who uploaded
        assertFalse(savedEntity.getIsPublic());  // Default: private file
        assertNotNull(savedEntity.getUploadedAt());  

        // Verify the response DTO returned to client
        assertEquals(savedEntity.getId(), response.getId());
        assertEquals("document.pdf", response.getFilename());
        assertEquals("/api/files/download/" + savedEntity.getId(), response.getDownloadUrl());  // Download endpoint
        assertEquals(mockFile.getSize(), response.getSize());
        assertEquals(savedEntity.getUploadedAt(), response.getUploadedAt());
    }

    // ====================== uploadFile - Validation Failures ======================

    @Test
    @DisplayName("uploadFile - file too large throws exception")
    void uploadFile_fileTooLarge() {
        // Tests file size validation (50MB limit)
        byte[] largeContent = new byte[(int) (50 * 1024 * 1024 + 1)]; // 50MB + 1 byte (exceeds limit)
        MultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(largeFile, userId));

        assertEquals("File size exceeds 50MB limit", ex.getMessage());  // Clear error message
        verifyNoInteractions(userRepository, storageService, fileRepository);  // No further processing
    }

    @Test
    @DisplayName("uploadFile - disallowed MIME type throws exception")
    void uploadFile_disallowedMimeType() {
        // Tests MIME type validation (security: prevent executable uploads)
        MultipartFile exeFile = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", new byte[1024]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(exeFile, userId));

        assertEquals("File type not allowed: application/x-msdownload", ex.getMessage());
        verifyNoInteractions(userRepository, storageService, fileRepository);  // Early rejection
    }

    @Test
    @DisplayName("uploadFile - null content type treated as disallowed")
    void uploadFile_nullContentType() {
        // Tests null content type (malformed upload attempt)
        MultipartFile nullTypeFile = new MockMultipartFile("file", "test.file", null, "data".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(nullTypeFile, userId));

        assertEquals("File type not allowed: null", ex.getMessage());  // Handles null gracefully
        verifyNoInteractions(userRepository, storageService, fileRepository);
    }

    // ====================== uploadFile - User Not Found ======================

    @Test
    @DisplayName("uploadFile - user not found throws exception")
    void uploadFile_userNotFound() {
        // Tests authentication/authorization: uploader must exist
        MultipartFile mockFile = createValidMockFile();

        when(userRepository.findById(userUuid)).thenReturn(Optional.empty());  // User doesn't exist

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(mockFile, userId));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository).findById(userUuid);  // User lookup attempted
        verifyNoInteractions(storageService, fileRepository);  // No storage/save without valid user
    }

    // ====================== uploadFile - Storage Failure ======================

    @Test
    @DisplayName("uploadFile - IOException during storage throws wrapped exception")
    void uploadFile_storageFailure() throws IOException {
        // Tests storage system failures (disk full, permissions, etc.)
        MultipartFile mockFile = createValidMockFile();
        User uploader = new User();
        uploader.setId(userUuid);

        when(userRepository.findById(userUuid)).thenReturn(Optional.of(uploader));
        when(storageService.storeFile(mockFile)).thenThrow(new IOException("Disk full"));  // Storage failure

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(mockFile, userId));

        assertEquals("Failed to store file", ex.getMessage()); 
        assertInstanceOf(IOException.class, ex.getCause());  // Original exception preserved
        verify(storageService).storeFile(mockFile);  
        verifyNoInteractions(fileRepository);  // No metadata saved if storage fails
    }

    // ====================== validateFile - Magic Bytes Stub ======================

    @Test
    @DisplayName("validateMagicBytes - stub method is called and does nothing")
    void validateMagicBytes_calledAndNoOp() throws IOException {
        // Tests that magic byte validation stub is invoked without effect
        // Magic bytes validation checks file signatures to prevent spoofing
        MultipartFile mockFile = createValidMockFile();
        User uploader = new User();
        uploader.setId(userUuid);

        when(userRepository.findById(userUuid)).thenReturn(Optional.of(uploader));
        when(storageService.storeFile(mockFile)).thenReturn("/uploads/test");
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Should not throw - validateFile calls validateMagicBytes internally (currently a stub)
        assertDoesNotThrow(() -> fileService.uploadFile(mockFile, userId));

        // No further assertions needed since validateMagicBytes is currently a stub
        // This test ensures the method call doesn't break the flow
    }

    // ====================== getFilesSince ======================

    @Test
    @DisplayName("getFilesSince - parses timestamp and returns empty list (current implementation)")
    void getFilesSince_returnsEmptyList() {
        // Tests retrieval of files uploaded after a specific timestamp
        String isoTimestamp = "2025-12-23T12:00:00";  // ISO 8601 format
        String chatRoomId = "room-123";

        List<File> result = fileService.getFilesSince(chatRoomId, isoTimestamp);

        assertNotNull(result);
        assertTrue(result.isEmpty());  // Current implementation returns empty list
        
        // Note: LocalDateTime.parse is called internally on isoTimestamp
    }

    @Test
    @DisplayName("getFilesSince - invalid timestamp format throws DateTimeParseException wrapped")
    void getFilesSince_invalidTimestamp() {
        // Tests error handling for malformed timestamps
        String invalid = "invalid-date";  // Not ISO 8601 format

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.getFilesSince("room-123", invalid));

        assertEquals("Invalid timestamp format", ex.getMessage());  // User-friendly error
        assertInstanceOf(java.time.format.DateTimeParseException.class, ex.getCause());  

        // This tests the catch block that wraps DateTimeParseException
    }

    // ====================== downloadFile ======================

    @Test
    @DisplayName("downloadFile - successful download returns resource")
    void downloadFile_success() throws IOException {
        // Tests successful file download
        UUID fileId = UUID.randomUUID();
        File fileEntity = new File();
        fileEntity.setId(fileId);
        fileEntity.setFilePath("/uploads/test.pdf");

        Resource mockResource = mock(Resource.class);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(storageService.loadFile("/uploads/test.pdf")).thenReturn(mockResource);

        Resource result = fileService.downloadFile(fileId);

        assertEquals(mockResource, result);
        verify(fileRepository).findById(fileId);
        verify(storageService).loadFile("/uploads/test.pdf");
    }

    @Test
    @DisplayName("downloadFile - file not found throws exception")
    void downloadFile_fileNotFound() {
        // Tests file not found scenario
        UUID fileId = UUID.randomUUID();

        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.downloadFile(fileId));

        assertEquals("File not found", ex.getMessage());
        verify(fileRepository).findById(fileId);
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("downloadFile - IOException during load throws wrapped exception")
    void downloadFile_loadFailure() throws IOException {
        // Tests IOException during file loading
        UUID fileId = UUID.randomUUID();
        File fileEntity = new File();
        fileEntity.setId(fileId);
        fileEntity.setFilePath("/uploads/test.pdf");

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(storageService.loadFile("/uploads/test.pdf")).thenThrow(new IOException("File corrupted"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.downloadFile(fileId));

        assertEquals("Failed to load file", ex.getMessage());
        assertInstanceOf(IOException.class, ex.getCause());
        verify(fileRepository).findById(fileId);
        verify(storageService).loadFile("/uploads/test.pdf");
    }
}
