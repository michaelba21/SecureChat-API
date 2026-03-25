package com.securechat.repository;

import com.securechat.entity.RefreshToken;
import com.securechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository 
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Find a refresh token by its token string (used during token refresh flow)
    Optional<RefreshToken> findByToken(String token);

    // Find all refresh tokens for a specific user (useful for session management)
    List<RefreshToken> findByUser(User user);

    // Delete all refresh tokens for a specific user (e.g., on logout all devices)
    void deleteAllByUserId(UUID userId);

    // Delete expired refresh tokens (cleanup task for security and database maintenance)
    void deleteByExpiryDateBefore(Instant cutoff);
}