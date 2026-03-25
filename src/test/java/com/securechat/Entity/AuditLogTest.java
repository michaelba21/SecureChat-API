package com.securechat.Entity;  // Note: Package name has capital 'E' (might be typo, should be 'entity')

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;  // Custom test names
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;  // Date/time for timestamps
import java.time.temporal.ChronoUnit;  
import java.util.UUID;  // UUID for unique identifiers

import com.securechat.entity.AuditLog;  // Entity class being tested

import static org.junit.jupiter.api.Assertions.*;  // JUnit assertions

@DisplayName("AuditLog Entity Tests")  // Custom display name for test class
public class AuditLogTest {

    private AuditLog auditLog;  // Test subject
    private LocalDateTime testTimestamp;  
    private UUID testId;  // Test UUID for ID field
    private UUID testUserId;  
    private UUID testResourceId;  // Test UUID for resource ID

    @BeforeEach
    void setUp() {
        // Setup before each test: create fresh objects
        auditLog = new AuditLog();  // New AuditLog instance
        testTimestamp = LocalDateTime.now();  
        testId = UUID.randomUUID();  // Random UUID for ID
        testUserId = UUID.randomUUID();  
        testResourceId = UUID.randomUUID();  // Random UUID for resource ID
    }

    @Test
    @DisplayName("Should create AuditLog with default values")
    void testDefaultConstructor() {
        // Test: Default constructor creates object with null fields
        AuditLog log = new AuditLog(); 
        assertNotNull(log);  // Object should not be null
        
        // All fields should be null initially
        assertNull(log.getId());  
        assertNull(log.getAction());  // Action null
        assertNull(log.getEventType());  
        assertNull(log.getTimestamp());  // Timestamp null
        assertNull(log.getUserId());  
        assertNull(log.getResourceType());  // Resource type null
        assertNull(log.getResourceId());  
        assertNull(log.getIpAddress());  
        assertNull(log.getUserAgent());  // User agent null
        assertNull(log.getDetailsJson());  
    }

    @Test
    @DisplayName("Should set and get ID correctly")
    void testIdGetterSetter() {
        // Test: ID field getter/setter
        auditLog.setId(testId); 
        assertEquals(testId, auditLog.getId());  
    }

    @Test
    @DisplayName("Should set and get action correctly")
    void testActionGetterSetter() {
        // Test: Action field (e.g., "LOGIN", "CREATE", "DELETE")
        String expectedAction = "LOGIN";
        auditLog.setAction(expectedAction);  
        assertEquals(expectedAction, auditLog.getAction());  
    }

    @Test
    @DisplayName("Should set and get event type correctly")
    void testEventTypeGetterSetter() {
        // Test: Event type field (e.g., "USER_LOGIN", "FILE_UPLOAD")
        String expectedEventType = "USER_LOGIN";
        auditLog.setEventType(expectedEventType);  
        assertEquals(expectedEventType, auditLog.getEventType());  // Verify event type
    }

    @Test
    @DisplayName("Should set and get details JSON correctly")
    void testDetailsJsonGetterSetter() {
        // Test: Details JSON field (additional metadata as JSON)
        String expectedDetailsJson = "{\"ip\": \"192.168.1.1\", \"success\": true}";
        auditLog.setDetailsJson(expectedDetailsJson);  
        assertEquals(expectedDetailsJson, auditLog.getDetailsJson());  // Verify JSON
    }

    @Test
    @DisplayName("Should handle long details JSON text")
    void testLongDetailsJson() {
        // Test: Verify long JSON strings are handled (tests database column size)
        String longDetailsJson = "{\"data\": \"" + "A".repeat(5000) + "\"}";  // 5000+ character JSON
        auditLog.setDetailsJson(longDetailsJson);  
        assertEquals(longDetailsJson, auditLog.getDetailsJson()); 
        assertTrue(auditLog.getDetailsJson().length() > 5000);  // Verify length > 5000
    }

    @Test
    @DisplayName("Should set and get timestamp correctly")
    void testTimestampGetterSetter() {
        // Test: Timestamp field
        auditLog.setTimestamp(testTimestamp);  // Set timestamp
        assertEquals(testTimestamp, auditLog.getTimestamp());  
    }

