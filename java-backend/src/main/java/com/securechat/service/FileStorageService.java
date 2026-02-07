package com.securechat.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    
    /**
     * Stores an uploaded file and returns a unique filename or file path
     * 
     * @param file The multipart file uploaded from client
     * @return 
     * @throws IOException If file storage fails (disk full, permissions, etc.)
     */
    String storeFile(MultipartFile file) throws IOException;
    
    /**
     * Loads a previously stored file as a Spring Resource
     * 
     * @param filename The identifier/path returned by storeFile()
     * @return Resource object for streaming the file
     * @throws IOException 
     */
    Resource loadFile(String filename) throws IOException;
    
    /**
     * Deletes a stored file from the storage system
     * 
     * @param filename The identifier/path of the file to delete
     * @throws IOException If file doesn't exist or deletion fails
     */
    void deleteFile(String filename) throws IOException;
}