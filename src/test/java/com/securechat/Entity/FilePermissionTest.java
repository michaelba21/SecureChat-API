package com.securechat.Entity;  

import jakarta.persistence.Column;  
import jakarta.persistence.Entity;  // JPA entity annotation
import jakarta.persistence.EnumType;  
import jakarta.persistence.Enumerated;  
import jakarta.persistence.FetchType;  
import jakarta.persistence.JoinColumn;  // JPA join column annotation
import jakarta.persistence.ManyToOne;  
import jakarta.persistence.Table;  // JPA table annotation

import org.junit.jupiter.api.DisplayName;  // Custom test names
import org.junit.jupiter.api.Test;

import com.securechat.entity.FilePermission;  
import com.securechat.entity.File;  // File entity (relationship)
import com.securechat.entity.User;  // User entity (relationship)

import java.lang.reflect.Field;  // Reflection for field inspection
import java.time.Duration;  
import java.time.LocalDateTime;  // Date/time for timestamps
import java.util.UUID; 

import static org.junit.jupiter.api.Assertions.*;  // JUnit assertions

@DisplayName("FilePermission Entity Tests")  // Custom display name for test class
class FilePermissionTest {

    // Test-only subclass to expose protected @PrePersist method for testing
    static class TestableFilePermission extends FilePermission {
        public void triggerOnCreate() {
            super.onCreate();  // Calls protected onCreate() method from parent
        }
    }

    @Test
    @DisplayName("Default constructor + Lombok accessors should work")
    void testDefaultConstructorAndAccessors() {
        // Test: Default constructor with getters/setters
        FilePermission fp = new FilePermission();  // Create with default constructor

        // Test data
        UUID id = UUID.randomUUID();  // Random ID
        File file = new File();  
        User user = new User();  // User entity
        FilePermission.PermissionType type = FilePermission.PermissionType.READ;  // Enum value
        LocalDateTime grantedAt = LocalDateTime.now().minusMinutes(5);  // Timestamp 5 minutes ago

        // Set all fields using setters
        fp.setId(id);
        fp.setFile(file);
        fp.setGrantedTo(user);
        fp.setPermissionType(type);
        fp.setGrantedAt(grantedAt);

        // Verify getters return correct values
        assertEquals(id, fp.getId());  // Verify ID
        assertSame(file, fp.getFile());  
        assertSame(user, fp.getGrantedTo());  // Verify same user reference
        assertEquals(type, fp.getPermissionType());  
        assertEquals(grantedAt, fp.getGrantedAt());  // Verify granted timestamp
    }

    @Test
    @DisplayName("All-args constructor sets fields")
    void testAllArgsConstructor() {
        // Test: Constructor with all parameters
        UUID id = UUID.randomUUID();  // Random ID
        File file = new File();  
        User user = new User();  
        LocalDateTime now = LocalDateTime.now(); 

        // Create using all-args constructor
        FilePermission fp = new FilePermission(
                id,
                file,
                user,
                FilePermission.PermissionType.WRITE,  // WRITE permission
                now
        );

        // Verify all fields set correctly
        assertEquals(id, fp.getId());  
        assertSame(file, fp.getFile());  
        assertSame(user, fp.getGrantedTo());  // User reference
        assertEquals(FilePermission.PermissionType.WRITE, fp.getPermissionType());  // Permission type
        assertEquals(now, fp.getGrantedAt());  // Granted timestamp
    }

    @Test
    @DisplayName("@PrePersist onCreate() should set grantedAt to current time")
    void testPrePersistSetsGrantedAt() {
        // Test: @PrePersist lifecycle callback sets timestamp
        TestableFilePermission fp = new TestableFilePermission();  
        fp.setGrantedAt(null);  // Explicitly set to null

        // Trigger the @PrePersist annotated method
        fp.triggerOnCreate();  // Calls protected onCreate()

        assertNotNull(fp.getGrantedAt(), "grantedAt should be initialized by @PrePersist");  // Should not be null
        // Verify timestamp is recent (within 3 seconds)
        assertTrue(Duration.between(fp.getGrantedAt(), LocalDateTime.now()).abs().getSeconds() < 3,
                "grantedAt should be very recent");
    }