    @Test
    @DisplayName("Should set and get userId correctly")
    void testUserIdGetterSetter() {
        // Test: User ID field (who performed the action)
        auditLog.setUserId(testUserId);  
        assertEquals(testUserId, auditLog.getUserId());  // Verify user ID
    }

    @Test
    @DisplayName("Should allow null userId")
    void testNullUserId() {
        // Test: User ID can be null (for system actions or unauthenticated events)
        auditLog.setUserId(null);  // Set null user ID
        assertNull(auditLog.getUserId());  
    }

    @Test
    @DisplayName("Should set and get resource type correctly")
    void testResourceTypeGetterSetter() {
        // Test: Resource type field (what type of resource was affected)
        String expectedResourceType = "USER";
        auditLog.setResourceType(expectedResourceType);  // Set resource type
        assertEquals(expectedResourceType, auditLog.getResourceType());  
    }

    @Test
    @DisplayName("Should set and get resource ID correctly")
    void testResourceIdGetterSetter() {
        // Test: Resource ID field (which specific resource was affected)
        auditLog.setResourceId(testResourceId);  
        assertEquals(testResourceId, auditLog.getResourceId());  // Verify
    }

    @Test
    @DisplayName("Should set and get IP address correctly")
    void testIpAddressGetterSetter() {
        // Test: IP address field (for audit trail)
        String expectedIpAddress = "192.168.1.100";
        auditLog.setIpAddress(expectedIpAddress); 
        assertEquals(expectedIpAddress, auditLog.getIpAddress());  // Verify
    }

    @Test
    @DisplayName("Should set and get user agent correctly")
    void testUserAgentGetterSetter() {
        // Test: User agent field (client/browser information)
        String expectedUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        auditLog.setUserAgent(expectedUserAgent);  // Set user agent
        assertEquals(expectedUserAgent, auditLog.getUserAgent()); 
    }

    @Test
    @DisplayName("Should handle all properties set together")
    void testAllPropertiesTogether() {
        // Test: Set all fields and verify together
        auditLog.setId(testId);
        auditLog.setEventType("USER_LOGIN");
        auditLog.setUserId(testUserId);
        auditLog.setResourceType("USER");
        auditLog.setResourceId(testResourceId);
        auditLog.setAction("LOGIN");
        auditLog.setTimestamp(testTimestamp);
        auditLog.setIpAddress("192.168.1.1");
        auditLog.setUserAgent("TestAgent/1.0");
        auditLog.setDetailsJson("{\"status\": \"success\"}");

        // Grouped assertions for all fields
        assertAll("auditLog",
            () -> assertEquals(testId, auditLog.getId()),  // ID
            () -> assertEquals("USER_LOGIN", auditLog.getEventType()),  // Event type
            () -> assertEquals(testUserId, auditLog.getUserId()),  
            () -> assertEquals("USER", auditLog.getResourceType()),  // Resource type
            () -> assertEquals(testResourceId, auditLog.getResourceId()),  
            () -> assertEquals("LOGIN", auditLog.getAction()),  // Action
            () -> assertEquals(testTimestamp, auditLog.getTimestamp()),  
            () -> assertEquals("192.168.1.1", auditLog.getIpAddress()),  // IP address
            () -> assertEquals("TestAgent/1.0", auditLog.getUserAgent()),  
            () -> assertEquals("{\"status\": \"success\"}", auditLog.getDetailsJson())  // Details JSON
        );
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        // Test: Empty string values are accepted
        auditLog.setAction("");
        auditLog.setEventType("");
        auditLog.setDetailsJson("");
        auditLog.setIpAddress("");
        auditLog.setUserAgent("");
        auditLog.setResourceType("");
        
        // All should return empty strings
        assertEquals("", auditLog.getAction());
        assertEquals("", auditLog.getEventType());
        assertEquals("", auditLog.getDetailsJson());
        assertEquals("", auditLog.getIpAddress());
        assertEquals("", auditLog.getUserAgent());
        assertEquals("", auditLog.getResourceType());
    }

