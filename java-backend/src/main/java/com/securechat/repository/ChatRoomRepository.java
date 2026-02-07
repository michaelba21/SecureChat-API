package com.securechat.repository;

import com.securechat.dto.ChatRoomDetailDto;
import com.securechat.dto.ChatRoomSummaryDto;
import com.securechat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository // Marks this interface as a Spring Data repository component
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    // Extends JpaRepository which provides CRUD operations for ChatRoom entities
    

    // Search chat rooms by name (case-insensitive partial match)
    List<ChatRoom> findByNameContainingIgnoreCase(String searchTerm);
    
    // Find all public chat rooms (isPrivate = false)
    List<ChatRoom> findByIsPrivateFalse();
    
    // Simplified query - returns ChatRoom without fetching lazy relationships
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id = :id")
    Optional<ChatRoom> findByIdSimple(@Param("id") UUID id);


    // Uses LEFT JOIN FETCH to eagerly load the createdBy relationship in single query
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.createdBy WHERE c.id = :id")
    Optional<ChatRoom> findByIdWithCreator(@Param("id") UUID id);

    // Fetch all chat rooms with their creators (prevents lazy loading in list operations)
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.createdBy")
    List<ChatRoom> findAllWithCreator();

    // DTO Projections - Returns DTOs directly from database query (no entity lazy loading issues)
    @Query("""
            SELECT new com.securechat.dto.ChatRoomSummaryDto(
                c.id, c.name, c.isPrivate, u.username
            )
            FROM ChatRoom c
            JOIN c.createdBy u
            """)
    List<ChatRoomSummaryDto> findAllSummaries();

    // Find chat room summaries for a specific user (only rooms they are active members of)
    @Query("""
            SELECT new com.securechat.dto.ChatRoomSummaryDto(
                c.id, c.name, c.isPrivate, u.username
            )
            FROM ChatRoom c
            JOIN c.createdBy u
            JOIN ChatRoomMember cm ON cm.chatRoom.id = c.id
            WHERE cm.user.id = :userId AND cm.isActive = true
            """)
    List<ChatRoomSummaryDto> findUserChatRoomSummaries(@Param("userId") UUID userId);

    // Get detailed chat room information by ID (includes all relevant fields)
    @Query("""
            SELECT new com.securechat.dto.ChatRoomDetailDto(
                c.id, c.name, c.description, c.isPrivate,
                u.username, u.id, c.createdAt, c.maxParticipants
            )
            FROM ChatRoom c
            JOIN c.createdBy u
            WHERE c.id = :id
            """)
    Optional<ChatRoomDetailDto> findDetailById(@Param("id") UUID id);

    // Find only public chat room summaries (for discoverability features)
    @Query("""
            SELECT new com.securechat.dto.ChatRoomSummaryDto(
                c.id, c.name, c.isPrivate, u.username
            )
            FROM ChatRoom c
            JOIN c.createdBy u
            WHERE c.isPrivate = false
            """)
    List<ChatRoomSummaryDto> findPublicChatRoomSummaries();

    // Find chatrooms created by a specific user (auto-implemented by Spring Data JPA)
    List<ChatRoom> findByCreatedBy_Id(UUID creatorId);
}

