package com.securechat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full DTO for single chatroom detail view.
 * Used by GET /api/chatrooms/{id} to avoid lazy loading issues.
 * I have created  this because it Record automatically generates constructor, getters, equals, hashCode, toString
 */
public record ChatRoomDetailDto(
        UUID id,
        String name,    // Display name of the chatroom
        String description,
        Boolean isPrivate,// Privacy flag - true for private, false for public
        String creatorName,
        UUID creatorId,
        LocalDateTime createdAt,
        Integer maxParticipants) {
}