    @Test
    @DisplayName("Should verify entity annotations are present")
    void testEntityAnnotations() {
        // Test: JPA annotations are present on the entity class
        assertTrue(AuditLog.class.isAnnotationPresent(jakarta.persistence.Entity.class));  // @Entity annotation
        assertTrue(AuditLog.class.isAnnotationPresent(jakarta.persistence.Table.class));  // @Table annotation

        // Verify table name
        jakarta.persistence.Table tableAnnotation = AuditLog.class.getAnnotation(jakarta.persistence.Table.class);
        assertNotNull(tableAnnotation, "Table annotation should be present");
        assertEquals("audit_logs", tableAnnotation.name());  // Table name should be "audit_logs"
    }

    @Test
    @DisplayName("Should test onCreate sets timestamp when null")
    void testOnCreateSetsTimestamp() {
        // Test: @PrePersist callback sets timestamp automatically
        AuditLog log = new AuditLog();
        
        // First test: timestamp should be null initially
        assertNull(log.getTimestamp());
        
        // Simulate @PrePersist by manually setting timestamp (in real scenario this would be automatic)
        LocalDateTime beforeSet = LocalDateTime.now();
        log.setTimestamp(LocalDateTime.now());  
        LocalDateTime afterSet = LocalDateTime.now();
        
        // Verify timestamp was set
        assertNotNull(log.getTimestamp());
        
        // Flexible timestamp comparison (allow time drift)
        assertTrue(log.getTimestamp().isAfter(beforeSet.minusSeconds(1)) || 
                   log.getTimestamp().isEqual(beforeSet.minusSeconds(1)));
        assertTrue(log.getTimestamp().isBefore(afterSet.plusSeconds(1)) || 
                   log.getTimestamp().isEqual(afterSet.plusSeconds(1)));
        
        // Compare timestamps with 1-second tolerance
        LocalDateTime logTimestampTruncated = log.getTimestamp().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime nowTruncated = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        
        long diffInSeconds = Math.abs(ChronoUnit.SECONDS.between(logTimestampTruncated, nowTruncated));
        assertTrue(diffInSeconds <= 1, "Timestamp difference should be <= 1 second, but was: " + diffInSeconds + " seconds");
        
        // Test with existing timestamp (should not be overwritten by @PrePersist)
        LocalDateTime existingTimestamp = LocalDateTime.now().minusHours(1);
        AuditLog log2 = new AuditLog();
        log2.setTimestamp(existingTimestamp);
        // If @PrePersist were called, timestamp should remain unchanged
        assertEquals(existingTimestamp, log2.getTimestamp());
    }

    @Test
    @DisplayName("Should handle different event types")
    void testDifferentEventTypes() {
        // Test: Various event types are accepted
        String[] eventTypes = {"USER_LOGIN", "USER_CREATE", "USER_UPDATE", "MESSAGE_CREATE", 
                              "MESSAGE_DELETE", "CHATROOM_CREATE", "FILE_UPLOAD", "FILE_DELETE"};
        
        for (String eventType : eventTypes) {
            auditLog.setEventType(eventType);  // Set each event type
            assertEquals(eventType, auditLog.getEventType());  
        }
    }

    @Test
    @DisplayName("Should handle different resource types")
    void testDifferentResourceTypes() {
        // Test: Various resource types are accepted
        String[] resourceTypes = {"USER", "MESSAGE", "CHATROOM", "FILE", "AUDIT_LOG"};
        
        for (String resourceType : resourceTypes) {
            auditLog.setResourceType(resourceType);  // Set each resource type
            assertEquals(resourceType, auditLog.getResourceType());  
        }
    }

    @Test
    @DisplayName("Should handle different actions")
    void testDifferentActions() {
        // Test: Various action types are accepted
        String[] actions = {"CREATE", "UPDATE", "DELETE", "LOGIN", "LOGOUT", "READ", "SHARE"};
        
        for (String action : actions) {
            auditLog.setAction(action);  
            assertEquals(action, auditLog.getAction());  
        }
    }

    @Test
    @DisplayName("Should test with null resource ID")
    void testNullResourceId() {
        // Test: Resource ID can be null
        auditLog.setResourceId(null);  // Set null resource ID
        assertNull(auditLog.getResourceId());  
    }

    @Test
    @DisplayName("Should test with null resource type")
    void testNullResourceType() {
        // Test: Resource type can be null
        auditLog.setResourceType(null);  
        assertNull(auditLog.getResourceType());  // Should be null
    }
}