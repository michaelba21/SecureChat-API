package com.securechat.Entity;  // Note: Package name has capital 'E' (might be typo, should be 'entity')

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;  // Spring utility for testing private methods/fields

import com.securechat.entity.AdminAction;  
import com.securechat.entity.User;  // User entity

import java.time.LocalDateTime;  
import java.util.UUID;  // UUID for unique identifiers

import static org.assertj.core.api.Assertions.*;  // AssertJ fluent assertions

class AdminActionTest {

    private AdminAction adminAction;  

    @BeforeEach
    void setUp() {
        adminAction = new AdminAction();  // Create fresh AdminAction before each test
    }

    @Test
    void prePersist_setsPerformedAt_whenNull() {
        // Test: @PrePersist callback sets timestamp when null
        LocalDateTime before = LocalDateTime.now();  

        // Trigger the @PrePersist annotated method using reflection (private method)
        ReflectionTestUtils.invokeMethod(adminAction, "onCreate");  // Calls private onCreate() method

        LocalDateTime after = LocalDateTime.now();  

        // Verify: performedAt should be set to current time
        assertThat(adminAction.getPerformedAt())
                .isNotNull()  // Should not be null
                .isAfterOrEqualTo(before)  
                .isBeforeOrEqualTo(after);  // Should be before or equal to after time
    }

    @Test
    void prePersist_doesNotOverrideExistingPerformedAt() {
        // Test: @PrePersist doesn't override existing timestamp
        LocalDateTime existing = LocalDateTime.of(2024, 1, 1, 12, 0);  // Fixed existing timestamp
        adminAction.setPerformedAt(existing);  

        ReflectionTestUtils.invokeMethod(adminAction, "onCreate");  // Trigger callback

        // Verify: Timestamp should remain unchanged
        assertThat(adminAction.getPerformedAt()).isEqualTo(existing);
    }

    @Test
    void id_getterAndSetter_workCorrectly() {
        // Test: ID field getter/setter
        UUID id = UUID.randomUUID();  // Generate random UUID
        adminAction.setId(id);  // Set ID
        assertThat(adminAction.getId()).isEqualTo(id);  
    }

    @Test
    void admin_relationship_getterAndSetter_workCorrectly() {
        // Test: Admin user relationship
        User admin = new User();  // Create admin user
        adminAction.setAdmin(admin);  // Set admin
        assertThat(adminAction.getAdmin()).isSameAs(admin); 
    }

    @Test
    void targetUser_relationship_getterAndSetter_workCorrectly() {
        // Test: Target user relationship
        User targetUser = new User();  // Create target user
        adminAction.setTargetUser(targetUser);  
        assertThat(adminAction.getTargetUser()).isSameAs(targetUser);  // Verify same reference

        adminAction.setTargetUser(null);  
        assertThat(adminAction.getTargetUser()).isNull();  // Should be null
    }

    @Test
    void actionType_enum_getterAndSetter_workCorrectly() {
        // Test: ActionType enum field
        adminAction.setActionType(AdminAction.AdminActionType.BAN_USER);  // Set to BAN_USER
        assertThat(adminAction.getActionType()).isEqualTo(AdminAction.AdminActionType.BAN_USER);  // Verify

        adminAction.setActionType(AdminAction.AdminActionType.DELETE_CHATROOM);  // Change to DELETE_CHATROOM
        assertThat(adminAction.getActionType()).isEqualTo(AdminAction.AdminActionType.DELETE_CHATROOM);  // Verify

        adminAction.setActionType(null);  
        assertThat(adminAction.getActionType()).isNull();  // Should be null
    }

    @Test
    void reason_getterAndSetter_workCorrectly() {
        // Test: Reason text field
        adminAction.setReason("Violated community guidelines");  
        assertThat(adminAction.getReason()).isEqualTo("Violated community guidelines");  // Verify

        adminAction.setReason(null);  
        assertThat(adminAction.getReason()).isNull();  // Should be null
    }

    @Test
    void performedAt_getterAndSetter_workCorrectly() {
        // Test: PerformedAt timestamp field
        LocalDateTime time = LocalDateTime.now(); 
        adminAction.setPerformedAt(time);  // Set timestamp
        assertThat(adminAction.getPerformedAt()).isEqualTo(time); 
    }

    @Test
    void ipAddress_getterAndSetter_workCorrectly() {
        // Test: IP address field (for audit logging)
        adminAction.setIpAddress("192.168.1.100");  // Set IP
        assertThat(adminAction.getIpAddress()).isEqualTo("192.168.1.100");  

        adminAction.setIpAddress(null);  // Test null
        assertThat(adminAction.getIpAddress()).isNull();  
    }

    @Test
    void metadata_getterAndSetter_workCorrectly() {
        // Test: Metadata field (JSON or additional data)
        String jsonMetadata = "{\"duration\": \"7 days\", \"scope\": \"chatroom\"}";  // JSON metadata
        adminAction.setMetadata(jsonMetadata);  // Set metadata
        assertThat(adminAction.getMetadata()).isEqualTo(jsonMetadata);  // Verify

        adminAction.setMetadata(null);  // Test null
        assertThat(adminAction.getMetadata()).isNull();  // Should be null
    }

    @Test
    void fullAdminAction_canBeConstructedCorrectly() {
        // Test: Complete AdminAction object with all fields
        User admin = new User();  // Admin user
        User targetUser = new User(); 
        UUID id = UUID.randomUUID();  // ID
        LocalDateTime time = LocalDateTime.now();  

        // Set all fields
        adminAction.setId(id);
        adminAction.setAdmin(admin);
        adminAction.setTargetUser(targetUser);
        adminAction.setActionType(AdminAction.AdminActionType.DEACTIVATE_USER);  // Action type
        adminAction.setReason("Suspicious activity");  // Reason
        adminAction.setPerformedAt(time);  // Timestamp
        adminAction.setIpAddress("10.0.0.1");  
        adminAction.setMetadata("{\"accountAge\": \"30 days\"}");  // Metadata

        // Verify all fields
        assertThat(adminAction.getId()).isEqualTo(id);  // ID
        assertThat(adminAction.getAdmin()).isSameAs(admin); 
        assertThat(adminAction.getTargetUser()).isSameAs(targetUser);  // Target user
        assertThat(adminAction.getActionType()).isEqualTo(AdminAction.AdminActionType.DEACTIVATE_USER);  // Action type
        assertThat(adminAction.getReason()).isEqualTo("Suspicious activity");  
        assertThat(adminAction.getPerformedAt()).isEqualTo(time);  // Timestamp
        assertThat(adminAction.getIpAddress()).isEqualTo("10.0.0.1");  
        assertThat(adminAction.getMetadata()).isEqualTo("{\"accountAge\": \"30 days\"}");  // Metadata
    }
}