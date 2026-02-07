package com.securechat.repository;

import com.securechat.dto.MessageListDto;
import com.securechat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

        // Standard paginated query for chat room messages (newest first, excludes deleted)
        Page<Message> findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(UUID chatRoomId, Pageable pageable);

        // Paginated query for messages after a specific timestamp (for loading recent messages)
        Page<Message> findByChatRoomIdAndTimestampGreaterThanAndIsDeletedFalse(UUID chatRoomId, LocalDateTime since,
                        Pageable pageable);

     
        // Get latest messages for polling when no "since" timestamp provided
        @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false ORDER BY m.timestamp DESC")
        List<Message> findTopByChatRoomIdOrderByTimestampDesc(@Param("chatRoomId") UUID chatRoomId);

        // Get messages after a specific timestamp for polling (newest first)
        @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.timestamp > :since AND m.isDeleted = false ORDER BY m.timestamp DESC")
        List<Message> findByChatRoomIdAndTimestampAfterOrderByTimestampDesc(
                        @Param("chatRoomId") UUID chatRoomId,
                        @Param("since") LocalDateTime since);

        // Get the most recent message timestamp in a chat room
        @Query("SELECT MAX(m.timestamp) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false")
        Optional<LocalDateTime> findLatestTimestampByChatRoomId(@Param("chatRoomId") UUID chatRoomId);

        // Fetch messages with sender eagerly loaded (prevents N+1 queries)
        @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chatRoom.id = :roomId")
        List<Message> findByChatRoomWithSender(@Param("roomId") UUID roomId);

        // Get messages after timestamp in chronological order (oldest first)
        @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.timestamp > :since AND m.isDeleted = false ORDER BY m.timestamp ASC")
        List<Message> findByChatRoomAndTimestampAfter(
                        @Param("chatRoomId") UUID chatRoomId,
                        @Param("since") LocalDateTime since);

        // Check if user is member of chat room (for authorization before message operations)
        @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ChatRoomMember cm WHERE cm.chatRoom.id = :chatRoomId AND cm.user.id = :userId")
        boolean isUserMemberOfChatRoom(@Param("chatRoomId") UUID chatRoomId, @Param("userId") UUID userId);

        // DTO Projection - Returns MessageListDto directly from query (no entity lazy loading)
        // Note: CONCAT('', m.messageType) converts MessageType enum to String in JPQL
        @Query("""
                        SELECT new com.securechat.dto.MessageListDto(
                            m.id, m.content, u.username, u.id, m.timestamp,
                            CONCAT('', m.messageType)
                        )
                        FROM Message m
                        JOIN m.sender u
                        WHERE m.id = :id
                        """)
        Optional<MessageListDto> findMessageDtoById(@Param("id") UUID id);

        // Paginated DTO projection for chat room messages (newest first)
        @Query("""
                        SELECT new com.securechat.dto.MessageListDto(
                            m.id, m.content, u.username, u.id, m.timestamp,
                            CONCAT('', m.messageType)
                        )
                        FROM Message m
                        JOIN m.sender u
                        WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false
                        ORDER BY m.timestamp DESC
                        """)
        Page<MessageListDto> findMessageDtosByChatRoomId(@Param("chatRoomId") UUID chatRoomId, Pageable pageable);
}



