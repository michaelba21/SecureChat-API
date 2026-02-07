package com.securechat.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;  // UUID class for unique identifiers

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

class FileUploadRequestTest {

    @Test
    void gettersAndSetters_workCorrectly_forAllFields() {
        // Test: Verify getters and setters work correctly for all fields
        
        // Arrange: Create FileUploadRequest object and test data
        FileUploadRequest request = new FileUploadRequest();  // Create DTO instance

        String filename = "document.pdf"; 
        String filePath = "/uploads/document.pdf";  // Test file path
        Long fileSize = 2048L;  // Test file size in bytes
        String mimeType = "application/pdf";  
        UUID userId = UUID.randomUUID();  // Random user ID
        Boolean isPublic = true;  

        // Act: Set all fields using setters
        request.setFilename(filename);
        request.setFilePath(filePath);
        request.setFileSize(fileSize);
        request.setMimeType(mimeType);
        request.setUserId(userId);
        request.setIsPublic(isPublic);

        // Assert: Verify all fields are correctly retrieved using getters
        assertThat(request.getFilename()).isEqualTo(filename);  // Verify filename
        assertThat(request.getFilePath()).isEqualTo(filePath);  
        assertThat(request.getFileSize()).isEqualTo(fileSize);  // Verify file size
        assertThat(request.getMimeType()).isEqualTo(mimeType);  
        assertThat(request.getUserId()).isEqualTo(userId);  // Verify user ID
        assertThat(request.getIsPublic()).isEqualTo(isPublic); 
    }

    @Test
    void setters_acceptNullValues() {
        // Test: Verify setters accept null values (important for validation/optional fields)
        
        FileUploadRequest request = new FileUploadRequest();  // Create new DTO

        // Act: Set all fields to null
        request.setFilename(null);  
        request.setFilePath(null);  // Set file path to null
        request.setFileSize(null);  
        request.setMimeType(null);  // Set MIME type to null
        request.setUserId(null);  
        request.setIsPublic(null);  // Set public flag to null

        // Assert: All fields should be null
        assertThat(request.getFilename()).isNull();  
        assertThat(request.getFilePath()).isNull();  // File path should be null
        assertThat(request.getFileSize()).isNull();  
        assertThat(request.getMimeType()).isNull();  // MIME type should be null
        assertThat(request.getUserId()).isNull();  
        assertThat(request.getIsPublic()).isNull();  // Public flag should be null
    }

    @Test
    void fullObject_canBeConstructedAndReadCorrectly() {
        // Test: Create a complete FileUploadRequest object and verify all fields
        
        FileUploadRequest request = new FileUploadRequest();  

        UUID userId = UUID.randomUUID();  // Generate random user ID for test

        // Act: Set all fields with sample values
        request.setFilename("image.jpg");  // Set JPEG filename
        request.setFilePath("/storage/images/image.jpg");  
        request.setFileSize(51200L);  // Set file size (50KB)
        request.setMimeType("image/jpeg");  // Set MIME type for JPEG
        request.setUserId(userId);  
        request.setIsPublic(false);  

        // Assert: Verify all fields using AssertJ's field assertions
        assertThat(request)
                .hasFieldOrPropertyWithValue("filename", "image.jpg")  
                .hasFieldOrPropertyWithValue("filePath", "/storage/images/image.jpg")  // Verify path
                .hasFieldOrPropertyWithValue("fileSize", 51200L)  
                .hasFieldOrPropertyWithValue("mimeType", "image/jpeg")  // Verify MIME type
                .hasFieldOrPropertyWithValue("userId", userId)  
                .hasFieldOrPropertyWithValue("isPublic", false);  // Verify private flag
    }

    @Test
    void defaultValues_areNull() {
        // Test: Verify default values are null for a newly created object (no-args constructor)
        
        FileUploadRequest request = new FileUploadRequest();  // Create DTO with default constructor

        // Assert: All fields should be null initially
        assertThat(request.getFilename()).isNull();  // Filename default is null
        assertThat(request.getFilePath()).isNull();  
        assertThat(request.getFileSize()).isNull();  // File size default is null
        assertThat(request.getMimeType()).isNull(); 
        assertThat(request.getUserId()).isNull();  // User ID default is null
        assertThat(request.getIsPublic()).isNull(); 
    }
}