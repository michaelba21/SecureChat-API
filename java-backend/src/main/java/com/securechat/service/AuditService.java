package com.securechat.service;

import com.securechat.entity.AuditLog;
import com.securechat.entity.User;
import com.securechat.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for logging security and audit events
 * Implements logging for compliance, security monitoring, and debugging purposes
 */
@Service 
public class AuditService {

    @Autowired // Injects the AuditLogRepository for database operations
    private AuditLogRepository auditLogRepository;

    /**
     * Log a security event with full context including HTTP request details
     * @param eventType Type/category of the event (e.g., "LOGIN", "FILE_UPLOAD")
     * @param user 
     * @param action Description of what happened
     * @param request 
     */
    public void logSecurityEvent(String eventType, User user, String action, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUserId(user != null ? user.getId() : null); // Handle null user (anonymous actions)
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now()); // Current server time
        log.setIpAddress(getClientIp(request)); // Extract real client IP (handles proxies)
        log.setUserAgent(request != null ? request.getHeader("User-Agent") : null); 
        
        auditLogRepository.save(log); // Persist to database
    }

    /**
     * Log a security event with resource details
     * Used when specific resource information is available (file, message, user)
     * @param eventType 
     * @param userId 
     * @param resourceType Type of resource involved (e.g., "FILE", "MESSAGE", "USER")
     * @param resourceId 
     * @param action 
     * @param ipAddress Client IP address
     * @param userAgent Client browser/device information
     */
    public void logSecurityEvent(String eventType, UUID userId, String resourceType, 
                                 UUID resourceId, String action, 
                                 String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        
        auditLogRepository.save(log);
    }

    /**
     * Log a simple event with minimal information
     * @param eventType Type/category of the event
     * @param userId 
     * @param action Description of what happened
     */
    public void logEvent(String eventType, UUID userId, String action) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }

    /**
     * Extract client IP address from HTTP request
     * @param request HTTP request object
     * @return 
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null; // Handle null request
        }
        
        // Check for proxy header first (common in load balancers/cloud environments)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor.trim())) {
            return xForwardedFor.split(",")[0].trim(); // First IP in chain is original client
        }
        return request.getRemoteAddr(); // Fall back to direct connection IP
    }
}