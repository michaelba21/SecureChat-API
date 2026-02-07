package com.securechat.service;

import com.securechat.entity.ChatRoom;
import com.securechat.entity.Message;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.repository.MessageRepository;
import com.securechat.repository.UserRepository;
import com.securechat.util.InputSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService - 100% Coverage Tests")  // Goal: Test all code paths
class MessageServiceTest {
    // This test class validates the MessageService which handles all message-related operations
    // including creation, retrieval, pagination, deletion, and real-time streaming

    @Mock
    private MessageRepository messageRepository;  // Repository for message data

    @Mock
    private UserRepository userRepository;  

    @Mock
    private ChatRoomRepository chatRoomRepository;  // Repository for chat room data

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;  // Repository for membership checks

    @Mock
    private InputSanitizer inputSanitizer;  

    @Mock
    private MessageStreamService messageStreamService;  // Service for real-time streaming

    @InjectMocks
    private MessageService messageService; 

    @Captor
    private ArgumentCaptor<Message> messageCaptor;  // Captures Message entities passed to save()

    // Test constants
    private final String userIdStr = "550e8400-e29b-41d4-a716-446655440000";
    private final String chatRoomIdStr = "660e8400-e29b-41d4-a716-446655440000";
    private final UUID userId = UUID.fromString(userIdStr);
    private final UUID chatRoomId = UUID.fromString(chatRoomIdStr);

    // ====================== createMessage ======================

    @Test
    @DisplayName("createMessage - success path")
    void createMessage_success() {
        // Tests successful message creation with XSS sanitization
        User sender = new User();
        sender.setId(userId);
        sender.setUsername("testuser");

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);

