package com.securechat.repository;

import com.securechat.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository // Marks this interface as a Spring Data repository component
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    // Extends JpaRepository which provides CRUD operations (save, findById, findAll, delete, etc.)
    
    /**
     * Find audit logs by user ID
     * Spring Data JPA automatically implements this method based on method name convention
     */
    List<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId);
    
    /**
     * Find audit logs by event type
     */
    List<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
    
    /**
     * Find audit logs within a time range
     */
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime start, // Start of time range (inclusive)
        LocalDateTime end    
    );
    
    /**
     * Find audit logs by IP address
     */
    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);
}