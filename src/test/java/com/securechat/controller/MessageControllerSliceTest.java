

package com.securechat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // MVC test slice
import org.springframework.boot.test.mock.mockito.MockBean; // Mock Spring beans
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // Mock user authentication
import org.springframework.test.web.servlet.MockMvc; // MVC testing

import com.securechat.service.MessageService;
import com.securechat.service.MessageStreamService;
import com.securechat.service.ChatRoomService;
import com.securechat.util.AuthUtil;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; // Security context

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import java.util.Optional;
import java.time.LocalDateTime;
import com.securechat.entity.Message;


@WebMvcTest(controllers = MessageController.class) 
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
@WithMockUser(username = "testuser", roles = { "USER" }) // Mock authenticated user for all tests
class MessageControllerSliceTest {

    @Autowired
    private MockMvc mockMvc; // MVC test client

    @MockBean
    private MessageService messageService; 

    @MockBean
    private MessageStreamService messageStreamService; // Mock message streaming service

    @MockBean
    private ChatRoomService chatRoomService; // Mock chat room service (required for membership checks)

    @MockBean
    private AuthUtil authUtil; 

    private Authentication mockAuth; // Mock authentication object

    @BeforeEach
    void setupAuth() {
        // Mock authentication context for each test
        mockAuth = new TestingAuthenticationToken("testuser", "password", "ROLE_USER"); // Create test authentication
        SecurityContextHolder.getContext().setAuthentication(mockAuth); // Set in security context

        // Mock AuthUtil to return a random UUID for user ID
        UUID userId = UUID.randomUUID(); 
        when(authUtil.getCurrentUserId(any())).thenReturn(userId); // Return random ID for any authentication

        // Mock membership check to prevent 500 errors (assume user is member of any chat room)
        when(chatRoomService.isMember(any(UUID.class), any(UUID.class)))
                .thenReturn(true); 
    }

    @Test
    @WithMockUser // Mock authenticated user
    void testSendMessage_Success() throws Exception {
        // Arrange: create test data
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID(); // Random user ID

        // Override AuthUtil mock to return specific user ID for this test
        when(authUtil.getCurrentUserId(any())).thenReturn(userId);

        // Create mock message entity
        Message mockMessage = Message.builder()
                .id(UUID.randomUUID()) // Random message ID
                .content("Hello") 
                .username("testUser") // Username
                .timestamp(LocalDateTime.now())
                .messageType(Message.MessageType.TEXT) // Text message type
                .build();

        // Mock service method to create message
        when(messageService.createMessage(any(UUID.class), any(UUID.class), any(String.class)))
                .thenReturn(mockMessage); // Return mock message

        // Mock message DTO lookup (returns empty to avoid NPE)
        when(messageService.getMessageDtoById(any(UUID.class)))
                .thenReturn(Optional.empty()); // Empty optional

        // Act & Assert: send POST request to create message
        mockMvc.perform(post("/api/chatrooms/" + chatRoomId + "/messages") // POST endpoint
                .principal(mockAuth) // Set authentication principal
                .contentType(MediaType.APPLICATION_JSON) 
                .content("{\"content\":\"Hello\"}")) 
                .andExpect(status().isCreated()); // Expect HTTP 201 Created
    }

    @Test
    void testSendMessage_Unauthenticated() throws Exception {
        // Arrange: clear security context to simulate unauthenticated user
        SecurityContextHolder.clearContext(); 
        
        UUID chatRoomId = UUID.randomUUID(); // Random chat room ID

        // Act & Assert: attempt to send message without authentication
        mockMvc.perform(post("/api/chatrooms/" + chatRoomId + "/messages") 
                .contentType(MediaType.APPLICATION_JSON) // JSON content type
                .content("{\"content\":\"Hello\"}")) 
                .andExpect(status().isForbidden()); // Expect HTTP 403 Forbidden (Spring Security blocks unauthenticated)
    }
}