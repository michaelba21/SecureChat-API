

package com.securechat.service;

import com.securechat.dto.*;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.User;
import com.securechat.exception.ResourceNotFoundException;
import com.securechat.exception.UnauthorizedException;
import com.securechat.exception.ValidationException;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito with JUnit 5
class ChatRoomServiceTest {

    @Mock // Mock repository for chat rooms
    private ChatRoomRepository chatRoomRepository;

    @Mock // Mock repository for chat room members
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock // Mock repository for users
    private UserRepository userRepository;

    @InjectMocks // Inject mocks into ChatRoomService instance
    private ChatRoomService chatRoomService;

    private User creator; // Chat room creator
    private User regularUser; // Regular user
    private ChatRoom chatRoom; // Test chat room
    private UUID chatRoomId; // Chat room ID
    private UUID creatorId; // Creator user ID
    private UUID regularUserId; // Regular user ID

    @BeforeEach // Runs before each test method
    void setUp() {
        // Generate test IDs
        creatorId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();
        chatRoomId = UUID.randomUUID();

        // Create creator user
        creator = new User();
        creator.setId(creatorId);
        creator.setUsername("creator");

        // Create regular user
        regularUser = new User();
        regularUser.setId(regularUserId);
        regularUser.setUsername("member");

        // Create test chat room
        chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);
        chatRoom.setName("Test Room");
        chatRoom.setDescription("Test Description");
        chatRoom.setCreatedBy(creator); // Set creator
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setIsPrivate(false); // Public room
        chatRoom.setMaxParticipants(50); // Max 50 participants
    }

    // ====================== CREATE CHAT ROOM ======================

    @Test
    void createChatRoom_Success() {
        // Arrange: create chat room request
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("My Room");
        request.setDescription("Desc");
        request.setIsPrivate(true); // Private room
        request.setMaxParticipants(100);

        // Mock repository save operations
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(i -> {
            ChatRoom cr = i.getArgument(0);
            cr.setId(UUID.randomUUID()); // Set generated ID
            return cr;
        });

        when(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .thenAnswer(i -> i.getArgument(0)); // Return saved member

        // Act: create chat room
        ChatRoom result = chatRoomService.createChatRoom(request, creator);

        // Assert: verify chat room properties
        assertNotNull(result.getId()); // ID should be generated
        assertEquals("My Room", result.getName()); // Correct name
        assertEquals("Desc", result.getDescription()); // Correct description
        assertEquals(creator, result.getCreatedBy()); // Correct creator
        assertTrue(result.getIsPrivate()); // Should be private
        assertEquals(100, result.getMaxParticipants()); // Correct max participants

        // Verify repository calls
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void createChatRoom_NullRequest_ThrowsException() {
        // Act & Assert: null request should throw NullPointerException
        assertThrows(NullPointerException.class, () -> chatRoomService.createChatRoom(null, creator));
    }

    @Test
    void createChatRoom_NullCreator_ShouldStillCreateChatRoom() {
        // Arrange: create request without creator
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("My Room");
        request.setDescription("Desc");
        request.setIsPrivate(false);
        request.setMaxParticipants(50);

        // Mock repository save
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(i -> {
            ChatRoom cr = i.getArgument(0);
            cr.setId(UUID.randomUUID());
            return cr;
        });

        // Act: create chat room with null creator
        ChatRoom result = chatRoomService.createChatRoom(request, null);

        // Assert: chat room should still be created
        assertNotNull(result);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, never()).save(any()); // No member should be added
    }

    // ====================== READ CHAT ROOMS ======================

    @Test
    void getAllChatRooms() {
        // Arrange: mock repository to return test chat room
        when(chatRoomRepository.findAll()).thenReturn(List.of(chatRoom));

        // Act: get all chat rooms
        List<ChatRoom> result = chatRoomService.getAllChatRooms();

        // Assert: should return one chat room
        assertEquals(1, result.size());
        verify(chatRoomRepository).findAll(); // Verify repository call
    }

    @Test
    void getChatRoomById_Found() {
        // Arrange: mock repository to find chat room
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: get chat room by ID
        Optional<ChatRoom> result = chatRoomService.getChatRoomById(chatRoomId);

        // Assert: should find chat room
        assertTrue(result.isPresent());
        assertEquals(chatRoom, result.get());
    }

    @Test
    void getChatRoomById_Found_NoCreator() {
        // Arrange: chat room without creator
        ChatRoom roomNoCreator = new ChatRoom();
        roomNoCreator.setId(chatRoomId);
        roomNoCreator.setCreatedBy(null); // No creator

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(roomNoCreator));

        // Act: get chat room
        Optional<ChatRoom> result = chatRoomService.getChatRoomById(chatRoomId);

        // Assert: should find chat room with null creator
        assertTrue(result.isPresent());
        assertNull(result.get().getCreatedBy());
    }

    @Test
    void getChatRoomById_NotFound() {
        // Arrange: mock empty result
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        // Act: get non-existent chat room
        Optional<ChatRoom> result = chatRoomService.getChatRoomById(chatRoomId);

        // Assert: should return empty optional
        assertFalse(result.isPresent());
    }

    @Test
    void getChatRoomById_NullId_ThrowsException() {
        // Act & Assert: null ID should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getChatRoomById(null));
    }

    @Test
    void searchChatRooms() {
        // Arrange: mock search results
        when(chatRoomRepository.findByNameContainingIgnoreCase("test"))
                .thenReturn(List.of(chatRoom));

        // Act: search chat rooms
        List<ChatRoom> result = chatRoomService.searchChatRooms("test");

        // Assert: should return matching chat room
        assertEquals(1, result.size());
        verify(chatRoomRepository).findByNameContainingIgnoreCase("test");
    }

    @Test
    void searchChatRooms_NullTerm_ThrowsException() {
        // Act & Assert: null search term should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.searchChatRooms(null));
    }

    @Test
    void searchChatRooms_EmptyTerm() {
        // Arrange: mock search with empty term
        when(chatRoomRepository.findByNameContainingIgnoreCase("")).thenReturn(List.of(chatRoom));

        // Act: search with empty string
        List<ChatRoom> result = chatRoomService.searchChatRooms("");

        // Assert: should return all chat rooms (empty search = no filter)
        assertEquals(1, result.size());
        verify(chatRoomRepository).findByNameContainingIgnoreCase("");
    }

    // ====================== USER CHAT ROOMS ======================

    @Test
    void getUserChatRooms() {
        // Arrange: create active chat room member
        ChatRoomMember activeMember = new ChatRoomMember();
        activeMember.setChatRoom(chatRoom);
        activeMember.setIsActive(true);

        when(chatRoomMemberRepository.findActiveByUserId(creatorId))
                .thenReturn(List.of(activeMember));

        // Act: get user's chat rooms
        List<ChatRoom> result = chatRoomService.getUserChatRooms(creatorId);

        // Assert: should return user's chat rooms
        assertEquals(1, result.size());
        assertEquals(chatRoom, result.get(0));

        verify(chatRoomMemberRepository).findActiveByUserId(creatorId);
    }

    @Test
    void getUserChatRooms_NullUserId_ThrowsException() {
        // Act & Assert: null user ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getUserChatRooms(null));
    }

    @Test
    void getUserChatRooms_NoChatRooms() {
        // Arrange: user has no chat rooms
        when(chatRoomMemberRepository.findActiveByUserId(creatorId))
                .thenReturn(List.of());

        // Act: get user's chat rooms
        List<ChatRoom> result = chatRoomService.getUserChatRooms(creatorId);

        // Assert: should return empty list
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserChatRooms_CreatorProxyInitialization() {
        // Arrange: mix of chat rooms with and without creators
        ChatRoomMember activeMember = new ChatRoomMember();
        activeMember.setChatRoom(chatRoom);
        activeMember.setIsActive(true);

        ChatRoom roomWithoutCreator = new ChatRoom();
        roomWithoutCreator.setId(chatRoomId);
        roomWithoutCreator.setCreatedBy(null); // No creator

        ChatRoomMember memberWithoutCreator = new ChatRoomMember();
        memberWithoutCreator.setChatRoom(roomWithoutCreator);
        memberWithoutCreator.setIsActive(true);

        when(chatRoomMemberRepository.findActiveByUserId(creatorId))
                .thenReturn(List.of(activeMember, memberWithoutCreator));

        // Act: get user's chat rooms
        List<ChatRoom> result = chatRoomService.getUserChatRooms(creatorId);

        // Assert: should return both chat rooms
        assertEquals(2, result.size());
    }

    @Test
    void getChatRoomMembers() {
        // Arrange: create chat room member
        ChatRoomMember member = new ChatRoomMember();
        member.setUser(creator);

        when(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .thenReturn(List.of(member));

        // Act: get chat room members
        List<ChatRoomMember> result = chatRoomService.getChatRoomMembers(chatRoomId);

        // Assert: should return members
        assertEquals(1, result.size());
        verify(chatRoomMemberRepository).findByChatRoomId(chatRoomId);
    }

    @Test
    void getChatRoomMembers_NullId_ThrowsException() {
        // Act & Assert: null chat room ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getChatRoomMembers(null));
    }

    @Test
    void getActiveChatRoomMembers() {
        // Arrange: mix of active and inactive members
        ChatRoomMember active = new ChatRoomMember();
        active.setIsActive(true);

        ChatRoomMember inactive = new ChatRoomMember();
        inactive.setIsActive(false);

        when(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .thenReturn(List.of(active, inactive));

        // Act: get active members only
        List<ChatRoomMember> result = chatRoomService.getActiveChatRoomMembers(chatRoomId);

        // Assert: should return only active members
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getActiveChatRoomMembers_NullId_ThrowsException() {
        // Act & Assert: null chat room ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getActiveChatRoomMembers(null));
    }

    // ====================== UPDATE CHAT ROOM ======================

    @Test
    void updateChatRoom_Success() {
        // Arrange: update map with new name
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "New Name");

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: name should be updated
        assertEquals("New Name", result.getName());
        verify(chatRoomRepository).findById(chatRoomId);
    }

    @Test
    void updateChatRoom_NullId_ThrowsException() {
        // Arrange: update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Some Name");

        // Act & Assert: null ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.updateChatRoom(null, updates));
    }

    @Test
    void updateChatRoom_NullUpdates_ThrowsException() {
        // Act & Assert: null updates should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, null));
    }

    @Test
    void updateChatRoom_NotFound_ThrowsException() {
        // Arrange: update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "New Name");

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        // Act & Assert: non-existent chat room should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, updates));
    }

    @Test
    void updateChatRoom_AllFields() {
        // Arrange: update all fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "New Name");
        updates.put("description", "New Description");
        updates.put("isPrivate", true);
        updates.put("maxParticipants", 100);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: all fields should be updated
        assertEquals("New Name", result.getName());
        assertEquals("New Description", result.getDescription());
        assertTrue(result.getIsPrivate());
        assertEquals(100, result.getMaxParticipants());
    }

    @Test
    void updateChatRoom_EmptyName_ShouldNotUpdate() {
        // Arrange: empty name in updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "   "); // Whitespace only

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: name should NOT be updated (keeps original)
        assertEquals("Test Room", result.getName()); // Should keep original
    }

    @Test
    void updateChatRoom_Legacy_NullValues_Ignored() {
        // Test: null values in update map should be ignored
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", null);
        updates.put("isPrivate", null);
        updates.put("maxParticipants", null);
        // Note: description behaves differently - allows setting to null

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: null values should not change fields
        assertEquals("Test Room", result.getName()); // Original name
        assertFalse(result.getIsPrivate()); // Original privacy
        assertEquals(50, result.getMaxParticipants()); // Original max participants
    }

    @Test
    void updateChatRoom_NullDescription() {
        // Arrange: set description to null
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", null);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: description should be null
        assertNull(result.getDescription());
    }

    @Test
    void updateChatRoom_MaxParticipantsZero_ShouldNotUpdate() {
        // Arrange: invalid max participants (0)
        Map<String, Object> updates = new HashMap<>();
        updates.put("maxParticipants", 0); // Invalid value

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: should keep original max participants
        assertEquals(50, result.getMaxParticipants()); // Should keep original
    }

    @Test
    void updateChatRoom_MaxParticipantsStringValue() {
        // Arrange: string value for max participants
        Map<String, Object> updates = new HashMap<>();
        updates.put("maxParticipants", "75"); // String value

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: should convert string to integer
        assertEquals(75, result.getMaxParticipants());
    }

    @Test
    void updateChatRoom_IsPrivateStringValue() {
        // Arrange: string value for isPrivate
        Map<String, Object> updates = new HashMap<>();
        updates.put("isPrivate", "true"); // String value

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act: update chat room
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, updates);

        // Assert: should convert string to boolean
        assertTrue(result.getIsPrivate());
    }

    @Test
    void updateMemberRole_Success() {
        // Arrange: existing member
        ChatRoomMember member = new ChatRoomMember();
        member.setRole("MEMBER");

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(member));
        when(chatRoomMemberRepository.save(member)).thenReturn(member);

        // Act: update member role
        ChatRoomMember result = chatRoomService.updateMemberRole(chatRoomId, regularUserId, "ADMIN");

        // Assert: role should be updated
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void updateMemberRole_NotFound_ThrowsException() {
        // Arrange: member not found
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty());

        // Act & Assert: should throw exception
        assertThrows(RuntimeException.class,
                () -> chatRoomService.updateMemberRole(chatRoomId, regularUserId, "ADMIN"));
    }

    // ====================== DELETE CHAT ROOM ======================

    @Test
    void deleteChatRoom_Success() {
        // Arrange: mock repository calls
        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));
        doNothing().when(chatRoomMemberRepository).deleteByChatRoomId(chatRoomId);

        // Act: delete chat room
        chatRoomService.deleteChatRoom(chatRoomId, creator);

        // Assert: verify all repository operations
        verify(chatRoomRepository).findByIdWithCreator(chatRoomId);
        verify(chatRoomMemberRepository).deleteByChatRoomId(chatRoomId); // Delete members first
        verify(chatRoomRepository).delete(chatRoom); // Then delete chat room
    }

    @Test
    void deleteChatRoom_NullId_ThrowsException() {
        // Act & Assert: null ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.deleteChatRoom(null, creator));
    }

    @Test
    void deleteChatRoom_NotFound_ThrowsException() {
        // Arrange: chat room not found
        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.empty());

        // Act & Assert: should throw ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.deleteChatRoom(chatRoomId, creator));
    }

    @Test
    void deleteChatRoom_NotOwner_ThrowsException() {
        // Arrange: non-owner user
        User nonOwner = new User();
        nonOwner.setId(UUID.randomUUID());
        nonOwner.setUsername("nonOwner");

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: non-owner should throw UnauthorizedException
        assertThrows(UnauthorizedException.class,
                () -> chatRoomService.deleteChatRoom(chatRoomId, nonOwner));
    }

    @Test
    void deleteChatRoom_CreatorNull_ThrowsException() {
        // Arrange: chat room has no creator
        chatRoom.setCreatedBy(null);
        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw UnauthorizedException
        assertThrows(UnauthorizedException.class,
                () -> chatRoomService.deleteChatRoom(chatRoomId, creator));
    }

    // ====================== ADD MEMBER ======================

    @Test
    void addMemberToChatRoom_NewMember_Success() {
        // Arrange: new member scenario
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty()); // Not a member yet
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId))
                .thenReturn(5L); // Room not full
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act: add member
        Optional<ChatRoomMember> result = chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId);

        // Assert: should create new member
        assertTrue(result.isPresent());
        assertEquals("MEMBER", result.get().getRole()); // Default role
        assertTrue(result.get().getIsActive()); // Should be active
    }

    @Test
    void addMemberToChatRoom_AlreadyActive_ThrowsException() {
        // Arrange: member already active
        ChatRoomMember existing = new ChatRoomMember();
        existing.setIsActive(true);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(existing)); // Already active member

        // Act & Assert: should throw exception for duplicate
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId));
    }

    @Test
    void addMemberToChatRoom_ReactivatesInactiveMember() {
        // Arrange: inactive member exists
        ChatRoomMember inactiveMember = new ChatRoomMember();
        inactiveMember.setIsActive(false);
        inactiveMember.setRole("MEMBER");

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(inactiveMember)); // Inactive member
        when(chatRoomMemberRepository.save(inactiveMember)).thenReturn(inactiveMember);

        // Act: add member (reactivate)
        Optional<ChatRoomMember> result = chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId);

        // Assert: should reactivate member
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsActive()); // Should now be active
        verify(chatRoomMemberRepository).save(inactiveMember); // Should save
    }

    @Test
    void addMemberToChatRoom_ChatRoomNotFound_ThrowsException() {
        // Arrange: chat room not found
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        // Act & Assert: should throw ResourceNotFoundException
        assertThrows(com.securechat.exception.ResourceNotFoundException.class,
                () -> chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId));
    }

    @Test
    void addMemberToChatRoom_UserNotFound_ThrowsException() {
        // Arrange: user not found
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.empty());

        // Act & Assert: should throw RuntimeException
        assertThrows(RuntimeException.class,
                () -> chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId));
    }

    @Test
    void addMemberToChatRoom_MaxParticipantsReached_ReturnsEmpty() {
        // Arrange: room at max capacity
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty()); // Not a member
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId))
                .thenReturn(100L); // At max capacity (maxParticipants = 100)

        // Act: try to add member
        Optional<ChatRoomMember> result = chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId);

        // Assert: should return empty optional (room full)
        assertFalse(result.isPresent());
    }

    @Test
    void addMemberToChatRoom_NullChatRoomId_ThrowsException() {
        // Act & Assert: null chat room ID should throw exception
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.addMemberToChatRoom(null, regularUserId));
    }

    @Test
    void addMemberToChatRoom_NullUserId_ThrowsException() {
        // Act & Assert: null user ID should throw exception
        assertThrows(RuntimeException.class,
                () -> chatRoomService.addMemberToChatRoom(chatRoomId, null));
    }

    // ====================== REMOVE MEMBER ======================

    @Test
    void removeMemberFromChatRoom_Success() {
        // Arrange: active member
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(true);

        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(true);
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(member));

        // Act: remove member
        chatRoomService.removeMemberFromChatRoom(chatRoomId, regularUserId);

        // Assert: member should be deactivated
        assertFalse(member.getIsActive()); // Should be inactive
        assertNotNull(member.getLastReadAt()); // Should have last read timestamp
        verify(chatRoomMemberRepository).save(member); // Should save changes
    }

    @Test
    void removeMemberFromChatRoom_MemberAlreadyInactive_NoChange() {
        // Arrange: already inactive member
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(false);

        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(true);
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(member)); // Already inactive

        // Act: remove member
        chatRoomService.removeMemberFromChatRoom(chatRoomId, regularUserId);

        // Assert: no save should occur (already inactive)
        verify(chatRoomMemberRepository, never()).save(any());
    }

    @Test
    void removeMemberFromChatRoom_MemberNotFound_ThrowsException() {
        // Arrange: member not found
        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(true);
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty()); // Member not found

        // Act & Assert: should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.removeMemberFromChatRoom(chatRoomId, regularUserId));

        verify(chatRoomMemberRepository, never()).save(any()); // No save
    }

    @Test
    void removeMemberFromChatRoom_NullChatRoomId_ThrowsException() {
        // Act & Assert: null chat room ID should throw exception
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.removeMemberFromChatRoom(null, regularUserId));
    }

        @Test
    void removeMemberFromChatRoom_ChatRoomNotFound_ThrowsResourceNotFound() {
        // Arrange: room does NOT exist
        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(false);

        // Act & Assert: should throw before even looking for member
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.removeMemberFromChatRoom(chatRoomId, regularUserId));

        // Verify we never reached member lookup or save
        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndUserId(any(), any());
        verify(chatRoomMemberRepository, never()).save(any());
    }

    @Test
    void isMember_Active_ReturnsTrue() {
        // Arrange: active member
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(true);

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(member));

        // Act: check membership
        assertTrue(chatRoomService.isMember(regularUserId, chatRoomId));
    }

    @Test
    void isMember_Inactive_ReturnsFalse() {
        // Arrange: inactive member
        ChatRoomMember member = new ChatRoomMember();
        member.setIsActive(false);

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.of(member));

        // Act: check membership
        assertFalse(chatRoomService.isMember(regularUserId, chatRoomId));
    }

    @Test
    void isMember_NotFound_ReturnsFalse() {
        // Arrange: member not found
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty());

        // Act: check membership
        assertFalse(chatRoomService.isMember(regularUserId, chatRoomId));
    }

    @Test
    void isMember_NullUserId_ReturnsFalse() {
        // Act & Assert: null user ID should return false
        assertFalse(chatRoomService.isMember(null, chatRoomId));
    }

    @Test
    void isMember_NullChatRoomId_ReturnsFalse() {
        // Act & Assert: null chat room ID should return false
        assertFalse(chatRoomService.isMember(regularUserId, null));
    }

    // ====================== DTO METHODS ======================

    @Test
    void getUserChatRoomSummaries_Success() {
        // Arrange: mock chat room summary
        ChatRoomSummaryDto summary = mock(ChatRoomSummaryDto.class);
        when(chatRoomRepository.findUserChatRoomSummaries(creatorId))
                .thenReturn(List.of(summary));

        // Act: get user chat room summaries
        List<ChatRoomSummaryDto> result = chatRoomService.getUserChatRoomSummaries(creatorId);

        // Assert: should return summaries
        assertEquals(1, result.size());
        verify(chatRoomRepository).findUserChatRoomSummaries(creatorId);
    }

    @Test
    void getUserChatRoomSummaries_NullUserId_ThrowsException() {
        // Act & Assert: null user ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getUserChatRoomSummaries(null));
    }

    @Test
    void getChatRoomDetail_Found() {
        // Arrange: mock chat room detail
        ChatRoomDetailDto detail = mock(ChatRoomDetailDto.class);
        when(chatRoomRepository.findDetailById(chatRoomId)).thenReturn(java.util.Optional.of(detail));

        // Act: get chat room detail
        Optional<ChatRoomDetailDto> result = chatRoomService.getChatRoomDetail(chatRoomId);

        // Assert: should return detail
        assertTrue(result.isPresent());
        assertEquals(detail, result.get());
    }

    @Test
    void getChatRoomDetail_NotFound() {
        // Arrange: detail not found
        when(chatRoomRepository.findDetailById(chatRoomId)).thenReturn(Optional.empty());

        // Act: get non-existent detail
        Optional<ChatRoomDetailDto> result = chatRoomService.getChatRoomDetail(chatRoomId);

        // Assert: should return empty
        assertFalse(result.isPresent());
    }

    @Test
    void getChatRoomDetail_NullId_ThrowsException() {
        // Act & Assert: null ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getChatRoomDetail(null));
    }

    @Test
    void getChatRoomMemberDtos_Success() {
        // Arrange: mock member DTO
        MemberDto memberDto = mock(MemberDto.class);
        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(true);
        when(chatRoomMemberRepository.findMembersByChatRoomId(chatRoomId))
                .thenReturn(List.of(memberDto));

        // Act: get member DTOs
        List<MemberDto> result = chatRoomService.getChatRoomMemberDtos(chatRoomId);

        // Assert: should return member DTOs
        assertEquals(1, result.size());
        verify(chatRoomMemberRepository).findMembersByChatRoomId(chatRoomId);
    }

    @Test
    void getChatRoomMemberDtos_NullId_ThrowsException() {
        // Act & Assert: null ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getChatRoomMemberDtos(null));
    }

    @Test
    void getChatRoomMemberDtos_ChatRoomNotFound_ThrowsResourceNotFound() {
        // Arrange: room does NOT exist
        when(chatRoomRepository.existsById(chatRoomId)).thenReturn(false);

        // Act & Assert: should throw ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.getChatRoomMemberDtos(chatRoomId));

        // Verify repository projection was never called
        verify(chatRoomMemberRepository, never()).findMembersByChatRoomId(any());
    }

    @Test
    void getChatroomsByCreator_Success() {
        // Arrange: mock repository
        when(chatRoomRepository.findByCreatedBy_Id(creatorId)).thenReturn(List.of(chatRoom));

        // Act: get chat rooms by creator
        List<com.securechat.dto.ChatRoomDTO> result = chatRoomService.getChatroomsByCreator(creatorId);

        // Assert: should return creator's chat rooms
        assertEquals(1, result.size());
        verify(chatRoomRepository).findByCreatedBy_Id(creatorId);
    }

    @Test
    void getChatroomsByCreator_NullId_ThrowsException() {
        // Act & Assert: null creator ID should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.getChatroomsByCreator(null));
    }

    @Test
    void getChatroomsByCreator_EmptyResult() {
        // Arrange: creator has no chat rooms
        when(chatRoomRepository.findByCreatedBy_Id(creatorId)).thenReturn(List.of());

        // Act: get chat rooms by creator
        List<com.securechat.dto.ChatRoomDTO> result = chatRoomService.getChatroomsByCreator(creatorId);

        // Assert: should return empty list
        assertTrue(result.isEmpty());
    }

    @Test
    void getPublicChatRooms_Success() {
        // Arrange: mock public chat room summary
        ChatRoomSummaryDto summary = mock(ChatRoomSummaryDto.class);
        when(chatRoomRepository.findPublicChatRoomSummaries()).thenReturn(List.of(summary));

        // Act: get public chat rooms
        List<ChatRoomSummaryDto> result = chatRoomService.getPublicChatRooms();

        // Assert: should return public chat rooms
        assertEquals(1, result.size());
        verify(chatRoomRepository).findPublicChatRoomSummaries();
    }

    @Test
    void getPublicChatRooms_Empty() {
        // Arrange: no public chat rooms
        when(chatRoomRepository.findPublicChatRoomSummaries()).thenReturn(List.of());

        // Act: get public chat rooms
        List<ChatRoomSummaryDto> result = chatRoomService.getPublicChatRooms();

        // Assert: should return empty list
        assertTrue(result.isEmpty());
    }

    // ====================== SAVE CHAT ROOM ======================

    @Test
    void saveChatRoom_Success() {
        // Arrange: mock repository save
        when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

        // Act: save chat room
        ChatRoom result = chatRoomService.saveChatRoom(chatRoom);

        // Assert: should return saved chat room
        assertEquals(chatRoom, result);
        verify(chatRoomRepository).save(chatRoom);
    }

    @Test
    void saveChatRoom_NullChatRoom_ThrowsException() {
        // Act & Assert: null chat room should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.saveChatRoom(null));
    }

    // ====================== UPDATE CHAT ROOM WITH REQUEST DTO ======================

    @Test
    void updateChatRoom_WithRequest_Success() {
        // Arrange: update request
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");
        request.setIsPrivate(true);
        request.setMaxParticipants(200);

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

        // Act: update chat room with request
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, request, creator);

        // Assert: all fields should be updated
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertTrue(result.getIsPrivate());
        assertEquals(200, result.getMaxParticipants());
        verify(chatRoomRepository).findByIdWithCreator(chatRoomId);
        verify(chatRoomRepository).save(chatRoom);
    }

    @Test
    void updateChatRoom_WithRequest_NullId_ThrowsException() {
        // Arrange: null ID
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();

        // Act & Assert: null ID should throw exception
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.updateChatRoom(null, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_NullRequest_ThrowsException() {
        // Act & Assert: null request should throw exception
        assertThrows(ValidationException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, null, creator));
    }

    @Test
    void updateChatRoom_WithRequest_NullUser_ThrowsException() {
        // Arrange: null user
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();

        // Act & Assert: null user should throw exception
        assertThrows(UnauthorizedException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, null));
    }

    @Test
    void updateChatRoom_WithRequest_NotFound_ThrowsException() {
        // Arrange: chat room not found
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.empty());

        // Act & Assert: should throw ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_NotOwner_ThrowsException() {
        // Arrange: non-owner user
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        User nonOwner = new User();
        nonOwner.setId(UUID.randomUUID());

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: non-owner should throw UnauthorizedException
        assertThrows(UnauthorizedException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, nonOwner));
    }

    @Test
    void updateChatRoom_WithRequest_CreatorNull_ThrowsException() {
        // Arrange: chat room has no creator
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        chatRoom.setCreatedBy(null); // No creator

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_NameTooLong_ThrowsException() {
        // Arrange: name too long (101 characters)
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setName("A".repeat(101)); // 101 characters

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw ValidationException
        assertThrows(ValidationException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_DescriptionTooLong_ThrowsException() {
        // Arrange: description too long (501 characters)
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setDescription("A".repeat(501)); // 501 characters

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw ValidationException
        assertThrows(ValidationException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_MaxParticipantsTooLow_ThrowsException() {
        // Arrange: max participants too low (0)
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setMaxParticipants(0);

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw ValidationException
        assertThrows(ValidationException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_MaxParticipantsTooHigh_ThrowsException() {
       
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setMaxParticipants(1001);

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // Act & Assert: should throw ValidationException
        assertThrows(ValidationException.class,
                () -> chatRoomService.updateChatRoom(chatRoomId, request, creator));
    }

    @Test
    void updateChatRoom_WithRequest_PartialUpdate() {
        // Arrange: partial update (only name)
        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setName("Partial Update"); // Only update name

        String originalDescription = chatRoom.getDescription();
        Boolean originalIsPrivate = chatRoom.getIsPrivate();
        Integer originalMaxParticipants = chatRoom.getMaxParticipants();

        when(chatRoomRepository.findByIdWithCreator(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

        // Act: partial update
        ChatRoom result = chatRoomService.updateChatRoom(chatRoomId, request, creator);

        // Assert: only name should change, other fields unchanged
        assertEquals("Partial Update", result.getName()); // Updated
        assertEquals(originalDescription, result.getDescription()); // Unchanged
        assertEquals(originalIsPrivate, result.getIsPrivate()); // Unchanged
        assertEquals(originalMaxParticipants, result.getMaxParticipants()); // Unchanged
    }

    // ====================== PRIVATE METHOD TESTS ======================

    @Test
    void addCreatorAsMember_NullCreator_DoesNothing() {
        // This test covers the path where creator is null in createChatRoom
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("Test");

        when(chatRoomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act: create chat room with null creator
        chatRoomService.createChatRoom(request, null);

        // Assert: should not add any members
        verify(chatRoomMemberRepository, never()).save(any());
    }

    @Test
    void addMemberToChatRoom_UserLookupFailsInCreateNewMember_ThrowsException() {
        // Test: user lookup fails during new member creation
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        // First call returns user (for initial check), second call (in createNewMember) returns empty
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser)).thenReturn(Optional.empty());
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty()); // Not a member
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId))
                .thenReturn(5L); 

        // Act & Assert: should throw RuntimeException
        assertThrows(RuntimeException.class,
                () -> chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId));
    }

    @Test
    void addMemberToChatRoom_RoomAtMaxCapacity_ReturnsEmptyAndDoesNotCreateMember() {
        // Arrange: room at max capacity
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, regularUserId))
                .thenReturn(Optional.empty()); // Not a member
        when(chatRoomMemberRepository.countByChatRoomIdAndIsActiveTrue(chatRoomId))
                .thenReturn((long) chatRoom.getMaxParticipants()); // Exactly at limit

        // Act: try to add member
        Optional<ChatRoomMember> result = chatRoomService.addMemberToChatRoom(chatRoomId, regularUserId);

        // Assert: should return empty Optional (room full)
        assertTrue(result.isEmpty(), "Should return empty Optional when room is full");

        // Verify: should not create/save new member
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }
}