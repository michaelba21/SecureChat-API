

package com.securechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securechat.dto.ChatRoomCreateRequest;
import com.securechat.dto.ChatRoomDTO;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.User;
import com.securechat.service.ChatRoomService;
import com.securechat.service.MessageService;
import com.securechat.service.UserService;
import com.securechat.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // Test slice for MVC only
import org.springframework.boot.test.mock.mockito.MockBean; // Mock Spring beans
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc; // MVC testing framework

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatRoomController.class) // Test only ChatRoomController
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simpler testing
class ChatRoomControllerSliceTest {

    @Autowired
    private MockMvc mockMvc; // MVC test client

    @Autowired
    private ObjectMapper objectMapper; // JSON mapper

    @MockBean
    private ChatRoomService chatRoomService; // Mock service layer

    @MockBean
    private MessageService messageService; // Required by controller (unused in these tests)

    @MockBean
    private UserService userService; // Mock user service

    @MockBean
    private AuthUtil authUtil; // Mock authentication utility

    private final UUID testUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"); // Fixed test user ID
    private User testUser; // Test user entity

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");

        // Stub AuthUtil methods to prevent NullPointerException in controller
        when(authUtil.getCurrentUserId(any())).thenReturn(testUserId); // Return test user ID
        when(authUtil.getAuthenticatedUser(any())).thenReturn(testUser); // Return test user
    }

    // Helper method to create mock authentication
    private Authentication mockAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true); // Mark as authenticated
        when(auth.getName()).thenReturn(testUserId.toString()); // Return user ID as name
        return auth;
    }

    @Test
    void testCreateChatRoom_Success() throws Exception {
        // Arrange: create request with unique name (timestamp ensures uniqueness)
        String roomName = "Test Room " + System.currentTimeMillis();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(roomName);
        request.setDescription("Public test room");
        request.setIsPrivate(false);
        request.setMaxParticipants(50);

        // Create expected entity to be returned by service
        ChatRoom createdRoom = new ChatRoom();
        createdRoom.setId(UUID.randomUUID());
        createdRoom.setName(roomName);
        createdRoom.setDescription("Public test room");
        createdRoom.setIsPrivate(false);
        createdRoom.setMaxParticipants(50);
        createdRoom.setCreatedBy(testUser); // Set creator

        ChatRoomDTO expectedDto = new ChatRoomDTO(); // DTO that controller returns
        expectedDto.setId(createdRoom.getId());
        expectedDto.setName(roomName);
        expectedDto.setIsPrivate(false);
        expectedDto.setMaxParticipants(50);

        // Mock service call
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequest.class), eq(testUser)))
                .thenReturn(createdRoom);

        // Act & Assert: perform POST request and verify response
        mockMvc.perform(post("/api/chatrooms")
                .principal(mockAuthenticated()) // Set authentication principal
                .contentType(MediaType.APPLICATION_JSON) 
                .content(objectMapper.writeValueAsString(request))) // Convert request to JSON
                .andExpect(status().isOk()) // Expect HTTP 200 (controller returns ok(), not created())
                .andExpect(jsonPath("$.name").value(roomName)) 
                .andExpect(jsonPath("$.isPrivate").value(false)) 
                .andExpect(jsonPath("$.maxParticipants").value(50)); // Verify participant limit
    }

    @Test
    void testCreatePrivateChatRoom_Success() throws Exception {
        // Arrange: create private room request
        String roomName = "Private Room " + System.currentTimeMillis();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(roomName);
        request.setDescription("Private chat");
        request.setIsPrivate(true); // Private room
        request.setMaxParticipants(10); // Lower limit for private room

        // Create expected private room entity
        ChatRoom createdRoom = new ChatRoom();
        createdRoom.setId(UUID.randomUUID());
        createdRoom.setName(roomName);
        createdRoom.setDescription("Private chat");
        createdRoom.setIsPrivate(true);
        createdRoom.setMaxParticipants(10);
        createdRoom.setCreatedBy(testUser);

        // Mock service call
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequest.class), eq(testUser)))
                .thenReturn(createdRoom);

        // Act & Assert: verify private room creation
        mockMvc.perform(post("/api/chatrooms")
                .principal(mockAuthenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(roomName))
                .andExpect(jsonPath("$.isPrivate").value(true)) // Verify private flag
                .andExpect(jsonPath("$.maxParticipants").value(10)); // Verify custom limit
    }

    @Test
    void testCreateChatRoom_MissingName_ReturnsBadRequest() throws Exception {
        // Arrange: request without name (invalid)
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setDescription("No name"); // Missing name
        request.setIsPrivate(false);
        request.setMaxParticipants(50);

        // Act & Assert: should return 400 Bad Request
        mockMvc.perform(post("/api/chatrooms")
                .principal(mockAuthenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Expect validation failure
    }

    @Test
    void testCreateChatRoom_EmptyName_ReturnsBadRequest() throws Exception {
        // Arrange: request with whitespace-only name (invalid)
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("   "); // Whitespace only
        request.setDescription("Empty name");
        request.setIsPrivate(false);

        // Act & Assert: should return 400 Bad Request
        mockMvc.perform(post("/api/chatrooms")
                .principal(mockAuthenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Expect validation failure
    }

    @Test
    void testCreateChatRoom_ZeroMaxParticipants_UsesDefault() throws Exception {
        // Arrange: request with zero max participants (should use default)
        String roomName = "Zero Limit Room";

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(roomName);
        request.setMaxParticipants(0); // Invalid: zero participants

        // Create room with default limit (50) applied by service
        ChatRoom createdRoom = new ChatRoom();
        createdRoom.setId(UUID.randomUUID());
        createdRoom.setName(roomName);
        createdRoom.setMaxParticipants(50); // Default applied
        createdRoom.setCreatedBy(testUser);

        // Mock service call
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequest.class), eq(testUser)))
                .thenReturn(createdRoom);

        // Act & Assert: verify default value is used
        mockMvc.perform(post("/api/chatrooms")
                .principal(mockAuthenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipants").value(50)); // Verify default 50
    }
}