package com.securechat.service;
import com.securechat.dto.MessageListDto;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.Message;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.MessageRepository;
import com.securechat.repository.UserRepository;
import com.securechat.util.InputSanitizer; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for message operations.
 *
 * Implements both:
 * 1. Message sending with validation
 * 2. Polling for new messages
 */
@Service
@RequiredArgsConstructor 
@Transactional  // All methods run within a database transaction
@Slf4j  // Lombok: provides logger instance
public class MessageService {

    // Repositories for database operations
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository; // For membership validation
    private final InputSanitizer inputSanitizer;  // HTML/script sanitization utility
    private final MessageStreamService messageStreamService;  // For real-time message streaming

    // Get a single message as DTO (Data Transfer Object) by ID
    public Optional<MessageListDto> getMessageDtoById(UUID id) {
        return messageRepository.findMessageDtoById(id);
    }

    // Overload method for string parameters (backward compatibility)
    @Transactional
    public Message createMessage(String chatRoomId, String userId, String content) {
        // Convert string IDs to UUIDs and delegate to main method
        return createMessage(UUID.fromString(chatRoomId), UUID.fromString(userId), content);
    }

    // Main method to create and persist a new message
    @Transactional
    public Message createMessage(UUID chatRoomId, UUID userId, String content) {
        try {
            log.debug("Creating message: roomId={}, userId={}, content length={}",
                    chatRoomId, userId, content != null ? content.length() : 0);

            // Step 1: Validate all required inputs
            if (chatRoomId == null || userId == null || content == null) {
                log.error("Null parameter: chatRoomId={}, userId={}, content={}", chatRoomId, userId, content);
                throw new IllegalArgumentException("ChatRoomId, userId, and content cannot be null");
            }

            // Step 2: Check if user is a member of the chat room (early validation)
            boolean isMember = chatRoomMemberRepository
                    .existsByChatRoomIdAndUserId(chatRoomId, userId);

            if (!isMember) {
                log.warn("User {} is not a member of chat room {}", userId, chatRoomId);
                throw new RuntimeException("User is not a member of this chat room");
            }

            // Step 3: Retrieve user entity from database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found: " + userId);
                    });

            // Step 4: Retrieve chat room entity from database
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> {
                        log.error("Chat room not found: {}", chatRoomId);
                        return new RuntimeException("Chat room not found: " + chatRoomId);
                    });

            // Step 5: Sanitize message content to prevent XSS attacks
            String sanitizedContent = inputSanitizer.sanitize(content);

            // Step 6: Create and configure new message entity
            Message message = new Message();
            message.setId(UUID.randomUUID());  // Generate unique ID
            message.setSender(user);  // Set sender reference
            message.setChatRoom(chatRoom); 
            message.setUsername(user.getUsername());  // Store username for quick access
            message.setContent(sanitizedContent);  // Set sanitized content
            message.setMessageType(Message.MessageType.TEXT);  
            message.setTimestamp(LocalDateTime.now());  
            message.setIsDeleted(false);  // Not deleted initially
            message.setIsEdited(false);  

            // Step 7: Persist message to database
            Message saved = messageRepository.save(message);
            log.debug("Message created successfully: id={}", saved.getId());


            // Real-time updates would typically be triggered from controller layer

            return saved;  // Return saved entity

        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            log.error("Invalid argument in createMessage: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            // Re-throw business logic errors
            log.error("Runtime error in createMessage: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Catch-all for unexpected errors
            log.error("Unexpected error in createMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create message: " + e.getMessage(), e);
        }
    }

    // Check if user is member of chat room (string parameter version)
    public boolean isChatRoomMember(String chatRoomId, String userId) {
        try {
            return chatRoomMemberRepository.existsByChatRoomIdAndUserId(
                    UUID.fromString(chatRoomId),  // Convert to UUID
                    UUID.fromString(userId));   
        } catch (IllegalArgumentException e) {
            // Return false for invalid UUID format
            return false;
        }
    }

    // Get messages after specific timestamp (for polling/updates)
    public List<Message> getMessagesSince(String chatRoomId, String sinceTimestamp) {
        LocalDateTime since = LocalDateTime.parse(sinceTimestamp);  

        return messageRepository.findByChatRoomAndTimestampAfter(
                UUID.fromString(chatRoomId), since);  // Query messages after given time
    }

    // Subscribe to real-time message stream for a chat room
    public Object getStreamForChatRoom(String chatRoomId) {
        return messageStreamService.subscribe(UUID.fromString(chatRoomId));
    }

    // Inner class for pagination parameters
    public static class PaginationRequest {
        private final int page;  // Page number (0-based)
        private final int size;  

        public PaginationRequest() {
            this(0, 20);  // Default: first page, 20 items
        }

        public PaginationRequest(Integer page, Integer size) {
            this.page = page != null ? page : 0;  
            this.size = size != null ? size : 20; // Default to 20 items
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int page() {
            return page;
        }

        public int size() {
            return size;
        }
    }

    // Get paginated messages as entity list
    public List<Message> getMessages(UUID chatRoomId, PaginationRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                Sort.by("timestamp").descending());  // Newest messages first
        Page<Message> page = messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(chatRoomId,
                pageable);
        return page.getContent();  // Extract list from page
    }

    // Get paginated messages as Page object (includes metadata)
    public Page<Message> getMessagesPaginated(UUID chatRoomId, PaginationRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                Sort.by("timestamp").descending());
        return messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(chatRoomId, pageable);
    }

    // Get paginated messages since specific time
    public Page<Message> getMessagesSincePaginated(UUID chatRoomId, LocalDateTime since, PaginationRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                Sort.by("timestamp").descending());
        return messageRepository.findByChatRoomIdAndTimestampGreaterThanAndIsDeletedFalse(chatRoomId, since, pageable);
    }

    /**
     * Get messages as DTOs to avoid lazy loading issues.
     * Uses DTO projection for better performance (avoids N+1 query problem).
     */
    @Transactional(readOnly = true)
    public Page<MessageListDto> getMessageDtosPaginated(UUID chatRoomId, PaginationRequest request) {
        Pageable pageable = PageRequest.of(request.page(), request.size());
        return messageRepository.findMessageDtosByChatRoomId(chatRoomId, pageable);
    }

    /**
     * Soft deletes a message by setting the isDeleted flag.
     * (Logical delete - message stays in database but marked as deleted)
     *
     * @param messageId
     * @param userId    ID of the user performing the deletion.
     */
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.softDelete(userId);  // Update message entity (sets isDeleted = true)
        messageRepository.save(message);  // Persist changes
        log.info("Message {} soft deleted by user {}", messageId, userId);
    }
}