package com.securechat.mapper;

import com.securechat.dto.FileUploadRequest;
import com.securechat.entity.File;
import com.securechat.entity.User;

/**
 * Utility class for converting between File entities and file-related DTOs.
 * This handles the transformation for file upload operations.
 */
public final class FileDtoMapper {

    // Private constructor that throws an exception to enforce utility class pattern
    private FileDtoMapper() {
        throw new IllegalAccessError("class com.securechat.mapper.FileDtoMapper cannot be instantiated");
    }

    /**
     * @param request The DTO containing file upload data (can be null)
     * @param owner The User entity who is uploading the file (file owner)
     * @return 
     */
    public static File toEntity(FileUploadRequest request, User owner) {
        if (request == null) {
            return null; // Handle null input gracefully
        }
        File file = new File();
        file.setUploader(owner); // Set the user who uploaded the file
        file.setFilename(request.getFilename());
        file.setFilePath(request.getFilePath()); // Server storage path where file is saved
        file.setFileSize(request.getFileSize()); 
        file.setMimeType(request.getMimeType()); // Content type (e.g., image/jpeg, application/pdf)
        file.setIsPublic(Boolean.TRUE.equals(request.getIsPublic())); 
        return file;
    }
}