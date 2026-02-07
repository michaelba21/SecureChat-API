package com.securechat.dto;
import java.util.UUID;

/**
 * Lightweight DTO for chatroom list views.
 * Used by GET /api/chatrooms to avoid lazy loading issues.
 */
public record ChatRoomSummaryDto(
        UUID id,
        String name,
        Boolean isPrivate,
        String creatorName) {
}
