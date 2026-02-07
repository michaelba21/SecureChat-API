package com.securechat.mapper;

import com.securechat.dto.FileUploadRequest;
import com.securechat.entity.File;
import com.securechat.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FileDtoMapper Tests")  // Descriptive test class name for better test reporting
class FileDtoMapperTest {
    // This test class validates the FileDtoMapper which converts FileUploadRequest DTOs
    // to File entities, specifically for file upload operations

    @Test
    @DisplayName("toEntity maps all fields correctly when request is valid")  // Readable test name
    void toEntity_mapsCorrectly() {
        // Arrange - Setup test data (Given)
        User owner = new User();
        owner.setId(java.util.UUID.randomUUID());  // Simulate a user with UUID

        // Create a complete file upload request with all fields populated
        FileUploadRequest request = new FileUploadRequest();
        request.setFilename("document.pdf");                    // Original filename
        request.setFilePath("/storage/uuid-document.pdf");     
        request.setFileSize(102400L);                           // File size in bytes (100 KB)
        request.setMimeType("application/pdf");                 
        request.setIsPublic(true);                              // Accessibility flag

        // Act - Execute the method under test (When)
        File file = FileDtoMapper.toEntity(request, owner);

        // Assert - Verify the results (Then)
        assertThat(file).isNotNull();  
        
        // Verify the owner/uploader relationship is correctly established
        assertThat(file.getUploader()).isSameAs(owner);  
        
        // Verify all fields are correctly mapped from request to entity
        assertThat(file.getFilename()).isEqualTo("document.pdf");
        assertThat(file.getFilePath()).isEqualTo("/storage/uuid-document.pdf");
        assertThat(file.getFileSize()).isEqualTo(102400L);
        assertThat(file.getMimeType()).isEqualTo("application/pdf");
        assertThat(file.getIsPublic()).isTrue();  // Public file accessible to others
    }

    @Test
    @DisplayName("toEntity handles isPublic = false correctly")
    void toEntity_handlesIsPublicFalse() {
        // Tests private file creation (isPublic = false)
        // Private files are only accessible to the uploader
        User owner = new User();
        FileUploadRequest request = new FileUploadRequest();
        request.setIsPublic(false);  // Explicitly private file

        File file = FileDtoMapper.toEntity(request, owner);

        assertThat(file.getIsPublic()).isFalse();  // Should be private
    }

    @Test
    @DisplayName("toEntity handles isPublic = null correctly (defaults to false)")
    void toEntity_handlesIsPublicNull() {
        // Tests null safety for isPublic field
        // When client doesn't specify visibility, defaults to private (false) for security
        User owner = new User();
        FileUploadRequest request = new FileUploadRequest();
        request.setIsPublic(null);  // Null value - client didn't specify

        File file = FileDtoMapper.toEntity(request, owner);

        assertThat(file.getIsPublic()).isFalse();  // Should default to private (secure default)
    }

    @Test
    @DisplayName("toEntity returns null when request is null")
    void toEntity_returnsNull_whenRequestIsNull() {
        // Tests null safety: Should handle null request gracefully
        // Prevents NullPointerException in calling code
        User owner = new User();

        File file = FileDtoMapper.toEntity(null, owner);

        assertThat(file).isNull();  // Should return null when request is null
    }

    @Test
    @DisplayName("Private constructor throws when instantiated (utility class)")
    void privateConstructor_throwsWhenInstantiated() {
        // Tests that FileDtoMapper cannot be instantiated (utility class pattern)
      
        // Using reflection to access the private constructor
        assertThatThrownBy(() -> {
            java.lang.reflect.Constructor<FileDtoMapper> constructor =
                    FileDtoMapper.class.getDeclaredConstructor();  // Get private constructor
            constructor.setAccessible(true);  // Bypass access restrictions for testing
            constructor.newInstance();  
        })
        .isInstanceOf(java.lang.reflect.InvocationTargetException.class)  // Wrapped exception
        .hasCauseInstanceOf(IllegalAccessError.class)  
        .extracting(Throwable::getCause)  // Extract the cause from InvocationTargetException
        .extracting(Throwable::getMessage)  
        .asString()  // Convert to string for assertion
        .contains("class com.securechat.mapper.FileDtoMapper cannot be instantiated");  // Verify error message
        
        
    }
}