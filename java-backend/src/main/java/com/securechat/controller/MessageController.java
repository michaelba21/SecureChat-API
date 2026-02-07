package com.securechat.controller;

import com.securechat.dto.MessageDTO;
import com.securechat.entity.Message;
import com.securechat.mapper.MessageDtoMapper;
import com.securechat.service.MessageService;
import com.securechat.service.MessageStreamService;
import com.securechat.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for message operations.
 */
@RestController
@RequestMapping("/api/chatrooms") // Base path for chat room related endpoints

@RequiredArgsConstructor // Lombok generates constructor with final fields
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class); // Logger instance

    private final MessageService messageService;
    private final MessageStreamService messageStreamService; // Real-time streaming service
    private final AuthUtil authUtil; // Centralized authentication utility

    /**
     * Send a message to a chat room.
     */
    @PostMapping("/{chatRoomId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable String chatRoomId,
            @Valid @RequestBody SendMessageRequest request, // Message content from request body
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new com.securechat.exception.UnauthorizedException("Unauthorized");
        }

        try {
            // Use AuthUtil to safely get authenticated user
            UUID userUuid = authUtil.getCurrentUserId(authentication);

            // PARSE ROOM ID TO UUID
            UUID roomUuid;
            try {
                roomUuid = UUID.fromString(chatRoomId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format: roomId={}", chatRoomId); // Invalid UUID error
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Validate content
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                logger.warn("Empty message content from user {}", userUuid); // Warning for empty content
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // 400 Bad Request
            }

            logger.debug("User {} sending message to chatroom {}", userUuid, roomUuid);

            // PASS UUIDs, NOT STRINGS
            Message createdMessage = messageService.createMessage( // Create message in database
                    roomUuid, userUuid, request.getContent().trim());

            // i have Used DTO projection to avoid LazyInitializationException
            com.securechat.dto.MessageListDto listDto = messageService.getMessageDtoById(createdMessage.getId()) // Fetch
                                                                                                                 // DTO
                                                                                                                 // projection
                    .orElseGet(() -> new com.securechat.dto.MessageListDto(
                            createdMessage.getId(),
                            createdMessage.getContent(), // Message content
                            createdMessage.getUsername(),
                            createdMessage.getSender() != null ? createdMessage.getSender().getId() : null, // Sender ID
                            createdMessage.getTimestamp(), // Message timestamp
                            createdMessage.getMessageType() != null ? createdMessage.getMessageType().name() : null // Message
                                                                                                                    // type
                    ));

            // Map MessageListDto back to MessageDTO
            MessageDTO messageDto = new MessageDTO(); // Create response DTO
            messageDto.setId(listDto.id());
            messageDto.setContent(listDto.content());
            // messageType in DTO expects string; set directly from DTO
            messageDto.setMessageType(listDto.messageType());
            messageDto.setTimestamp(listDto.timestamp()); // Set timestamp
            messageDto.setUserId(listDto.senderId());
            messageDto.setUsername(listDto.senderName()); // Set username
            messageDto.setChatRoomId(roomUuid); // Use known roomUuid
            messageDto.setIsDeleted(false);
            messageDto.setIsEdited(false);

            // Broadcast via SSE
            messageStreamService.publish(roomUuid, "new-message", messageDto); // Publish to SSE stream

            logger.debug("Message sent successfully by user {} to chatroom {}", userUuid, roomUuid); // Success log
            return ResponseEntity.status(HttpStatus.CREATED).body(messageDto); // 201 Created with message DTO

        } catch (RuntimeException e) {
            logger.error("Error sending message: {}", e.getMessage(), e); // Error log with stack trace
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("not a member")) { // Check if user not a member
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else if (msg.contains("not found")) { // Check if resource not found
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
            }
        } catch (Exception e) {
            logger.error("Unexpected error sending message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Get messages with pagination.
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<org.springframework.data.domain.Page<com.securechat.dto.MessageDTO>> getMessages(
            @PathVariable String chatRoomId,
            @RequestParam(defaultValue = "0") Integer page, // Page number (default 0)
            @RequestParam(defaultValue = "20") Integer size, // Page size (default 20)
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new com.securechat.exception.UnauthorizedException("Unauthorized");
        }

        UUID userId = authUtil.getCurrentUserId(authentication);

        if (!messageService.isChatRoomMember(chatRoomId, userId.toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        }

        var pageResult = messageService.getMessagesPaginated(UUID.fromString(chatRoomId), // Get paginated messages
                new MessageService.PaginationRequest(page, size)); // Pagination parameters

        var dtoPage = pageResult.map(MessageDtoMapper::toDto); // Map entities to DTOs

        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Poll for new messages since a given timestamp.
     */
    @GetMapping("/{chatRoomId}/messages/poll")
    public ResponseEntity<List<MessageDTO>> pollMessages(
            @PathVariable String chatRoomId,
            @RequestParam(value = "since", required = false) String sinceTimestamp, // Optional timestamp filter
            org.springframework.security.core.Authentication authentication) { // Current user

        if (authentication == null || authentication.getName() == null) {
            throw new com.securechat.exception.UnauthorizedException("Unauthorized");
        }

        UUID userId = authUtil.getCurrentUserId(authentication);

        if (!messageService.isChatRoomMember(chatRoomId, userId.toString())) { // Check membership
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        }

        if (sinceTimestamp == null || sinceTimestamp.isEmpty()) {
            sinceTimestamp = java.time.LocalDateTime.now().minusDays(1).toString(); // Default: last 24 hours
        }

        List<Message> newMessages = messageService.getMessagesSince(chatRoomId, sinceTimestamp); // Get messages since
                                                                                                 // timestamp

        List<MessageDTO> dtos = newMessages.stream() // Convert entities to DTOs
                .map(MessageDtoMapper::toDto)
                .collect(Collectors.toList()); // Collect to list

        return ResponseEntity.ok(dtos);
    }

    /**
     * Here I have created "Real-time message streaming". I have used here the
     * "Server-Sent Events (SSE)"
     */
    @GetMapping(value = "/{chatRoomId}/stream", produces = "text/event-stream") // SSE content type
    public Object streamMessages(@PathVariable String chatRoomId) {
        return messageService.getStreamForChatRoom(chatRoomId);
    }

    // Request DTO for sending messages
    public static class SendMessageRequest {
        private String content; // Message content field

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}