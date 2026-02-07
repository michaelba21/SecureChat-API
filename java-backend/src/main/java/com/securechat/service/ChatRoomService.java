package com.securechat.service;

import com.securechat.dto.ChatRoomCreateRequest;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @NotNull
    public ChatRoom createChatRoom(@NotNull ChatRoomCreateRequest request, @NotNull User creator) {
        // Create new chat room entity from request data
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(request.getName());
        chatRoom.setDescription(request.getDescription());
        chatRoom.setCreatedBy(creator); // Set User entity from authentication
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setIsPrivate(request.getIsPrivate());
        chatRoom.setMaxParticipants(request.getMaxParticipants());

        // Save chat room and add creator as first member with ADMIN role
        chatRoom = chatRoomRepository.save(chatRoom);
        addCreatorAsMember(chatRoom, creator);
        return chatRoom;
    }

    // Helper method to add chat room creator as ADMIN member
    private void addCreatorAsMember(ChatRoom chatRoom, User creator) {
        if (creator == null) {
            return;
        }

        ChatRoomMember member = new ChatRoomMember();
        member.setChatRoom(chatRoom);
        member.setUser(creator);
        member.setJoinedAt(LocalDateTime.now());
        member.setRole("ADMIN");
        member.setIsActive(true);
        chatRoomMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        // Retrieve all chat rooms from database
        return chatRoomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoom> getChatRoomById(@NotNull UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }
        // Fetch chat room by ID and initialize creator proxy to avoid
        // LazyInitializationException
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(id);
        chatRoomOpt.ifPresent(room -> {
            if (room.getCreatedBy() != null) {
                room.getCreatedBy().getId(); // Touch to initialize proxy
            }
        });
        return chatRoomOpt;
    }

    @Transactional
    public void deleteChatRoom(@NotNull UUID id, User currentUser) {
        if (id == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }

        // Fetch chat room with creator loaded to verify ownership
        ChatRoom chatRoom = chatRoomRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new com.securechat.exception.ResourceNotFoundException("ChatRoom not found"));

        // Only allow deletion by chat room owner
        if (chatRoom.getCreatedBy() == null || !chatRoom.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.securechat.exception.UnauthorizedException("Only the owner can delete this chatroom");
        }

        // Delete all members first (maintain referential integrity)
        chatRoomMemberRepository.deleteByChatRoomId(id);
        // Then delete the chat room
        chatRoomRepository.delete(chatRoom);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<ChatRoom> searchChatRooms(@NotNull String searchTerm) {
        if (searchTerm == null) {
            throw new IllegalArgumentException("Search term cannot be null");
        }
        // Search chat rooms by name (case-insensitive)
        return chatRoomRepository.findByNameContainingIgnoreCase(searchTerm);
    }

    @Transactional
    @NotNull
    public Optional<ChatRoomMember> addMemberToChatRoom(@NotNull UUID chatRoomId, @NotNull UUID userId) {
        // Check if chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new com.securechat.exception.ResourceNotFoundException("Chat room not found with id: " + chatRoomId));

        // Verify user exists
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is already a member
        Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId,
                userId);
        if (existingMember.isPresent()) {
            // If member exists but is inactive, reactivate them
            ChatRoomMember member = existingMember.get();
            if (Boolean.TRUE.equals(member.getIsActive())) {
                throw new IllegalArgumentException("User is already a member of this chat room");
            }
            member.setIsActive(true);
            member.setJoinedAt(LocalDateTime.now());
            return Optional.of(chatRoomMemberRepository.save(member));
        }

        // Create new member if not previously a member
        return createNewMember(chatRoom, userId);
    }

    // Create a new member with MEMBER role, checking max participant limit
    private Optional<ChatRoomMember> createNewMember(ChatRoom chatRoom, UUID userId) {
        // Check if room has reached maximum participant limit
        long currentMembers = chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        if (currentMembers >= chatRoom.getMaxParticipants()) {
            return Optional.empty(); // Room is full
        }

        // Create new member with MEMBER role
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ChatRoomMember member = new ChatRoomMember();
        member.setChatRoom(chatRoom);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        member.setRole("MEMBER");
        member.setIsActive(true);
        return Optional.of(chatRoomMemberRepository.save(member));
    }

    @Transactional
    public void removeMemberFromChatRoom(@NotNull UUID chatRoomId, @NotNull UUID userId) {
        // Check if chat room exists
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new com.securechat.exception.ResourceNotFoundException("Chat room not found with id: " + chatRoomId);
        }

        // Soft delete: mark member as inactive instead of deleting record
        Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this chat room");
        }

        ChatRoomMember member = memberOpt.get();
        // Early exit if member is already inactive (idempotent operation)
        if (!member.getIsActive()) {
            return;
        }
        member.setIsActive(false);
        member.setLastReadAt(LocalDateTime.now()); // Record when they last read messages
        chatRoomMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<ChatRoomMember> getChatRoomMembers(@NotNull UUID chatRoomId) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }
        // Get all members (including inactive) for the chat room
        return chatRoomMemberRepository.findByChatRoomId(chatRoomId);
    }

    @Transactional
    @NotNull
    public ChatRoom updateChatRoom(@NotNull UUID chatRoomId, @NotNull java.util.Map<String, Object> updates) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null for update");
        }

        // Find existing chat room
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatRoomId));

        // Apply updates from map (legacy method for backward compatibility)
        if (updates.containsKey("name") && updates.get("name") != null) {
            String name = (String) updates.get("name");
            if (!name.trim().isEmpty()) {
                chatRoom.setName(name.trim());
            }
        }
        if (updates.containsKey("description")) {
            String description = (String) updates.get("description");
            chatRoom.setDescription(description != null ? description.trim() : null);
        }
        if (updates.containsKey("isPrivate") && updates.get("isPrivate") != null) {
            Object val = updates.get("isPrivate");
            chatRoom.setIsPrivate(val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(val.toString()));
        }
        if (updates.containsKey("maxParticipants") && updates.get("maxParticipants") != null) {
            Object val = updates.get("maxParticipants");
            Integer maxParticipants = val instanceof Integer ? (Integer) val : Integer.parseInt(val.toString());
            if (maxParticipants > 0) {
                chatRoom.setMaxParticipants(maxParticipants);
            }
        }

        return chatRoom;
    }

    @Transactional
    @NotNull
    public ChatRoom updateChatRoom(
            @NotNull UUID chatRoomId,
            @NotNull com.securechat.dto.ChatRoomUpdateRequest request,
            @NotNull User currentUser) {

        // Validate all inputs
        if (chatRoomId == null) {
            throw new com.securechat.exception.ResourceNotFoundException("ChatRoom ID cannot be null");
        }
        if (request == null) {
            throw new com.securechat.exception.ValidationException("Update request cannot be null");
        }
        if (currentUser == null) {
            throw new com.securechat.exception.UnauthorizedException("Current user is required");
        }

        // Fetch chat room with creator for ownership verification
        ChatRoom chatRoom = chatRoomRepository.findByIdWithCreator(chatRoomId)
                .orElseThrow(() -> new com.securechat.exception.ResourceNotFoundException(
                        "ChatRoom not found with id: " + chatRoomId));

        if (chatRoom.getCreatedBy() == null) {
            throw new IllegalStateException("Data corruption: ChatRoom missing creator");
        }

        // Only allow updates by chat room owner
        if (!chatRoom.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.securechat.exception.UnauthorizedException("You are not the owner of this chatroom");
        }

        // Apply validated updates with business rules
        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            if (name.length() > 100) {
                throw new com.securechat.exception.ValidationException("Chatroom name cannot exceed 100 characters");
            }
            chatRoom.setName(name);
        }

        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            if (description.length() > 500) {
                throw new com.securechat.exception.ValidationException("Description cannot exceed 500 characters");
            }
            chatRoom.setDescription(description);
        }

        if (request.getIsPrivate() != null) {
            chatRoom.setIsPrivate(request.getIsPrivate());
        }

        if (request.getMaxParticipants() != null) {
            if (request.getMaxParticipants() < 1 || request.getMaxParticipants() > 1000) {
                throw new com.securechat.exception.ValidationException("Max participants must be between 1 and 1000");
            }
            chatRoom.setMaxParticipants(request.getMaxParticipants());
        }

        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("ChatRoom cannot be null");
        }
        // Generic save method for direct entity persistence
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<ChatRoom> getUserChatRooms(@NotNull UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        // Get all active chat rooms where user is a member
        List<ChatRoomMember> members = chatRoomMemberRepository.findActiveByUserId(userId);
        return members.stream()
                .map(member -> {
                    ChatRoom room = member.getChatRoom();
                    if (room.getCreatedBy() != null) {
                        room.getCreatedBy().getId(); // Initialize proxy
                    }
                    return room;
                })
                .toList();
    }

    @Transactional
    public ChatRoomMember updateMemberRole(UUID chatRoomId, UUID userId, String role) {
        // Update member's role (e.g., from MEMBER to MODERATOR)
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setRole(role);
        return chatRoomMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<ChatRoomMember> getActiveChatRoomMembers(@NotNull UUID chatRoomId) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }
        // Filter only active members from all members list
        return chatRoomMemberRepository.findByChatRoomId(chatRoomId).stream()
                .filter(member -> member.getIsActive())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isMember(@NotNull UUID userId, @NotNull UUID chatRoomId) {
        if (userId == null || chatRoomId == null) {
            return false;
        }
        // Check if user is an active member of the chat room
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(ChatRoomMember::getIsActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<com.securechat.dto.ChatRoomSummaryDto> getUserChatRoomSummaries(@NotNull UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        // Get summarized view of user's chat rooms (DTO projection)
        return chatRoomRepository.findUserChatRoomSummaries(userId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<com.securechat.dto.ChatRoomDetailDto> getChatRoomDetail(@NotNull UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }
        // Get detailed view of chat room with aggregated info (DTO projection)
        return chatRoomRepository.findDetailById(id);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<com.securechat.dto.MemberDto> getChatRoomMemberDtos(@NotNull UUID chatRoomId) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("ChatRoom ID cannot be null");
        }
        // Check if chat room exists first
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new com.securechat.exception.ResourceNotFoundException("Chat room not found with id: " + chatRoomId);
        }
        // Get member information as DTOs (projection, not full entities)
        return chatRoomMemberRepository.findMembersByChatRoomId(chatRoomId);
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<com.securechat.dto.ChatRoomDTO> getChatroomsByCreator(@NotNull UUID creatorId) {
        if (creatorId == null) {
            throw new IllegalArgumentException("Creator ID cannot be null");
        }
        // Get all chat rooms created by a specific user
        List<ChatRoom> rooms = chatRoomRepository.findByCreatedBy_Id(creatorId);
        return rooms.stream().map(com.securechat.mapper.ChatRoomDtoMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    @NotNull
    public List<com.securechat.dto.ChatRoomSummaryDto> getPublicChatRooms() {
        // Get summaries of all public (non-private) chat rooms
        return chatRoomRepository.findPublicChatRoomSummaries();
    }
}
