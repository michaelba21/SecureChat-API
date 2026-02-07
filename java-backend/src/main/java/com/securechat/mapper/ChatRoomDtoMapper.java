package com.securechat.mapper;

import com.securechat.dto.ChatRoomCreateRequest;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.User;

/**
 * Maps ChatRoom related DTOs (Data Transfer Objects).
 */
public final class ChatRoomDtoMapper {

    // Private constructor to prevent instantiation (utility class pattern)
    private ChatRoomDtoMapper() {}

    /**
     * Converts a ChatRoomCreateRequest DTO to a ChatRoom entity.
     * @param request The DTO containing chat room creation data (can be null)
     * @param creator
     * @return ChatRoom entity ready for database persistence, or null if request is null
     */
    public static ChatRoom toEntity(ChatRoomCreateRequest request, User creator) {
        if (request == null) {
            return null; // Handle null input gracefully
        }
        ChatRoom room = new ChatRoom();
        room.setName(request.getName()); // Set chat room name from request
        room.setDescription(request.getDescription()); // Set optional description
        room.setIsPrivate(Boolean.TRUE.equals(request.getIsPrivate())); 
        room.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 50); // Default to 50 if not specified
        room.setCreatedBy(creator); // Set the user who created the room
        return room;
    }

    /**
     * Converts a ChatRoom entity to a ChatRoomDTO.
     * @param chatRoom 
     * @return ChatRoomDTO containing the chat room's data, or null if input is null
     */
    public static com.securechat.dto.ChatRoomDTO toDto(ChatRoom chatRoom) {
        if (chatRoom == null) {
            return null; // Handle null input gracefully
        }

        // Use builder pattern for cleaner DTO construction
        return com.securechat.dto.ChatRoomDTO.builder()
            .id(chatRoom.getId()) 
            .name(chatRoom.getName()) // Chat room name
            .description(chatRoom.getDescription()) 
            .creatorId(chatRoom.getCreatedBy() != null ? chatRoom.getCreatedBy().getId() : null) 
            .creatorName(chatRoom.getCreatedBy() != null ? chatRoom.getCreatedBy().getUsername() : null)
            .createdAt(chatRoom.getCreatedAt()) // When the chat room was created
            .isPrivate(chatRoom.getIsPrivate()) 
            .maxParticipants(chatRoom.getMaxParticipants()) // Maximum allowed participants
            .build();
    }
}