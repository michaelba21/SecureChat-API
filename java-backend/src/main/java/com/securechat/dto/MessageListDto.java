package com.securechat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for message list projections.
 * Used to avoid ChatRoom lazy loading when returning message lists.
 */
public record MessageListDto(
        UUID id,
        String content,
        String senderName,
        UUID senderId,
        LocalDateTime timestamp,
        String messageType) {
}
