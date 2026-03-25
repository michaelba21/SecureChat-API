package com.securechat.service;
import com.securechat.repository.ChatRoomMemberRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Custom security service for authorization checks
 * Used by Spring Security's @PreAuthorize annotations for method-level security
 */
@Service("securityService")  // Explicit bean name for referencing in SpEL expressions
public class SecurityService {

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;  // Repository for chat room membership data

    /**
     * Check if a user is an active member of a chatroom
     * Used in @PreAuthorize expressions like: @PreAuthorize("@securityService.isChatRoomMember(#chatRoomId, #userId)")
     * 
     * @param chatRoomId The UUID of the chat room to check
     * @param userId 
     * @return true if user is an active member of the chat room, false otherwise
     */
    public boolean isChatRoomMember(@NotNull UUID chatRoomId, @NotNull UUID userId) {
        // Defensive null checking - return false for invalid inputs
        if (chatRoomId == null || userId == null) {
            return false;
        }
        
        // Query database for membership record and check if member is active
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(member -> member.getIsActive())  
                .orElse(false);  // Return false if no membership record found (Optional empty)
    }

    /**
     * Check if a user is the owner/administrator of a chatroom
     * Used in @PreAuthorize expressions for administrative operations
     * 
     * @param chatRoomId 
     * @param userId The UUID of the user to verify ownership for
     * @return true if user has ADMIN role in the chat room, false otherwise
     */
    public boolean isChatRoomOwner(@NotNull UUID chatRoomId, @NotNull UUID userId) {
        // Defensive null checking - return false for invalid inputs
        if (chatRoomId == null || userId == null) {
            return false;
        }
        
        // Query database for membership record and check if role is ADMIN
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(member -> "ADMIN".equals(member.getRole()))  
                .orElse(false);  // Return false if no membership record found or role is not ADMIN
    }
}