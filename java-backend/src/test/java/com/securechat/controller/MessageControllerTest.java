
package com.securechat.controller;

import com.securechat.dto.MessageDTO;
import com.securechat.dto.MessageListDto;
import com.securechat.entity.Message;
import com.securechat.entity.User;
import com.securechat.exception.UnauthorizedException;
import com.securechat.service.MessageService;
import com.securechat.service.MessageStreamService;
import com.securechat.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito with JUnit 5
@MockitoSettings(strictness = Strictness.LENIENT)  // FIX: Allow lenient stubbing (prevents "UnnecessaryStubbingException")
@DisplayName("MessageController - Complete Test Coverage") // Test class description
class MessageControllerTest {

    @Mock // Mock authentication utility
    private AuthUtil authUtil;

    @Mock // Mock message service
    private MessageService messageService;

    @Mock // Mock message streaming service
    private MessageStreamService messageStreamService;

    @InjectMocks // Inject mocks into MessageController instance
    private MessageController messageController;

    private final UUID testChatRoomId = UUID.randomUUID(); // Test chat room ID
    private final UUID testUserId = UUID.randomUUID(); // Test user ID
    private final UUID testMessageId = UUID.randomUUID(); // Test message ID
    private Authentication auth; // Mock authentication
    private User testUser; // Test user entity

    @BeforeEach // Runs before each test method
    void setUp() {
        auth = mock(Authentication.class); // Create mock authentication
        testUser = new User(); // Create test user
        testUser.setId(testUserId); // Set user ID
        testUser.setUsername("testuser"); // Set username
        
        // Lenient mocking (won't fail if not used in all tests)
        lenient().when(auth.isAuthenticated()).thenReturn(true); // Always authenticated
        lenient().when(auth.getName()).thenReturn(testUserId.toString()); // User ID as name
        lenient().when(authUtil.getCurrentUserId(any(Authentication.class))).thenReturn(testUserId); // Return user ID
    }

    // Helper method to setup security context with authentication
    private void setupAuthenticatedContext() {
        SecurityContext context = mock(SecurityContext.class); // Mock security context
        lenient().when(context.getAuthentication()).thenReturn(auth); // Set authentication
        SecurityContextHolder.setContext(context); // Set in SecurityContextHolder
    }

    // ==============================================
    // SEND MESSAGE TESTS - Complete Coverage
    // Tests for POST /api/chatrooms/{chatRoomId}/messages
    // ==============================================

    @Nested // Group send message tests
    @DisplayName("POST /api/chatrooms/{chatRoomId}/messages")
    class SendMessageTests {

