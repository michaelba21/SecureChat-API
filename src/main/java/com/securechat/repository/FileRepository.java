package com.securechat.repository;

import com.securechat.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository // Marks this interface as a Spring Data repository component
public interface FileRepository extends JpaRepository<File, UUID> {
   
    
    /**
     * Find files uploaded by a specific user after a certain date/time.

     * @param uploaderId 
     * @param since The cutoff date/time (exclusive) - only files uploaded after this time are returned
     * @return List of File entities matching the criteria
     */
    List<File> findByUploaderIdAndUploadedAtAfter(UUID uploaderId, LocalDateTime since);
}