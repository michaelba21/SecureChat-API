package com.securechat.repository;

import com.securechat.dto.MemberDto;
import com.securechat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository // Marks this interface as a Spring Data repository component
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {
    // Extends JpaRepository which provides CRUD operations for ChatRoomMember entities
    
    // Check if a membership exists for a specific user in a specific chat room
    boolean existsByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    // Find a specific membership by chat room ID and user ID (returns Optional for null safety)
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

   
    List<ChatRoomMember> findByChatRoomId(UUID chatRoomId);

    // Find all memberships for a specific user (across all chat rooms)
    List<ChatRoomMember> findByUserId(UUID userId);

    // Count active members in a specific chat room (for participant limit enforcement)
    long countByChatRoomIdAndIsActiveTrue(UUID chatRoomId);

    // Delete methods

    // Delete a specific membership (soft delete via isActive flag or physical delete)
    void deleteByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
    
    // Custom delete query for removing all memberships from a chat room
    @Modifying 
    @Transactional // Ensures the operation runs within a transaction
    @Query("DELETE FROM ChatRoomMember c WHERE c.chatRoom.id = :chatRoomId") // JPQL delete query
    void deleteByChatRoomId(@Param("chatRoomId") UUID chatRoomId); // @Param binds method parameter to query parameter

    // Custom query for active members by chat room ID
    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId AND crm.isActive = true")
    List<ChatRoomMember> findActiveMembersByChatRoomId(@Param("chatRoomId") UUID chatRoomId);

    // Simpler query without fetch join - will initialize lazy relationships in service layer
    @Query("SELECT cm FROM ChatRoomMember cm WHERE cm.user.id = :userId AND cm.isActive = true")
    List<ChatRoomMember> findActiveByUserId(@Param("userId") UUID userId);

    // DTO Projection - Returns MemberDto objects directly (avoids lazy loading issues)
    @Query("""
            SELECT new com.securechat.dto.MemberDto(
                u.id, u.username, cm.role, cm.joinedAt
            )
            FROM ChatRoomMember cm
            JOIN cm.user u
            WHERE cm.chatRoom.id = :chatRoomId AND cm.isActive = true
            """)
    List<MemberDto> findMembersByChatRoomId(@Param("chatRoomId") UUID chatRoomId);
}

