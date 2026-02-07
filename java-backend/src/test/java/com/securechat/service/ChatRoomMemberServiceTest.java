package com.securechat.service;

import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomMemberServiceTest {
    // This test class validates the ChatRoomMemberService which manages
    // user memberships in chat rooms (joining, leaving, role management)

    @Mock private ChatRoomRepository chatRoomRepository;  // Repository for chat room data
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;  
    @Mock private UserRepository userRepository;  // Repository for user data

    @InjectMocks private ChatRoomMemberService chatRoomMemberService;  // Service under test

    // Test data identifiers
    private UUID chatRoomId;
    private UUID userId;
    private UUID adminId;
    
    // Test entities
    private ChatRoom chatRoom;
    private User user;
    private ChatRoomMember member;

    @BeforeEach
    void setUp() {
        // Setup method runs before each test to create fresh test data
        chatRoomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        // Create a test chat room
        chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);
        chatRoom.setName("Test Room");
        chatRoom.setMaxParticipants(10);  // Room capacity limit

        // Create a regular test user
        user = new User();
        user.setId(userId);
        user.setUsername("regularuser");

        // Create an admin user (for role testing)
        User admin = new User();
        admin.setId(adminId);
        admin.setUsername("admin");

        // Create a test chat room member relationship
        member = new ChatRoomMember();
        member.setChatRoom(chatRoom);  // Associated chat room
        member.setUser(user);         
        member.setIsActive(true);      // Currently active in room
        member.setRole("MEMBER");     
        member.setJoinedAt(LocalDateTime.now());  // When user joined
    }

    @Test
    void joinChatRoom_Success_NewMember() {
        // Tests successful joining of a chat room as a new member
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId)).thenReturn(5L);  // Room has 5/10 members
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)).thenReturn(Optional.empty());  // Not a member yet
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(i -> i.getArgument(0));  

        ChatRoomMember result = chatRoomMemberService.joinChatRoom(chatRoomId, userId);

        // Verify new member was created with correct properties
        assertNotNull(result);
        assertTrue(result.getIsActive());  // Should be active
        assertEquals("MEMBER", result.getRole()); 
        assertEquals(user, result.getUser());  
        assertEquals(chatRoom, result.getChatRoom());  // Correct chat room
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));  // Should save new member
    }

    @Test
    void joinChatRoom_AlreadyActiveMember_ReturnsExisting() {
        // Tests joining when user is already an active member (idempotent operation)
        member.setIsActive(true);  // Already active member

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)).thenReturn(Optional.of(member));

        ChatRoomMember result = chatRoomMemberService.joinChatRoom(chatRoomId, userId);

        assertEquals(member, result);  // Should return existing member
        verify(chatRoomMemberRepository, never()).save(any());  // Should not save since already member
    }

    @Test
    void joinChatRoom_ChatRoomNotFound_ThrowsException() {
        // Tests error when chat room doesn't exist
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.joinChatRoom(chatRoomId, userId));
        assertEquals("ChatRoom not found", exception.getMessage());  // Clear error message
    }

    @Test
    void joinChatRoom_UserNotFound_ThrowsException() {
        // Tests error when user doesn't exist
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.joinChatRoom(chatRoomId, userId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void joinChatRoom_RoomIsFull_ThrowsException() {
        // Tests error when chat room has reached maximum capacity
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId)).thenReturn(10L);  // Room is full (10/10)
        // Note: findByChatRoomIdAndUserId stubbing intentionally removed for this test case

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.joinChatRoom(chatRoomId, userId));

        assertEquals("ChatRoom is full", exception.getMessage());  // Capacity limit error
        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndUserId(any(), any());  // Should check capacity first
    }

    @Test
    void leaveChatRoom_Success() {
        // Tests successful leaving of a chat room
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(i -> i.getArgument(0));

        chatRoomMemberService.leaveChatRoom(chatRoomId, userId);

        // Verify member is marked as inactive and read timestamp is updated
        assertFalse(member.getIsActive());  // Should be inactive after leaving
        assertNotNull(member.getLastReadAt());  
        verify(chatRoomMemberRepository).save(member);  // Should persist changes
    }

    @Test
    void leaveChatRoom_MemberNotFound_ThrowsException() {
        // Tests error when trying to leave a room where user is not a member
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.leaveChatRoom(chatRoomId, userId));
        assertEquals("Member not found", exception.getMessage());
    }

    @Test
    void getActiveMembers_ReturnsCount() {
        // Tests counting active members in a chat room
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId)).thenReturn(7L);

        long count = chatRoomMemberService.getActiveMembers(chatRoomId);

        assertEquals(7L, count);  // Should return correct count
        verify(chatRoomMemberRepository).countByChatRoomIdAndIsActiveTrue(chatRoomId);
    }

    @Test
    void updateMemberRole_Success() {
        // Tests successful role update (e.g., promoting to ADMIN)
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.save(member)).thenReturn(member);

        ChatRoomMember result = chatRoomMemberService.updateMemberRole(chatRoomId, userId, "ADMIN");

        assertEquals("ADMIN", result.getRole());  // Role should be updated
        verify(chatRoomMemberRepository).save(member);  // Should persist role change
    }

    @Test
    void updateMemberRole_MemberNotFound_ThrowsException() {
        // Tests error when updating role for non-member
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.updateMemberRole(chatRoomId, userId, "ADMIN"));
        assertEquals("Member not found", exception.getMessage());
    }

    @Test
    void markAsRead_Success() {
        // Tests marking chat room messages as read for a member
        // Spy on the member to verify markAsRead() method is called
        ChatRoomMember spyMember = spy(member);
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(spyMember));
        when(chatRoomMemberRepository.save(spyMember)).thenReturn(spyMember);

        chatRoomMemberService.markAsRead(chatRoomId, userId);

        verify(spyMember).markAsRead();  // Should call entity's markAsRead method
        verify(chatRoomMemberRepository).save(spyMember);  // Should persist read status
    }

    @Test
    void markAsRead_MemberNotFound_ThrowsException() {
        // Tests error when marking as read for non-member
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.markAsRead(chatRoomId, userId));
        assertEquals("Member not found", exception.getMessage());
    }

    @Test
    void isUserAdmin_ReturnsTrue_WhenAdmin() {
        // Tests admin check returns true for ADMIN role
        member.setRole("ADMIN");  // User has admin role
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        boolean isAdmin = chatRoomMemberService.isUserAdmin(chatRoomId, userId);

        assertTrue(isAdmin);  // Should return true for ADMIN role
    }

    @Test
    void isUserAdmin_ReturnsFalse_WhenNotAdmin() {
        // Tests admin check returns false for non-ADMIN role
        member.setRole("MEMBER");  // Regular member role
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        assertFalse(chatRoomMemberService.isUserAdmin(chatRoomId, userId));  // Should return false
    }

    @Test
    void isUserAdmin_ReturnsFalse_WhenNotMember() {
        // Tests admin check returns false when user is not a member
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        assertFalse(chatRoomMemberService.isUserAdmin(chatRoomId, userId));  // Non-members are not admins
    }

    @Test
    void getChatRoomMember_Success() {
        // Tests retrieving a specific chat room member
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        ChatRoomMember result = chatRoomMemberService.getChatRoomMember(chatRoomId, userId);

        assertEquals(member, result);  // Should return the member
    }

    @Test
    void getChatRoomMember_NotFound_ThrowsException() {
        // Tests error when member doesn't exist
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatRoomMemberService.getChatRoomMember(chatRoomId, userId));
        assertEquals("Member not found", exception.getMessage());
    }
}