        @Test
        @DisplayName("Should successfully send message and return 201 Created")
        void sendMessage_ValidRequest_ReturnsCreated() {
            // Given: setup test data
            setupAuthenticatedContext(); // Setup security context
            MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
            request.setContent("Hello World"); // Message content

            // Create mock message entity
            Message createdMessage = new Message();
            createdMessage.setId(testMessageId);
            createdMessage.setContent("Hello World");
            createdMessage.setTimestamp(LocalDateTime.now());
            createdMessage.setSender(testUser);
            createdMessage.setUsername("testuser");

            // Create message DTO for response
            MessageListDto listDto = new MessageListDto(
                testMessageId,
                "Hello World",
                "testuser",
                testUserId,
                LocalDateTime.now(),
                "TEXT"
            );

            // Mock service calls
            when(messageService.createMessage(eq(testChatRoomId), eq(testUserId), eq("Hello World")))
                .thenReturn(createdMessage); // Return created message
            when(messageService.getMessageDtoById(testMessageId))
                .thenReturn(Optional.of(listDto)); // Return message DTO

            // When: call controller method
            ResponseEntity<MessageDTO> response = messageController.sendMessage(
                testChatRoomId.toString(), request, auth);

            // Then: verify response
            assertEquals(HttpStatus.CREATED, response.getStatusCode()); // HTTP 201
            assertNotNull(response.getBody()); // Response body not null
            assertEquals("Hello World", response.getBody().getContent()); // Correct content

            // Verify service method was called
            verify(messageService).createMessage(testChatRoomId, testUserId, "Hello World");
            // Verify streaming service published message
            verify(messageStreamService).publish(eq(testChatRoomId), eq("new-message"), any(MessageDTO.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void sendMessage_InvalidUUID_ReturnsBadRequest() {
            // Given: invalid UUID string
            setupAuthenticatedContext();
            MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
            request.setContent("Hello");

            // When: call with invalid UUID
            ResponseEntity<MessageDTO> response = messageController.sendMessage(
                "invalid-uuid", request, auth);

            // Then: verify 400 Bad Request
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()); // HTTP 400
            // Verify service was NOT called
            verify(messageService, never()).createMessage(any(UUID.class), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("Should return 400 for empty content")
        void sendMessage_EmptyContent_ReturnsBadRequest() {
            // Given: whitespace-only content
            setupAuthenticatedContext();
            MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
            request.setContent("   "); // whitespace only

            // When: call with empty content
            ResponseEntity<MessageDTO> response = messageController.sendMessage(
                testChatRoomId.toString(), request, auth);

            // Then: verify 400 Bad Request
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()); // HTTP 400
            // Verify service was NOT called
            verify(messageService, never()).createMessage(any(UUID.class), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void sendMessage_NullAuthentication_ThrowsUnauthorized() {
            // Given: null authentication
            MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
            request.setContent("Hello");

            // When & Then: should throw UnauthorizedException
            assertThrows(UnauthorizedException.class, () ->
                messageController.sendMessage(testChatRoomId.toString(), request, null));
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication name is null")
        void sendMessage_NullAuthenticationName_ThrowsUnauthorized() {
            // Given: authentication with null name
            MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
            request.setContent("Hello");
            
            Authentication nullNameAuth = mock(Authentication.class);
            when(nullNameAuth.getName()).thenReturn(null); // Null name

            // When & Then: should throw UnauthorizedException
            assertThrows(UnauthorizedException.class, () ->
                messageController.sendMessage(testChatRoomId.toString(), request, nullNameAuth));
        }
    }

    // ==============================================
    // GET MESSAGES TESTS - Complete Coverage
    // Tests for GET /api/chatrooms/{chatRoomId}/messages
    // ==============================================

    @Nested // Group get messages tests
    @DisplayName("GET /api/chatrooms/{chatRoomId}/messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return paginated messages successfully")
        void getMessages_ValidRequest_ReturnsPaginatedMessages() {
            // Given: setup test data with pagination
            setupAuthenticatedContext();
            Message message = new Message();
            message.setId(testMessageId);
            message.setContent("Test message");
            message.setTimestamp(LocalDateTime.now());
            message.setSender(testUser);

            // Create paginated result
            Page<Message> messagePage = new PageImpl<>(List.of(message), PageRequest.of(0, 20), 1);
            
            // Mock service calls
            when(messageService.isChatRoomMember(eq(testChatRoomId.toString()), eq(testUserId.toString())))
                .thenReturn(true); // User is member
            when(messageService.getMessagesPaginated(eq(testChatRoomId), any(MessageService.PaginationRequest.class)))
                .thenReturn(messagePage); // Return paginated messages

            // When: call controller method
            ResponseEntity<Page<MessageDTO>> response = messageController.getMessages(
                testChatRoomId.toString(), 0, 20, auth);

            // Then: verify response
            assertEquals(HttpStatus.OK, response.getStatusCode()); // HTTP 200
            assertNotNull(response.getBody()); // Response body not null
            assertEquals(1, response.getBody().getTotalElements()); // One message
        }

        @Test
        @DisplayName("Should return 403 when user is not a member")
        void getMessages_UserNotMember_ReturnsForbidden() {
            // Given: user is not member of chat room
            setupAuthenticatedContext();
            when(messageService.isChatRoomMember(eq(testChatRoomId.toString()), eq(testUserId.toString())))
                .thenReturn(false); // User NOT member

            // When: call controller method
            ResponseEntity<Page<MessageDTO>> response = messageController.getMessages(
                testChatRoomId.toString(), 0, 20, auth);

            // Then: verify 403 Forbidden
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()); // HTTP 403
            // Verify messages service was NOT called
            verify(messageService, never()).getMessagesPaginated(any(), any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void getMessages_NullAuthentication_ThrowsUnauthorized() {
            // When & Then: null authentication should throw exception
            assertThrows(UnauthorizedException.class, () ->
                messageController.getMessages(testChatRoomId.toString(), 0, 20, null));
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication name is null")
        void getMessages_NullAuthenticationName_ThrowsUnauthorized() {
            // Given: authentication with null name
            Authentication nullNameAuth = mock(Authentication.class);
            when(nullNameAuth.getName()).thenReturn(null); // Null name

            // When & Then: should throw UnauthorizedException
            assertThrows(UnauthorizedException.class, () ->
                messageController.getMessages(testChatRoomId.toString(), 0, 20, nullNameAuth));
        }
    }

    // ==============================================
    // POLL MESSAGES TESTS - Complete Coverage
    // Tests for GET /api/chatrooms/{chatRoomId}/messages/poll
    // ==============================================

    @Nested // Group poll messages tests
    @DisplayName("GET /api/chatrooms/{chatRoomId}/messages/poll")
    class PollMessagesTests {

        @Test
        @DisplayName("Should return messages since timestamp")
        void pollMessages_WithTimestamp_ReturnsMessages() {
            // Given: timestamp parameter
            setupAuthenticatedContext();
            String sinceTimestamp = "2024-01-01T10:00:00";
            Message message = new Message();
            message.setId(testMessageId);
            message.setContent("Test poll message");
            message.setTimestamp(LocalDateTime.now());

            // Mock service calls
            when(messageService.isChatRoomMember(eq(testChatRoomId.toString()), eq(testUserId.toString())))
                .thenReturn(true); // User is member
            when(messageService.getMessagesSince(eq(testChatRoomId.toString()), eq(sinceTimestamp)))
                .thenReturn(List.of(message)); // Return messages since timestamp

            // When: call controller method
            ResponseEntity<List<MessageDTO>> response = messageController.pollMessages(
                testChatRoomId.toString(), sinceTimestamp, auth);

            // Then: verify response
            assertEquals(HttpStatus.OK, response.getStatusCode()); // HTTP 200
            assertNotNull(response.getBody()); 
            assertEquals(1, response.getBody().size()); // One message
        }

        @Test
        @DisplayName("Should return 403 when user is not a member")
        void pollMessages_UserNotMember_ReturnsForbidden() {
            // Given: user is not member
            setupAuthenticatedContext();
            when(messageService.isChatRoomMember(eq(testChatRoomId.toString()), eq(testUserId.toString())))
                .thenReturn(false); // User NOT member

            // When: call controller method
            ResponseEntity<List<MessageDTO>> response = messageController.pollMessages(
                testChatRoomId.toString(), "2024-01-01T10:00:00", auth);

            // Then: verify 403 Forbidden
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()); // HTTP 403
            // Verify messages service was NOT called
            verify(messageService, never()).getMessagesSince(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void pollMessages_NullAuthentication_ThrowsUnauthorized() {
            // When & Then: null authentication should throw exception
            assertThrows(UnauthorizedException.class, () ->
                messageController.pollMessages(testChatRoomId.toString(), "2024-01-01T10:00:00", null));
        }
    }

    // ==============================================
    // STREAM MESSAGES TESTS - Complete Coverage
    // Tests for GET /api/chatrooms/{chatRoomId}/stream
    // ==============================================

    @Nested // Group stream messages tests
    @DisplayName("GET /api/chatrooms/{chatRoomId}/stream")
    class StreamMessagesTests {

        @Test
        @DisplayName("Should call message service for stream")
        void streamMessages_CallsMessageService() {
            // Given: mock stream object
            Object expectedStream = new Object(); 
            when(messageService.getStreamForChatRoom(testChatRoomId.toString()))
                .thenReturn(expectedStream); // Return mock stream

            // When: call controller method
            Object result = messageController.streamMessages(testChatRoomId.toString());

            // Then: verify result matches expected stream
            assertEquals(expectedStream, result); // Same object returned
            verify(messageService).getStreamForChatRoom(testChatRoomId.toString()); // Service called
        }
    }

    // ==============================================
    // SEND MESSAGE REQUEST INNER CLASS TESTS
    // Tests for SendMessageRequest inner class
    // ==============================================

    @Test
    @DisplayName("SendMessageRequest getters and setters work correctly")
    void sendMessageRequest_GettersAndSetters_WorkCorrectly() {
        // Given: create request object
        MessageController.SendMessageRequest request = new MessageController.SendMessageRequest();
        String expectedContent = "Test content";

        // When: set and get content
        request.setContent(expectedContent);
        String actualContent = request.getContent();

        // Then: verify getter returns set value
        assertEquals(expectedContent, actualContent);
    }
}