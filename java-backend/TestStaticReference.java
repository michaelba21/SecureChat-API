import java.util.UUID;

// Base class representing an audit log entry with a unique identifier
class AuditLog {
    private UUID id;  
    
    // Getter method for the ID - returns the UUID
    public UUID getId() { return id; }
    
    // Setter method for the ID - allows setting the UUID
    public void setId(UUID id) { this.id = id; }
}

class MessageAuditLogTest {
    public static void main(String[] args) {
        // Create an instance of MessageAuditLog (which extends AuditLog)
        MessageAuditLog auditLog = new MessageAuditLog();
        
        // Generate a random UUID for testing
        UUID id = UUID.randomUUID();
        
        // Set the generated ID on the auditLog instance
        auditLog.setId(id);
        
   
        // Call getId() on the auditLog instance (not on the AuditLog class)
        System.out.println("ID: " + auditLog.getId());
    }
}

// MessageAuditLog extends AuditLog, inheriting all its fields and methods
class MessageAuditLog extends AuditLog {
    // Simple extension - inherits all functionality from AuditLog
}