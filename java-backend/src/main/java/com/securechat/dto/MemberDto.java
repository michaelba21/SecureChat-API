package com.securechat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for chatroom member information.
 * Used by GET /api/chatrooms/{id}/members to avoid entity relationships.
 */
public record MemberDto(
        UUID userId,
        String username,
        String role,
        LocalDateTime joinedAt) {
}
