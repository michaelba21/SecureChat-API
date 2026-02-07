package com.securechat.service;

import com.securechat.entity.ChatRoomMember;
import com.securechat.repository.ChatRoomMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService Tests")
class SecurityServiceTest {
    // This test class validates the SecurityService which handles authorization checks
    // for chat room access and ownership permissions

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;  // Repository for membership data

    @InjectMocks
    private SecurityService securityService;  // Service under test

    // Test data
    private final UUID chatRoomId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("isChatRoomMember returns true when member exists and is active")
    void isChatRoomMember_returnsTrue_whenActiveMemberExists() {
        // Tests that active chat room members are correctly identified
        // Arrange
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(true);  // Member is active in the chat room

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        // Act
        boolean result = securityService.isChatRoomMember(chatRoomId, userId);

        // Assert
        assertTrue(result);  // Should return true for active member
        verify(chatRoomMemberRepository).findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Test
    @DisplayName("isChatRoomMember returns false when member exists but is inactive")
    void isChatRoomMember_returnsFalse_whenInactiveMemberExists() {
        // Tests that inactive (left) members are not considered current members
        // Arrange
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(false);  // Member has left the chat room (inactive)

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        // Act
        boolean result = securityService.isChatRoomMember(chatRoomId, userId);

        // Assert
        assertFalse(result);  // Should return false for inactive member
    }

    @Test
    @DisplayName("isChatRoomMember returns false when no member found")
    void isChatRoomMember_returnsFalse_whenNoMemberFound() {
        // Tests that non-members are correctly identified
        // Arrange
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());  // No membership record exists

        // Act
        boolean result = securityService.isChatRoomMember(chatRoomId, userId);

        // Assert
        assertFalse(result);  // Should return false for non-member
    }

    @Test
    @DisplayName("isChatRoomMember returns false when chatRoomId is null")
    void isChatRoomMember_returnsFalse_whenChatRoomIdIsNull() {
        // Tests null safety for chat room ID
        // Act
        boolean result = securityService.isChatRoomMember(null, userId);

        // Assert
        assertFalse(result);  // Should return false for null chat room ID
        verifyNoInteractions(chatRoomMemberRepository);  // Should not query repository
    }

    @Test
    @DisplayName("isChatRoomMember returns false when userId is null")
    void isChatRoomMember_returnsFalse_whenUserIdIsNull() {
        // Tests null safety for user ID
        // Act
        boolean result = securityService.isChatRoomMember(chatRoomId, null);

        // Assert
        assertFalse(result);  // Should return false for null user ID
        verifyNoInteractions(chatRoomMemberRepository);  // Should not query repository
    }

    @Test
    @DisplayName("isChatRoomOwner returns true when member has ADMIN role")
    void isChatRoomOwner_returnsTrue_whenAdminRole() {
        // Tests that users with ADMIN role are recognized as chat room owners
        // Arrange
        ChatRoomMember member = new ChatRoomMember();
        member.setRole("ADMIN");  // Owner/admin role

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        // Act
        boolean result = securityService.isChatRoomOwner(chatRoomId, userId);

        // Assert
        assertTrue(result);  // Should return true for ADMIN role
    }

    @Test
    @DisplayName("isChatRoomOwner returns false when member has non-ADMIN role")
    void isChatRoomOwner_returnsFalse_whenNonAdminRole() {
        // Tests that regular members (non-ADMIN) are not recognized as owners
        // Arrange
        ChatRoomMember member = new ChatRoomMember();
        member.setRole("MEMBER");  // Regular member role

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        // Act
        boolean result = securityService.isChatRoomOwner(chatRoomId, userId);

        // Assert
        assertFalse(result);  // Should return false for non-ADMIN role
    }

    @Test
    @DisplayName("isChatRoomOwner returns false when no member found")
    void isChatRoomOwner_returnsFalse_whenNoMemberFound() {
        // Tests that non-members are not owners
        // Arrange
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());  // No membership

        // Act
        boolean result = securityService.isChatRoomOwner(chatRoomId, userId);

        // Assert
        assertFalse(result);  // Should return false for non-member
    }

    @Test
    @DisplayName("isChatRoomOwner returns false when chatRoomId is null")
    void isChatRoomOwner_returnsFalse_whenChatRoomIdIsNull() {
        // Tests null safety for chat room ID in ownership check
        // Act
        boolean result = securityService.isChatRoomOwner(null, userId);

        // Assert
        assertFalse(result);  // Should return false for null chat room ID
        verifyNoInteractions(chatRoomMemberRepository);  // Should not query repository
    }

    @Test
    @DisplayName("isChatRoomOwner returns false when userId is null")
    void isChatRoomOwner_returnsFalse_whenUserIdIsNull() {
        // Tests null safety for user ID in ownership check
        // Act
        boolean result = securityService.isChatRoomOwner(chatRoomId, null);

        // Assert
        assertFalse(result);  // Should return false for null user ID
        verifyNoInteractions(chatRoomMemberRepository);  // Should not query repository
    }
}