    @Test
    @DisplayName("@PrePersist onCreate() should overwrite existing grantedAt with current time")
    void testPrePersistOverwritesGrantedAt() {
        // Test: @PrePersist always sets grantedAt to current time (even if already set)
        TestableFilePermission fp = new TestableFilePermission();  
        LocalDateTime old = LocalDateTime.now().minusDays(1);  // Old timestamp (yesterday)
        fp.setGrantedAt(old);  // Set old timestamp

        fp.triggerOnCreate();  // Trigger @PrePersist

        assertNotNull(fp.getGrantedAt());  
        assertTrue(fp.getGrantedAt().isAfter(old), "grantedAt should be updated");  // Should be newer than old
    }

    @Test
    @DisplayName("Entity and table annotations should be present with correct name")
    void testEntityAnnotations() {
        // Test: JPA entity and table annotations
        assertTrue(FilePermission.class.isAnnotationPresent(Entity.class));  // Should have @Entity
        assertTrue(FilePermission.class.isAnnotationPresent(Table.class));  

        Table table = FilePermission.class.getAnnotation(Table.class);  
        assertEquals("file_permissions", table.name());  // Table name should be "file_permissions"
    }

    @Test
    @DisplayName("permissionType should be @Enumerated STRING")
    void testEnumeratedAnnotation() throws NoSuchFieldException {
        // Test: Enum field has correct JPA mapping
        Field field = FilePermission.class.getDeclaredField("permissionType");  // Get field via reflection

        Enumerated enumerated = field.getAnnotation(Enumerated.class);  
        assertNotNull(enumerated);  // Should have @Enumerated annotation
        assertEquals(EnumType.STRING, enumerated.value());  // Should be stored as STRING (not ORDINAL)
    }

    @Test
    @DisplayName("Relationships: file & grantedTo should be Lazy @ManyToOne with correct @JoinColumn")
    void testRelationshipAnnotations() throws NoSuchFieldException {
        // Test: JPA relationship annotations are correct

        // Test 'file' field relationship
        Field fileField = FilePermission.class.getDeclaredField("file");  // Get file field
        ManyToOne fileManyToOne = fileField.getAnnotation(ManyToOne.class);  
        JoinColumn fileJoin = fileField.getAnnotation(JoinColumn.class);  // Get @JoinColumn annotation

        assertNotNull(fileManyToOne);  // Should have @ManyToOne
        assertEquals(FetchType.LAZY, fileManyToOne.fetch());  // Should be LAZY fetch
        assertEquals("file_id", fileJoin.name());  // Join column name should be "file_id"
        assertFalse(fileJoin.nullable());  // Should not be nullable (required relationship)

        // Test 'grantedTo' field relationship
        Field grantedToField = FilePermission.class.getDeclaredField("grantedTo");  // Get grantedTo field
        ManyToOne grantedToManyToOne = grantedToField.getAnnotation(ManyToOne.class);  
        JoinColumn grantedToJoin = grantedToField.getAnnotation(JoinColumn.class);  // Get @JoinColumn

        assertNotNull(grantedToManyToOne);  // Should have @ManyToOne
        assertEquals(FetchType.LAZY, grantedToManyToOne.fetch());  
        assertEquals("user_id", grantedToJoin.name());  // Join column name should be "user_id"
        assertFalse(grantedToJoin.nullable());  
    }

    @Test
    @DisplayName("PermissionType enum values")
    void testPermissionTypeValues() {
        // Test: PermissionType enum has expected values
        var values = FilePermission.PermissionType.values();  
        assertEquals(3, values.length);  // Should have exactly 3 values

        // Verify all expected values exist
        assertTrue(java.util.Arrays.asList(values).contains(FilePermission.PermissionType.READ));  // READ
        assertTrue(java.util.Arrays.asList(values).contains(FilePermission.PermissionType.WRITE));  // WRITE
        assertTrue(java.util.Arrays.asList(values).contains(FilePermission.PermissionType.DELETE));  // DELETE
    }

    @Test
    @DisplayName("grantedAt column should be non-nullable")
    void testGrantedAtColumnNotNull() throws NoSuchFieldException {
        // Test: grantedAt column is marked as not nullable
        Field grantedAtField = FilePermission.class.getDeclaredField("grantedAt");  
        Column col = grantedAtField.getAnnotation(Column.class);  // Get @Column annotation

        assertNotNull(col); 
        assertFalse(col.nullable());  // Should be NOT NULL in database
    }
}