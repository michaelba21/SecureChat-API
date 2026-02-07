package com.securechat.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data // Lombok annotation - generates getters, setters, toString, equals, and hashCode
@Builder 
@NoArgsConstructor 
@AllArgsConstructor // Lombok annotation - generates constructor with all arguments
public class ChatRoomDTO {
    private UUID id;                // Unique identifier for chatroom
    private String name;            
    private String description;     
    private UUID creatorId;         // Reference to user who created the chatroom
    private String creatorName;     
    private LocalDateTime createdAt; 
    private Boolean isPrivate;      // Privacy setting - true=private, false=public
    private Integer maxParticipants; 
}