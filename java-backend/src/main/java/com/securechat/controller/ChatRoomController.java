
package com.securechat.controller;

import com.securechat.dto.ChatRoomSummaryDto;
import com.securechat.dto.ChatRoomDetailDto;
import com.securechat.dto.MemberDto;
import com.securechat.dto.MessageDTO;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.User;
import com.securechat.dto.ChatRoomCreateRequest;
import com.securechat.dto.ChatRoomDTO;
import com.securechat.dto.ChatRoomUpdateRequest;
import com.securechat.mapper.ChatRoomDtoMapper;
import com.securechat.mapper.MessageDtoMapper;
import com.securechat.service.ChatRoomService;
import com.securechat.service.MessageService;
import com.securechat.service.UserService;
import com.securechat.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.securechat.exception.ResourceNotFoundException;
import com.securechat.exception.UnauthorizedException;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomController.class);

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthUtil authUtil;

    /**
     * Get all chat rooms for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryDto>> getAllChatRooms(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized attempt to fetch chat rooms");
            throw new UnauthorizedException("Unauthorized");
        }

        User user = authUtil.getAuthenticatedUser(authentication);
        logger.debug("Fetching chat rooms for user: {}", user.getId());
        List<ChatRoomSummaryDto> chatRooms = chatRoomService.getUserChatRoomSummaries(user.getId());
        return ResponseEntity.ok(chatRooms);
    }

    /**
     * Get a specific chat room by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomDetailDto> getChatRoomById(@PathVariable UUID id, Authentication authentication) {
        // Check if room exists first (before authentication check)
        if (!chatRoomService.getChatRoomById(id).isPresent()) {
            throw new ResourceNotFoundException("Chat room not found with id: " + id);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized attempt to fetch chat room: {}", id);
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);
        logger.debug("Fetching chat room: {} for user: {}", id, currentUser.getId());

        // Check if user is a member
        if (!chatRoomService.isMember(currentUser.getId(), id)) {
            throw new UnauthorizedException("You are not a member of this chat room");
        }

        ChatRoomDetailDto chatRoom = chatRoomService.getChatRoomDetail(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + id));
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * Create a new chat room.
     */
    @PostMapping
    public ResponseEntity<?> createChatRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User creator = authUtil.getAuthenticatedUser(authentication);
        logger.info("Creating chat room: name={}, creator={}", request.getName(), creator.getId());

        ChatRoom chatRoom = chatRoomService.createChatRoom(request, creator);
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * Update an existing chat room.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChatRoomDTO> updateChatRoom(
            @PathVariable UUID id,
            @Valid @RequestBody ChatRoomUpdateRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);
        logger.debug("User {} updating chat room: {}", currentUser.getId(), id);

        ChatRoom updatedRoom = chatRoomService.updateChatRoom(id, request, currentUser);
        ChatRoomDTO updatedRoomDto = ChatRoomDtoMapper.toDto(updatedRoom);
        return ResponseEntity.ok(updatedRoomDto);
    }

    /**
     * Delete a chat room.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);
        logger.debug("User {} attempting to delete chat room: {}", currentUser.getId(), id);

        chatRoomService.deleteChatRoom(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Join a chat room.
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinChatRoom(@PathVariable UUID id, Authentication authentication) {
        // Check if room exists first (before authentication check)
        if (!chatRoomService.getChatRoomById(id).isPresent()) {
            throw new ResourceNotFoundException("Chat room not found with id: " + id);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);
        logger.debug("User {} attempting to join chat room: {}", currentUser.getId(), id);

        try {
            Optional<ChatRoomMember> member = chatRoomService.addMemberToChatRoom(id, currentUser.getId());

            if (member.isEmpty()) {
                throw new IllegalArgumentException("Cannot join chat room");
            }

            return ResponseEntity.ok().body("Joined chatroom successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Leave a chat room.
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveChatRoom(@PathVariable UUID id, Authentication authentication) {
        // Check if room exists first (before authentication check)
        if (!chatRoomService.getChatRoomById(id).isPresent()) {
            throw new ResourceNotFoundException("Chat room not found with id: " + id);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);
        logger.debug("User {} attempting to leave chat room: {}", currentUser.getId(), id);

        try {
            chatRoomService.removeMemberFromChatRoom(id, currentUser.getId());
            return ResponseEntity.ok().body("Left chatroom successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get members of a chat room.
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getChatRoomMembers(@PathVariable UUID id, Authentication authentication) {
        // Check if room exists first (before authentication check)
        if (!chatRoomService.getChatRoomById(id).isPresent()) {
            throw new ResourceNotFoundException("Chat room not found with id: " + id);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User currentUser = authUtil.getAuthenticatedUser(authentication);

        // Check if user is a member
        if (!chatRoomService.isMember(currentUser.getId(), id)) {
            throw new UnauthorizedException("You are not a member of this chat room");
        }

        List<MemberDto> members = chatRoomService.getChatRoomMemberDtos(id);
        return ResponseEntity.ok(members);
    }

    /**
     * Add a member to a chat room.
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberDto> addMember(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> requestBody,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User user = authUtil.getAuthenticatedUser(authentication);
        UUID memberId = requestBody.get("userId");
        logger.debug("User {} attempting to add member {} to chat room: {}", user.getId(), memberId, id);

        chatRoomService.addMemberToChatRoom(id, memberId);
        List<MemberDto> members = chatRoomService.getChatRoomMemberDtos(id);
        MemberDto addedMember = members.stream()
                .filter(m -> m.userId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Failed to retrieve added member"));
        return ResponseEntity.ok(addedMember);
    }

    /**
     * Remove a member from a chat room.
     */
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized");
        }

        User user = authUtil.getAuthenticatedUser(authentication);
        logger.debug("User {} attempting to remove member {} from chat room: {}", user.getId(), memberId, id);

        chatRoomService.removeMemberFromChatRoom(id, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get messages for a specific chat room.
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized attempt to fetch messages");
            throw new UnauthorizedException("Unauthorized");
        }

        logger.debug("Fetching messages for chat room: {}, page: {}, size: {}", id, page, size);
        MessageService.PaginationRequest paginationRequest = new MessageService.PaginationRequest(page, size);
        List<MessageDTO> messages = messageService.getMessages(id, paginationRequest).stream()
                .map(MessageDtoMapper::toDto)
                .toList();
        return ResponseEntity.ok(messages);
    }
}