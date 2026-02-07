
package com.securechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securechat.dto.*;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.entity.Message;
import com.securechat.entity.User;
import com.securechat.exception.ResourceNotFoundException;
import com.securechat.exception.UnauthorizedException;
import com.securechat.exception.ValidationException;
import com.securechat.service.ChatRoomService;
import com.securechat.service.MessageService;
import com.securechat.service.UserService;
import com.securechat.util.AuthUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // MVC test slice
import org.springframework.boot.test.mock.mockito.MockBean; // Mock Spring beans
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc; // MVC testing

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class) // Test only ChatRoomController
@AutoConfigureMockMvc(addFilters = false) // Disable security filters, mock auth manually
@DisplayName("ChatRoomController → Full branch coverage tests") // Test class description
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc; // MVC test client

    @Autowired
    private ObjectMapper objectMapper; // JSON mapper

    @MockBean
    private ChatRoomService chatRoomService; // Mock chat room service

    @MockBean
    private MessageService messageService; // Mock message service

    @MockBean
    private UserService userService; // Not used but kept for completeness

    @MockBean
    private AuthUtil authUtil; // Mock authentication utility

    private final UUID testUserId = UUID.randomUUID(); // Test user ID
    private final UUID otherUserId = UUID.randomUUID(); // Another user ID
    private User testUser; // Test user entity
    private Authentication mockAuth; // Mock authentication

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("tester");

        // Create mock authentication
        mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true); // Always authenticated
        when(mockAuth.getName()).thenReturn(testUserId.toString()); // Return user ID as name

        // Default happy-path authUtil mocks
        when(authUtil.getAuthenticatedUser(any())).thenReturn(testUser); // Return test user
    }

    // ────────────────────────────────────────────────
    //  GET /api/chatrooms - Get all chat rooms
    // ────────────────────────────────────────────────

    @Nested // Group tests for GET /api/chatrooms
    @DisplayName("GET /api/chatrooms")
    class GetAllChatRooms {

        @Test
        void authenticated_returnsSummaries() throws Exception {
            // Arrange: create summary DTO
            var summary = new ChatRoomSummaryDto(UUID.randomUUID(), "Room1", true, "last msg");
            when(chatRoomService.getUserChatRoomSummaries(testUserId)).thenReturn(List.of(summary));

            // Act & Assert: GET request returns room summaries
            mockMvc.perform(get("/api/chatrooms").principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$[0].name").value("Room1")); // Verify room name
        }

        @Test
        void serviceThrowsRuntime_returns500() throws Exception {
            // Arrange: service throws exception
            when(chatRoomService.getUserChatRoomSummaries(testUserId))
                    .thenThrow(new RuntimeException("boom"));

            // Act & Assert: returns internal server error
            mockMvc.perform(get("/api/chatrooms").principal(mockAuth))
                   .andExpect(status().isInternalServerError()); // HTTP 500
        }
    }

    // ────────────────────────────────────────────────
    //  GET /api/chatrooms/{id} - Get specific chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/chatrooms/{id}")
    class GetChatRoomById {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void roomNotFound_throws404_beforeAuthCheck() throws Exception {
            // Arrange: room not found
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.empty());

            // Act & Assert: returns 404 before checking authentication
            mockMvc.perform(get("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void notAuthenticated_throws401() throws Exception {
            // Act & Assert: no authentication principal → 404 (due to earlier check)
            mockMvc.perform(get("/api/chatrooms/{id}", roomId)) // No principal
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void notMember_throws403() throws Exception {
            // Arrange: room exists but user is not member
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.isMember(testUserId, roomId)).thenReturn(false);

            // Act & Assert: returns forbidden
            mockMvc.perform(get("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isForbidden()); // HTTP 403
        }

        @Test
        void memberButDetailMissing_throws404() throws Exception {
            // Arrange: user is member but detail not found
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.isMember(testUserId, roomId)).thenReturn(true);
            when(chatRoomService.getChatRoomDetail(roomId)).thenReturn(Optional.empty());

            // Act & Assert: returns not found
            mockMvc.perform(get("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void success_returnsDetailDto() throws Exception {
            // Arrange: create detail DTO
            var detail = new ChatRoomDetailDto(roomId, "Room", "desc", false, "owner", testUserId, LocalDateTime.now(), 100);
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.isMember(testUserId, roomId)).thenReturn(true);
            when(chatRoomService.getChatRoomDetail(roomId)).thenReturn(Optional.of(detail));

            // Act & Assert: returns room details
            mockMvc.perform(get("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$.name").value("Room")); // Verify room name
        }
    }

    // ────────────────────────────────────────────────
    //  POST /api/chatrooms - Create chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/chatrooms")
    class CreateChatRoom {

        @Test
        void success_returnsCreatedRoom() throws Exception {
            // Arrange: create room entity
            ChatRoom room = new ChatRoom();
            room.setId(UUID.randomUUID());
            room.setName("New");

            // Mock service call
            when(chatRoomService.createChatRoom(any(ChatRoomCreateRequest.class), eq(testUser))).thenReturn(room);

            // Act & Assert: POST creates room
            mockMvc.perform(post("/api/chatrooms")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON) // JSON content
                            .content("{\"name\":\"New\"}")) // Request body
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$.name").value("New")); // Verify created room name
        }

        @Test
        void invalidRequest_throws400() throws Exception {
            // Act & Assert: empty name → validation fails
            mockMvc.perform(post("/api/chatrooms")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}")) // Empty name
                   .andExpect(status().isBadRequest()); // HTTP 400
        }

        @Test
        void serviceThrowsRuntime_returns500() throws Exception {
            // Arrange: service throws exception
            when(chatRoomService.createChatRoom(any(), eq(testUser)))
                    .thenThrow(new RuntimeException());

            // Act & Assert: returns internal server error
            mockMvc.perform(post("/api/chatrooms")
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Boom\"}"))
                   .andExpect(status().isInternalServerError()); // HTTP 500
        }
    }

    // ────────────────────────────────────────────────
    //  PUT /api/chatrooms/{id} - Update chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/chatrooms/{id}")
    class UpdateChatRoom {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void success_returnsUpdatedDto() throws Exception {
            // Arrange: create updated room
            ChatRoom updated = new ChatRoom();
            updated.setId(roomId);
            updated.setName("Updated");

            // Mock service call
            when(chatRoomService.updateChatRoom(eq(roomId), any(ChatRoomUpdateRequest.class), eq(testUser)))
                    .thenReturn(updated);

            // Act & Assert: PUT updates room
            mockMvc.perform(put("/api/chatrooms/{id}", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}")) // Update data
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$.name").value("Updated")); // Verify updated name
        }

        @Test
        void validationException_returns400() throws Exception {
            // Arrange: service throws validation exception
            when(chatRoomService.updateChatRoom(eq(roomId), any(), eq(testUser)))
                    .thenThrow(new ValidationException("bad name"));

            // Act & Assert: returns bad request
            mockMvc.perform(put("/api/chatrooms/{id}", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}")) // Invalid name
                   .andExpect(status().isBadRequest()); // HTTP 400
        }

        @Test
        void notFound_throws404() throws Exception {
            // Arrange: service throws not found exception
            when(chatRoomService.updateChatRoom(eq(roomId), any(), eq(testUser)))
                    .thenThrow(new ResourceNotFoundException("gone"));

            // Act & Assert: returns not found
            mockMvc.perform(put("/api/chatrooms/{id}", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\"}"))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void unauthorized_throws403() throws Exception {
            // Arrange: service throws unauthorized exception
            when(chatRoomService.updateChatRoom(eq(roomId), any(), eq(testUser)))
                    .thenThrow(new UnauthorizedException("not owner"));

            // Act & Assert: returns forbidden
            mockMvc.perform(put("/api/chatrooms/{id}", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\"}"))
                   .andExpect(status().isForbidden()); // HTTP 403
        }

        @Test
        void genericException_returns500() throws Exception {
            // Arrange: service throws generic exception
            when(chatRoomService.updateChatRoom(eq(roomId), any(), eq(testUser)))
                    .thenThrow(new RuntimeException("db"));

            // Act & Assert: returns internal server error
            mockMvc.perform(put("/api/chatrooms/{id}", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\"}"))
                   .andExpect(status().isInternalServerError()); // HTTP 500
        }
    }

    // ────────────────────────────────────────────────
    //  DELETE /api/chatrooms/{id} - Delete chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/chatrooms/{id}")
    class DeleteChatRoom {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void success_returns204() throws Exception {
            // Arrange: service does nothing (success)
            doNothing().when(chatRoomService).deleteChatRoom(eq(roomId), eq(testUser));

            // Act & Assert: DELETE returns no content
            mockMvc.perform(delete("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isNoContent()); // HTTP 204
        }

        @Test
        void notFound_throws404() throws Exception {
            // Arrange: service throws not found exception
            doThrow(new ResourceNotFoundException("gone"))
                    .when(chatRoomService).deleteChatRoom(eq(roomId), eq(testUser));

            // Act & Assert: returns not found
            mockMvc.perform(delete("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void unauthorized_throws403() throws Exception {
            // Arrange: service throws unauthorized exception
            doThrow(new UnauthorizedException("not owner"))
                    .when(chatRoomService).deleteChatRoom(eq(roomId), eq(testUser));

            // Act & Assert: returns forbidden
            mockMvc.perform(delete("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isForbidden()); // HTTP 403
        }

        @Test
        void genericException_returns500() throws Exception {
            // Arrange: service throws generic exception
            doThrow(new RuntimeException("db"))
                    .when(chatRoomService).deleteChatRoom(eq(roomId), eq(testUser));

            // Act & Assert: returns internal server error
            mockMvc.perform(delete("/api/chatrooms/{id}", roomId).principal(mockAuth))
                   .andExpect(status().isInternalServerError()); // HTTP 500
        }
    }

    // ────────────────────────────────────────────────
    //  POST /api/chatrooms/{id}/join - Join chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/chatrooms/{id}/join")
    class JoinChatRoom {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void roomNotFound_throws404() throws Exception {
            // Arrange: room not found
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.empty());

            // Act & Assert: returns not found
            mockMvc.perform(post("/api/chatrooms/{id}/join", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void success_returnsOkMessage() throws Exception {
            // Arrange: room exists, member added successfully
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.addMemberToChatRoom(roomId, testUserId)).thenReturn(Optional.of(new ChatRoomMember()));

            // Act & Assert: returns success message
            mockMvc.perform(post("/api/chatrooms/{id}/join", roomId).principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(content().string("Joined chatroom successfully")); // Success message
        }

        @Test
        void addMemberReturnsEmpty_throws400() throws Exception {
            // Arrange: room exists but member not added (Optional.empty())
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.addMemberToChatRoom(roomId, testUserId)).thenReturn(Optional.empty());

            // Act & Assert: returns bad request
            mockMvc.perform(post("/api/chatrooms/{id}/join", roomId).principal(mockAuth))
                   .andExpect(status().isBadRequest()); // HTTP 400
        }

        @Test
        void addMemberThrowsIllegalArg_throws400() throws Exception {
            // Arrange: service throws IllegalArgumentException
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.addMemberToChatRoom(roomId, testUserId))
                    .thenThrow(new IllegalArgumentException("already member"));

            // Act & Assert: returns bad request
            mockMvc.perform(post("/api/chatrooms/{id}/join", roomId).principal(mockAuth))
                   .andExpect(status().isBadRequest()); // HTTP 400
        }
    }

    // ────────────────────────────────────────────────
    //  POST /api/chatrooms/{id}/leave - Leave chat room
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/chatrooms/{id}/leave")
    class LeaveChatRoom {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void roomNotFound_throws404() throws Exception {
            // Arrange: room not found
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.empty());

            // Act & Assert: returns not found
            mockMvc.perform(post("/api/chatrooms/{id}/leave", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void success_returnsOkMessage() throws Exception {
            // Arrange: room exists, member removed successfully
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            doNothing().when(chatRoomService).removeMemberFromChatRoom(roomId, testUserId);

            // Act & Assert: returns success message
            mockMvc.perform(post("/api/chatrooms/{id}/leave", roomId).principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(content().string("Left chatroom successfully")); // Success message
        }

        @Test
        void removeThrowsIllegalArg_throws400() throws Exception {
            // Arrange: service throws IllegalArgumentException
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            doThrow(new IllegalArgumentException("not member"))
                    .when(chatRoomService).removeMemberFromChatRoom(roomId, testUserId);

            // Act & Assert: returns bad request
            mockMvc.perform(post("/api/chatrooms/{id}/leave", roomId).principal(mockAuth))
                   .andExpect(status().isBadRequest()); // HTTP 400
        }
    }

    // ────────────────────────────────────────────────
    //  GET /api/chatrooms/{id}/members - Get members
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/chatrooms/{id}/members")
    class GetMembers {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void roomNotFound_throws404() throws Exception {
            // Arrange: room not found
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.empty());

            // Act & Assert: returns not found
            mockMvc.perform(get("/api/chatrooms/{id}/members", roomId).principal(mockAuth))
                   .andExpect(status().isNotFound()); // HTTP 404
        }

        @Test
        void notMember_throws403() throws Exception {
            // Arrange: room exists but user not a member
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.isMember(testUserId, roomId)).thenReturn(false);

            // Act & Assert: returns forbidden
            mockMvc.perform(get("/api/chatrooms/{id}/members", roomId).principal(mockAuth))
                   .andExpect(status().isForbidden()); // HTTP 403
        }

        @Test
        void success_returnsList() throws Exception {
            // Arrange: create member DTO
            var member = new MemberDto(otherUserId, "other", "MEMBER", LocalDateTime.now());
            when(chatRoomService.getChatRoomById(roomId)).thenReturn(Optional.of(new ChatRoom()));
            when(chatRoomService.isMember(testUserId, roomId)).thenReturn(true);
            when(chatRoomService.getChatRoomMemberDtos(roomId)).thenReturn(List.of(member));

            // Act & Assert: returns member list
            mockMvc.perform(get("/api/chatrooms/{id}/members", roomId).principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$[0].username").value("other")); // Verify username
        }
    }

    // ────────────────────────────────────────────────
    //  POST /api/chatrooms/{id}/members - Add member
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/chatrooms/{id}/members")
    class AddMember {

        UUID roomId = UUID.randomUUID(); // Test room ID
        UUID newMemberId = UUID.randomUUID(); // New member ID

        @Test
        void success_returnsAddedMember() throws Exception {
            // Arrange: create member DTO
            var memberDto = new MemberDto(newMemberId, "newguy", "MEMBER", LocalDateTime.now());
            when(chatRoomService.addMemberToChatRoom(roomId, newMemberId)).thenReturn(Optional.of(new ChatRoomMember()));
            when(chatRoomService.getChatRoomMemberDtos(roomId)).thenReturn(List.of(memberDto));

            // Act & Assert: POST adds member
            mockMvc.perform(post("/api/chatrooms/{id}/members", roomId)
                            .principal(mockAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"" + newMemberId + "\"}")) // User ID in JSON
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$.username").value("newguy")); // Verify added member
        }

        // Additional test cases could include: not found, unauthorized, duplicate member, etc.
    }

    // ────────────────────────────────────────────────
    //  DELETE /api/chatrooms/{id}/members/{memberId} - Remove member
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/chatrooms/{id}/members/{memberId}")
    class RemoveMember {

        UUID roomId = UUID.randomUUID(); // Test room ID
        UUID memberId = UUID.randomUUID(); // Member ID to remove

        @Test
        void success_returns204() throws Exception {
            // Arrange: service does nothing (success)
            doNothing().when(chatRoomService).removeMemberFromChatRoom(roomId, memberId);

            // Act & Assert: DELETE returns no content
            mockMvc.perform(delete("/api/chatrooms/{id}/members/{memberId}", roomId, memberId)
                            .principal(mockAuth))
                   .andExpect(status().isNoContent()); // HTTP 204
        }

        // Additional test cases could include: not found, unauthorized, self-remove forbidden, etc.
    }

    // ────────────────────────────────────────────────
    //  GET /api/chatrooms/{id}/messages - Get messages
    // ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/chatrooms/{id}/messages")
    class GetMessages {

        UUID roomId = UUID.randomUUID(); // Test room ID

        @Test
        void success_withPagination() throws Exception {
            // Arrange: create test message
            var msg = Message.builder()
                    .id(UUID.randomUUID())
                    .content("hello")
                    .sender(testUser)
                    .username("tester")
                    .timestamp(LocalDateTime.now())
                    .build();
            // Mock service call with pagination
            doReturn(List.of(msg)).when(messageService).getMessages(eq(roomId), any(MessageService.PaginationRequest.class));

            // Act & Assert: GET with pagination parameters
            mockMvc.perform(get("/api/chatrooms/{id}/messages", roomId)
                            .param("page", "1") // Page number
                            .param("size", "20") 
                            .principal(mockAuth))
                   .andExpect(status().isOk()) // HTTP 200
                   .andExpect(jsonPath("$[0].content").value("hello")); // Verify message content
        }

        @Test
        void defaultPagination_uses0_50() throws Exception {
            // Act & Assert: GET without pagination parameters uses defaults
            mockMvc.perform(get("/api/chatrooms/{id}/messages", roomId).principal(mockAuth))
                   .andExpect(status().isOk()); 
            // Could verify default parameters if needed
        }
    }
}