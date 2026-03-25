package com.securechat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data 
@Builder // Lombok annotation - enables builder pattern for easy object construction
public class FileUploadResponse {
    private UUID id;               // Unique identifier for the uploaded file record
    private String filename;       
    private String downloadUrl;    // URL/path for clients to retrieve the file
    private long size;             
    private LocalDateTime uploadedAt; 
}