        // Mock dependencies
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(true);
        when(inputSanitizer.sanitize(eq("Hello <script>alert(1)</script>"))).thenReturn("Hello alert(1)");  // XSS removed
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(sender));
        when(chatRoomRepository.findById(eq(chatRoomId))).thenReturn(Optional.of(chatRoom));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        // Execute message creation
        Message message = messageService.createMessage(chatRoomIdStr, userIdStr, "Hello <script>alert(1)</script>");

        // Verify save was called
        verify(messageRepository).save(messageCaptor.capture());

        // Verify saved message properties
        Message saved = messageCaptor.getValue();
        assertEquals("Hello alert(1)", saved.getContent());  // Sanitized content
        assertEquals(sender, saved.getSender());  
        assertEquals("testuser", saved.getUsername());  // Username from sender
        assertEquals(chatRoom, saved.getChatRoom());  
        assertFalse(saved.getIsDeleted());  
        assertNotNull(saved.getTimestamp());  // Auto-generated timestamp
        assertNotNull(saved.getId()); 

        // Return value should not be null
        assertNotNull(message);
    }

    @Test
    @DisplayName("createMessage - user not member throws")
    void createMessage_userNotMember() {
        // Tests authorization: only chat room members can send messages
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.createMessage(chatRoomIdStr, userIdStr, "msg"));

        assertEquals("User is not a member of this chat room", ex.getMessage());
        // No further interactions since membership check failed
        verifyNoInteractions(inputSanitizer, userRepository, chatRoomRepository);
    }

    @Test
    @DisplayName("createMessage - user not found throws")
    void createMessage_userNotFound() {
        // Tests error when sender user doesn't exist
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(true);
        when(userRepository.findById(eq(userId))).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.createMessage(chatRoomIdStr, userIdStr, "msg"));

        
        assertEquals("User not found: " + userIdStr, ex.getMessage());
        // Sanitizer is called only after user/chatroom/membership checks
        verifyNoInteractions(inputSanitizer);
        verifyNoInteractions(chatRoomRepository);
    }

    @Test
    @DisplayName("createMessage - chat room not found throws")
    void createMessage_chatRoomNotFound() {
        // Tests error when chat room doesn't exist
        User sender = new User();
        sender.setId(userId);

        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(true);
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(sender));
        when(chatRoomRepository.findById(eq(chatRoomId))).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.createMessage(chatRoomIdStr, userIdStr, "msg"));

        // FIXED: Now matches actual message with ID
        assertEquals("Chat room not found: " + chatRoomIdStr, ex.getMessage());
    }

    // ====================== isChatRoomMember ======================

    @Test
    @DisplayName("isChatRoomMember - returns true")
    void isChatRoomMember_true() {
        // Tests membership check returning true
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(true);
        assertTrue(messageService.isChatRoomMember(chatRoomIdStr, userIdStr));
    }

    @Test
    @DisplayName("isChatRoomMember - returns false")
    void isChatRoomMember_false() {
        // Tests membership check returning false
        when(chatRoomMemberRepository.existsByChatRoomIdAndUserId(eq(chatRoomId), eq(userId))).thenReturn(false);
        assertFalse(messageService.isChatRoomMember(chatRoomIdStr, userIdStr));
    }

    // ====================== getMessagesSince ======================

    @Test
    @DisplayName("getMessagesSince - returns messages after timestamp")
    void getMessagesSince_success() {
        // Tests retrieving messages sent after a specific timestamp
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setContent("new");
        User u = new User();
        u.setId(userId);
        u.setUsername("test");
        msg.setSender(u);
        ChatRoom cr = new ChatRoom();
        cr.setId(chatRoomId);
        msg.setChatRoom(cr);
        msg.setTimestamp(LocalDateTime.now());

        when(messageRepository.findByChatRoomAndTimestampAfter(eq(chatRoomId), eq(since)))
                .thenReturn(List.of(msg));

        List<Message> result = messageService.getMessagesSince(chatRoomIdStr, since.toString());

        assertEquals(1, result.size());
        Message retrieved = result.get(0);
        assertEquals("new", retrieved.getContent());
        assertEquals(userId, retrieved.getSender().getId());
        assertEquals("test", retrieved.getSender().getUsername());
        assertEquals(chatRoomId, retrieved.getChatRoom().getId());
    }

    @Test
    @DisplayName("getMessagesSince - invalid timestamp throws")
    void getMessagesSince_invalidTimestamp() {
        // Tests error handling for malformed timestamps
        assertThrows(DateTimeParseException.class,
                () -> messageService.getMessagesSince(chatRoomIdStr, "invalid-date"));
    }

    @Test
    @DisplayName("getMessagesSince - empty list")
    void getMessagesSince_empty() {
        // Tests no messages found after timestamp
        LocalDateTime since = LocalDateTime.now();
        when(messageRepository.findByChatRoomAndTimestampAfter(eq(chatRoomId), eq(since)))
                .thenReturn(List.of());

        List<Message> result = messageService.getMessagesSince(chatRoomIdStr, since.toString());
        assertTrue(result.isEmpty());  // Should return empty list
    }

    // ====================== getStreamForChatRoom ======================

    @Test
    @DisplayName("getStreamForChatRoom - delegates to stream service")
    void getStreamForChatRoom() {
        // Tests real-time streaming setup
        SseEmitter stream = mock(SseEmitter.class);
        when(messageStreamService.subscribe(eq(chatRoomId))).thenReturn(stream);

        Object result = messageService.getStreamForChatRoom(chatRoomIdStr);
        assertSame(stream, result);  // Should return stream from stream service
    }

    // ====================== PaginationRequest ======================

    @Nested
    @DisplayName("PaginationRequest inner class")
    class PaginationRequestTests {
        // Tests the inner class used for pagination parameters

        @Test
        void defaultConstructor() {
            MessageService.PaginationRequest req = new MessageService.PaginationRequest();
            assertEquals(0, req.getPage());  
            assertEquals(20, req.getSize());  // Default size = 20
        }

        @Test
        void constructorWithNulls() {
            MessageService.PaginationRequest req = new MessageService.PaginationRequest(null, null);
            assertEquals(0, req.getPage());  // Null page defaults to 0
            assertEquals(20, req.getSize());  
        }

        @Test
        void constructorWithValues() {
            MessageService.PaginationRequest req = new MessageService.PaginationRequest(5, 50);
            assertEquals(5, req.getPage());  // Custom page
            assertEquals(50, req.getSize()); 
        }

        @Test
        void constructorWithPageOnly() {
            MessageService.PaginationRequest req = new MessageService.PaginationRequest(3, null);
            assertEquals(3, req.getPage());  
            assertEquals(20, req.getSize());  // Default size
        }

        @Test
        void constructorWithSizeOnly() {
            MessageService.PaginationRequest req = new MessageService.PaginationRequest(null, 100);
            assertEquals(0, req.getPage());  // Default page
            assertEquals(100, req.getSize());  
        }
    }

    // ====================== getMessages (list) ======================

    @Test
    @DisplayName("getMessages - returns content from page")
    void getMessages_list() {
        // Tests paginated message retrieval returning List
        Page<Message> page = new PageImpl<>(List.of(new Message()));
        when(messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(page);

        List<Message> result = messageService.getMessages(chatRoomId,
                new MessageService.PaginationRequest(1, 30));

        assertEquals(1, result.size());
        // Verify correct pagination parameters used
        verify(messageRepository).findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(eq(chatRoomId),
                eq(PageRequest.of(1, 30, Sort.by("timestamp").descending())));
    }

    // ====================== getMessagesPaginated ======================

    @Test
    @DisplayName("getMessagesPaginated - returns full page")
    void getMessagesPaginated() {
        // Tests paginated message retrieval returning Page object
        Page<Message> page = new PageImpl<>(List.of(new Message()), PageRequest.of(0, 10), 50);
        when(messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(page);

        Page<Message> result = messageService.getMessagesPaginated(chatRoomId,
                new MessageService.PaginationRequest(0, 10));

        assertSame(page, result);  // Should return the same page object
        verify(messageRepository).findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(eq(chatRoomId),
                eq(PageRequest.of(0, 10, Sort.by("timestamp").descending())));
    }

    // ====================== getMessagesSincePaginated ======================

    @Test
    @DisplayName("getMessagesSincePaginated - returns page with since filter")
    void getMessagesSincePaginated() {
        // Tests paginated message retrieval with timestamp filter
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        Page<Message> page = new PageImpl<>(List.of(new Message()));
        when(messageRepository.findByChatRoomIdAndTimestampGreaterThanAndIsDeletedFalse(
                eq(chatRoomId), eq(since), any(Pageable.class)))
                .thenReturn(page);

        Page<Message> result = messageService.getMessagesSincePaginated(chatRoomId, since,
                new MessageService.PaginationRequest(2, 25));

        assertSame(page, result);
        verify(messageRepository).findByChatRoomIdAndTimestampGreaterThanAndIsDeletedFalse(
                eq(chatRoomId), eq(since), eq(PageRequest.of(2, 25, Sort.by("timestamp").descending())));
    }

    // ====================== deleteMessage ======================

    @Nested
    @DisplayName("deleteMessage tests")
    class DeleteMessageTests {
        // Tests message deletion (soft delete)

        @Test
        @DisplayName("deleteMessage - success: soft deletes and saves")
        void deleteMessage_success() {
            // Tests successful soft deletion
            UUID messageId = UUID.randomUUID();
            Message message = new Message();
            message.setId(messageId);
            message.setDeletedBy(null); // initial state

            when(messageRepository.findById(eq(messageId))).thenReturn(Optional.of(message));
            when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

            messageService.deleteMessage(messageId, userId);

            verify(messageRepository).save(message);
            assertTrue(message.getIsDeleted());  // Should be marked as deleted
            assertEquals(userId, message.getDeletedBy());  // Should record who deleted it
        }

        @Test
        @DisplayName("deleteMessage - message not found throws")
        void deleteMessage_notFound() {
            // Tests error when message doesn't exist
            UUID messageId = UUID.randomUUID();

            when(messageRepository.findById(eq(messageId))).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> messageService.deleteMessage(messageId, userId));

            assertEquals("Message not found", ex.getMessage());
            verify(messageRepository, never()).save(any());  // Should not save
        }
    }
}

