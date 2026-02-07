package com.securechat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // Lombok annotation - generates no-argument constructor

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private UUID id;              
    private String content;       // The actual message text/content
    private String messageType;   
    private LocalDateTime timestamp; 
    private Boolean isEdited;     // Flag indicating if message has been modified
    private LocalDateTime editedAt; 
    private Boolean isDeleted;    
    private UUID userId;          // New field: Identifier of the user who sent the message
    private String username;      
    private UUID chatRoomId;      // Identifier of the chatroom containing this message
    
    // Legacy fields for backward compatibility - mapping to old naming convention
    private UUID senderId;        // Legacy: Same as userId (maintained for compatibility)
    private String senderName;    
}
