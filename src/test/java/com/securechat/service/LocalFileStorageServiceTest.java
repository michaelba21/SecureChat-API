package com.securechat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class LocalFileStorageServiceTest {
    // This test class validates the LocalFileStorageService which handles
    // physical file storage operations (save, load, delete) on the local filesystem

    private LocalFileStorageService fileStorageService;

    @TempDir
    Path tempDir;  // JUnit 5 temporary directory for isolated file operations

    @BeforeEach
    void setUp() throws IOException {
        // Setup before each test: create service instance with temp directory
        fileStorageService = new LocalFileStorageService(tempDir.toString());
        fileStorageService.init();  // Initialize service (creates directory if needed)
    }

    @Test
    void init_createsUploadDirectory_whenNotExists() {
        // Tests that the service creates the upload directory during initialization
        assertThat(Files.exists(tempDir)).isTrue();  
        assertThat(Files.isDirectory(tempDir)).isTrue();  // It's a directory (not a file)
    }

    @Test
    void storeFile_savesFileWithUniqueName_andReturnsFilename() throws IOException {
        // Tests successful file storage with unique filename generation
        String originalName = "test-image.jpg";
        byte[] content = "test file content".getBytes();
        // Create a mock multipart file (simulates HTTP file upload)
        MultipartFile multipartFile = new MockMultipartFile("file", originalName, "image/jpeg", content);

        // Store the file - returns unique filename
        String savedFilename = fileStorageService.storeFile(multipartFile);

        // Verify filename is generated and contains original name
        assertThat(savedFilename)
                .isNotNull()
                .contains("_test-image.jpg");  // Pattern: {uuid}_{originalName}

        // Verify file was actually saved to disk
        Path savedPath = tempDir.resolve(savedFilename);
        assertThat(Files.exists(savedPath)).isTrue();  
        assertThat(Files.readAllBytes(savedPath)).isEqualTo(content);  // Content matches
    }

    @Test
    void storeFile_sanitizesFilename() throws IOException {
        // Tests filename sanitization to prevent path traversal attacks
        String originalName = "dangerous/../file.exe";  // Malicious filename with path traversal
        MultipartFile multipartFile = new MockMultipartFile("file", originalName, "application/octet-stream", "data".getBytes());

        String savedFilename = fileStorageService.storeFile(multipartFile);

        // Verify dangerous characters are sanitized
        assertThat(savedFilename).doesNotContain("..");  // Path traversal removed
        assertThat(savedFilename).contains("_dangerous___file.exe");  
    }

    @Test
    void loadFile_returnsResource_whenFileExists() throws IOException {
        // Tests loading an existing file as Spring Resource
        String filename = "test.txt";
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, "content".getBytes());  // Create test file

        // Load file as Resource
        Resource resource = fileStorageService.loadFile(filename);

        // Verify resource properties
        assertThat(resource).isInstanceOf(UrlResource.class);  // Should be UrlResource
        assertThat(resource.exists()).isTrue();  
        assertThat(resource.isReadable()).isTrue();  // File is readable
        assertThat(resource.getInputStream()).hasContent("content");  
    }

    @Test
    void loadFile_throwsSecurityException_onPathTraversal() {
        // Tests security: prevents loading files outside upload directory
        String filename = "../secrets.txt";  

        assertThatThrownBy(() -> fileStorageService.loadFile(filename))
                .isInstanceOf(SecurityException.class);  // Should throw SecurityException
    }

    @Test
    void loadFile_throwsFileNotFoundException_whenFileDoesNotExist() {
        // Tests error handling for non-existent files
        String filename = "nonexistent.txt";

        assertThatThrownBy(() -> fileStorageService.loadFile(filename))
                .isInstanceOf(FileNotFoundException.class);  // Should throw FileNotFoundException
    }

    @Test
    void deleteFile_removesFile_whenExists() throws IOException {
        // Tests successful file deletion
        String filename = "to-delete.txt";
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, "delete me".getBytes());  // Create file

        fileStorageService.deleteFile(filename); 

        assertThat(Files.exists(filePath)).isFalse();  // File should no longer exist
    }

    @Test
    void deleteFile_throwsSecurityException_onPathTraversal() {
        // Tests security: prevents deleting files outside upload directory
        String filename = "../../etc/passwd";  // Attempt to delete system file

        assertThatThrownBy(() -> fileStorageService.deleteFile(filename))
                .isInstanceOf(SecurityException.class);  
    }

    @Test
    void deleteFile_doesNothing_whenFileDoesNotExist() throws IOException {
        // Tests idempotent behavior: deleting non-existent file doesn't throw
        String filename = "not-here.txt";

        assertThatCode(() -> fileStorageService.deleteFile(filename))
                .doesNotThrowAnyException();  // Should not throw exception
        // This is important for cleanup operations that might be called multiple times
    }
}