package com.securechat.service;

import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service // Marks this class as a Spring service component (business logic layer)
public class ChatRoomMemberService {

    @Autowired // Injects ChatRoomMemberRepository for member-related database operations
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired // Injects ChatRoomRepository for chat room-related database operations
    private ChatRoomRepository chatRoomRepository;

    @Autowired // Injects UserRepository for user-related database operations
    private UserRepository userRepository;

    /**
     * Adds a user to a chat room as a member
     * @param chatRoomId ID of the chat room to join
     * @param userId 
     * @return Created/updated ChatRoomMember entity
     */
    public ChatRoomMember joinChatRoom(UUID chatRoomId, UUID userId) {
        // Verify chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check participant limit
        long activeMembers = chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId);
        if (activeMembers >= chatRoom.getMaxParticipants()) {
            throw new RuntimeException("ChatRoom is full");
        }

        // Check if user is already an active member (prevents duplicate memberships)
        java.util.Optional<ChatRoomMember> existing = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
        if (existing.isPresent() && existing.get().getIsActive()) {
            return existing.get(); // Return existing active membership
        }

        // Create new membership
        ChatRoomMember member = new ChatRoomMember();
        member.setChatRoom(chatRoom);
        member.setUser(user);
        member.setIsActive(true);
        member.setRole("MEMBER"); // Set default role (not ADMIN or MODERATOR)

        return chatRoomMemberRepository.save(member); // Persist to database
    }

    /**
     * Removes a user from a chat room (soft delete - sets isActive to false)
     * @param chatRoomId
     * @param userId ID of the user leaving the chat room
     */
    public void leaveChatRoom(UUID chatRoomId, UUID userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Soft delete - mark as inactive instead of physical deletion
        member.setIsActive(false);
        member.setLastReadAt(LocalDateTime.now()); // Record last read time as leaving time
        chatRoomMemberRepository.save(member); 
    }

    /**
     * Gets the number of active members in a chat room
     * @param chatRoomId 
     * @return Count of active members
     */
    public long getActiveMembers(UUID chatRoomId) {
        return chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId);
    }

    /**
     * Updates a member's role in a chat room
     * @param chatRoomId 
     * @param userId
     * @param role New role (ADMIN, MODERATOR, MEMBER)
     * @return
     */
    public ChatRoomMember updateMemberRole(UUID chatRoomId, UUID userId, String role) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setRole(role); // Update role
        return chatRoomMemberRepository.save(member); // Persist change
    }

    /**
     * Updates the lastReadAt timestamp to current time
     * @param chatRoomId
     * @param userId ID of the user marking messages as read
     */
    public void markAsRead(UUID chatRoomId, UUID userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.markAsRead(); // Calls utility method in entity class (sets lastReadAt = now)
        chatRoomMemberRepository.save(member); 
    }

    /**
     * Checks if a user has admin privileges in a chat room
     * @param chatRoomId 
     * @param userId ID of the user to check
     * @return 
     */
    public boolean isUserAdmin(UUID chatRoomId, UUID userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(member -> "ADMIN".equals(member.getRole())) // Check if role is "ADMIN"
                .orElse(false); // Return false if member not found
    }

    /**
     * Gets a chat room member by chat room ID and user ID
     * @param chatRoomId
     * @param userId 
     * @return ChatRoomMember entity if found
     * @throws RuntimeException 
     */
    public ChatRoomMember getChatRoomMember(UUID chatRoomId, UUID userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }
}