package com.securechat.mapper;

import com.securechat.dto.MessageResponse;
import com.securechat.entity.Message;

/**
 * Intended for use in controller layer (e.g., SSE streaming) to keep
 * This ensures controllers work with DTOs while services work with entities.
 */
public final class MessageDtoMapper {

    // Private constructor to prevent instantiation (utility class pattern)
    private MessageDtoMapper() {}

    /**
     * Used primarily for Server-Sent Events (SSE) streaming where real-time
     * message updates are sent to clients.
     * @param message 
     * @return MessageResponse DTO for real-time streaming, or null if input is null
     */
    public static MessageResponse toResponse(Message message) {
        if (message == null) {
            return null; // Handle null input gracefully
        }

        MessageResponse dto = new MessageResponse();
        dto.setId(message.getId()); 
        dto.setContent(message.getContent()); // Message text content
        dto.setMessageType(message.getMessageType()); 
        dto.setTimestamp(message.getTimestamp()); 
        dto.setIsEdited(message.getIsEdited()); // Whether message has been edited
        dto.setEditedAt(message.getEditedAt()); 

        // Include sender information if available
        if (message.getSender() != null) {
            dto.setUserId(message.getSender().getId()); // Sender's user ID
        }
        dto.setUsername(message.getUsername());
        
        // Include chat room information if available
        if (message.getChatRoom() != null) {
            dto.setChatRoomId(message.getChatRoom().getId()); // Chat room ID
        }

        return dto;
    }

    /**
     * Used for standard API responses (non-streaming).
     * Similar to toResponse() but includes additional fields like deletion status.
     * @param message 
     * @return MessageDTO for standard API responses, or null if input is null
     */
    public static com.securechat.dto.MessageDTO toDto(Message message) {
        if (message == null) {
            return null; 
        }
        com.securechat.dto.MessageDTO dto = new com.securechat.dto.MessageDTO();
        dto.setId(message.getId()); 
        dto.setContent(message.getContent()); // Message text content
        dto.setMessageType(message.getMessageType().name());
        dto.setTimestamp(message.getTimestamp()); // When message was sent
        dto.setIsEdited(message.getIsEdited()); 
        dto.setEditedAt(message.getEditedAt()); 
        dto.setIsDeleted(message.getIsDeleted()); // Soft delete status (not included in streaming response)

        // Include sender information if available
        if (message.getSender() != null) {
            dto.setUserId(message.getSender().getId()); // Sender's user ID
        }
        dto.setUsername(message.getUsername());
        
        // Include chat room information if available
        if (message.getChatRoom() != null) {
            dto.setChatRoomId(message.getChatRoom().getId()); // Chat room ID
        }
        return dto;
    }
}