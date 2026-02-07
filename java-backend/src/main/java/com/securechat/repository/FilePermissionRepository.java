package com.securechat.repository;

import com.securechat.entity.FilePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository // Marks this interface as a Spring Data repository component
public interface FilePermissionRepository extends JpaRepository<FilePermission, UUID> {
    // Extends JpaRepository which provides CRUD operations for FilePermission entities
  
    // Find all permissions for a specific file
    List<FilePermission> findByFileId(UUID fileId);
  
    // Find a specific permission for a file and user combination
    java.util.Optional<FilePermission> findByFileIdAndGrantedToId(UUID fileId, UUID userId);
}