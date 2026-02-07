
package com.securechat.service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;
    // Constructor injection with default value if property not set
    @Autowired
    public LocalFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }
// Executes after dependency injection to ensure directory exists
    @PostConstruct
    public void init() throws IOException {
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
                log.info("Initialized upload directory at: {}", uploadDir);
            } catch (IOException e) {
                log.error("Could not create upload directory: {}", uploadDir, e);
                throw e;
            }
        }
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String sanitizedName = sanitizeFilename(originalName != null ? originalName : "file");
        // Generate unique filename to prevent collisions
        String uniqueFileName = UUID.randomUUID() + "_" + sanitizedName;
        Path filePath = uploadDir.resolve(uniqueFileName).normalize();

        // Extra security check: ensure the resolved path is still inside the upload directory
        if (!filePath.startsWith(uploadDir)) {
            log.error("Attempted path traversal detected during store: {}", uniqueFileName);
            throw new SecurityException("Cannot store file outside of upload directory");
        }

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file {} at {}", uniqueFileName, filePath);
            return uniqueFileName;
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", uniqueFileName, e.getMessage());
            throw e;
        }
    }

    @Override
    public Resource loadFile(String filename) throws IOException {
        Path filePath = validateAndResolvePath(filename);
        Resource resource = new UrlResource(filePath.toUri());
  // Validate filename and resolve to absolute path and Check if file exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("File not found or not readable: {}", filename);
            throw new FileNotFoundException("File not found: " + filename);
        }

        return resource;
    }

    @Override
    public void deleteFile(String filename) throws IOException {
        Path filePath = validateAndResolvePath(filename);
        if (Files.deleteIfExists(filePath)) {
            log.debug("Deleted file: {}", filename);
        } else {
            log.debug("Delete skipped, file not found: {}", filename);
        }
    }
 // Centralized path validation and resolution logic
    private Path validateAndResolvePath(String filename) {
        if (filename == null || filename.contains("..")) {
            log.error("Invalid filename or path traversal attempt: {}", filename);
            throw new SecurityException("Invalid filename");
        }
        
        Path filePath = uploadDir.resolve(filename).normalize();
        if (!filePath.startsWith(uploadDir)) {
            log.error("Path traversal attempt detected: {}", filename);
            throw new SecurityException("Access denied");
        }
        return filePath;
    }

    /**
     * Sanitizes the filename to prevent path traversal while keeping the extension intact.
     * Removes non-safe characters and handles ".." sequences.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }

        // Step 1: Replace ".." with "___" to prevent directory traversal
        String cleaned = filename.replace("..", "___");

        // Step 2: Remove all non-alphanumeric characters except dots, underscores, and hyphens
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "");

        // Step 3: Add leading underscore to indicate sanitization
        cleaned = "_" + cleaned;

        // Step 4: Safety fallback
        if (cleaned.isBlank() || cleaned.matches("[._]+")) {
            return "file";
        }

        return cleaned;
    }
}