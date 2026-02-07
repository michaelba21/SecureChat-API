package com.securechat.Entity; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;  // Spring utility for testing private methods

import com.securechat.entity.Message;  // Message entity (relationship)
import com.securechat.entity.MessageAuditLog;  // Entity class being tested
import com.securechat.entity.User;  

import java.time.LocalDateTime;  // Date/time for timestamps
import java.util.UUID;  

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

class MessageAuditLogTest {

    private MessageAuditLog auditLog;  

    @BeforeEach
    void setUp() {
        auditLog = new MessageAuditLog();  // Create fresh MessageAuditLog before each test
    }

    @Test
    void prePersist_setsPerformedAt_whenNull() {
        // Test: @PrePersist callback sets timestamp when null
        LocalDateTime before = LocalDateTime.now();  

        // Trigger the @PrePersist annotated method using reflection (private method)
        ReflectionTestUtils.invokeMethod(auditLog, "onCreate");  // Calls private onCreate() method

        LocalDateTime after = LocalDateTime.now();  

        // Verify: performedAt should be set to current time
        assertThat(auditLog.getPerformedAt())
                .isNotNull()  
                .isAfterOrEqualTo(before)  // Should be after or equal to before time
                .isBeforeOrEqualTo(after);  
    }

    @Test
    void prePersist_doesNotOverrideExistingPerformedAt() {
        // Test: @PrePersist doesn't override existing timestamp
        LocalDateTime existing = LocalDateTime.of(2024, 1, 1, 12, 0);  // Fixed existing timestamp
        auditLog.setPerformedAt(existing);  

        ReflectionTestUtils.invokeMethod(auditLog, "onCreate");  // Trigger callback

        // Verify: Timestamp should remain unchanged
        assertThat(auditLog.getPerformedAt()).isEqualTo(existing);
    }

    @Test
    void id_getterAndSetter_workCorrectly() {
        // Test: ID field getter/setter
        UUID id = UUID.randomUUID();  // Generate random UUID
        auditLog.setId(id);  
        assertThat(auditLog.getId()).isEqualTo(id);  
    }

    @Test
    void message_relationship_getterAndSetter_workCorrectly() {
        // Test: Message relationship field
        Message message = new Message();  // Create message entity
        auditLog.setMessage(message); 
        assertThat(auditLog.getMessage()).isSameAs(message);  // Verify same reference
    }

    @Test
    void performer_relationship_getterAndSetter_workCorrectly() {
        // Test: Performer relationship field (who performed the audit action)
        User performer = new User();  // Create user entity
        auditLog.setPerformer(performer);  
        assertThat(auditLog.getPerformer()).isSameAs(performer);  // Verify same reference
    }

    @Test
    void content_getterAndSetter_workCorrectly() {
        // Test: Content field (current message content after action)
        auditLog.setContent("Original message content");  
        assertThat(auditLog.getContent()).isEqualTo("Original message content");  

        auditLog.setContent(null);  // Test null
        assertThat(auditLog.getContent()).isNull();  
    }

    @Test
    void action_enum_getterAndSetter_workCorrectly() {
        // Test: Action enum field (CREATE, UPDATE, DELETE)
        auditLog.setAction(MessageAuditLog.AuditAction.CREATE);  // Set to CREATE
        assertThat(auditLog.getAction()).isEqualTo(MessageAuditLog.AuditAction.CREATE);  // Verify

        auditLog.setAction(MessageAuditLog.AuditAction.DELETE);  // Change to DELETE
        assertThat(auditLog.getAction()).isEqualTo(MessageAuditLog.AuditAction.DELETE);  // Verify

        auditLog.setAction(null);  
        assertThat(auditLog.getAction()).isNull();  // Should be null
    }

    @Test
    void performedAt_getterAndSetter_workCorrectly() {
        // Test: PerformedAt timestamp field
        LocalDateTime time = LocalDateTime.now();  // Current time
        auditLog.setPerformedAt(time); 
        assertThat(auditLog.getPerformedAt()).isEqualTo(time);  // Verify
    }

    @Test
    void ipAddress_getterAndSetter_workCorrectly() {
        // Test: IP address field (for audit trail)
        auditLog.setIpAddress("192.168.1.100");  // Set IP
        assertThat(auditLog.getIpAddress()).isEqualTo("192.168.1.100");  

        auditLog.setIpAddress(null);  // Test null
        assertThat(auditLog.getIpAddress()).isNull();  
    }

    @Test
    void userAgent_getterAndSetter_workCorrectly() {
        // Test: User agent field (client/browser information)
        auditLog.setUserAgent("Mozilla/5.0 Test Agent");  // Set user agent
        assertThat(auditLog.getUserAgent()).isEqualTo("Mozilla/5.0 Test Agent");  // Verify

        auditLog.setUserAgent(null);  
        assertThat(auditLog.getUserAgent()).isNull();  // Should be null
    }

    @Test
    void reason_getterAndSetter_workCorrectly() {
        // Test: Reason field (why the action was performed)
        auditLog.setReason("Inappropriate content");  // Set reason
        assertThat(auditLog.getReason()).isEqualTo("Inappropriate content"); 

        auditLog.setReason(null);  
        assertThat(auditLog.getReason()).isNull();  // Should be null
    }

    @Test
    void oldContent_getterAndSetter_workCorrectly() {
        // Test: Old content field (previous message content before update)
        auditLog.setOldContent("Previous version of message");  // Set old content
        assertThat(auditLog.getOldContent()).isEqualTo("Previous version of message");  // Verify

        auditLog.setOldContent(null);  
        assertThat(auditLog.getOldContent()).isNull();  // Should be null
    }

    @Test
    void fullAuditLog_canBeConstructedCorrectly() {
        // Test: Complete MessageAuditLog object with all fields
        Message message = new Message();  // Message entity
        User performer = new User();  
        UUID id = UUID.randomUUID();  
        LocalDateTime time = LocalDateTime.now();  // Timestamp

        // Set all fields
        auditLog.setId(id);
        auditLog.setMessage(message);
        auditLog.setPerformer(performer);
        auditLog.setContent("New content");  // Current content
        auditLog.setAction(MessageAuditLog.AuditAction.UPDATE);  
        auditLog.setPerformedAt(time);  // When performed
        auditLog.setIpAddress("127.0.0.1");  // IP address
        auditLog.setUserAgent("TestClient/1.0");  
        auditLog.setReason("Correction");  
        auditLog.setOldContent("Old content");  // Previous content

        // Verify all fields
        assertThat(auditLog.getId()).isEqualTo(id);  // ID
        assertThat(auditLog.getMessage()).isSameAs(message);  // Message
        assertThat(auditLog.getPerformer()).isSameAs(performer);  
        assertThat(auditLog.getContent()).isEqualTo("New content");  // Content
        assertThat(auditLog.getAction()).isEqualTo(MessageAuditLog.AuditAction.UPDATE);  // Action
        assertThat(auditLog.getPerformedAt()).isEqualTo(time);  
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");  // IP address
        assertThat(auditLog.getUserAgent()).isEqualTo("TestClient/1.0");  
        assertThat(auditLog.getReason()).isEqualTo("Correction");  
        assertThat(auditLog.getOldContent()).isEqualTo("Old content");  // Old content
    }
}